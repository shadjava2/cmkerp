#!/usr/bin/env bash
# Détecte les risques d'intégration CLINIQUE (DDL / écritures non gated).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0

echo "== CLINIQUE legacy compliance =="

RULE="$ROOT/../.cursor/rules/clinic-legacy-protection.mdc"
RULE2="$ROOT/.cursor/rules/clinic-legacy-protection.mdc"
if [ -f "$RULE" ] || [ -f "$RULE2" ]; then
  echo "OK: règle clinic-legacy-protection présente"
else
  echo "FAIL: règle clinic-legacy-protection manquante"
  FAIL=1
fi

if [ -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V30__erp_clinic_contrats_and_sync_outbox.sql" ]; then
  echo "OK: migration V30 erp_contrats / outbox"
else
  echo "FAIL: migration V30 absente"
  FAIL=1
fi

# Interdit : Flyway/Liquibase sur package clinique SQL Server schema
if command -v rg >/dev/null 2>&1; then
  DDL_HITS=$(rg -n "ddl-auto\s*[:=]\s*(update|create)" \
    "$ROOT/cmkerp-platform/src/main/resources" \
    "$ROOT/cmkerp-gateway/src/main/resources" \
    -g '!**/target/**' || true)
  # CREATE TABLE dans code Java ciblant clinique (heuristique)
  CREATE_HITS=$(rg -n "CREATE TABLE|ALTER TABLE|DROP TABLE|TRUNCATE" \
    "$ROOT/cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/external/clinique" \
    "$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/facturation" \
    -g '!**/test/**' || true)
else
  DDL_HITS=""
  CREATE_HITS=""
fi

if [ -n "${DDL_HITS:-}" ]; then
  echo "FAIL: ddl-auto dangereux détecté"
  echo "$DDL_HITS"
  FAIL=1
else
  echo "OK: pas de ddl-auto update/create sur configs scannées"
fi

if echo "${CREATE_HITS:-}" | grep -qiE 'CREATE TABLE|ALTER TABLE|DROP TABLE|TRUNCATE'; then
  # Autoriser commentaires / docs strings uniquement si vraiment du SQL exécuté — heuristique stricte
  if echo "$CREATE_HITS" | grep -vE '^\s*//|^\s*\*|migration' | grep -qiE 'CREATE TABLE|ALTER TABLE|DROP TABLE|TRUNCATE'; then
    echo "FAIL: DDL potentiel dans composants CLINIQUE/facturation"
    echo "$CREATE_HITS"
    FAIL=1
  else
    echo "OK: pas de DDL exécutable détecté dans intégration CLINIQUE"
  fi
else
  echo "OK: pas de DDL détecté dans intégration CLINIQUE"
fi

GUARD="$ROOT/cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/external/clinique/sync/ClinicLegacyWriteGuard.java"
if [ -f "$GUARD" ]; then
  echo "OK: ClinicLegacyWriteGuard présent"
else
  echo "FAIL: ClinicLegacyWriteGuard manquant ($GUARD)"
  FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
  echo "CLINIQUE COMPLIANCE: FAILED"
  exit 1
fi
echo "CLINIQUE COMPLIANCE: PASSED"
exit 0
