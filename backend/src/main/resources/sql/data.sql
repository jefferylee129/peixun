-- =====================================================================
-- 国企培训管理系统 初始化数据(系统基础数据 + 演示业务数据)
-- 默认账号: admin/admin123(超级管理员) tradmin/admin123(培训管理员)
--           zhangsan/user123 等演示员工
-- =====================================================================
USE training_db;

-- 一、部门 -----------------------------------------------------------------
INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, order_num, leader, status) VALUES
(1, 0, '0',   '集团总部',   1, '王总',   '0'),
(2, 1, '0,1', '办公室',     1, '王五',   '0'),
(3, 1, '0,1', '人力资源部', 2, '李培训', '0'),
(4, 1, '0,1', '财务部',     3, '张三',   '0'),
(5, 1, '0,1', '安全生产部', 4, '李四',   '0'),
(6, 0, '0',   '华东分公司', 2, '钱经理', '0'),
(7, 6, '0,6', '运维部',     1, '赵六',   '0'),
(8, 6, '0,6', '营销部',     2, '孙七',   '0');

-- 二、用户(admin/tradmin 密码 admin123, 其余 user123) ------------------------
INSERT INTO sys_user (id, dept_id, username, password, nickname, employee_no, gender, phone, position, user_type, status) VALUES
(1, 3, 'admin',    '$2y$10$kjZHvBhTx6tOMZx0m4QHW.zuhiufABYT.iHllvVXz4WRXIDMh.7JC', '系统管理员', 'EMP001', '0', '13800000001', '系统管理岗', 'manager',      '0'),
(2, 3, 'tradmin',  '$2y$10$kjZHvBhTx6tOMZx0m4QHW.zuhiufABYT.iHllvVXz4WRXIDMh.7JC', '李培训',     'EMP002', '1', '13800000002', '培训管理岗', 'manager',      '0'),
(3, 4, 'zhangsan', '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '张三',       'EMP003', '0', '13800000003', '会计',       'professional', '0'),
(4, 5, 'lisi',     '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '李四',       'EMP004', '0', '13800000004', '安全专工',   'professional', '0'),
(5, 2, 'wangwu',   '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '王五',       'EMP005', '0', '13800000005', '文秘',       'staff',        '0'),
(6, 7, 'zhaoliu',  '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '赵六',       'EMP006', '0', '13800000006', '运维班长',   'professional', '0'),
(7, 8, 'sunqi',    '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '孙七',       'EMP007', '1', '13800000007', '客户经理',   'staff',        '0'),
(8, 4, 'zhouba',   '$2y$10$4Xcz4PLTHJf/yLLzcD/l9u.Xo0rbTBpjo628g0937Pk0ADaJbMKHi', '周八',       'EMP008', '0', '13800000008', '出纳',       'professional', '0');

-- 三、角色 -----------------------------------------------------------------
INSERT INTO sys_role (id, role_name, role_key, order_num, remark) VALUES
(1, '超级管理员', 'admin',          1, '拥有全部权限'),
(2, '培训管理员', 'training_admin', 2, '培训业务全流程管理'),
(3, '普通员工',   'employee',       3, '需求申报、报名、评估、学时查询');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3);

-- 四、菜单 -----------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(1,   0,   '工作台',   'dashboard',      'dashboard/index', 'C', '',                 'Odometer',      1),
(100, 0,   '培训管理', 'training',       '',                'M', '',                 'Collection',    2),
(101, 100, '需求征集', 'need-collection','need/collection', 'C', 'biz:need:list',    'Bell',          1),
(102, 100, '需求申报', 'need',           'need/index',      'C', '',                 'EditPen',       2),
(103, 100, '培训计划', 'plan',           'plan/index',      'C', 'biz:plan:list',    'Calendar',      3),
(104, 100, '培训课程', 'course',         'course/index',    'C', 'biz:course:list',  'Reading',       4),
(105, 100, '培训师资', 'trainer',        'trainer/index',   'C', 'biz:trainer:list', 'Avatar',        5),
(106, 100, '培训班次', 'classes',        'class/index',     'C', 'biz:class:list',   'School',        6),
(107, 100, '外部课程同步', 'integration', 'integration/index', 'C', 'biz:integration:list', 'Connection', 7),
(200, 0,   '学时与评估','hours-eval',    '',                'M', '',                 'DataAnalysis',  3),
(201, 200, '学时台账', 'hours',          'hours/index',     'C', 'biz:hours:list',   'List',          1),
(202, 200, '达标统计', 'hours-stats',    'hours/stats',     'C', 'biz:hours:list',   'TrendCharts',   2),
(203, 200, '评估模板', 'eval-template',  'eval/template',   'C', 'biz:eval:list',    'Document',      3),
(204, 200, '评估汇总', 'eval-record',    'eval/record',     'C', 'biz:eval:list',    'ChatDotRound',  4),
(300, 0,   '统计分析', 'stats',          'stats/index',     'C', 'biz:stats:view',   'PieChart',      4),
(400, 0,   '系统管理', 'system',         '',                'M', '',                 'Setting',       5),
(401, 400, '用户管理', 'system/user',    'system/user',     'C', 'system:user:list', 'User',          1),
(402, 400, '部门管理', 'system/dept',    'system/dept',     'C', 'system:dept:list', 'OfficeBuilding',2),
(403, 400, '角色管理', 'system/role',    'system/role',     'C', 'system:role:list', 'UserFilled',    3),
(404, 400, '菜单管理', 'system/menu',    'system/menu',     'C', 'system:menu:list', 'Menu',          4),
(405, 400, '参数设置', 'system/config',  'system/config',   'C', 'system:config:list','Tools',        5),
(406, 400, '操作日志', 'system/log',     'system/log',      'C', 'system:log:list',  'Tickets',       6),
(500, 0,   '个人事务', 'mine',           '',                'M', '',                 'Suitcase',      6),
(501, 500, '我的班级', 'my-class',       'mine/class',      'C', '',                 'Notebook',      1),
(502, 500, '我的学时', 'my-hours',       'mine/hours',      'C', '',                 'AlarmClock',    2),
-- 按钮
(1101, 101, '需求审核/征集维护', '', '', 'F', 'biz:need:audit',   '', 1),
(1110, 103, '计划新增', '', '', 'F', 'biz:plan:add',     '', 1),
(1111, 103, '计划编辑', '', '', 'F', 'biz:plan:edit',    '', 2),
(1112, 103, '计划删除', '', '', 'F', 'biz:plan:remove',  '', 3),
(1113, 103, '计划审批', '', '', 'F', 'biz:plan:approve', '', 4),
(1120, 104, '课程新增', '', '', 'F', 'biz:course:add',    '', 1),
(1121, 104, '课程编辑', '', '', 'F', 'biz:course:edit',   '', 2),
(1122, 104, '课程删除', '', '', 'F', 'biz:course:remove', '', 3),
(1123, 104, '课程审核', '', '', 'F', 'biz:course:audit',  '', 4),
(1130, 105, '师资新增', '', '', 'F', 'biz:trainer:add',    '', 1),
(1131, 105, '师资编辑', '', '', 'F', 'biz:trainer:edit',   '', 2),
(1132, 105, '师资删除', '', '', 'F', 'biz:trainer:remove', '', 3),
(1140, 106, '班次新增', '', '', 'F', 'biz:class:add',    '', 1),
(1141, 106, '班次编辑', '', '', 'F', 'biz:class:edit',   '', 2),
(1142, 106, '班次删除', '', '', 'F', 'biz:class:remove', '', 3),
(1143, 106, '班次签到', '', '', 'F', 'biz:class:sign',   '', 4),
(1144, 107, '接入配置维护', '', '', 'F', 'biz:integration:edit', '', 1),
(1145, 107, '同步与入库', '', '', 'F', 'biz:integration:sync', '', 2),
(1150, 201, '学时补录', '', '', 'F', 'biz:hours:add',    '', 1),
(1151, 201, '学时认定', '', '', 'F', 'biz:hours:audit',  '', 2),
(1152, 201, '学时删除', '', '', 'F', 'biz:hours:remove', '', 3),
(1160, 203, '模板维护', '', '', 'F', 'biz:eval:template', '', 1),
(1170, 401, '用户新增', '', '', 'F', 'system:user:add',     '', 1),
(1171, 401, '用户编辑', '', '', 'F', 'system:user:edit',    '', 2),
(1172, 401, '用户删除', '', '', 'F', 'system:user:remove',  '', 3),
(1173, 401, '重置密码', '', '', 'F', 'system:user:resetPwd','', 4),
(1180, 402, '部门新增', '', '', 'F', 'system:dept:add',    '', 1),
(1181, 402, '部门编辑', '', '', 'F', 'system:dept:edit',   '', 2),
(1182, 402, '部门删除', '', '', 'F', 'system:dept:remove', '', 3),
(1190, 403, '角色新增', '', '', 'F', 'system:role:add',    '', 1),
(1191, 403, '角色编辑', '', '', 'F', 'system:role:edit',   '', 2),
(1192, 403, '角色删除', '', '', 'F', 'system:role:remove', '', 3),
(1200, 404, '菜单新增', '', '', 'F', 'system:menu:add',    '', 1),
(1201, 404, '菜单编辑', '', '', 'F', 'system:menu:edit',   '', 2),
(1202, 404, '菜单删除', '', '', 'F', 'system:menu:remove', '', 3),
(1210, 405, '参数新增', '', '', 'F', 'system:config:add',    '', 1),
(1211, 405, '参数编辑', '', '', 'F', 'system:config:edit',   '', 2),
(1212, 405, '参数删除', '', '', 'F', 'system:config:remove', '', 3);

-- 五、角色-菜单(training_admin 全业务; employee 申报+个人事务) -----------------
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1),(2, 100),(2, 101),(2, 102),(2, 103),(2, 104),(2, 105),(2, 106),
(2, 200),(2, 201),(2, 202),(2, 203),(2, 204),(2, 300),(2, 500),(2, 501),(2, 502),
(2, 1101),(2, 1110),(2, 1111),(2, 1112),(2, 1113),(2, 1120),(2, 1121),(2, 1122),(2, 1123),
(2, 1130),(2, 1131),(2, 1132),(2, 1140),(2, 1141),(2, 1142),(2, 1143),
(2, 107),(2, 1144),(2, 1145),
(2, 1150),(2, 1151),(2, 1152),(2, 1160),
(3, 1),(3, 102),(3, 500),(3, 501),(3, 502);

-- 六、系统参数(学时要求参考《干部教育培训工作条例》, 可按企业制度调整) --------
INSERT INTO sys_config (id, config_name, config_key, config_value, remark) VALUES
(1, '管理干部年度学时要求',     'hour.required.manager',      '110', '管理干部每年培训学时下限'),
(2, '专业技术人员年度学时要求', 'hour.required.professional', '90',  '专业技术人员每年培训学时下限'),
(3, '一般员工年度学时要求',     'hour.required.staff',        '90',  '一般员工每年培训学时下限'),
(4, '迟到学时折算系数',         'hour.factor.late',           '0.5', '迟到按节学时×系数折算');

-- 七、演示业务数据 -----------------------------------------------------------
-- 需求征集
INSERT INTO biz_need_collection (id, title, year, start_time, end_time, description, status) VALUES
(1, '2026年度培训需求征集', 2026, '2025-11-01 00:00:00', '2025-12-31 23:59:59', '请各部门结合年度重点工作与员工能力短板, 认真填报培训需求。', '1');

-- 培训需求
INSERT INTO biz_need (id, year, dept_id, user_id, collection_id, title, need_type, content, reason, expected_time, participants, status, audit_user_id, audit_time, audit_remark) VALUES
(1, 2026, 4, 3, 1, '业财融合与财务数字化转型培训', 'skill',   '开展业财融合、财务共享中心运营及数字化工具应用培训。', '财务部数字化转型需要', '2026-06', 20, '1', NULL, NULL, NULL),
(2, 2026, 5, 4, 1, '新版安全生产法规解读培训',     'safety',  '解读新修订安全生产法规, 结合事故案例开展警示教育。',   '法规更新后全员贯标需要', '2026-03', 40, '2', 2, '2026-01-05 10:00:00', '已纳入2026年度安全生产培训计划'),
(3, 2026, 2, 5, NULL, '公文写作与行政事务能力提升', 'general', '公文规范、会议纪要、督查督办与沟通协调能力培训。',     '办公室新入职人员较多', '2026-09', 15, '0', NULL, NULL, NULL);

-- 培训计划
INSERT INTO biz_plan (id, plan_name, year, plan_type, dept_id, start_date, end_date, target_users, content, budget, status, submit_time, approve_user_id, approve_time, approve_remark) VALUES
(1, '2026年度安全生产培训计划', 2026, 'year',    5, '2026-03-01', '2026-11-30', '安全管理人员、特种作业人员', '分两期开展安全生产法律法规解读与实操培训, 全员持证上岗。', 25.00, '2', '2026-01-10 09:00:00', 2, '2026-01-15 14:00:00', '同意, 请按期组织实施'),
(2, '2026年新员工入职培训计划', 2026, 'special', 3, '2026-04-01', '2026-04-30', '2026年新入职员工', '企业文化、规章制度、岗位基础技能集中培训。', 8.00, '1', '2026-02-01 09:00:00', NULL, NULL, NULL);

-- 师资
INSERT INTO biz_trainer (id, trainer_name, gender, phone, trainer_type, org_name, title, specialty, level, intro, status) VALUES
(1, '陈建国', '0', '13900000001', 'internal', NULL,               '教授级高级工程师', '管理科学', '高级讲师', '从事企业管理与人才发展工作20余年。', '0'),
(2, '刘志安', '0', '13900000002', 'internal', NULL,               '注册安全工程师',   '安全工程', '中级讲师', '集团安全生产部资深专家, 多次主讲安全培训。', '0'),
(3, '吴慧敏', '1', '13900000003', 'internal', NULL,               '高级会计师',       '财务管理', '中级讲师', '财务部业务骨干, 擅长业财融合与报表分析。', '0'),
(4, '郑拓',   '0', '13900000004', 'external', '省电力培训中心',   '高级培训师',       '电力安全', '特邀专家', '省电力培训中心特聘讲师, 实操经验丰富。', '0');

-- 课程
INSERT INTO biz_course (id, course_code, course_name, category, course_type, hours, level, target_users, outline, trainer_id, status, audit_user_id, audit_time, audit_remark) VALUES
(1, 'KC-AQ-001', '安全生产法律法规解读',           'safety', 'internal', 8.0,  '中级', '安全管理人员、特种作业人员', '新安法解读; 事故案例分析; 双重预防机制建设。', 2, '2', 2, '2026-01-20 10:00:00', '同意发布'),
(2, 'KC-GL-002', '管理者的团队领导力',             'mgmt',   'internal', 12.0, '高级', '中层管理人员', '目标管理; 团队激励; 跨部门协同。', 1, '2', 2, '2026-01-20 10:05:00', '同意发布'),
(3, 'KC-CW-003', '财务报表分析与业财融合',         'skill',  'internal', 12.0, '中级', '财务及业务骨干', '三大报表解读; 业财融合方法论; 数字化工具实操。', 3, '2', 2, '2026-01-20 10:10:00', '同意发布'),
(4, 'KC-XY-004', '新员工企业文化导入',             'general','internal', 6.0,  '初级', '新入职员工', '企业发展史; 企业文化; 员工行为规范。', 1, '2', 2, '2026-01-20 10:15:00', '同意发布'),
(5, 'KC-AQ-005', '高处作业与临时用电安全实操',     'safety', 'external', 16.0, '高级', '特种作业人员', '高处作业规范; 临时用电安全; 实操演练与考核。', 4, '2', 2, '2026-01-20 10:20:00', '同意发布');

-- 班次
INSERT INTO biz_class (id, class_name, plan_id, class_type, dept_id, start_date, end_date, location, capacity, budget, target_users, description, status) VALUES
(1, '2026年第一期安全生产培训班', 1, 'internal', 5, '2026-03-10', '2026-03-11', '集团培训中心三楼教室', 40, 3.50, '安全管理人员、特种作业人员', '法规解读+安全实操, 结业考核。', '3'),
(2, '2026年第二期安全生产培训班', 1, 'internal', 5, '2026-10-20', '2026-11-05', '集团培训中心三楼教室', 40, 3.80, '安全管理人员、特种作业人员', '第一期滚动复训班。', '2'),
(3, '业财融合专题培训班',         NULL, 'internal', 3, '2026-11-10', '2026-11-10', '集团二号会议室', 30, 2.00, '财务及业务骨干', '结合需求申报开设, 报名中。', '1');

-- 课表(节次)
INSERT INTO biz_class_schedule (id, class_id, course_id, trainer_id, teach_date, start_time, end_time, hours, location, status) VALUES
(1, 1, 1, 2, '2026-03-10', '09:00:00', '12:00:00', 3.0, '集团培训中心三楼教室', '1'),
(2, 1, 1, 2, '2026-03-10', '14:00:00', '17:00:00', 3.0, '集团培训中心三楼教室', '1'),
(3, 1, 5, 4, '2026-03-11', '09:00:00', '12:00:00', 4.0, '实训基地', '1'),
(4, 2, 1, 2, '2026-10-20', '09:00:00', '12:00:00', 3.0, '集团培训中心三楼教室', '1'),
(5, 2, 5, 4, '2026-10-21', '09:00:00', '12:00:00', 4.0, '实训基地', '1'),
(6, 2, 1, 2, '2026-11-04', '09:00:00', '12:00:00', 3.0, '集团培训中心三楼教室', '0'),
(7, 2, 5, 4, '2026-11-05', '09:00:00', '16:00:00', 6.0, '实训基地', '0'),
(8, 3, 3, 3, '2026-11-10', '09:00:00', '12:00:00', 3.0, '集团二号会议室', '0'),
(9, 3, 3, 3, '2026-11-10', '14:00:00', '17:00:00', 3.0, '集团二号会议室', '0');

-- 班级学员(累计学时与签到勾稽)
INSERT INTO biz_class_student (id, class_id, user_id, enroll_time, status, total_hours) VALUES
(1, 1, 3, '2026-02-20 09:00:00', '2', 10.0),
(2, 1, 4, '2026-02-20 09:00:00', '2', 10.0),
(3, 1, 5, '2026-02-20 09:00:00', '2', 6.0),
(4, 1, 6, '2026-02-20 09:00:00', '2', 10.0),
(5, 1, 7, '2026-02-21 09:00:00', '2', 8.5),
(6, 1, 8, '2026-02-21 09:00:00', '2', 6.0),
(7, 2, 4, '2026-10-08 09:00:00', '0', 7.0),
(8, 2, 6, '2026-10-08 09:00:00', '0', 5.5),
(9, 3, 3, '2026-10-25 09:00:00', '0', 0.0);

-- 签到记录
INSERT INTO biz_attendance (schedule_id, class_id, user_id, sign_type, sign_time, hours_obtained) VALUES
(1, 1, 3, 'normal', '2026-03-10 08:55:00', 3.0),
(1, 1, 4, 'normal', '2026-03-10 08:56:00', 3.0),
(1, 1, 5, 'normal', '2026-03-10 08:50:00', 3.0),
(1, 1, 6, 'normal', '2026-03-10 08:58:00', 3.0),
(1, 1, 7, 'late',   '2026-03-10 09:20:00', 1.5),
(1, 1, 8, 'normal', '2026-03-10 08:52:00', 3.0),
(2, 1, 3, 'normal', '2026-03-10 13:55:00', 3.0),
(2, 1, 4, 'normal', '2026-03-10 13:56:00', 3.0),
(2, 1, 5, 'normal', '2026-03-10 13:53:00', 3.0),
(2, 1, 6, 'normal', '2026-03-10 13:57:00', 3.0),
(2, 1, 7, 'normal', '2026-03-10 13:54:00', 3.0),
(2, 1, 8, 'normal', '2026-03-10 13:52:00', 3.0),
(3, 1, 3, 'normal', '2026-03-11 08:56:00', 4.0),
(3, 1, 4, 'normal', '2026-03-11 08:57:00', 4.0),
(3, 1, 5, 'leave',  '2026-03-11 09:00:00', 0.0),
(3, 1, 6, 'normal', '2026-03-11 08:55:00', 4.0),
(3, 1, 7, 'normal', '2026-03-11 08:58:00', 4.0),
(3, 1, 8, 'absent', '2026-03-11 09:00:00', 0.0),
(4, 2, 4, 'normal', '2026-10-20 08:56:00', 3.0),
(4, 2, 6, 'late',   '2026-10-20 09:15:00', 1.5),
(5, 2, 4, 'normal', '2026-10-21 08:56:00', 4.0),
(5, 2, 6, 'normal', '2026-10-21 08:57:00', 4.0);

-- 学时台账(签到自动, 已认定)
INSERT INTO biz_hour_record (user_id, year, class_id, schedule_id, course_id, source, hours, status, record_date, remark) VALUES
(3, 2026, 1, 1, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(3, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(3, 2026, 1, 3, 5, 1, 4.0, '1', '2026-03-11', '签到自动生成'),
(4, 2026, 1, 1, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(4, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(4, 2026, 1, 3, 5, 1, 4.0, '1', '2026-03-11', '签到自动生成'),
(4, 2026, 2, 4, 1, 1, 3.0, '1', '2026-10-20', '签到自动生成'),
(4, 2026, 2, 5, 5, 1, 4.0, '1', '2026-10-21', '签到自动生成'),
(5, 2026, 1, 1, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(5, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(6, 2026, 1, 1, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(6, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(6, 2026, 1, 3, 5, 1, 4.0, '1', '2026-03-11', '签到自动生成'),
(6, 2026, 2, 4, 1, 1, 1.5, '1', '2026-10-20', '签到自动生成'),
(6, 2026, 2, 5, 5, 1, 4.0, '1', '2026-10-21', '签到自动生成'),
(7, 2026, 1, 1, 1, 1, 1.5, '1', '2026-03-10', '签到自动生成(迟到折算)'),
(7, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(7, 2026, 1, 3, 5, 1, 4.0, '1', '2026-03-11', '签到自动生成'),
(8, 2026, 1, 1, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成'),
(8, 2026, 1, 2, 1, 1, 3.0, '1', '2026-03-10', '签到自动生成');

-- 补录与外部认定
INSERT INTO biz_hour_record (user_id, year, class_id, schedule_id, course_id, source, hours, status, record_date, remark) VALUES
(3, 2026, NULL, NULL, NULL, 2, 8.0, '0', '2026-09-15', '注册会计师继续教育学时补录'),
(5, 2026, NULL, NULL, NULL, 3, 6.0, '1', '2026-06-20', '集团党校轮训学时认定');

-- 评估模板(柯氏四级)
INSERT INTO biz_eval_template (id, template_name, eval_level, items, status, description) VALUES
(1, '培训班次一级评估(反应层)通用问卷', 1,
 '[{"key":"q1","title":"课程内容实用性","type":"score","required":true},{"key":"q2","title":"讲师授课水平","type":"score","required":true},{"key":"q3","title":"课程组织与安排","type":"score","required":true},{"key":"q4","title":"培训收获与岗位相关性","type":"score","required":true},{"key":"t1","title":"您对本次培训的意见建议","type":"text","required":false}]',
 '0', '适用于全部班次结业时的满意度评估'),
(2, '管理能力培训三级评估(行为层)问卷', 3,
 '[{"key":"q1","title":"培训后工作行为改善程度","type":"score","required":true},{"key":"t1","title":"上级观察反馈","type":"text","required":false}]',
 '0', '培训结束3个月后, 由直接上级参与评价');

-- 评估记录(第一期班)
INSERT INTO biz_eval_record (template_id, class_id, user_id, scores, total_score, comment, submit_time) VALUES
(1, 1, 3, '{"q1":5,"q2":4,"q3":4,"q4":5,"t1":"希望增加实操演练时间"}', 18.0, '组织有序, 收获很大。', '2026-03-12 09:00:00'),
(1, 1, 4, '{"q1":4,"q2":4,"q3":5,"q4":4}', 17.0, NULL, '2026-03-12 09:05:00'),
(1, 1, 6, '{"q1":4,"q2":5,"q3":4,"q4":4,"t1":"建议增加事故案例分析"}', 17.0, '建议缩短理论课时。', '2026-03-12 09:10:00');

-- 八、行业对标优化菜单(档案/消息/证书/考试) ---------------------------------
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

INSERT INTO sys_config (id, config_name, config_key, config_value, remark) VALUES
(5, '预警-时间过半学时进度', 'warn.ratio.mid', '0.3', '年度时间进度达50%时学时进度低于该值记黄色预警'),
(6, '预警-三季度学时进度', 'warn.ratio.q3', '0.5', '年度时间进度达75%时学时进度低于该值记橙色预警'),
(7, '预警-年末学时进度', 'warn.ratio.end', '0.7', '年度时间进度达90%时学时进度低于该值记红色预警');

-- 九、v1.5 行业对标优化菜单(学习地图/证书查验) -----------------------------
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, perms, icon, order_num) VALUES
(607, 100, '学习地图配置', 'learning-map', 'map/index',  'C', 'biz:map:list', 'Guide',   11),
(608, 500, '我的学习地图', 'my-map',        'map/my',     'C', '',             'Compass', 6),
(609, 500, '证书查验',     'cert-verify',   'cert/verify','C', '',             'Search',  7),
(1340, 607, '学习地图维护', '', '', 'F', 'biz:map:edit', '', 1);

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 607),(2, 608),(2, 609),(2, 1340),
(3, 608),(3, 609);

INSERT INTO sys_config (id, config_name, config_key, config_value, remark) VALUES
(8,  '定时自动提醒开关',   'notify.enabled',          '1', '每日自动扫描开班提醒/评估催办/学时预警; 0 关闭'),
(9,  '开班提醒提前天数',   'notify.class.remind.days','3', '班次开始前 N 天提醒未参训学员'),
(10, '评估催办触发天数',   'notify.eval.remind.days', '3', '结班满 N 天仍未评估的学员自动催办');

-- 十、结业证书示例(班次1已结业; 防伪码仅示例, 正式环境由发证时随机生成) ------------
INSERT INTO biz_certificate (id, cert_no, user_id, class_id, hours, grade, verify_code, issue_date) VALUES
(1, 'QGPX-2026-000001', 3, 1, 10.0, '合格', '315728', '2026-03-12'),
(2, 'QGPX-2026-000002', 4, 1, 10.0, '合格', '842061', '2026-03-12'),
(3, 'QGPX-2026-000003', 6, 1, 10.0, '合格', '597304', '2026-03-12');
