-- =====================================================================
-- 国企培训管理系统 数据库初始化脚本(MySQL 8.0)
-- 包含: 建库建表 + 系统基础数据 + 演示业务数据
-- 重复执行安全: 全部使用 IF NOT EXISTS / 固定主键
-- =====================================================================

CREATE DATABASE IF NOT EXISTS training_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE training_db;

-- ---------------------------------------------------------------------
-- 一、系统管理表
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_dept (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '部门ID',
  parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
  ancestors   VARCHAR(200) NOT NULL DEFAULT '0' COMMENT '祖级路径',
  dept_name   VARCHAR(50) NOT NULL COMMENT '部门名称',
  order_num   INT DEFAULT 0 COMMENT '显示顺序',
  leader      VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  phone       VARCHAR(20) DEFAULT NULL,
  status      CHAR(1) NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  del_flag    TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_dept_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '部门表';

CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  dept_id     BIGINT DEFAULT NULL COMMENT '部门ID',
  username    VARCHAR(30) NOT NULL COMMENT '登录账号',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  nickname    VARCHAR(30) NOT NULL COMMENT '姓名',
  employee_no VARCHAR(30) DEFAULT NULL COMMENT '工号',
  gender      CHAR(1) DEFAULT '0' COMMENT '0男 1女',
  phone       VARCHAR(20) DEFAULT NULL,
  email       VARCHAR(50) DEFAULT NULL,
  position    VARCHAR(50) DEFAULT NULL COMMENT '岗位',
  user_type   VARCHAR(20) NOT NULL DEFAULT 'staff' COMMENT 'manager管理干部 professional专业技术人员 staff一般员工',
  status      CHAR(1) NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  remark      VARCHAR(500) DEFAULT NULL,
  KEY idx_user_dept (dept_id),
  KEY idx_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name   VARCHAR(30) NOT NULL COMMENT '角色名称',
  role_key    VARCHAR(50) NOT NULL COMMENT '角色标识',
  order_num   INT DEFAULT 0,
  status      CHAR(1) NOT NULL DEFAULT '0',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  remark      VARCHAR(500) DEFAULT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id   BIGINT NOT NULL DEFAULT 0,
  menu_name   VARCHAR(50) NOT NULL COMMENT '菜单/按钮名称',
  path        VARCHAR(100) DEFAULT '' COMMENT '路由地址',
  component   VARCHAR(100) DEFAULT '' COMMENT '前端组件路径',
  menu_type   CHAR(1) NOT NULL DEFAULT 'C' COMMENT 'M目录 C菜单 F按钮',
  perms       VARCHAR(100) DEFAULT '' COMMENT '权限标识',
  icon        VARCHAR(50) DEFAULT '' COMMENT '图标',
  order_num   INT DEFAULT 0,
  visible     CHAR(1) NOT NULL DEFAULT '0' COMMENT '0显示 1隐藏',
  status      CHAR(1) NOT NULL DEFAULT '0',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_menu_parent (parent_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_config (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_name  VARCHAR(50) NOT NULL COMMENT '参数名称',
  config_key   VARCHAR(50) NOT NULL COMMENT '参数键名',
  config_value VARCHAR(200) DEFAULT '' COMMENT '参数值',
  remark       VARCHAR(500) DEFAULT NULL,
  del_flag     TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统参数表';

CREATE TABLE IF NOT EXISTS sys_oper_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT DEFAULT NULL,
  username    VARCHAR(30) DEFAULT NULL,
  module      VARCHAR(50) DEFAULT NULL COMMENT '业务模块',
  operation   VARCHAR(100) DEFAULT NULL COMMENT '操作内容',
  method      VARCHAR(200) DEFAULT NULL COMMENT '方法',
  params      TEXT COMMENT '请求参数',
  ip          VARCHAR(50) DEFAULT NULL,
  status      CHAR(1) DEFAULT '0' COMMENT '0成功 1失败',
  error_msg   VARCHAR(500) DEFAULT NULL,
  oper_time   DATETIME DEFAULT NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_log_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ---------------------------------------------------------------------
-- 二、培训业务表
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS biz_need_collection (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(100) NOT NULL COMMENT '征集标题',
  year        INT NOT NULL COMMENT '年度',
  start_time  DATETIME DEFAULT NULL,
  end_time    DATETIME DEFAULT NULL,
  description VARCHAR(1000) DEFAULT NULL,
  status      VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0进行中 1已结束',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训需求征集任务表';

CREATE TABLE IF NOT EXISTS biz_need (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  year          INT NOT NULL COMMENT '年度',
  dept_id       BIGINT DEFAULT NULL COMMENT '申报部门',
  user_id       BIGINT NOT NULL COMMENT '申报人',
  collection_id BIGINT DEFAULT NULL COMMENT '关联征集任务',
  title         VARCHAR(200) NOT NULL COMMENT '需求标题',
  need_type     VARCHAR(20) NOT NULL DEFAULT 'skill' COMMENT 'party党性教育 mgmt管理能力 skill岗位技能 safety安全合规 general通用素质',
  content       TEXT COMMENT '需求描述',
  reason        TEXT COMMENT '申报理由',
  expected_time VARCHAR(20) DEFAULT NULL COMMENT '期望培训时间 如2026-06',
  participants  INT DEFAULT NULL COMMENT '建议参训人数',
  status        VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0草稿 1待审核 2已采纳 3已驳回',
  audit_user_id BIGINT DEFAULT NULL,
  audit_time    DATETIME DEFAULT NULL,
  audit_remark  VARCHAR(500) DEFAULT NULL,
  del_flag      TINYINT NOT NULL DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_need_year (year),
  KEY idx_need_user (user_id),
  KEY idx_need_dept (dept_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训需求表';

CREATE TABLE IF NOT EXISTS biz_plan (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  plan_name      VARCHAR(200) NOT NULL COMMENT '计划名称',
  year           INT NOT NULL,
  plan_type      VARCHAR(20) NOT NULL DEFAULT 'year' COMMENT 'year年度 quarter季度 month月度 special专项',
  dept_id        BIGINT DEFAULT NULL COMMENT '主办部门',
  start_date     DATE DEFAULT NULL,
  end_date       DATE DEFAULT NULL,
  target_users   VARCHAR(200) DEFAULT NULL COMMENT '培训对象',
  content        TEXT COMMENT '计划内容',
  budget         DECIMAL(12, 2) DEFAULT NULL COMMENT '预算(万元)',
  status         VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0草稿 1待审批 2已批准 3已驳回',
  submit_time    DATETIME DEFAULT NULL,
  approve_user_id BIGINT DEFAULT NULL,
  approve_time   DATETIME DEFAULT NULL,
  approve_remark VARCHAR(500) DEFAULT NULL,
  del_flag       TINYINT NOT NULL DEFAULT 0,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_plan_year (year)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训计划表';

CREATE TABLE IF NOT EXISTS biz_course (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_code   VARCHAR(30) DEFAULT NULL COMMENT '课程编码',
  course_name   VARCHAR(200) NOT NULL,
  category      VARCHAR(20) NOT NULL DEFAULT 'general' COMMENT 'party党性教育 mgmt管理能力 skill岗位技能 safety安全合规 general通用素质',
  course_type   VARCHAR(20) NOT NULL DEFAULT 'internal' COMMENT 'internal内训 external外训 online线上',
  hours         DECIMAL(6, 1) NOT NULL DEFAULT 0 COMMENT '标准学时',
  level         VARCHAR(20) DEFAULT NULL COMMENT '初级/中级/高级',
  target_users  VARCHAR(200) DEFAULT NULL,
  outline       TEXT COMMENT '课程大纲',
  trainer_id    BIGINT DEFAULT NULL COMMENT '主讲讲师',
  status        VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0草稿 1待审核 2已发布 3已下架',
  source_type   TINYINT NOT NULL DEFAULT 0 COMMENT '0自建 1外部同步',
  source_id     BIGINT DEFAULT NULL COMMENT '来源外部系统ID',
  external_code VARCHAR(100) DEFAULT NULL COMMENT '外部课程唯一编码',
  external_url  VARCHAR(500) DEFAULT NULL COMMENT '外部学习链接',
  audit_user_id BIGINT DEFAULT NULL,
  audit_time    DATETIME DEFAULT NULL,
  audit_remark  VARCHAR(500) DEFAULT NULL,
  del_flag      TINYINT NOT NULL DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_course_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训课程表';

CREATE TABLE IF NOT EXISTS biz_trainer (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  trainer_name VARCHAR(30) NOT NULL,
  gender      CHAR(1) DEFAULT '0',
  phone       VARCHAR(20) DEFAULT NULL,
  email       VARCHAR(50) DEFAULT NULL,
  trainer_type VARCHAR(20) NOT NULL DEFAULT 'internal' COMMENT 'internal内部讲师 external外部专家',
  org_name    VARCHAR(100) DEFAULT NULL COMMENT '所在单位',
  title       VARCHAR(50) DEFAULT NULL COMMENT '职称/职务',
  specialty   VARCHAR(200) DEFAULT NULL COMMENT '专业领域',
  level       VARCHAR(20) DEFAULT NULL COMMENT '初级讲师/中级讲师/高级讲师/特邀专家',
  intro       TEXT COMMENT '师资简介',
  status      VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训师资表';

CREATE TABLE IF NOT EXISTS biz_class (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_name  VARCHAR(200) NOT NULL COMMENT '班次名称',
  plan_id     BIGINT DEFAULT NULL COMMENT '关联培训计划',
  class_type  VARCHAR(20) NOT NULL DEFAULT 'internal' COMMENT 'internal内训 external外训 online线上 mixed混合',
  dept_id     BIGINT DEFAULT NULL COMMENT '主办部门',
  start_date  DATE DEFAULT NULL,
  end_date    DATE DEFAULT NULL,
  location    VARCHAR(200) DEFAULT NULL COMMENT '培训地点',
  capacity    INT DEFAULT NULL COMMENT '人数上限',
  budget      DECIMAL(12, 2) DEFAULT NULL COMMENT '培训经费(万元)',
  target_users VARCHAR(200) DEFAULT NULL,
  description TEXT,
  status      VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0筹备中 1报名中 2进行中 3已结束 4已取消',
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_class_status (status),
  KEY idx_class_plan (plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训班次表';

CREATE TABLE IF NOT EXISTS biz_class_student (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id    BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  enroll_time DATETIME DEFAULT NULL COMMENT '报名时间',
  status      VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0已报名 2已完成 3缺勤',
  total_hours DECIMAL(6, 1) DEFAULT 0 COMMENT '本班累计学时',
  remark      VARCHAR(500) DEFAULT NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_cs_class (class_id),
  KEY idx_cs_user (user_id),
  UNIQUE KEY uk_cs_class_user (class_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '班级学员表';

CREATE TABLE IF NOT EXISTS biz_class_schedule (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id    BIGINT NOT NULL,
  course_id   BIGINT DEFAULT NULL,
  trainer_id  BIGINT DEFAULT NULL COMMENT '授课讲师',
  teach_date  DATE DEFAULT NULL,
  start_time  TIME DEFAULT NULL,
  end_time    TIME DEFAULT NULL,
  hours       DECIMAL(6, 1) NOT NULL DEFAULT 0 COMMENT '本节学时',
  location    VARCHAR(200) DEFAULT NULL,
  sign_code   VARCHAR(8) DEFAULT NULL COMMENT '自助签到码(6位)',
  status      VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0待上课 1已完成 2已取消',
  remark      VARCHAR(500) DEFAULT NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sche_class (class_id),
  KEY idx_sche_trainer (trainer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '课程安排(课表)表';

CREATE TABLE IF NOT EXISTS biz_attendance (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  schedule_id    BIGINT NOT NULL,
  class_id       BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  sign_type      VARCHAR(10) NOT NULL DEFAULT 'normal' COMMENT 'normal正常 late迟到 leave请假 absent缺勤',
  sign_time      DATETIME DEFAULT NULL,
  hours_obtained DECIMAL(6, 1) DEFAULT 0 COMMENT '本节获得学时',
  remark         VARCHAR(500) DEFAULT NULL,
  del_flag       TINYINT NOT NULL DEFAULT 0,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_att_schedule (schedule_id),
  KEY idx_att_user (user_id),
  UNIQUE KEY uk_att_schedule_user (schedule_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '考勤签到表';

CREATE TABLE IF NOT EXISTS biz_hour_record (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NOT NULL COMMENT '学时归属人',
  year         INT NOT NULL COMMENT '归属年度',
  class_id     BIGINT DEFAULT NULL,
  schedule_id  BIGINT DEFAULT NULL,
  course_id    BIGINT DEFAULT NULL,
  source       INT NOT NULL DEFAULT 1 COMMENT '1签到自动 2手工补录 3外部认定 4线上学习',
  hours        DECIMAL(6, 1) NOT NULL DEFAULT 0,
  status       VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0待认定 1已认定 2已驳回',
  record_date  DATE DEFAULT NULL COMMENT '学时日期',
  audit_user_id BIGINT DEFAULT NULL,
  audit_time   DATETIME DEFAULT NULL,
  audit_remark VARCHAR(500) DEFAULT NULL,
  remark       VARCHAR(500) DEFAULT NULL,
  del_flag     TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_hour_user (user_id, year),
  KEY idx_hour_class (class_id),
  UNIQUE KEY uk_hour_schedule_user (schedule_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '培训学时台账表';

CREATE TABLE IF NOT EXISTS biz_eval_template (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_name VARCHAR(100) NOT NULL,
  eval_level   INT NOT NULL DEFAULT 1 COMMENT '1一级反应层 2二级学习层 3三级行为层 4四级结果层(柯氏四级)',
  items        TEXT NOT NULL COMMENT '评估条目JSON数组',
  status       VARCHAR(4) NOT NULL DEFAULT '0' COMMENT '0启用 1停用',
  description  VARCHAR(500) DEFAULT NULL,
  del_flag     TINYINT NOT NULL DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评估问卷模板表';

CREATE TABLE IF NOT EXISTS biz_eval_record (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  class_id    BIGINT NOT NULL,
  user_id     BIGINT NOT NULL COMMENT '参评人',
  scores      TEXT COMMENT '作答JSON对象',
  total_score DECIMAL(8, 1) DEFAULT NULL COMMENT '打分项合计',
  comment     TEXT COMMENT '总体评语',
  submit_time DATETIME DEFAULT NULL,
  del_flag    TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_eval_class (class_id),
  KEY idx_eval_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评估记录表';

-- =====================================================================
-- 行业对标优化: 消息 / 证书 / 题库 / 考试
-- =====================================================================
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
  verify_code VARCHAR(8) DEFAULT NULL COMMENT '防伪码(6位), 公开查验双要素',
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

-- =====================================================================
-- v1.5 行业对标优化: 岗位学习地图 / 自动提醒留痕
-- =====================================================================
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

CREATE TABLE IF NOT EXISTS biz_notify_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  notify_type VARCHAR(30) NOT NULL COMMENT 'classStart/evalPending/hourWarn',
  biz_key     VARCHAR(120) NOT NULL COMMENT '去重键, 如 class:12:user:3',
  user_id     BIGINT NOT NULL,
  sent_time   DATETIME NOT NULL,
  KEY idx_notify_key (notify_type, biz_key),
  KEY idx_notify_time (sent_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自动提醒发送留痕';
