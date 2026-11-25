# Applique V28 (MODULE_FACTURATION) sur la base primaire du .env local et vérifie ADMIN.
# Usage (depuis cmkerp) :
#   .\scripts\apply-rbac-v28-facturation.ps1
param(
  [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $Root ".env"
$Migration = Join-Path $Root "cmkerp-platform\src\main\resources\db\migration\V28__rbac_module_facturation_and_labels.sql"

if (-not (Test-Path -LiteralPath $EnvFile)) {
  throw "Fichier .env introuvable: $EnvFile"
}
if (-not (Test-Path -LiteralPath $Migration)) {
  throw "Migration V28 introuvable: $Migration"
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

# Extraire host/port/database de l'URL JDBC
$url = $env:CMK_PRIMARY_DB_URL
if ($url -notmatch 'jdbc:mysql://([^:/]+):(\d+)/([^?]+)') {
  throw "CMK_PRIMARY_DB_URL non parsable: $url"
}
$hostName = $Matches[1]
$port = $Matches[2]
$db = $Matches[3]

Write-Host "Base cible: ${hostName}:${port}/$db"
Write-Host "Application de V28..."

Get-Content -LiteralPath $Migration -Raw -Encoding UTF8 |
  & $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch
if ($LASTEXITCODE -ne 0) { throw "Echec application V28 (exit $LASTEXITCODE)" }

$verifySql = @"
INSERT IGNORE INTO roles_permissions (fkRole, fkPermission, datecreate, dateupdate)
SELECT 5, p.id, NOW(), NOW()
FROM permissions p
WHERE p.nom IN ('MODULE_FACTURATION','SIDEBAR_FACTURATION_ACCUEIL')
  AND NOT EXISTS (
    SELECT 1 FROM roles_permissions rp
    WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
  );

SELECT p.id, p.nom, p.description, p.parent,
  CASE WHEN rp.id IS NOT NULL THEN 'AUTORISE' ELSE 'NON AUTORISE' END AS acces_admin
FROM permissions p
LEFT JOIN roles_permissions rp
  ON rp.fkPermission = p.id AND rp.fkRole = 5
WHERE p.nom IN ('MODULE_FACTURATION','SIDEBAR_FACTURATION_ACCUEIL');

SELECT COUNT(*) AS catalog FROM permissions;
SELECT COUNT(*) AS admin_missing
FROM permissions p
WHERE NOT EXISTS (
  SELECT 1 FROM roles_permissions rp
  WHERE rp.fkRole = 5 AND rp.fkPermission = p.id
);
"@

Write-Host "Verification:"
& $MysqlExe --host=$hostName --port=$port --user=$env:CMK_PRIMARY_DB_USER "--password=$env:CMK_PRIMARY_DB_PASSWORD" --database=$db --batch --table -e $verifySql
if ($LASTEXITCODE -ne 0) { throw "Echec verification SQL (exit $LASTEXITCODE)" }

Write-Host ""
Write-Host "OK: catalogue Facturation present. Redemarrez le gateway puis reconnectez-vous ADMIN."
Write-Host "  cd $Root"
Write-Host "  .\stop-gateway.ps1 -Force"
Write-Host "  .\run-gateway.ps1 -KillPort"
