#!/usr/bin/env bash
# Contrôle bloquant RBAC — hardcodes d'autorisation + présence du moteur.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WS="$(cd "$ROOT/.." && pwd)"
FAIL=0

echo "== RBAC compliance check =="

AUTH_PATTERN='SCHADRACK_USER_ID|SCHADRACK_USERNAME|SCHADRACK_ONLY|equalsIgnoreCase\("schadrack"\)|"schadrack"\.equals|username\.toLowerCase\(\) === .schadrack.|userId === 1 \|\|'
ROLE_PATTERN='role\.equals\(|username\.equals\("admin"\)|Long\.valueOf\(1\)\.equals\(user\.getId'

# 1. Hardcodes d'autorisation (pas les signatures / mocks nominatifs UI)
if command -v rg >/dev/null 2>&1; then
  HITS=$(rg -n -i "$AUTH_PATTERN" \
    "$WS/cmkerp" "$WS/consolenotification/src" "$WS/cmkerp-frontend/src" \
    -g '!**/node_modules/**' -g '!**/target/**' -g '!**/dist/**' \
    -g '!**/scripts/**' -g '!**/docs/**' -g '!**/dossierPatientCareMock.ts' \
    || true)
else
  HITS=$(grep -RInE "$AUTH_PATTERN" \
    "$WS/cmkerp" "$WS/consolenotification/src" "$WS/cmkerp-frontend/src" 2>/dev/null \
    | grep -Ev '/(node_modules|target|dist|scripts|docs)/' \
    | grep -Ev 'dossierPatientCareMock|MPAKA MABI SCHADRACK' \
    || true)
fi

if [ -n "$HITS" ]; then
  echo "FAIL: hardcode d'autorisation détecté:"
  echo "$HITS"
  FAIL=1
else
  echo "OK: aucun hardcode d'autorisation nominatif"
fi

# 2. Patterns d'attribution métier hardcodés
if command -v rg >/dev/null 2>&1; then
  ROLE_HITS=$(rg -n "$ROLE_PATTERN" \
    "$WS/cmkerp" "$WS/consolenotification/src" \
    -g '!**/node_modules/**' -g '!**/target/**' -g '!**/test/**' -g '!**/*Test.java' -g '!**/docs/**' \
    || true)
else
  ROLE_HITS=$(grep -RInE "$ROLE_PATTERN" \
    "$WS/cmkerp" "$WS/consolenotification/src" 2>/dev/null \
    | grep -Ev '/(node_modules|target|test|docs)/' \
    | grep -Ev 'Test\.java' \
    || true)
fi

if [ -n "$ROLE_HITS" ]; then
  echo "FAIL: attribution/contrôle rôle ou user hardcodé:"
  echo "$ROLE_HITS"
  FAIL=1
else
  echo "OK: pas de role.equals / Long.valueOf(1) métier"
fi

# 3. Présence du catalogue API + moteur
test -f "$ROOT/cmkerp-shared-kernel/src/main/java/cd/shad/erp/cmk/cmkerp/sharedkernel/security/ApiPermissionCatalog.java"
test -f "$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/security/RbacEnforcementFilter.java"
echo "OK: catalogue API + filtre RBAC présents"

# 4. Règles Cursor
test -f "$WS/.cursor/rules/rbac-security.mdc"
test -f "$WS/AGENTS.md"
echo "OK: règles permanentes présentes"

# 5. Migrations V24–V27 + seed autonome
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V24__rbac_hierarchical_standard.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V25__rbac_phase2_full_catalog.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V26__rbac_admin_console_permissions.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V27__rbac_functional_catalog_exhaustive.sql"
test -f "$ROOT/scripts/rbac-seed-full-catalog.sql"
# Contrôle statique : permissions fonctionnelles déclarées dans le seed
grep -q "DMI_CABINET_PRISE_EN_CHARGE_CREATE" "$ROOT/scripts/rbac-seed-full-catalog.sql"
grep -q "DMI_ACCUEIL_NOUVEAU_DOSSIER" "$ROOT/scripts/rbac-seed-full-catalog.sql"
grep -q "STOCK_PRODUIT_CREATE" "$ROOT/scripts/rbac-seed-full-catalog.sql"
grep -q "MODULE_FACTURATION" "$ROOT/scripts/rbac-seed-full-catalog.sql"
grep -q "SIDEBAR_FACTURATION_MEDICAMENTS" "$ROOT/scripts/rbac-seed-full-catalog.sql"
grep -q "FACTURATION_DASHBOARD_KPI_MEDICAMENTS_VIEW" "$ROOT/scripts/rbac-seed-full-catalog.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V28__rbac_module_facturation_and_labels.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V29__rbac_facturation_v2_sidebar.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V30__erp_clinic_contrats_and_sync_outbox.sql"
grep -q "FACTURATION_CONTRATS_VIEW" "$ROOT/scripts/rbac-seed-full-catalog.sql"
echo "OK: migrations V24–V30 + seed fonctionnel présentes"

# 6. Matrice de couverture
if [ -f "$WS/docs/security/RBAC_COVERAGE_MATRIX.md" ] || [ -f "$ROOT/docs/security/RBAC_COVERAGE_MATRIX.md" ]; then
  echo "OK: matrice de couverture présente"
else
  echo "FAIL: RBAC_COVERAGE_MATRIX.md manquante"
  FAIL=1
fi

# 7. Couverture obligatoire ApiPermissionCatalog pour les contrôleurs métier /api/v1
# Un endpoint implémenté sans règle catalogue = FAIL (DENY BY DEFAULT doit rester actif).
CATALOG="$ROOT/cmkerp-shared-kernel/src/main/java/cd/shad/erp/cmk/cmkerp/sharedkernel/security/ApiPermissionCatalog.java"
REQUIRED_CATALOG_PATHS=(
  "/api/v1/facturation/contrats"
  "/api/v1/facturation/clinic-sync"
  "/api/v1/stocks/facturation-sync"
)
for p in "${REQUIRED_CATALOG_PATHS[@]}"; do
  if ! grep -q "\"$p\"" "$CATALOG"; then
    echo "FAIL: chemin API absent du ApiPermissionCatalog: $p"
    FAIL=1
  fi
done

# Extraire les @RequestMapping("/api/v1/...") de classe (hors tests) et vérifier une règle catalogue
MAPPINGS_FILE=$(mktemp)
if command -v rg >/dev/null 2>&1; then
  rg -n --no-heading '@RequestMapping\("/api/v1/[^"]+"' \
    "$ROOT" \
    -g '**/*Controller.java' -g '**/*RestController.java' \
    -g '!**/test/**' -g '!**/*Test.java' -g '!**/target/**' \
    | sed -E 's/.*"(\/api\/v1\/[^"]+)".*/\1/' \
    | sort -u > "$MAPPINGS_FILE" || true
else
  grep -RInE '@RequestMapping\("/api/v1/[^"]+"' "$ROOT" --include='*Controller.java' --include='*RestController.java' 2>/dev/null \
    | grep -Ev '/(test|target)/' \
    | sed -E 's/.*"(\/api\/v1\/[^"]+)".*/\1/' \
    | sort -u > "$MAPPINGS_FILE" || true
fi

UNCOVERED=""
while IFS= read -r base; do
  [ -z "$base" ] && continue
  # Auth / health / portails publics exclus
  case "$base" in
    /api/v1/auth|/api/v1/auth/*|/api/v1/health|/api/v1/portail-fournisseur*) continue ;;
  esac
  # Couvert si :
  # - règle exacte sur ce mapping de classe
  # - ou au moins une règle enfant ("$base/...")
  # - ou une règle parent plus courte déjà cataloguée
  if grep -Fq "\"$base\"" "$CATALOG" || grep -Fq "\"$base/" "$CATALOG"; then
    continue
  fi
  covered=0
  prefix="$base"
  while [ "$prefix" != "/api/v1" ] && [ "$prefix" != "/" ]; do
    prefix="${prefix%/*}"
    if [ -n "$prefix" ] && grep -Fq "\"$prefix\"" "$CATALOG"; then
      covered=1
      break
    fi
  done
  if [ "$covered" -eq 0 ]; then
    UNCOVERED="${UNCOVERED}${base}"$'\n'
  fi
done < "$MAPPINGS_FILE"
rm -f "$MAPPINGS_FILE"

if [ -n "$UNCOVERED" ]; then
  echo "FAIL: contrôleurs /api/v1 sans règle ApiPermissionCatalog (DENY BY DEFAULT):"
  echo "$UNCOVERED"
  FAIL=1
else
  echo "OK: contrôleurs /api/v1 couverts par ApiPermissionCatalog (préfixe ou parent)"
fi

# 8. Permissions facturation présentes dans le seed
for code in FACTURATION_CONTRATS_VIEW FACTURATION_CONTRATS_IMPORT FACTURATION_SYNC_VIEW FACTURATION_SYNC_DRY_RUN; do
  if ! grep -q "$code" "$ROOT/scripts/rbac-seed-full-catalog.sql"; then
    echo "FAIL: permission absente du seed: $code"
    FAIL=1
  fi
done
echo "OK: permissions facturation contrats/sync présentes dans le seed"

if [ "$FAIL" -ne 0 ]; then
  echo "RBAC COMPLIANCE: FAILED"
  exit 1
fi
echo "RBAC COMPLIANCE: PASSED"
exit 0
