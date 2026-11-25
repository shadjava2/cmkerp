package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.ApiPermissionCatalog;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filtre d'enforcement RBAC centralisé — source de vérité live (pas JWT seul).
 *
 * <p>Ordre : après authentification JWT. Refuse par défaut les endpoints métier
 * non catalogués.
 */
@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class RbacEnforcementFilter extends OncePerRequestFilter {

  private final PermissionAuthorizationPort permissionAuthorization;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return ApiPermissionCatalog.isPublicPath(path)
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getRequestURI();
    String method = request.getMethod();

    if (ApiPermissionCatalog.isAuthenticatedOnly(path, method)) {
      filterChain.doFilter(request, response);
      return;
    }

    Long userId = resolveUserId(request);
    if (userId == null) {
      writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
          "Authentification requise.");
      return;
    }

    Optional<ApiPermissionCatalog.Rule> matched = ApiPermissionCatalog.match(method, path);
    if (matched.isEmpty()) {
      if (path.contains("/api/v1/")) {
        log.warn("RBAC GAP: endpoint métier sans règle path={} method={} userId={}",
            path, method, userId);
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, "PERMISSION_DENIED",
            "Endpoint non couvert par le catalogue RBAC - acces refuse par defaut.");
        return;
      }
      filterChain.doFilter(request, response);
      return;
    }

    String[] codes = matched.get().anyOfPermissions();
    boolean allowed = permissionAuthorization.hasAnyPermission(userId, Arrays.asList(codes));
    if (!allowed) {
      log.info("RBAC DENIED userId={} {} {} requiredAny={}", userId, method, path,
          Arrays.toString(codes));
      writeJson(response, HttpServletResponse.SC_FORBIDDEN, "PERMISSION_DENIED",
          "Vos autorisations ont été mises à jour. Vous ne disposez plus des droits nécessaires pour effectuer cette opération.");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Long resolveUserId(HttpServletRequest request) {
    String token = extractToken(request);
    if (token != null && jwtTokenProvider.validateToken(token)) {
      try {
        return jwtTokenProvider.getUserIdFromToken(token);
      } catch (Exception ex) {
        log.debug("RBAC: userId JWT indisponible - {}", ex.getMessage());
      }
    }
    return null;
  }

  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }
    if (request.getCookies() != null) {
      for (var cookie : request.getCookies()) {
        if ("cmk-auth-token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  private static void writeJson(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
    response.getWriter().write(
        "{\"status\":" + status
            + ",\"code\":\"" + code + "\""
            + ",\"error\":\"" + code + "\""
            + ",\"message\":\"" + message.replace("\"", "\\\"") + "\"}");
  }
}
