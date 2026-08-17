package cd.shad.erp.cmk.cmkerp.gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Filtre de rate limiting pour protéger l'API contre les abus et les attaques DDoS applicatives.
 *
 * <p>
 * Implémente un rate limiting distribué basé sur Bucket4j avec Redis :
 * <ul>
 * <li>Limitation par IP ou par utilisateur (via token JWT)</li>
 * <li>Configuration différente dev/prod</li>
 * <li>Protection prioritaire des endpoints publics sensibles</li>
 * <li>Rate limiting distribué : quotas partagés entre toutes les instances via Redis</li>
 * </ul>
 *
 * <p>
 * Configuration via application.yml :
 * <ul>
 * <li>cmkerp.rate-limit.enabled : activer/désactiver le rate limiting</li>
 * <li>cmkerp.rate-limit.requests-per-window : nombre de requêtes autorisées</li>
 * <li>cmkerp.rate-limit.window-size-seconds : taille de la fenêtre (secondes)</li>
 * <li>cmkerp.rate-limit.strategy : "IP" ou "USER"</li>
 * </ul>
 *
 * <p>
 * Architecture :
 * <ul>
 * <li>Utilise ProxyManager basé sur Redis pour le stockage distribué des buckets</li>
 * <li>Les buckets sont partagés entre toutes les instances de l'API</li>
 * <li>Scalabilité horizontale : fonctionne avec plusieurs instances JAR derrière un load balancer</li>
 * </ul>
 *

 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Exécuter avant les filtres de sécurité
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${cmkerp.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${cmkerp.rate-limit.requests-per-window:1000}")
    private int requestsPerWindow;

    @Value("${cmkerp.rate-limit.window-size-seconds:60}")
    private int windowSizeSeconds;

    @Value("${cmkerp.rate-limit.strategy:IP}")
    private String strategy;

    @Value("${cmkerp.rate-limit.protected-paths:}")
    private Set<String> protectedPaths;

    @Value("${cmkerp.rate-limit.fallback-on-redis-error:true}")
    private boolean fallbackOnRedisError;

    @Value("${cmkerp.rate-limit.redis-error-threshold:10}")
    private int redisErrorThreshold;

    private final ProxyManager<String> proxyManager;
    private final MeterRegistry meterRegistry;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Circuit breaker simple pour détecter si Redis est down
    private final AtomicBoolean redisAvailable = new AtomicBoolean(true);
    private volatile int consecutiveRedisErrors = 0;

    // Métriques Micrometer (initialisées dans @PostConstruct)
    private Counter rateLimitExceededCounter;
    private Counter rateLimitAllowedCounter;
    private Counter rateLimitRedisErrorCounter;
    private Counter rateLimitFallbackCounter;
    private Timer rateLimitCheckTimer;

    /**
     * Initialise les métriques Micrometer après l'injection des dépendances.
     */
    @jakarta.annotation.PostConstruct
    public void initMetrics() {
        rateLimitExceededCounter = Counter.builder("cmkerp.rate_limit.exceeded")
                .description("Nombre de requêtes bloquées par le rate limiting")
                .tag("strategy", strategy)
                .register(meterRegistry);

        rateLimitAllowedCounter = Counter.builder("cmkerp.rate_limit.allowed")
                .description("Nombre de requêtes autorisées par le rate limiting")
                .tag("strategy", strategy)
                .register(meterRegistry);

        rateLimitRedisErrorCounter = Counter.builder("cmkerp.rate_limit.redis.errors")
                .description("Nombre d'erreurs Redis lors du rate limiting")
                .register(meterRegistry);

        rateLimitFallbackCounter = Counter.builder("cmkerp.rate_limit.fallback")
                .description("Nombre de requêtes autorisées en mode fallback (Redis indisponible)")
                .register(meterRegistry);

        rateLimitCheckTimer = Timer.builder("cmkerp.rate_limit.check.duration")
                .description("Durée de vérification du rate limiting")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = request.getRequestURI();

        // Vérifier si le chemin est protégé
        boolean isProtected = protectedPaths != null && protectedPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));

        // Pour les endpoints non protégés, pas de rate limiting strict
        // (on laisse passer, sauf si c'est une stratégie globale)
        if (!isProtected && !strategy.equals("GLOBAL")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Identifier la clé (IP ou utilisateur)
        String key = getRateLimitKey(request);

        // Vérifier le rate limiting avec gestion d'erreur Redis robuste
        boolean allowed = checkRateLimit(key, requestPath);

        if (allowed) {
            rateLimitAllowedCounter.increment();
            filterChain.doFilter(request, response);
        } else {
            rateLimitExceededCounter.increment();
            log.warn("Rate limit exceeded for key: {} on path: {}", key, requestPath);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    /**
     * Détermine la clé de rate limiting (IP ou utilisateur).
     */
    private String getRateLimitKey(HttpServletRequest request) {
        if ("USER".equals(strategy)) {
            // Extraire le token JWT et utiliser l'ID utilisateur
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Pour simplifier, on utilise le token complet comme clé
                // En production, on pourrait extraire l'user ID du token
                return "USER:" + authHeader.substring(7).hashCode();
            }
        }
        // Par défaut, utiliser l'IP
        return "IP:" + getClientIpAddress(request);
    }

    /**
     * Extrait l'adresse IP réelle du client (gère les proxies).
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Vérifie le rate limiting avec gestion d'erreur Redis robuste et fallback.
     *
     * <p>
     * Stratégie de fallback :
     * <ul>
     * <li>Si Redis est disponible : vérification normale du rate limiting</li>
     * <li>Si Redis est indisponible et fallback activé : autoriser toutes les requêtes</li>
     * <li>Si Redis est indisponible et fallback désactivé : bloquer toutes les requêtes</li>
     * </ul>
     *
     * <p>
     * Circuit breaker : après redisErrorThreshold erreurs consécutives,
     * Redis est considéré comme indisponible jusqu'à la prochaine réussite.
     *
     * @param key la clé de rate limiting (IP ou USER)
     * @param requestPath le chemin de la requête (pour logging)
     * @return true si la requête est autorisée, false sinon
     */
    private boolean checkRateLimit(String key, String requestPath) {
        // Si Redis est considéré comme indisponible, utiliser le fallback
        if (!redisAvailable.get()) {
            if (fallbackOnRedisError) {
                log.debug("Redis unavailable, allowing request in fallback mode for key: {}", key);
                rateLimitFallbackCounter.increment();
                return true; // Fallback : autoriser la requête
            } else {
                log.warn("Redis unavailable and fallback disabled, blocking request for key: {}", key);
                return false; // Pas de fallback : bloquer la requête
            }
        }

        // Tentative de vérification du rate limiting via Redis avec métrique Timer
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Supplier<BucketConfiguration> configurationSupplier = () -> createBucketConfiguration();
            Bucket bucket = proxyManager.builder()
                    .build(key, configurationSupplier);

            boolean allowed = bucket.tryConsume(1);

            // Réinitialiser le compteur d'erreurs en cas de succès
            if (consecutiveRedisErrors > 0) {
                consecutiveRedisErrors = 0;
                if (!redisAvailable.get()) {
                    redisAvailable.set(true);
                    log.info("Redis connection restored, rate limiting back to normal");
                }
            }

            sample.stop(rateLimitCheckTimer);
            return allowed;
        } catch (Exception e) {
            sample.stop(rateLimitCheckTimer);
            // Gestion d'erreur Redis robuste
            handleRedisError(e, key, requestPath);
            // Retourner selon la stratégie de fallback
            return fallbackOnRedisError;
        }
    }

    /**
     * Gère les erreurs Redis avec circuit breaker et métriques.
     *
     * @param exception l'exception Redis
     * @param key la clé de rate limiting
     * @param requestPath le chemin de la requête
     */
    private void handleRedisError(Exception exception, String key, String requestPath) {
        consecutiveRedisErrors++;
        rateLimitRedisErrorCounter.increment();

        // Log selon la criticité
        if (consecutiveRedisErrors <= 3) {
            log.warn("Redis error during rate limiting check (attempt {}/{}): {} - key: {}, path: {}",
                    consecutiveRedisErrors, redisErrorThreshold,
                    exception.getClass().getSimpleName(), key, requestPath);
        } else {
            log.error("Multiple Redis errors during rate limiting ({} consecutive errors). " +
                            "Redis may be unavailable. Exception: {} - key: {}, path: {}",
                    consecutiveRedisErrors, exception.getMessage(), key, requestPath, exception);
        }

        // Activer le circuit breaker si le seuil est atteint
        if (consecutiveRedisErrors >= redisErrorThreshold && redisAvailable.get()) {
            redisAvailable.set(false);
            log.error("Circuit breaker activated: Redis considered unavailable after {} consecutive errors. " +
                            "Fallback mode: {}",
                    consecutiveRedisErrors, fallbackOnRedisError ? "ENABLED (allowing requests)" : "DISABLED (blocking requests)");
        }
    }

    /**
     * Crée la configuration d'un bucket avec les limites configurées.
     *
     * <p>
     * Cette méthode est utilisée par ProxyManager pour créer la configuration
     * d'un bucket lorsqu'il n'existe pas encore dans Redis.
     *
     * <p>
     * Utilise Bandwidth.builder() avec la nouvelle API (Bandwidth.classic() est déprécié).
     * La limite est définie comme : requestsPerWindow requêtes par windowSizeSeconds secondes.
     * Le réapprovisionnement est "greedy" (continu) pour une meilleure précision.
     *
     * @return BucketConfiguration avec les limites de rate limiting
     */
    private BucketConfiguration createBucketConfiguration() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(requestsPerWindow)
                .refillGreedy(requestsPerWindow, Duration.ofSeconds(windowSizeSeconds))
                .build();
        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }
}

