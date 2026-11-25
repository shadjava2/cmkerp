package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige une ou plusieurs permissions effectives (résolues depuis la DB / cache).
 *
 * <p>Les permissions JWT ne constituent pas une autorité définitive : le moteur
 * RBAC vérifie les droits effectifs au moment de l'appel.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

  /** Codes de permission (colonne {@code permissions.nom}). */
  String[] value();

  /** Si true, toutes les permissions sont requises ; sinon au moins une. */
  boolean requireAll() default false;
}
