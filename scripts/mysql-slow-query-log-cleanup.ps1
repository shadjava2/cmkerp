# Script PowerShell de nettoyage du slow query log MySQL
# Supprime les logs plus anciens que la période de rétention configurée
#
# Usage:
#   .\mysql-slow-query-log-cleanup.ps1 [retention-days]
#
# Par défaut: 7 jours de rétention

param(
    [int]$RetentionDays = 7
)

$MysqlDataDir = "C:\ProgramData\MySQL\MySQL Server 8.0\Data"  # Ajuster selon votre installation MySQL
$SlowQueryLogFile = Join-Path $MysqlDataDir "slow-query.log"

# Vérifier si le fichier existe
if (-not (Test-Path $SlowQueryLogFile)) {
    Write-Host "Fichier slow query log non trouvé: $SlowQueryLogFile" -ForegroundColor Red
    Write-Host "Vérifier la variable MySQL slow_query_log_file:" -ForegroundColor Yellow
    Write-Host "  mysql> SHOW VARIABLES LIKE 'slow_query_log_file';" -ForegroundColor Yellow
    exit 1
}

# Calculer la date limite (il y a X jours)
$CutoffDate = (Get-Date).AddDays(-$RetentionDays).ToString("yyyy-MM-dd")

Write-Host "Nettoyage du slow query log MySQL" -ForegroundColor Cyan
Write-Host "  Fichier: $SlowQueryLogFile" -ForegroundColor Gray
Write-Host "  Rétention: $RetentionDays jours" -ForegroundColor Gray
Write-Host "  Date limite: $CutoffDate" -ForegroundColor Gray
Write-Host ""

# Créer une sauvegarde avant nettoyage
$BackupFile = "$SlowQueryLogFile.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
Copy-Item $SlowQueryLogFile $BackupFile
Write-Host "Sauvegarde créée: $BackupFile" -ForegroundColor Green

# Lire le fichier et filtrer les entrées récentes
$Lines = Get-Content $SlowQueryLogFile
$FilteredLines = @()
$PrintLine = $false

foreach ($Line in $Lines) {
    # Parser le timestamp MySQL (format: # Time: YYYY-MM-DDTHH:MM:SS.######Z)
    if ($Line -match '# Time: (\d{4}-\d{2}-\d{2})T') {
        $LogDate = $Matches[1]
        if ($LogDate -ge $CutoffDate) {
            $PrintLine = $true
        } else {
            $PrintLine = $false
        }
    }

    if ($PrintLine) {
        $FilteredLines += $Line
    }
}

# Écrire le fichier filtré
$FilteredLines | Set-Content $SlowQueryLogFile

$BackupSize = (Get-Item $BackupFile).Length / 1MB
$NewSize = (Get-Item $SlowQueryLogFile).Length / 1MB

Write-Host "Nettoyage terminé" -ForegroundColor Green
Write-Host "  Taille avant: $([math]::Round($BackupSize, 2)) MB" -ForegroundColor Gray
Write-Host "  Taille après: $([math]::Round($NewSize, 2)) MB" -ForegroundColor Gray
