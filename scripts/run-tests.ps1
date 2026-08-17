# ==========================================
# Script PowerShell d'exécution des tests
# ==========================================
# Exécute les tests frontend (Vitest) et backend (JUnit 5)
#
# Usage:
#   .\run-tests.ps1 [frontend|backend|all]
# ==========================================

param(
    [Parameter(Position=0)]
    [ValidateSet("frontend", "backend", "all")]
    [string]$Target = "all"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$FrontendDir = Join-Path $ProjectRoot "cmkerp-frontend"
$BackendDir = Join-Path $ProjectRoot "cmkerp-stocks"

# Fonction pour exécuter les tests frontend
function Run-FrontendTests {
    Write-Host "🧪 Exécution des tests Frontend (Vitest)..." -ForegroundColor Blue

    if (-not (Test-Path (Join-Path $FrontendDir "package.json"))) {
        Write-Host "❌ package.json non trouvé dans $FrontendDir" -ForegroundColor Red
        exit 1
    }

    Push-Location $FrontendDir

    try {
        Write-Host "📦 Installation des dépendances..." -ForegroundColor Yellow
        npm install --silent

        Write-Host "🧪 Exécution des tests..." -ForegroundColor Yellow
        npm run test

        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Les tests frontend ont échoué" -ForegroundColor Red
            exit 1
        }

        Write-Host "✅ Tests frontend terminés avec succès" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}

# Fonction pour exécuter les tests backend
function Run-BackendTests {
    Write-Host "🧪 Exécution des tests Backend (JUnit 5)..." -ForegroundColor Blue

    if (-not (Test-Path (Join-Path $BackendDir "pom.xml"))) {
        Write-Host "❌ pom.xml non trouvé dans $BackendDir" -ForegroundColor Red
        exit 1
    }

    Push-Location $BackendDir

    try {
        Write-Host "🧪 Exécution des tests Maven..." -ForegroundColor Yellow
        mvn test -DskipTests=false

        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Les tests backend ont échoué" -ForegroundColor Red
            exit 1
        }

        Write-Host "✅ Tests backend terminés avec succès" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}

# Main
switch ($Target) {
    "frontend" {
        Run-FrontendTests
    }
    "backend" {
        Run-BackendTests
    }
    "all" {
        Write-Host "🚀 Exécution de tous les tests...`n" -ForegroundColor Blue
        Run-FrontendTests
        Write-Host ""
        Run-BackendTests
        Write-Host "`n✅ Tous les tests terminés avec succès" -ForegroundColor Green
    }
}
