-- =====================================================================
-- 升级脚本: v1.5 行业对标优化(第二轮)
--   岗位学习地图 / 经费预算执行 / 定时自动提醒 / 签到码自助签到 / 证书查验
-- 幂等可重复执行; 适用于已执行 upgrade-optimize.sql 的环境
-- 执行提醒: mysql 客户端需加 --default-character-set=utf8mb4, 否则菜单中文会乱码
-- =====================================================================
USE training_db;

-- 一、新表 ---------------------------------------------------------------
-- 岗位学习地图: 人员类型 × 年度必修课程(配置数据, 删除走物理删除以避免软删与唯一键冲突)
CREATE TABLE IF NOT EXISTS biz_learning_map (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  year        INT NOT NULL COMMENT '年度',
  user_type   VARCHAR(20) NOT NULL COMMENT 'manager/professional/staff',
  course_id   BIGINT NOT NULL COMMENT '必修课程',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_map (year, user_type, course_id),
  KEY idx_map_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '岗位学习地图(年度必修课)';

-- 自动提醒发送留痕: 同类提醒同一业务对象 7 天内不重复
CREATE TABLE IF NOT EXISTS biz_notify_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  notify_type VARCHAR(30) NOT NULL COMMENT 'classStart/evalPending/hourWarn',
  biz_key     VARCHAR(120) NOT NULL COMMENT '去重键, 如 class:12:user:3',
  user_id     BIGINT NOT NULL,
  sent_time   DATETIME NOT NULL,
  KEY idx_notify_key (notify_type, biz_key),
  KEY idx_notify_time (sent_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动提醒发送留痕';

-- 二、课表增加签到码列(幂等) ----------------------------------------------
DROP PROCEDURE IF EXISTS add_schedule_sign_code;
DELIMITER $$
CREATE PROCEDURE add_schedule_sign_code()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_class_schedule' AND COLUMN_NAME = 'sign_code') THEN
    ALTER TABLE biz_class_schedule ADD COLUMN sign_code VARCHAR(8) DEFAULT NULL COMMENT '自助签到码(6位)';
  END IF;
END$$
DELIMITER ;
CALL add_schedule_sign_code();
DROP PROCEDURE IF EXISTS add_schedule_sign_code;

-- 二之二、结业证书增加防伪码(公开查验需"证书编号+防伪码"双要素, 防止编号被顺序枚举) ----
DROP PROCEDURE IF EXISTS add_cert_verify_code;
DELIMITER $$
CREATE PROCEDURE add_cert_verify_code()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_certificate' AND COLUMN_NAME = 'verify_code') THEN
    ALTER TABLE biz_certificate ADD COLUMN verify_code VARCHAR(8) DEFAULT NULL COMMENT '防伪码(6位), 公开查验双要素';
  END IF;
END$$
DELIMITER ;
CALL add_cert_verify_code();
DROP PROCEDURE IF EXISTS add_cert_verify_code;
-- 存量证书回填防伪码
UPDATE biz_certificate SET verify_code = LPAD(FLOOR(RAND() * 1000000), 6, '0')
WHERE verify_code IS NULL OR verify_code = '';

-- 三、菜单(幂等: 先删后插指定ID) ------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (607, 608, 609, 1340);
DELETE FROM sys_menu WHERE id IN (607, 608, 609, 1340);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(607, 100, '学习地图配置', 'learning-map', 'map/index',  'C', 'biz:map:list', 'Guide',       11),
(608, 500, '我的学习地图', 'my-map',        'map/my',     'C', '',             'Compass',     6),
(609, 500, '证书查验',     'cert-verify',   'cert/verify','C', '',             'Search',      7),
(1340, 607, '学习地图维护', '', '', 'F', 'biz:map:edit', '', 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 607),(2, 608),(2, 609),(2, 1340),
(3, 608),(3, 609);

-- 四、参数(幂等) -----------------------------------------------------------
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 8, '定时自动提醒开关', 'notify.enabled', '1', '每日自动扫描开班提醒/评估催办/学时预警; 0 关闭'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 8);
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 9, '开班提醒提前天数', 'notify.class.remind.days', '3', '班次开始前 N 天提醒未参训学员'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 9);
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 10, '评估催办触发天数', 'notify.eval.remind.days', '3', '结班满 N 天仍未评估的学员自动催办'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 10);
