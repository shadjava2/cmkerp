# Applique V30 (erp_contrats + outbox + RBAC Contrats/Sync) sur MySQL primaire .env
# Usage : .\scripts\apply-rbac-v30-clinic-contrats.ps1
param(
  [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"
$Migration = Join-Path $Root "cmkerp-platform\src\main\resources\db\migration\V30__erp_clinic_contrats_and_sync_outbox.sql"

if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Fichier .env introuvable: $EnvFile" }
if (-not (Test-Path -LiteralPath $Migration)) { throw "Migration V30 introuvable: $Migration" }
if (-not (Test-Path -LiteralPath $MysqlExe)) { throw "mysql.exe introuvable: $MysqlExe" }

Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
  $entry = $_.Trim()
  if ($entry.Length -eq 0 -or $entry.StartsWith("#")) { return }
  if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { throw "Ligne .env invalide: $entry" }
  $name = $Matches[1]
  $value = $Matches[2].Trim()
  if ($value.Length -ge 2 -and (
      ($value.StartsWith('"') -and $value.EndsWith('"')) -or
      ($value.StartsWith("'") -and $value.EndsWith("'")))) {
    $value = $value.Substring(1, $value.Length - 2)
  }
  Set-Item -Path "Env:$name" -Value $value
}

if (-not $env:CMK_PRIMARY_DB_USER -or -not $env:CMK_PRIMARY_DB_PASSWORD) {
  throw "CMK_PRIMARY_DB_USER / CMK_PRIMARY_DB_PASSWORD absents du .env"
}

$url = $env:CMK_PRIMARY_DB_URL
if ($url -notmatch 'jdbc:mysql://([^:/]+):(\d+)/([^?]+)') { throw "CMK_PRIMARY_DB_URL non parsable: $url" }
$hostName = $Matches[1]; $port = $Matches[2]; $db = $Matches[3]

Write-Host "Base cible: ${hostName}:${port}/$db"
Write-Host "Application de V30..."

Get-Content -LiteralPath $Migration -Raw -Encoding UTF8 |
  & $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch
if ($LASTEXITCODE -ne 0) { throw "Echec application V30 (exit $LASTEXITCODE)" }

$verifySql = @"
INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
SELECT 5, p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom LIKE 'FACTURATION_CONTRATS_%'
   OR p.nom LIKE 'FACTURATION_SYNC_%'
   OR p.nom IN ('SIDEBAR_FACTURATION_SYNCHRONISATION','SIDEBAR_FACTURATION_LOCATIONS','SIDEBAR_FACTURATION_CONTRATS');

SHOW TABLES LIKE 'erp_contrats';
SHOW TABLES LIKE 'erp_sync_outbox';
SELECT nom FROM permissions WHERE nom LIKE 'FACTURATION_CONTRATS_%' OR nom LIKE 'FACTURATION_SYNC_%' ORDER BY nom;
"@

Write-Host "Verification:"
& $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch --table -e $verifySql
if ($LASTEXITCODE -ne 0) { throw "Echec verification SQL (exit $LASTEXITCODE)" }

Write-Host "OK: V30 applique. Redemarrez le gateway puis reconnectez ADMIN."
