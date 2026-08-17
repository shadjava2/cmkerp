# Script PowerShell pour réparer Flyway V4
# Ce script supprime l'entrée échouée de la migration V4 dans flyway_schema_history

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "RÉPARATION FLYWAY - Migration V4" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration (modifiez selon votre environnement)
$dbHost = "localhost"
$dbPort = "32768"
$dbName = "cmkerp-v24dev"
$dbUser = "shad"
$dbPassword = "SDconceptsrdc@243"  # ⚠️ Modifiez si nécessaire

Write-Host "Connexion à MySQL..." -ForegroundColor Yellow
Write-Host "Host: $dbHost`:$dbPort" -ForegroundColor Gray
Write-Host "Database: $dbName" -ForegroundColor Gray
Write-Host ""

# Commande SQL pour supprimer l'entrée échouée
$sqlCommand = @"
USE `$dbName`;

-- Supprimer l'entrée échouée de la migration V4
DELETE FROM `flyway_schema_history`
WHERE `version` = '4'
  AND `type` = 'SQL'
  AND `description` = 'add performance indexes'
  AND `success` = 0;

-- Vérifier que l'entrée a été supprimée
SELECT * FROM `flyway_schema_history` WHERE `version` = '4';
"@

# Sauvegarder dans un fichier temporaire
$tempFile = [System.IO.Path]::GetTempFileName() + ".sql"
$sqlCommand | Out-File -FilePath $tempFile -Encoding UTF8

Write-Host "Exécution du script SQL..." -ForegroundColor Yellow

try {
    # Exécuter via mysql CLI (si disponible)
    $mysqlPath = "mysql"

    # Construire la commande
    $fullCommand = "$mysqlPath -h $dbHost -P $dbPort -u $dbUser -p$dbPassword $dbName < `"$tempFile`""

    Write-Host "Commande: $mysqlPath -h $dbHost -P $dbPort -u $dbUser -p**** $dbName" -ForegroundColor Gray

    # Exécuter
    $result = Invoke-Expression $fullCommand 2>&1

    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Réparation réussie !" -ForegroundColor Green
        Write-Host "L'entrée échouée de la migration V4 a été supprimée." -ForegroundColor Green
        Write-Host ""
        Write-Host "Vous pouvez maintenant redémarrer l'application." -ForegroundColor Cyan
    } else {
        Write-Host ""
        Write-Host "❌ Erreur lors de l'exécution." -ForegroundColor Red
        Write-Host "Vérifiez que MySQL CLI est installé et accessible." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Alternative: Exécutez manuellement le script SQL:" -ForegroundColor Yellow
        Write-Host "  $tempFile" -ForegroundColor Gray
    }
} catch {
    Write-Host ""
    Write-Host "❌ Erreur: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Exécutez manuellement le script SQL:" -ForegroundColor Yellow
    Write-Host "  $tempFile" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Ou connectez-vous à MySQL et exécutez:" -ForegroundColor Yellow
    Write-Host "  USE $dbName;" -ForegroundColor Gray
    Write-Host "  DELETE FROM flyway_schema_history WHERE version = '4' AND success = 0;" -ForegroundColor Gray
}

# Nettoyer
Remove-Item $tempFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan

