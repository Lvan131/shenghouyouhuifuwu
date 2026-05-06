USE `youhuifuwu`;

ALTER TABLE `merchant_profile`
  ADD COLUMN IF NOT EXISTS `distance_km` DECIMAL(6, 2) DEFAULT NULL COMMENT 'distance in km' AFTER `address`;

UPDATE `merchant_profile`
SET `distance_km` = CASE `id`
  WHEN 2001 THEN 0.32
  WHEN 2002 THEN 0.68
  ELSE `distance_km`
END
WHERE `distance_km` IS NULL OR `distance_km` = 0;
