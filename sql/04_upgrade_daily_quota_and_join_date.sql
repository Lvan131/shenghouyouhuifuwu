USE `youhuifuwu`;

SET @activity_daily_quota_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity'
    AND COLUMN_NAME = 'daily_quota'
);
SET @activity_daily_quota_sql = IF(
  @activity_daily_quota_exists = 0,
  'ALTER TABLE `activity` ADD COLUMN `daily_quota` INT NULL COMMENT ''daily quota'' AFTER `quota`',
  'SELECT 1'
);
PREPARE activity_daily_quota_stmt FROM @activity_daily_quota_sql;
EXECUTE activity_daily_quota_stmt;
DEALLOCATE PREPARE activity_daily_quota_stmt;

UPDATE `activity`
SET `daily_quota` = CASE
  WHEN `quota` IS NOT NULL AND `quota` > 0 THEN LEAST(`quota`, 20)
  ELSE 10
END
WHERE `daily_quota` IS NULL OR `daily_quota` <= 0;

ALTER TABLE `activity`
  MODIFY COLUMN `daily_quota` INT NOT NULL COMMENT 'daily quota';

SET @join_date_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_join_record'
    AND COLUMN_NAME = 'participation_date'
);
SET @join_date_sql = IF(
  @join_date_exists = 0,
  'ALTER TABLE `activity_join_record` ADD COLUMN `participation_date` DATE NULL COMMENT ''participation date'' AFTER `user_id`',
  'SELECT 1'
);
PREPARE join_date_stmt FROM @join_date_sql;
EXECUTE join_date_stmt;
DEALLOCATE PREPARE join_date_stmt;

UPDATE `activity_join_record`
SET `participation_date` = DATE(`join_time`)
WHERE `participation_date` IS NULL;

SET @join_date_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'activity_join_record'
    AND INDEX_NAME = 'idx_activity_join_record_participation_date'
);
SET @join_date_index_sql = IF(
  @join_date_index_exists = 0,
  'CREATE INDEX `idx_activity_join_record_participation_date` ON `activity_join_record` (`participation_date`)',
  'SELECT 1'
);
PREPARE join_date_index_stmt FROM @join_date_index_sql;
EXECUTE join_date_index_stmt;
DEALLOCATE PREPARE join_date_index_stmt;
