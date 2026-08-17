# Recompile tout le workspace Maven puis rafraichit Eclipse/STS.
# Apres execution : dans STS -> clic droit sur cmkerp -> Maven -> Update Project (Force Update).
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
if (-not (Test-Path $env:JAVA_HOME)) {
  Write-Error "Java 21 introuvable: $env:JAVA_HOME"
}

Write-Host "Maven clean install (cmkerp)..."
Push-Location $root
mvn clean install -DskipTests -q
if ($LASTEXITCODE -ne 0) {
  Pop-Location
  Write-Error "Echec Maven. Corrigez les erreurs affichees ci-dessus."
}
Pop-Location

Write-Host ""
Write-Host "OK - JAR gateway: $root\cmkerp-gateway\target\cmkerp-gateway-4.1.1.jar"
Write-Host "Dans STS : Maven > Update Project... (cocher tous les modules cmkerp-*)"
Write-Host "Puis Run GatewayApplication ou: .\run-gateway.ps1 -SkipBuild"
