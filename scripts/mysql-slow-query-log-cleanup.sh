#!/bin/bash

# Script de nettoyage du slow query log MySQL
# Supprime les logs plus anciens que la période de rétention configurée
#
# Usage:
#   ./mysql-slow-query-log-cleanup.sh [retention-days]
#
# Par défaut: 7 jours de rétention

RETENTION_DAYS=${1:-7}
MYSQL_DATA_DIR="/var/lib/mysql"  # Ajuster selon votre installation MySQL
SLOW_QUERY_LOG_FILE="${MYSQL_DATA_DIR}/slow-query.log"

# Vérifier si le fichier existe
if [ ! -f "$SLOW_QUERY_LOG_FILE" ]; then
    echo "Fichier slow query log non trouvé: $SLOW_QUERY_LOG_FILE"
    echo "Vérifier la variable MySQL slow_query_log_file:"
    echo "  mysql> SHOW VARIABLES LIKE 'slow_query_log_file';"
    exit 1
fi

# Calculer la date limite (il y a X jours)
CUTOFF_DATE=$(date -d "${RETENTION_DAYS} days ago" +%Y-%m-%d 2>/dev/null || date -v-${RETENTION_DAYS}d +%Y-%m-%d 2>/dev/null)

if [ -z "$CUTOFF_DATE" ]; then
    echo "Erreur: Impossible de calculer la date de coupure"
    exit 1
fi

echo "Nettoyage du slow query log MySQL"
echo "  Fichier: $SLOW_QUERY_LOG_FILE"
echo "  Rétention: $RETENTION_DAYS jours"
echo "  Date limite: $CUTOFF_DATE"
echo ""

# Créer une sauvegarde avant nettoyage
BACKUP_FILE="${SLOW_QUERY_LOG_FILE}.backup.$(date +%Y%m%d_%H%M%S)"
cp "$SLOW_QUERY_LOG_FILE" "$BACKUP_FILE"
echo "Sauvegarde créée: $BACKUP_FILE"

# Extraire les lignes récentes (7 derniers jours)
# Le format du slow query log MySQL inclut des timestamps
TEMP_FILE="${SLOW_QUERY_LOG_FILE}.tmp"

# Filtrer les entrées récentes (simplifié - nécessite parsing selon format MySQL)
# Note: Cette approche est basique. Pour une solution robuste, utiliser un parser MySQL
awk -v cutoff="$CUTOFF_DATE" '
BEGIN {
    split(cutoff, date_parts, "-")
    cutoff_epoch = mktime(date_parts[1] " " date_parts[2] " " date_parts[3] " 0 0 0")
}
/^# Time:/ {
    # Parser le timestamp MySQL (format: # Time: YYYY-MM-DDTHH:MM:SS.######Z)
    if (match($0, /Time: ([0-9]{4}-[0-9]{2}-[0-9]{2})T/, arr)) {
        split(arr[1], date_parts, "-")
        log_epoch = mktime(date_parts[1] " " date_parts[2] " " date_parts[3] " 0 0 0")
        if (log_epoch >= cutoff_epoch) {
            print_line = 1
        } else {
            print_line = 0
        }
    }
}
{
    if (print_line) {
        print
    }
}
' "$SLOW_QUERY_LOG_FILE" > "$TEMP_FILE"

# Remplacer le fichier original
mv "$TEMP_FILE" "$SLOW_QUERY_LOG_FILE"

echo "Nettoyage terminé"
echo "  Taille avant: $(du -h "$BACKUP_FILE" | cut -f1)"
echo "  Taille après: $(du -h "$SLOW_QUERY_LOG_FILE" | cut -f1)"
