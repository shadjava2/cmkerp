package cd.shad.erp.cmk.cmkerp.gateway.security;

import java.util.Arrays;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.PermissionAuthorizationPort;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect déclaratif : {@code @RequirePermission} → vérification live via le moteur RBAC.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequirePermissionAspect {

  private final PermissionAuthorizationPort permissionAuthorization;
  private final JwtTokenProvider jwtTokenProvider;
  private final HttpServletRequest request;

  @Around("@annotation(requirePermission)")
  public Object enforce(ProceedingJoinPoint joinPoint, RequirePermission requirePermission)
      throws Throwable {
    Long userId = resolveUserId();
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
          "Authentification requise.");
    }

    List<String> codes = Arrays.stream(requirePermission.value())
        .filter(c -> c != null && !c.isBlank())
        .toList();

    if (codes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Permission inconnue : accès refusé.");
    }

    boolean allowed = requirePermission.requireAll()
        ? permissionAuthorization.hasAllPermissions(userId, codes)
        : permissionAuthorization.hasAnyPermission(userId, codes);

    if (!allowed) {
      log.info("RBAC DENIED userId={} method={} codes={}", userId,
          joinPoint.getSignature().toShortString(), codes);
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Vous ne disposez pas des autorisations nécessaires pour effectuer cette opération.");
    }

    return joinPoint.proceed();
  }

  private Long resolveUserId() {
    String token = extractToken();
    if (token != null && jwtTokenProvider.validateToken(token)) {
      try {
        return jwtTokenProvider.getUserIdFromToken(token);
      } catch (Exception ignored) {
        // fallback Authentication
      }
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return null;
    }
    return null;
  }

  private String extractToken() {
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
}
