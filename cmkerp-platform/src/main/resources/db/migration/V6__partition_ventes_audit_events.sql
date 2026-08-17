-- ==========================================
-- FLYWAY MIGRATION V6 - Partitionnement Tables Ventes et Audit Events
-- ==========================================
-- Partitionnement par mois (RANGE COLUMNS) pour optimiser les performances
-- et faciliter l'archivage des données anciennes
--
-- Tables partitionnées :
-- - ventes : Partitionnement par datecreate (mensuel)
-- - audit_events : Partitionnement par created_at (mensuel)
--
-- Stratégie :
-- - Partitions mensuelles pour les 12 derniers mois
-- - Partition "future" pour les données au-delà
-- - Partition "archive" pour les données très anciennes (> 12 mois)
-- ==========================================

-- ==========================================
-- PARTITIONNEMENT TABLE VENTES
-- ==========================================
-- Partitionnement par datecreate (mensuel)
-- Avantages :
-- - Requêtes filtrées par date plus rapides (partition pruning)
-- - Archivage facile des anciennes partitions
-- - Maintenance simplifiée (DROP PARTITION au lieu de DELETE)
-- ==========================================

-- Vérifier si la table existe et n'est pas déjà partitionnée
SET @ventes_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
    AND table_name = 'ventes'
);

SET @ventes_partitioned = (
    SELECT COUNT(*)
    FROM information_schema.partitions
    WHERE table_schema = DATABASE()
    AND table_name = 'ventes'
    AND partition_name IS NOT NULL
);

-- Partitionner seulement si la table existe et n'est pas déjà partitionnée
SET @sql_ventes = IF(
    @ventes_exists > 0 AND @ventes_partitioned = 0,
    CONCAT(
        'ALTER TABLE ventes ',
        'PARTITION BY RANGE COLUMNS(datecreate) (',
        -- Partition pour données très anciennes (avant 2024)
        'PARTITION p_archive VALUES LESS THAN (''2024-01-01 00:00:00''),',
        -- Partitions mensuelles pour 2024
        'PARTITION p_2024_01 VALUES LESS THAN (''2024-02-01 00:00:00''),',
        'PARTITION p_2024_02 VALUES LESS THAN (''2024-03-01 00:00:00''),',
        'PARTITION p_2024_03 VALUES LESS THAN (''2024-04-01 00:00:00''),',
        'PARTITION p_2024_04 VALUES LESS THAN (''2024-05-01 00:00:00''),',
        'PARTITION p_2024_05 VALUES LESS THAN (''2024-06-01 00:00:00''),',
        'PARTITION p_2024_06 VALUES LESS THAN (''2024-07-01 00:00:00''),',
        'PARTITION p_2024_07 VALUES LESS THAN (''2024-08-01 00:00:00''),',
        'PARTITION p_2024_08 VALUES LESS THAN (''2024-09-01 00:00:00''),',
        'PARTITION p_2024_09 VALUES LESS THAN (''2024-10-01 00:00:00''),',
        'PARTITION p_2024_10 VALUES LESS THAN (''2024-11-01 00:00:00''),',
        'PARTITION p_2024_11 VALUES LESS THAN (''2024-12-01 00:00:00''),',
        'PARTITION p_2024_12 VALUES LESS THAN (''2025-01-01 00:00:00''),',
        -- Partitions mensuelles pour 2025
        'PARTITION p_2025_01 VALUES LESS THAN (''2025-02-01 00:00:00''),',
        'PARTITION p_2025_02 VALUES LESS THAN (''2025-03-01 00:00:00''),',
        'PARTITION p_2025_03 VALUES LESS THAN (''2025-04-01 00:00:00''),',
        'PARTITION p_2025_04 VALUES LESS THAN (''2025-05-01 00:00:00''),',
        'PARTITION p_2025_05 VALUES LESS THAN (''2025-06-01 00:00:00''),',
        'PARTITION p_2025_06 VALUES LESS THAN (''2025-07-01 00:00:00''),',
        'PARTITION p_2025_07 VALUES LESS THAN (''2025-08-01 00:00:00''),',
        'PARTITION p_2025_08 VALUES LESS THAN (''2025-09-01 00:00:00''),',
        'PARTITION p_2025_09 VALUES LESS THAN (''2025-10-01 00:00:00''),',
        'PARTITION p_2025_10 VALUES LESS THAN (''2025-11-01 00:00:00''),',
        'PARTITION p_2025_11 VALUES LESS THAN (''2025-12-01 00:00:00''),',
        'PARTITION p_2025_12 VALUES LESS THAN (''2026-01-01 00:00:00''),',
        -- Partition future pour données au-delà
        'PARTITION p_future VALUES LESS THAN MAXVALUE',
        ')'
    ),
    'SELECT ''Table ventes n''''existe pas ou est déjà partitionnée'''
);

PREPARE stmt_ventes FROM @sql_ventes;
EXECUTE stmt_ventes;
DEALLOCATE PREPARE stmt_ventes;

-- ==========================================
-- PARTITIONNEMENT TABLE AUDIT_EVENTS
-- ==========================================
-- Partitionnement par created_at (mensuel)
-- Avantages :
-- - Requêtes filtrées par date plus rapides
-- - Archivage facile des anciennes partitions
-- - Performance améliorée pour les requêtes récentes
-- ==========================================

-- Vérifier si la table existe et n'est pas déjà partitionnée
SET @audit_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
    AND table_name = 'audit_events'
);

SET @audit_partitioned = (
    SELECT COUNT(*)
    FROM information_schema.partitions
    WHERE table_schema = DATABASE()
    AND table_name = 'audit_events'
    AND partition_name IS NOT NULL
);

-- Partitionner seulement si la table existe et n'est pas déjà partitionnée
SET @sql_audit = IF(
    @audit_exists > 0 AND @audit_partitioned = 0,
    CONCAT(
        'ALTER TABLE audit_events ',
        'PARTITION BY RANGE COLUMNS(created_at) (',
        -- Partition pour données très anciennes (avant 2024)
        'PARTITION p_archive VALUES LESS THAN (''2024-01-01 00:00:00''),',
        -- Partitions mensuelles pour 2024
        'PARTITION p_2024_01 VALUES LESS THAN (''2024-02-01 00:00:00''),',
        'PARTITION p_2024_02 VALUES LESS THAN (''2024-03-01 00:00:00''),',
        'PARTITION p_2024_03 VALUES LESS THAN (''2024-04-01 00:00:00''),',
        'PARTITION p_2024_04 VALUES LESS THAN (''2024-05-01 00:00:00''),',
        'PARTITION p_2024_05 VALUES LESS THAN (''2024-06-01 00:00:00''),',
        'PARTITION p_2024_06 VALUES LESS THAN (''2024-07-01 00:00:00''),',
        'PARTITION p_2024_07 VALUES LESS THAN (''2024-08-01 00:00:00''),',
        'PARTITION p_2024_08 VALUES LESS THAN (''2024-09-01 00:00:00''),',
        'PARTITION p_2024_09 VALUES LESS THAN (''2024-10-01 00:00:00''),',
        'PARTITION p_2024_10 VALUES LESS THAN (''2024-11-01 00:00:00''),',
        'PARTITION p_2024_11 VALUES LESS THAN (''2024-12-01 00:00:00''),',
        'PARTITION p_2024_12 VALUES LESS THAN (''2025-01-01 00:00:00''),',
        -- Partitions mensuelles pour 2025
        'PARTITION p_2025_01 VALUES LESS THAN (''2025-02-01 00:00:00''),',
        'PARTITION p_2025_02 VALUES LESS THAN (''2025-03-01 00:00:00''),',
        'PARTITION p_2025_03 VALUES LESS THAN (''2025-04-01 00:00:00''),',
        'PARTITION p_2025_04 VALUES LESS THAN (''2025-05-01 00:00:00''),',
        'PARTITION p_2025_05 VALUES LESS THAN (''2025-06-01 00:00:00''),',
        'PARTITION p_2025_06 VALUES LESS THAN (''2025-07-01 00:00:00''),',
        'PARTITION p_2025_07 VALUES LESS THAN (''2025-08-01 00:00:00''),',
        'PARTITION p_2025_08 VALUES LESS THAN (''2025-09-01 00:00:00''),',
        'PARTITION p_2025_09 VALUES LESS THAN (''2025-10-01 00:00:00''),',
        'PARTITION p_2025_10 VALUES LESS THAN (''2025-11-01 00:00:00''),',
        'PARTITION p_2025_11 VALUES LESS THAN (''2025-12-01 00:00:00''),',
        'PARTITION p_2025_12 VALUES LESS THAN (''2026-01-01 00:00:00''),',
        -- Partition future pour données au-delà
        'PARTITION p_future VALUES LESS THAN MAXVALUE',
        ')'
    ),
    'SELECT ''Table audit_events n''''existe pas ou est déjà partitionnée'''
);

PREPARE stmt_audit FROM @sql_audit;
EXECUTE stmt_audit;
DEALLOCATE PREPARE stmt_audit;

-- ==========================================
-- NOTES IMPORTANTES
-- ==========================================
-- 1. Les partitions sont créées pour 24 mois (2024-2025)
-- 2. Une partition "archive" contient toutes les données avant 2024
-- 3. Une partition "future" contient toutes les données après 2025
-- 4. Utiliser le service PartitionManagementService pour :
--    - Créer automatiquement les partitions futures
--    - Supprimer les partitions anciennes (> 12 mois)
--    - Maintenir les partitions à jour
-- ==========================================
