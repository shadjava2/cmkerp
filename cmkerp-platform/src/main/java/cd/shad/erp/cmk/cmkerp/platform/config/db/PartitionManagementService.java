package cd.shad.erp.cmk.cmkerp.platform.config.db;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de gestion automatique des partitions pour les tables partitionnées.
 *
 * <p>
 * Responsabilités :
 * <ul>
 * <li>Créer automatiquement les partitions futures (mensuelles)</li>
 * <li>Supprimer les partitions anciennes (> 12 mois)</li>
 * <li>Maintenir les partitions à jour</li>
 * </ul>
 *
 * <p>
 * Tables gérées :
 * <ul>
 * <li><b>ventes</b> : Partitionnée par `datecreate`</li>
 * <li><b>audit_events</b> : Partitionnée par `created_at`</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionManagementService implements CommandLineRunner {

  private final JdbcTemplate jdbcTemplate;

  private static final int MONTHS_TO_KEEP = 12; // Conserver 12 mois de données
  private static final int MONTHS_TO_CREATE_AHEAD = 3; // Créer 3 mois à l'avance

  /**
   * Exécuté au démarrage de l'application pour initialiser les partitions.
   */
  @Override
  public void run(String... args) {
    log.info("🚀 [PartitionManagementService] Démarrage de la gestion des partitions...");
    try {
      managePartitions("ventes", "datecreate");
      managePartitions("audit_events", "created_at");
      log.info("✅ [PartitionManagementService] Gestion des partitions terminée avec succès");
    } catch (Exception e) {
      log.error("❌ [PartitionManagementService] Erreur lors de la gestion des partitions", e);
    }
  }

  /**
   * Tâche planifiée : Exécutée le 1er de chaque mois à 2h du matin pour maintenir les partitions.
   */
  @Scheduled(cron = "0 0 2 1 * ?") // 1er de chaque mois à 2h
  public void scheduledPartitionMaintenance() {
    log.info("🔄 [PartitionManagementService] Maintenance planifiée des partitions...");
    try {
      managePartitions("ventes", "datecreate");
      managePartitions("audit_events", "created_at");
      log.info("✅ [PartitionManagementService] Maintenance planifiée terminée avec succès");
    } catch (Exception e) {
      log.error("❌ [PartitionManagementService] Erreur lors de la maintenance planifiée", e);
    }
  }

  /**
   * Gère les partitions pour une table donnée.
   *
   * @param tableName Nom de la table
   * @param dateColumn Nom de la colonne de date utilisée pour le partitionnement
   */
  private void managePartitions(String tableName, String dateColumn) {
    log.info("📊 [PartitionManagementService] Gestion des partitions pour la table: {}", tableName);

    // Vérifier si la table est partitionnée
    if (!isTablePartitioned(tableName)) {
      log.warn("⚠️ [PartitionManagementService] La table {} n'est pas partitionnée, ignorée",
          tableName);
      return;
    }

    // Créer les partitions futures
    createFuturePartitions(tableName, dateColumn);

    // Supprimer les partitions anciennes
    dropOldPartitions(tableName);
  }

  /**
   * Vérifie si une table est partitionnée.
   */
  private boolean isTablePartitioned(String tableName) {
    String sql = """
        SELECT COUNT(*)
        FROM information_schema.partitions
        WHERE table_schema = DATABASE()
        AND table_name = ?
        AND partition_name IS NOT NULL
        """;
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
    return count != null && count > 0;
  }

  /**
   * Crée les partitions futures nécessaires.
   */
  private void createFuturePartitions(String tableName, String dateColumn) {
    log.debug("🔮 [PartitionManagementService] Création des partitions futures pour {}", tableName);

    LocalDate today = LocalDate.now();
    LocalDate maxDate = today.plusMonths(MONTHS_TO_CREATE_AHEAD);

    // Récupérer la dernière partition existante
    String lastPartition = getLastPartition(tableName);
    if (lastPartition == null) {
      log.warn("⚠️ [PartitionManagementService] Aucune partition trouvée pour {}", tableName);
      return;
    }

    // Créer les partitions manquantes jusqu'à maxDate
    LocalDate currentMonth = getNextMonthToCreate(tableName);
    while (currentMonth.isBefore(maxDate) || currentMonth.isEqual(maxDate)) {
      String partitionName = "p_" + currentMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));
      LocalDate nextMonth = currentMonth.plusMonths(1);

      if (!partitionExists(tableName, partitionName)) {
        createPartition(tableName, partitionName, currentMonth, nextMonth);
      }

      currentMonth = nextMonth;
    }
  }

  /**
   * Récupère le nom de la dernière partition (non "future").
   */
  private String getLastPartition(String tableName) {
    String sql = """
        SELECT partition_name
        FROM information_schema.partitions
        WHERE table_schema = DATABASE()
        AND table_name = ?
        AND partition_name IS NOT NULL
        AND partition_name != 'p_future'
        AND partition_name != 'p_archive'
        ORDER BY partition_ordinal_position DESC
        LIMIT 1
        """;
    try {
      return jdbcTemplate.queryForObject(sql, String.class, tableName);
    } catch (Exception e) {
      log.debug("Aucune partition trouvée pour {}", tableName);
      return null;
    }
  }

  /**
   * Détermine le prochain mois pour lequel créer une partition.
   */
  private LocalDate getNextMonthToCreate(String tableName) {
    String lastPartition = getLastPartition(tableName);
    if (lastPartition == null) {
      return LocalDate.now().withDayOfMonth(1);
    }

    // Extraire la date de la partition (format: p_2024_01)
    try {
      String datePart = lastPartition.substring(2); // Enlever "p_"
      String[] parts = datePart.split("_");
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);
      return LocalDate.of(year, month, 1).plusMonths(1);
    } catch (Exception e) {
      log.warn("⚠️ [PartitionManagementService] Impossible de parser la partition: {}",
          lastPartition);
      return LocalDate.now().withDayOfMonth(1).plusMonths(1);
    }
  }

  /**
   * Vérifie si une partition existe.
   */
  private boolean partitionExists(String tableName, String partitionName) {
    String sql = """
        SELECT COUNT(*)
        FROM information_schema.partitions
        WHERE table_schema = DATABASE()
        AND table_name = ?
        AND partition_name = ?
        """;
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, partitionName);
    return count != null && count > 0;
  }

  /**
   * Crée une nouvelle partition.
   */
  private void createPartition(String tableName, String partitionName, LocalDate startDate,
      LocalDate endDate) {
    String startDateStr =
        startDate.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    String endDateStr =
        endDate.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    // Récupérer la partition "future" pour la réorganiser
    String sql = String.format(
        "ALTER TABLE %s REORGANIZE PARTITION p_future INTO (PARTITION %s VALUES LESS THAN ('%s'), PARTITION p_future VALUES LESS THAN MAXVALUE)",
        tableName, partitionName, endDateStr);

    try {
      jdbcTemplate.execute(sql);
      log.info("✅ [PartitionManagementService] Partition créée: {}.{} ({} - {})", tableName,
          partitionName, startDateStr, endDateStr);
    } catch (Exception e) {
      log.error("❌ [PartitionManagementService] Erreur lors de la création de la partition {}.{}",
          tableName, partitionName, e);
    }
  }

  /**
   * Supprime les partitions anciennes (> 12 mois).
   */
  private void dropOldPartitions(String tableName) {
    log.debug("🗑️ [PartitionManagementService] Suppression des partitions anciennes pour {}",
        tableName);

    LocalDate cutoffDate = LocalDate.now().minusMonths(MONTHS_TO_KEEP).withDayOfMonth(1);

    // Récupérer toutes les partitions (sauf archive et future)
    String sql = """
        SELECT partition_name, partition_description
        FROM information_schema.partitions
        WHERE table_schema = DATABASE()
        AND table_name = ?
        AND partition_name IS NOT NULL
        AND partition_name != 'p_future'
        AND partition_name != 'p_archive'
        ORDER BY partition_ordinal_position
        """;

    List<Map<String, Object>> partitions = jdbcTemplate.queryForList(sql, tableName);

    for (Map<String, Object> partition : partitions) {
      String partitionName = (String) partition.get("partition_name");

      if (isPartitionOld(partitionName, cutoffDate)) {
        dropPartition(tableName, partitionName);
      }
    }
  }

  /**
   * Vérifie si une partition est ancienne (avant la date de coupure).
   */
  private boolean isPartitionOld(String partitionName, LocalDate cutoffDate) {
    try {
      // Extraire la date de la partition (format: p_2024_01)
      String datePart = partitionName.substring(2); // Enlever "p_"
      String[] parts = datePart.split("_");
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);
      LocalDate partitionDate = LocalDate.of(year, month, 1);

      return partitionDate.isBefore(cutoffDate);
    } catch (Exception e) {
      log.warn("⚠️ [PartitionManagementService] Impossible de parser la partition: {}",
          partitionName);
      return false;
    }
  }

  /**
   * Supprime une partition.
   */
  private void dropPartition(String tableName, String partitionName) {
    String sql = String.format("ALTER TABLE %s DROP PARTITION %s", tableName, partitionName);

    try {
      jdbcTemplate.execute(sql);
      log.info("✅ [PartitionManagementService] Partition supprimée: {}.{}", tableName,
          partitionName);
    } catch (Exception e) {
      log.error(
          "❌ [PartitionManagementService] Erreur lors de la suppression de la partition {}.{}",
          tableName, partitionName, e);
    }
  }
}
