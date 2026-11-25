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

# 5. Migrations V24/V25
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V24__rbac_hierarchical_standard.sql"
test -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V25__rbac_phase2_full_catalog.sql"
echo "OK: migrations V24/V25 présentes"

# 6. Matrice de couverture
if [ -f "$WS/docs/security/RBAC_COVERAGE_MATRIX.md" ] || [ -f "$ROOT/docs/security/RBAC_COVERAGE_MATRIX.md" ]; then
  echo "OK: matrice de couverture présente"
else
  echo "FAIL: RBAC_COVERAGE_MATRIX.md manquante"
  FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
  echo "RBAC COMPLIANCE: FAILED"
  exit 1
fi
echo "RBAC COMPLIANCE: PASSED"
exit 0
