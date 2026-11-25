# Applique V29 (Facturation V2 SIDEBAR_* + KPI dashboard) sur la base primaire du .env local.
# Usage (depuis cmkerp) :
#   .\scripts\apply-rbac-v29-facturation.ps1
param(
  [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"
$Migration = Join-Path $Root "cmkerp-platform\src\main\resources\db\migration\V29__rbac_facturation_v2_sidebar.sql"

if (-not (Test-Path -LiteralPath $EnvFile)) {
  throw "Fichier .env introuvable: $EnvFile"
}
if (-not (Test-Path -LiteralPath $Migration)) {
  throw "Migration V29 introuvable: $Migration"
}
if (-not (Test-Path -LiteralPath $MysqlExe)) {
  throw "mysql.exe introuvable: $MysqlExe"
}

Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
  $entry = $_.Trim()
  if ($entry.Length -eq 0 -or $entry.StartsWith("#")) { return }
  if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
    throw "Ligne .env invalide: $entry"
  }
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
if ($url -notmatch 'jdbc:mysql://([^:/]+):(\d+)/([^?]+)') {
  throw "CMK_PRIMARY_DB_URL non parsable: $url"
}
$hostName = $Matches[1]
$port = $Matches[2]
$db = $Matches[3]

Write-Host "Base cible: ${hostName}:${port}/$db"
Write-Host "Application de V29..."

Get-Content -LiteralPath $Migration -Raw -Encoding UTF8 |
  & $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch
if ($LASTEXITCODE -ne 0) { throw "Echec application V29 (exit $LASTEXITCODE)" }

$verifySql = @"
INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
SELECT 5, p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom LIKE 'SIDEBAR_FACTURATION_%'
   OR p.nom LIKE 'FACTURATION_DASHBOARD_%'
   OR p.nom = 'MODULE_FACTURATION';

SELECT p.nom, p.description,
  CASE WHEN rp.id IS NOT NULL THEN 'AUTORISE' ELSE 'NON AUTORISE' END AS acces_admin
FROM permissions p
LEFT JOIN roles_permissions rp
  ON rp.fkPermission = p.id AND rp.fkRole = 5
WHERE p.nom LIKE 'SIDEBAR_FACTURATION_%'
   OR p.nom LIKE 'FACTURATION_DASHBOARD_%'
   OR p.nom = 'MODULE_FACTURATION'
ORDER BY p.nom;
"@

Write-Host "Verification:"
& $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch --table -e $verifySql
if ($LASTEXITCODE -ne 0) { throw "Echec verification SQL (exit $LASTEXITCODE)" }

Write-Host ""
Write-Host "OK: Facturation V2 RBAC present. Redemarrez le gateway puis reconnectez-vous ADMIN."
