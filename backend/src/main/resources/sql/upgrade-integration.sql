-- =====================================================================
-- 升级脚本: 外部课程同步(云学堂等) 集成能力
-- 幂等可重复执行; 适用于已按 schema.sql 初始化的环境
-- =====================================================================
USE training_db;

-- 一、课程表扩展: 外部来源标识(MySQL 8.0 无 ADD COLUMN IF NOT EXISTS, 用存储过程实现幂等) ---
DROP PROCEDURE IF EXISTS add_course_columns;
DELIMITER $$
CREATE PROCEDURE add_course_columns()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_course' AND COLUMN_NAME = 'source_type') THEN
    ALTER TABLE biz_course ADD COLUMN source_type TINYINT NOT NULL DEFAULT 0 COMMENT '0自建 1外部同步';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_course' AND COLUMN_NAME = 'source_id') THEN
    ALTER TABLE biz_course ADD COLUMN source_id BIGINT DEFAULT NULL COMMENT '来源外部系统ID';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_course' AND COLUMN_NAME = 'external_code') THEN
    ALTER TABLE biz_course ADD COLUMN external_code VARCHAR(100) DEFAULT NULL COMMENT '外部课程唯一编码';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = 'training_db' AND TABLE_NAME = 'biz_course' AND COLUMN_NAME = 'external_url') THEN
    ALTER TABLE biz_course ADD COLUMN external_url VARCHAR(500) DEFAULT NULL COMMENT '外部学习链接';
  END IF;
END$$
DELIMITER ;
CALL add_course_columns();
DROP PROCEDURE IF EXISTS add_course_columns;

-- 二、外部系统数据源配置 -------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_course_source (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_name     VARCHAR(100) NOT NULL COMMENT '数据源名称',
  source_type     VARCHAR(20)  NOT NULL DEFAULT 'mock' COMMENT 'yunxuetang云学堂 / api通用REST / mock演示',
  base_url        VARCHAR(300) DEFAULT NULL COMMENT '开放平台地址',
  token_path      VARCHAR(200) DEFAULT NULL COMMENT '令牌接口路径',
  list_path       VARCHAR(200) DEFAULT NULL COMMENT '课程列表接口路径',
  app_key         VARCHAR(100) DEFAULT NULL,
  app_secret      VARCHAR(200) DEFAULT NULL,
  data_path       VARCHAR(100) DEFAULT NULL COMMENT '列表数据JSON路径 如 data.list',
  field_mapping   TEXT COMMENT '字段映射JSON(外部字段名)',
  category_mapping TEXT COMMENT '分类映射JSON 源分类->本系统分类',
  extra_config    TEXT COMMENT '其他配置JSON(defaultCategory/maxPages等)',
  status          VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  last_sync_time  DATETIME DEFAULT NULL,
  remark          VARCHAR(500) DEFAULT NULL,
  del_flag        TINYINT NOT NULL DEFAULT 0,
  create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '外部课程数据源';

-- 三、外部课程暂存表(审核入库制) ----------------------------------------------
CREATE TABLE IF NOT EXISTS biz_external_course (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_id     BIGINT NOT NULL,
  external_id   VARCHAR(100) NOT NULL COMMENT '外部课程唯一ID',
  course_name   VARCHAR(200) NOT NULL,
  category_raw  VARCHAR(100) DEFAULT NULL COMMENT '源分类原文',
  category      VARCHAR(20)  DEFAULT NULL COMMENT '映射后本系统分类',
  course_type   VARCHAR(20)  NOT NULL DEFAULT 'online',
  hours         DECIMAL(6,1) DEFAULT 0,
  level         VARCHAR(20)  DEFAULT NULL,
  target_users  VARCHAR(200) DEFAULT NULL,
  description   TEXT,
  url           VARCHAR(500) DEFAULT NULL,
  raw_json      TEXT COMMENT '原始报文留档',
  status        VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0待入库 1已入库 2已忽略',
  course_id     BIGINT DEFAULT NULL COMMENT '入库后的课程ID',
  sync_time     DATETIME DEFAULT NULL,
  del_flag      TINYINT NOT NULL DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ext_course (source_id, external_id),
  KEY idx_ext_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '外部课程暂存表';

-- 四、同步日志 -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_sync_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_id   BIGINT DEFAULT NULL,
  source_name VARCHAR(100) DEFAULT NULL,
  action      VARCHAR(20) DEFAULT NULL COMMENT 'pull拉取 import入库 test测试',
  status      VARCHAR(4) DEFAULT NULL COMMENT '0成功 1失败 2部分成功',
  total       INT DEFAULT 0,
  success     INT DEFAULT 0,
  fail        INT DEFAULT 0,
  skip        INT DEFAULT 0,
  message     VARCHAR(500) DEFAULT NULL,
  detail      TEXT COMMENT '逐条错误明细',
  start_time  DATETIME DEFAULT NULL,
  end_time    DATETIME DEFAULT NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_log_source (source_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '外部同步日志';

-- 五、菜单与权限(培训管理→外部课程同步) ---------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(107, 100, '外部课程同步', 'integration', 'integration/index', 'C', 'biz:integration:list', 'Connection', 7) ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path);
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(1144, 107, '接入配置维护', '', '', 'F', 'biz:integration:edit', '', 1) ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(1145, 107, '同步与入库',   '', '', 'F', 'biz:integration:sync', '', 2) ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (2,107),(2,1144),(2,1145) ON DUPLICATE KEY UPDATE role_id = role_id;
