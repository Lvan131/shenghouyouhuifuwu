USE `youhuifuwu`;

DELETE FROM `activity_audit_record`;
DELETE FROM `announcement_read_record`;
DELETE FROM `announcement`;
DELETE FROM `merchant_rating`;
DELETE FROM `merchant_subscription`;
DELETE FROM `activity_join_record`;
DELETE FROM `activity`;
DELETE FROM `merchant_profile`;
DELETE FROM `user_profile`;
DELETE FROM `sys_account`;

INSERT INTO `sys_account` (`id`, `username`, `password_hash`, `role`, `login_type`, `wx_openid`, `status`, `last_login_time`)
VALUES
  (1, 'admin', 'Admin@2026', 'ADMIN', 'PASSWORD', NULL, 1, NOW()),
  (2, 'merchant01', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (3, 'merchant02', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (4, 'merchant03', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (5, 'merchant04', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (6, 'merchant05', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (7, 'merchant06', 'Merchant@2026', 'MERCHANT', 'PASSWORD', NULL, 1, NOW()),
  (1001, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-001', 1, NOW()),
  (1002, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-002', 1, NOW()),
  (1003, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-003', 1, NOW()),
  (1004, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-004', 1, NOW()),
  (1005, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-005', 1, NOW()),
  (1006, NULL, 'User@2026', 'USER', 'WECHAT', 'mock-user-006', 1, NOW());

INSERT INTO `user_profile` (`id`, `account_id`, `real_name`, `user_no`, `user_type`, `phone`, `avatar_url`)
VALUES
  (1101, 1001, '张三', '20230001', 'STUDENT', '13800000001', NULL),
  (1102, 1002, '李老师', 'T2023001', 'TEACHER', '13800000002', NULL),
  (1103, 1003, '王同学', '20230002', 'STUDENT', '13800000003', NULL),
  (1104, 1004, '陈同学', '20230003', 'STUDENT', '13800000004', NULL),
  (1105, 1005, '刘老师', 'T2023002', 'TEACHER', '13800000005', NULL),
  (1106, 1006, '赵同学', '20230004', 'STUDENT', '13800000006', NULL);

INSERT INTO `merchant_profile` (
  `id`, `account_id`, `merchant_name`, `merchant_type`, `contact_name`, `contact_phone`,
  `address`, `distance_km`, `longitude`, `latitude`, `description`, `status`
)
VALUES
  (2001, 2, '校园奶茶站', '餐饮', '王店长', '13900000001', '北门向东 50 米', 0.32, 120.318500, 31.498900, '主打奶茶、果饮和学生价甜品。', 1),
  (2002, 3, '文具生活馆', '零售', '赵店长', '13900000002', '南门商业街 2 号', 0.68, 120.319200, 31.497400, '提供文具、日用品和节日礼盒。', 1),
  (2003, 4, '校园咖啡角', '餐饮', '周店长', '13900000003', '图书馆西侧一层', 0.45, 120.317900, 31.499300, '咖啡、贝果和自习套餐是店内热门。', 1),
  (2004, 5, '水果轻食铺', '生鲜', '吴店长', '13900000004', '宿舍区东门 8 号', 0.82, 120.320100, 31.496800, '每日供应水果杯、轻食沙拉和鲜榨果汁。', 1),
  (2005, 6, '打印快修站', '服务', '孙店长', '13900000005', '教学楼 A 座旁', 0.55, 120.318100, 31.497900, '打印、装订、证件照和设备简修都可处理。', 1),
  (2006, 7, '运动装备屋', '零售', '钱店长', '13900000006', '体育馆南侧 1 号', 1.20, 120.321000, 31.495900, '提供球类器材、运动配件和社团团购服务。', 1);

INSERT INTO `activity` (
  `id`, `merchant_id`, `title`, `activity_type`, `cover_image`, `content`, `quota`, `daily_quota`, `joined_count`,
  `start_time`, `end_time`, `status`, `audit_remark`, `audited_by`, `audited_at`
)
VALUES
  (3001, 2001, '奶茶第二杯半价', '折扣', NULL, '到店出示小程序活动页即可享受指定奶茶第二杯半价。', 120, 20, 3, '2026-04-01 09:00:00', '2026-04-20 22:00:00', 'APPROVED', '内容合规，适合首页推荐。', 1, NOW()),
  (3002, 2001, '周末小食套餐立减 8 元', '满减', NULL, '周五到周日购买小食套餐，现场核销可直接立减 8 元。', 80, 15, 2, '2026-04-03 10:00:00', '2026-04-18 20:30:00', 'APPROVED', '活动信息完整。', 1, NOW()),
  (3003, 2002, '文具满 30 减 6', '满减', NULL, '店内指定文具单笔消费满 30 元立减 6 元，社团采购同样可用。', 90, 18, 1, '2026-04-02 08:30:00', '2026-04-28 21:00:00', 'APPROVED', '适合学生日常消费。', 1, NOW()),
  (3004, 2003, '咖啡买一送一', '折扣', NULL, '经典美式和拿铁参与买一送一，下午两点后到店可直接使用。', 100, 16, 2, '2026-04-02 11:00:00', '2026-04-25 21:30:00', 'APPROVED', '优惠力度清晰。', 1, NOW()),
  (3005, 2004, '轻食沙拉限时 8 折', '折扣', NULL, '轻食沙拉、鸡胸肉卷和水果酸奶杯限时 8 折，适合健身和减脂人群。', 60, 12, 1, '2026-04-04 09:00:00', '2026-04-22 19:30:00', 'APPROVED', '商品范围明确。', 1, NOW()),
  (3006, 2005, '打印装订学生套餐', '套餐', NULL, '论文打印 30 页以内加简装订，凭学生证享受套餐优惠价。', 70, 10, 2, '2026-04-05 08:00:00', '2026-04-30 18:00:00', 'APPROVED', '场景明确，审核通过。', 1, NOW()),
  (3007, 2006, '运动水壶下单送毛巾', '赠品', NULL, '购买指定运动水壶即可赠送便携毛巾一条，数量有限，送完即止。', 50, 8, 1, '2026-04-06 09:30:00', '2026-04-26 20:00:00', 'APPROVED', '促销内容合规。', 1, NOW()),
  (3008, 2003, '社团专属咖啡券', '团购', NULL, '社团负责人可申请团购咖啡券，审核通过后生成专属兑换码。', 40, 10, 0, '2026-04-07 10:00:00', '2026-05-01 18:00:00', 'PENDING', NULL, NULL, NULL),
  (3009, 2006, '球拍租借体验日', '体验', NULL, '提供羽毛球拍与乒乓球拍免费体验，但报名规则仍需补充说明。', 30, 6, 0, '2026-04-08 14:00:00', '2026-04-24 18:00:00', 'PENDING', NULL, NULL, NULL);

INSERT INTO `activity_join_record` (`id`, `activity_id`, `user_id`, `participation_date`, `join_time`, `status`, `remark`)
VALUES
  (4001, 3001, 1101, '2026-04-18', NOW(), 'JOINED', NULL),
  (4002, 3001, 1103, '2026-04-18', NOW(), 'JOINED', NULL),
  (4003, 3001, 1104, '2026-04-19', NOW(), 'JOINED', NULL),
  (4004, 3002, 1101, '2026-04-18', NOW(), 'JOINED', NULL),
  (4005, 3002, 1106, '2026-04-19', NOW(), 'JOINED', NULL),
  (4006, 3003, 1102, '2026-04-20', NOW(), 'JOINED', NULL),
  (4007, 3004, 1103, '2026-04-18', NOW(), 'JOINED', NULL),
  (4008, 3004, 1105, '2026-04-19', NOW(), 'JOINED', NULL),
  (4009, 3005, 1104, '2026-04-21', NOW(), 'JOINED', NULL),
  (4010, 3006, 1101, '2026-04-22', NOW(), 'JOINED', NULL),
  (4011, 3006, 1102, '2026-04-22', NOW(), 'JOINED', NULL),
  (4012, 3007, 1106, '2026-04-23', NOW(), 'JOINED', NULL);

INSERT INTO `merchant_subscription` (`id`, `user_id`, `merchant_id`, `created_at`)
VALUES
  (4501, 1101, 2001, NOW()),
  (4502, 1101, 2005, NOW()),
  (4503, 1102, 2003, NOW()),
  (4504, 1103, 2001, NOW()),
  (4505, 1103, 2004, NOW()),
  (4506, 1104, 2002, NOW()),
  (4507, 1105, 2003, NOW()),
  (4508, 1106, 2006, NOW());

INSERT INTO `merchant_rating` (`id`, `merchant_id`, `user_id`, `activity_id`, `score`, `content`, `created_at`, `updated_at`)
VALUES
  (4701, 2001, 1103, 3001, 5, '活动优惠力度不错，商家服务也很快。', NOW(), NOW()),
  (4702, 2003, 1105, 3004, 4, '到店核验顺畅，整体体验很好。', NOW(), NOW()),
  (4703, 2005, 1102, 3006, 5, '打印效率高，沟通也很耐心。', NOW(), NOW());

INSERT INTO `announcement` (`id`, `title`, `content`, `status`, `published_by`, `published_at`)
VALUES
  (5001, '活动发布提醒', '商家发布活动时请补充完整活动时间、人数限制和核销说明，方便同学们快速报名。', 1, 1, NOW()),
  (5002, '春季校园福利周', '本周平台集中上线餐饮、打印和运动类优惠，首页与附近商家页均可查看。', 1, 1, NOW()),
  (5003, '账号安全提示', '用户可在我的页面修改密码，若使用微信首次登录，默认密码为 123456。', 1, 1, NOW());

INSERT INTO `activity_audit_record` (`id`, `activity_id`, `auditor_id`, `audit_result`, `audit_remark`, `created_at`)
VALUES
  (6001, 3001, 1, 'APPROVED', '内容合规，适合首页推荐。', NOW()),
  (6002, 3002, 1, 'APPROVED', '活动信息完整。', NOW()),
  (6003, 3003, 1, 'APPROVED', '适合学生日常消费。', NOW()),
  (6004, 3004, 1, 'APPROVED', '优惠力度清晰。', NOW()),
  (6005, 3005, 1, 'APPROVED', '商品范围明确。', NOW()),
  (6006, 3006, 1, 'APPROVED', '场景明确，审核通过。', NOW()),
  (6007, 3007, 1, 'APPROVED', '促销内容合规。', NOW());
