USE `youhuifuwu`;

SET @add_merchant_rating_table = (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE TABLE `merchant_rating` (
      `id` BIGINT NOT NULL COMMENT ''primary key'',
      `merchant_id` BIGINT NOT NULL COMMENT ''merchant id'',
      `user_id` BIGINT NOT NULL COMMENT ''user id'',
      `activity_id` BIGINT DEFAULT NULL COMMENT ''latest related activity id'',
      `score` INT NOT NULL COMMENT ''rating score 1-5'',
      `content` VARCHAR(500) DEFAULT NULL COMMENT ''rating content'',
      `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''created time'',
      `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''updated time'',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_merchant_rating_user_merchant` (`merchant_id`, `user_id`),
      KEY `idx_merchant_rating_user_id` (`user_id`),
      KEY `idx_merchant_rating_activity_id` (`activity_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''merchant rating''',
    'SELECT 1'
  )
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'merchant_rating'
);
PREPARE stmt FROM @add_merchant_rating_table;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
