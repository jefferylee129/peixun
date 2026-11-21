-- =====================================================================
-- 升级脚本: 行业对标优化(一人一档 / 学时预警消息 / 结业证书 / 在线考试)
-- 幂等可重复执行; 适用于已按 schema.sql 初始化的环境
-- 执行提醒: mysql 客户端需加 --default-character-set=utf8mb4, 否则菜单中文会乱码
-- =====================================================================
USE training_db;

-- 一、新表 ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_message (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL COMMENT '接收人',
  title       VARCHAR(200) NOT NULL,
  content     VARCHAR(1000) DEFAULT NULL,
  msg_type    VARCHAR(20)  NOT NULL DEFAULT 'system' COMMENT 'warn/enroll/eval/cert/exam/system',
  ref_id      BIGINT DEFAULT NULL COMMENT '关联业务ID',
  is_read     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  KEY idx_msg_user (user_id, is_read),
  KEY idx_msg_type (msg_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '站内消息';

CREATE TABLE IF NOT EXISTS biz_certificate (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  cert_no     VARCHAR(50) DEFAULT NULL COMMENT '证书编号 QGPX-YYYY-XXXXXX(插入后回填)',
  user_id     BIGINT NOT NULL,
  class_id    BIGINT NOT NULL,
  hours       DECIMAL(6, 1) DEFAULT 0 COMMENT '结业学时',
  grade       VARCHAR(20) DEFAULT '合格' COMMENT '合格/优秀',
  issue_date  DATE DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cert_class_user (class_id, user_id),
  UNIQUE KEY uk_cert_no (cert_no),
  KEY idx_cert_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '结业证书';

CREATE TABLE IF NOT EXISTS biz_question (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id   BIGINT DEFAULT NULL COMMENT '关联课程(可空)',
  q_type      VARCHAR(10) NOT NULL COMMENT 'single单选/multiple多选/judge判断',
  stem        VARCHAR(1000) NOT NULL,
  options     TEXT COMMENT 'JSON [{"key":"A","text":"..."}]; 判断题为["对","错"]',
  answer      VARCHAR(20) NOT NULL COMMENT 'A / ABD / 对',
  analysis    VARCHAR(500) DEFAULT NULL,
  status      CHAR(1) NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  KEY idx_q_course (course_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考试题库';

CREATE TABLE IF NOT EXISTS biz_exam (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(200) NOT NULL,
  class_id     BIGINT NOT NULL COMMENT '关联班次',
  course_id    BIGINT DEFAULT NULL,
  duration_min INT NOT NULL DEFAULT 30 COMMENT '考试时长(分钟)',
  pass_score   DECIMAL(5, 1) NOT NULL DEFAULT 60,
  total_score  DECIMAL(5, 1) NOT NULL DEFAULT 100,
  hour_reward  DECIMAL(4, 1) NOT NULL DEFAULT 0 COMMENT '及格计学时',
  question_ids VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '题目ID逗号分隔',
  start_time   DATETIME DEFAULT NULL,
  end_time     DATETIME DEFAULT NULL,
  status       CHAR(1) NOT NULL DEFAULT '0' COMMENT '0草稿 1已发布 2已截止',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  del_flag     TINYINT NOT NULL DEFAULT 0,
  KEY idx_exam_class (class_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '在线考试';

CREATE TABLE IF NOT EXISTS biz_exam_record (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id     BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  answers     TEXT COMMENT '作答JSON {qid:"A"|"ABD"}',
  score       DECIMAL(5, 1) DEFAULT 0,
  passed      TINYINT DEFAULT 0,
  start_time  DATETIME DEFAULT NULL COMMENT '首次开卷时间',
  submit_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_er_exam_user (exam_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考试作答';

-- 二、菜单(幂等: 先删后插指定ID) ------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN
  (600,601,602,603,604,605,606,1300,1310,1320,1321,1330);
DELETE FROM sys_menu WHERE id IN
  (600,601,602,603,604,605,606,1300,1310,1320,1321,1330);

INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(600, 100, '培训档案',   'archive',     'archive/index',   'C', 'biz:archive:other', 'FolderOpened', 8),
(601, 100, '题库管理',   'question',    'exam/question',   'C', 'biz:question:list', 'Collection',   9),
(602, 100, '考试管理',   'exam',        'exam/index',      'C', 'biz:exam:list',     'Finished',    10),
(603, 200, '证书台账',   'certificates','cert/index',      'C', 'biz:cert:list',     'Medal',        5),
(604, 500, '我的档案',   'my-archive',  'archive/my',      'C', '',                 'FolderChecked',3),
(605, 500, '消息中心',   'messages',    'message/index',   'C', '',                 'Bell',         4),
(606, 500, '在线考试',   'my-exam',     'exam/my',         'C', '',                 'EditPen',      5),
(1300, 600, '查看他人档案', '', '', 'F', 'biz:archive:other', '', 1),
(1310, 601, '题目维护', '', '', 'F', 'biz:question:edit', '', 1),
(1320, 602, '考试维护', '', '', 'F', 'biz:exam:edit',     '', 1),
(1321, 602, '考试发布', '', '', 'F', 'biz:exam:publish',  '', 2),
(1330, 603, '证书补发', '', '', 'F', 'biz:cert:issue',    '', 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 600),(2, 601),(2, 602),(2, 603),(2, 604),(2, 605),(2, 606),
(2, 1300),(2, 1310),(2, 1320),(2, 1321),(2, 1330),
(3, 604),(3, 605),(3, 606);

-- 三、参数(幂等) -----------------------------------------------------------
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 5, '预警-时间过半学时进度', 'warn.ratio.mid', '0.3', '年度时间进度达50%时学时进度低于该值记黄色预警'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 5);
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 6, '预警-三季度学时进度', 'warn.ratio.q3', '0.5', '年度时间进度达75%时学时进度低于该值记橙色预警'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 6);
INSERT INTO sys_config (id, config_name, config_key, config_value, remark)
SELECT 7, '预警-年末学时进度', 'warn.ratio.end', '0.7', '年度时间进度达90%时学时进度低于该值记红色预警'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE id = 7);
