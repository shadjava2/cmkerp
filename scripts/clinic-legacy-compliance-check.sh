#!/usr/bin/env bash
# Détecte les risques d'intégration CLINIQUE (DDL / écritures non gated / réplication obligatoire).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0

echo "== CLINIQUE legacy compliance =="

RULE="$ROOT/../.cursor/rules/clinic-legacy-protection.mdc"
RULE2="$ROOT/.cursor/rules/clinic-legacy-protection.mdc"
MANDATORY="$ROOT/../.cursor/rules/erp-mandatory-legacy-replication.mdc"
MANDATORY2="$ROOT/.cursor/rules/erp-mandatory-legacy-replication.mdc"
if [ -f "$RULE" ] || [ -f "$RULE2" ]; then
  echo "OK: règle clinic-legacy-protection présente"
else
  echo "FAIL: règle clinic-legacy-protection manquante"
  FAIL=1
fi
if [ -f "$MANDATORY" ] || [ -f "$MANDATORY2" ]; then
  echo "OK: règle erp-mandatory-legacy-replication présente"
else
  echo "FAIL: règle erp-mandatory-legacy-replication manquante"
  FAIL=1
fi

if [ -f "$ROOT/cmkerp-platform/src/main/resources/db/migration/V30__erp_clinic_contrats_and_sync_outbox.sql" ]; then
  echo "OK: migration V30 erp_contrats / outbox"
else
  echo "FAIL: migration V30 absente"
  FAIL=1
fi

if command -v rg >/dev/null 2>&1; then
  DDL_HITS=$(rg -n "ddl-auto\s*[:=]\s*(update|create)" \
    "$ROOT/cmkerp-platform/src/main/resources" \
    "$ROOT/cmkerp-gateway/src/main/resources" \
    -g '!**/target/**' || true)
  CREATE_HITS=$(rg -n "CREATE TABLE|ALTER TABLE|DROP TABLE|TRUNCATE" \
    "$ROOT/cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/external/clinique" \
    "$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/facturation" \
    -g '!**/test/**' || true)
  WILDCARD=$(rg -n "write-entities\s*[:=].*\*|WRITE_ENTITIES=\*" \
    "$ROOT" -g '!**/target/**' -g '!**/.git/**' || true)
  DELETE_SQL=$(rg -n "DELETE\s+FROM\s+dbo\.|delete\s+from\s+dbo\." \
    "$ROOT/cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/external/clinique" \
    "$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/facturation" \
    -g '!**/test/**' -i || true)
  GUARD_USE=$(rg -n "ClinicLegacyWriteGuard|assertBeachEntityWriteAllowed|assertBeachWriteAllowed" \
    "$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/facturation" \
    -g '!**/test/**' || true)
else
  DDL_HITS=""
  CREATE_HITS=""
  WILDCARD=""
  DELETE_SQL=""
  GUARD_USE=""
fi

if [ -n "${DDL_HITS:-}" ]; then
  echo "FAIL: ddl-auto dangereux détecté"
  echo "$DDL_HITS"
  FAIL=1
else
  echo "OK: pas de ddl-auto update/create sur configs scannées"
fi

if echo "${CREATE_HITS:-}" | grep -qiE 'CREATE TABLE|ALTER TABLE|DROP TABLE|TRUNCATE'; then
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

if [ -n "${WILDCARD:-}" ]; then
  echo "FAIL: allow-list Beach avec wildcard * détectée"
  echo "$WILDCARD"
  FAIL=1
else
  echo "OK: pas de WRITE_ENTITIES=*"
fi

if [ -n "${DELETE_SQL:-}" ]; then
  echo "FAIL: DELETE dbo.* potentiel dans intégration CLINIQUE"
  echo "$DELETE_SQL"
  FAIL=1
else
  echo "OK: pas de DELETE dbo.* dans intégration CLINIQUE"
fi

if [ -z "${GUARD_USE:-}" ]; then
  echo "FAIL: ClinicLegacyWriteGuard non référencé dans facturation gateway"
  FAIL=1
else
  echo "OK: ClinicLegacyWriteGuard utilisé côté facturation"
fi

GUARD="$ROOT/cmkerp-platform/src/main/java/cd/shad/erp/cmk/cmkerp/platform/external/clinique/sync/ClinicLegacyWriteGuard.java"
if [ -f "$GUARD" ]; then
  echo "OK: ClinicLegacyWriteGuard présent"
else
  echo "FAIL: ClinicLegacyWriteGuard manquant ($GUARD)"
  FAIL=1
fi

WORKER="$ROOT/cmkerp-gateway/src/main/java/cd/shad/erp/cmk/cmkerp/gateway/facturation/ClinicBeachOutboxWorker.java"
if [ -f "$WORKER" ] && grep -q "TAYANTDROIT" "$WORKER"; then
  echo "OK: worker Beach couvre TAYANTDROIT"
else
  echo "FAIL: worker Beach sans branche TAYANTDROIT"
  FAIL=1
fi

if [ "$FAIL" -ne 0 ]; then
  echo "CLINIQUE COMPLIANCE: FAILED"
  exit 1
fi
echo "CLINIQUE COMPLIANCE: PASSED"
exit 0
