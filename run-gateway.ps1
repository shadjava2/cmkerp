# Demarre cmkerp-gateway avec Java 21 (recompile stocks + platform + gateway)
param(
  [switch]$Background,
  [switch]$SkipBuild,
  [switch]$KillPort,
  [string]$Profile = $(if ($env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE } else { "dev" })
)

$ErrorActionPreference = "Stop"
$Java21 = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\bin\java.exe"
$Jar = Join-Path $PSScriptRoot "cmkerp-gateway\target\cmkerp-gateway-4.1.1.jar"
$OutLog = Join-Path $PSScriptRoot "gateway-out.log"
$ErrLog = Join-Path $PSScriptRoot "gateway-err.log"

if (-not (Test-Path $Java21)) {
  Write-Error "Java 21 introuvable: $Java21"
}

if ($KillPort) {
  Write-Host "Liberation du port 8999..."
  & (Join-Path $PSScriptRoot "stop-gateway.ps1") -Force -Quiet
}

if (-not $SkipBuild) {
  Write-Host "Compilation Maven (stocks + platform + gateway)..."
  $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
  Push-Location $PSScriptRoot
  mvn -pl cmkerp-gateway -am install -DskipTests -q
  if ($LASTEXITCODE -ne 0) {
    Pop-Location
    Write-Error "Echec Maven. Corrigez les erreurs puis relancez."
  }
  Pop-Location
}

if (-not (Test-Path $Jar)) {
  Write-Error "JAR introuvable apres build: $Jar"
}

# Module stock-intelligence actif par défaut au démarrage local
$env:CMK_STOCK_INTELLIGENCE_ENABLED = "true"
$env:CMK_STOCK_INTELLIGENCE_MORNING = "true"
$env:CMK_STOCK_INTELLIGENCE_EVENING = "true"
$env:MANAGEMENT_HEALTH_MAIL_ENABLED = "false"

$javaArgs = @("-jar", $Jar, "--spring.profiles.active=$Profile")

if ($Background) {
  Write-Host "Demarrage gateway en arriere-plan (profil: $Profile)..."
  Write-Host "Logs: $OutLog / $ErrLog"
  Start-Process -FilePath $Java21 -ArgumentList $javaArgs `
    -RedirectStandardOutput $OutLog `
    -RedirectStandardError $ErrLog `
    -WorkingDirectory $PSScriptRoot `
    -WindowStyle Hidden
  Write-Host "Gateway lancee. Attendez Tomcat started on port 8999."
} else {
  Write-Host "Demarrage gateway (profil: $Profile)..."
  & $Java21 @javaArgs
}
