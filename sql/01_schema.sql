CREATE DATABASE IF NOT EXISTS `youhuifuwu`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `youhuifuwu`;

CREATE TABLE IF NOT EXISTS `sys_account` (
  `id` BIGINT NOT NULL COMMENT 'account id',
  `username` VARCHAR(50) DEFAULT NULL COMMENT 'username',
  `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'password',
  `role` VARCHAR(20) NOT NULL COMMENT 'USER / MERCHANT / ADMIN',
  `login_type` VARCHAR(20) NOT NULL COMMENT 'WECHAT / PASSWORD',
  `wx_openid` VARCHAR(64) DEFAULT NULL COMMENT 'wechat openid',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled 0 disabled',
  `last_login_time` DATETIME DEFAULT NULL COMMENT 'last login time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_account_username` (`username`),
  UNIQUE KEY `uk_sys_account_wx_openid` (`wx_openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='system account';

CREATE TABLE IF NOT EXISTS `user_profile` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `account_id` BIGINT NOT NULL COMMENT 'account id',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT 'real name',
  `user_no` VARCHAR(30) DEFAULT NULL COMMENT 'student or teacher number',
  `user_type` VARCHAR(20) NOT NULL COMMENT 'STUDENT / TEACHER',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT 'phone',
  `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profile_account_id` (`account_id`),
  KEY `idx_user_profile_user_no` (`user_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user profile';

CREATE TABLE IF NOT EXISTS `merchant_profile` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `account_id` BIGINT NOT NULL COMMENT 'account id',
  `merchant_name` VARCHAR(100) NOT NULL COMMENT 'merchant name',
  `merchant_type` VARCHAR(50) NOT NULL COMMENT 'merchant type',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT 'contact name',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT 'contact phone',
  `address` VARCHAR(255) NOT NULL COMMENT 'address',
  `distance_km` DECIMAL(6, 2) DEFAULT NULL COMMENT 'distance in km',
  `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'longitude',
  `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT 'latitude',
  `description` VARCHAR(500) DEFAULT NULL COMMENT 'description',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled 0 disabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_profile_account_id` (`account_id`),
  KEY `idx_merchant_profile_type` (`merchant_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant profile';

CREATE TABLE IF NOT EXISTS `activity` (
  `id` BIGINT NOT NULL COMMENT 'activity id',
  `merchant_id` BIGINT NOT NULL COMMENT 'merchant id',
  `title` VARCHAR(100) NOT NULL COMMENT 'title',
  `activity_type` VARCHAR(50) NOT NULL COMMENT 'activity type',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT 'cover image',
  `content` TEXT NOT NULL COMMENT 'content',
  `quota` INT DEFAULT NULL COMMENT 'quota',
  `daily_quota` INT NOT NULL COMMENT 'daily quota',
  `joined_count` INT NOT NULL DEFAULT 0 COMMENT 'joined count',
  `start_time` DATETIME NOT NULL COMMENT 'start time',
  `end_time` DATETIME NOT NULL COMMENT 'end time',
  `status` VARCHAR(20) NOT NULL COMMENT 'DRAFT / PENDING / APPROVED / REJECTED / OFFLINE / FINISHED',
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT 'audit remark',
  `audited_by` BIGINT DEFAULT NULL COMMENT 'audited by',
  `audited_at` DATETIME DEFAULT NULL COMMENT 'audited at',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  KEY `idx_activity_merchant_id` (`merchant_id`),
  KEY `idx_activity_status` (`status`),
  KEY `idx_activity_type` (`activity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity';

CREATE TABLE IF NOT EXISTS `activity_join_record` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `activity_id` BIGINT NOT NULL COMMENT 'activity id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `participation_date` DATE DEFAULT NULL COMMENT 'participation date',
  `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'join time',
  `status` VARCHAR(20) NOT NULL DEFAULT 'JOINED' COMMENT 'JOINED / CANCELED / USED',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT 'remark',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_join_record_user_activity` (`activity_id`, `user_id`),
  KEY `idx_activity_join_record_user_id` (`user_id`),
  KEY `idx_activity_join_record_participation_date` (`participation_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity join record';

CREATE TABLE IF NOT EXISTS `merchant_subscription` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `merchant_id` BIGINT NOT NULL COMMENT 'merchant id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_subscription_user_merchant` (`user_id`, `merchant_id`),
  KEY `idx_merchant_subscription_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant subscription';

CREATE TABLE IF NOT EXISTS `merchant_rating` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `merchant_id` BIGINT NOT NULL COMMENT 'merchant id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `activity_id` BIGINT DEFAULT NULL COMMENT 'latest related activity id',
  `score` INT NOT NULL COMMENT 'rating score 1-5',
  `content` VARCHAR(500) DEFAULT NULL COMMENT 'rating content',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_rating_user_merchant` (`merchant_id`, `user_id`),
  KEY `idx_merchant_rating_user_id` (`user_id`),
  KEY `idx_merchant_rating_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='merchant rating';

CREATE TABLE IF NOT EXISTS `announcement` (
  `id` BIGINT NOT NULL COMMENT 'announcement id',
  `title` VARCHAR(100) NOT NULL COMMENT 'title',
  `content` TEXT NOT NULL COMMENT 'content',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 published 0 offline',
  `published_by` BIGINT DEFAULT NULL COMMENT 'published by',
  `published_at` DATETIME DEFAULT NULL COMMENT 'published at',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='announcement';

CREATE TABLE IF NOT EXISTS `announcement_read_record` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `announcement_id` BIGINT NOT NULL COMMENT 'announcement id',
  `user_id` BIGINT NOT NULL COMMENT 'user id',
  `read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'read time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_read_record_user_announcement` (`announcement_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='announcement read record';

CREATE TABLE IF NOT EXISTS `activity_audit_record` (
  `id` BIGINT NOT NULL COMMENT 'primary key',
  `activity_id` BIGINT NOT NULL COMMENT 'activity id',
  `auditor_id` BIGINT NOT NULL COMMENT 'auditor account id',
  `audit_result` VARCHAR(20) NOT NULL COMMENT 'APPROVED / REJECTED',
  `audit_remark` VARCHAR(255) DEFAULT NULL COMMENT 'audit remark',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
  PRIMARY KEY (`id`),
  KEY `idx_activity_audit_record_activity_id` (`activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity audit record';
