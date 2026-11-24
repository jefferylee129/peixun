# 国企培训管理系统(Training Management System)

基于 **Spring Boot 3 + MyBatis-Plus + Vue 3 + Element Plus** 的企业培训全流程管理系统,围绕"**需求征集 → 计划编制 → 课程/师资 → 开班实施 → 签到计学时 → 评估改进**"的 PDCA 闭环设计,并按国企管理特点内置**学时达标管理、分级审批、操作留痕审计**等能力。

## 功能模块

| 模块 | 功能 |
| --- | --- |
| 工作台 | 统计卡片、月度学时趋势、课程分布、部门学时、我的学时进度 |
| 培训需求 | 年度需求征集任务、员工需求申报、需求审核采纳/驳回、需求台账、Excel 导入导出 |
| 培训计划 | 年度/季度/月度/专项计划编制、提交审批、批准下达、驳回重报、Excel 导入导出 |
| 培训课程 | 课程库(分类/形式/学时/大纲/讲师)、起草-审核-发布-下架状态流、Excel 导入导出 |
| 培训师资 | 内部讲师/外部专家、讲师等级、授课记录与累计学时统计、Excel 导入导出 |
| 培训班次 | 开班、发布报名、自主报名/指定学员、排课(课表)、状态流转、班次/学员/课表 Excel 导入导出 |
| 签到与学时 | 按节次签到(正常/迟到/请假/缺勤)、结课自动折算学时生成台账、手工补录、外部认定、学时认定流、Excel 批量导入导出 |
| 学时达标 | 按人员类型设定年度学时要求(默认管理干部110/其他90,可配置)、达标率统计、Excel 台账导出 |
| 培训评估 | 柯氏四级评估模板(问卷条目可配)、学员在线评分、逐项均分/总体均分/开放意见汇总、双 Sheet Excel 导出 |
| 在线考试 | 题库(单选/多选/判断)、挂班次组卷、限时作答自动判分、及格计学时、成绩单与档案聚合(柯氏二级落地) |
| 结业证书 | 结班自动对已完成学员发证、编号 QGPX-YYYY-XXXXXX、证书预览/打印、班次幂等补发 |
| 培训档案 | 一人一档:基本信息+年度学时来源构成+参训履历时间线+评估/证书/考试成绩聚合,员工查自己、管理员查全员 |
| 消息与预警 | 站内消息中心(顶栏未读红点)、开班/评估催办/证书/考试/公告自动触达,年度学时分级预警(黄/橙/红)与一键推送 |
| 岗位学习地图 | 按人员类型配置年度必修课程、我的必修清单与完成率、全员必修进度卡 |
| 经费预算执行 | 计划预算 vs 班次经费、执行率、按部门/计划拆分、超预算标识 |
| 自动提醒 | 每日自动扫描开班提醒/评估催办/学时预警,去重窗口防打扰,可手动触发 |
| 自助签到 | 课节 6 位签到码,员工输码自助签到,防代签/防篡改,进入既有学时链路 |
| 证书查验与档案导出 | 编号+防伪码双要素公开核验(脱敏+限流),一人一档 Excel 归档导出 |
| 统计分析 | 覆盖率、人均学时、计划完成率、班次/计划/需求/课程多维图表 |
| 系统管理 | 用户/部门/角色/菜单权限(RBAC)、参数配置、操作日志审计、用户 Excel 导入导出 |

> Excel 规范: 列表页均提供「导出Excel」与「批量导入」(含模板下载);导入逐行校验并反馈成功/失败/跳过原因,重复数据自动幂等跳过;导入模板在 `导入对话框` 内可直接下载。 |
>
> 行业对标优化模块(档案/消息预警/证书/考试)数据库增量见 `sql/upgrade-optimize.sql`(执行时 mysql 客户端需加 `--default-character-set=utf8mb4`)。
> v1.5 增量(学习地图/自动提醒/签到码/防伪码)见 `sql/upgrade-v1.5.sql`;版本演进详见 [docs/更新日志.md](docs/更新日志.md)。

## 技术栈

- 后端:JDK 17+ / Spring Boot 3.3 / MyBatis-Plus 3.5 / JWT(jjwt) / MySQL 8.0
- 前端:Vue 3.5 / Vite 5 / Element Plus 2.8 / Pinia / Vue Router / ECharts / Axios
- 认证:JWT 无状态登录 + 菜单/按钮级权限注解拦截(`@RequirePerm`)
- 审计:`@OperLog` 切面自动记录操作人/参数/结果到操作日志表

## 目录结构

```
├── backend/                    # 后端(Spring Boot)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/soe/training/
│       │   ├── common/         # 统一返回/分页/异常
│       │   ├── config/         # MP分页、字段填充、CORS、拦截器注册
│       │   ├── security/       # JWT、登录上下文、权限拦截、操作日志切面
│       │   ├── domain/         # 实体 + VO + DTO
│       │   ├── mapper/         # MyBatis-Plus Mapper(含统计SQL)
│       │   ├── service/        # 业务服务
│       │   └── controller/     # REST 接口
│       └── resources/
│           ├── application.yml
│           └── sql/            # schema.sql 建表 + data.sql 初始化演示数据
├── frontend/                   # 前端(Vue3 + Element Plus)
│   └── src/
│       ├── api/                # auth/system/biz/train 四个接口模块
│       ├── layout/             # 侧边栏+顶栏布局
│       ├── views/              # 各业务页面
│       ├── router/ store/ directives/ utils/
├── docs/需求文档.md            # 需求规格说明书(FR 编号/规则/验收/追溯)
│   docs/设计方案.md            # 行业对标与详细设计说明
│   docs/更新日志.md            # 版本演进与功能更新明细
│   docs/测试报告.md            # 各轮测试结论
└── docker-compose.yml          # 一键 MySQL(自动执行初始化脚本)
```

## 外部系统集成(云学堂等)

参考行业主流企业学习平台(云学堂开放平台、UMU 开放 API、企业微信/钉钉生态)对接模式，以及 ETL 暂存审核、幂等同步、审计留痕等企业集成实践，系统提供可配置的外部课程同步能力：

| 能力 | 说明 |
| --- | --- |
| 数据源管理 | 支持云学堂(OAuth2 取令牌+分页拉课程)、通用 REST API(字段映射可配)、演示数据源(离线)三种类型；地址/路径/字段映射/分类映射均可配置，适配不同租约的开放平台文档 |
| 测试连接 | 保存前可一键测试，网络不通/凭证错误时优雅反馈 |
| 同步拉取 | 立即同步进入“待入库”池(不直接写课程库)，分页拉取、增量幂等(来源+外部ID唯一) |
| 审核入库 | 待入库课程逐条或批量确认入库为正式课程(默认已发布、线上形式)，入库时自动做分类映射兑底 |
| 忽略/清理 | 不需要的课程可忽略 |
| 同步日志 | 每次拉取/入库/测试连接均留痕(总数/成功/失败/跳过/错误明细)，满足国企审计要求 |
| 安全 | App Secret 列表与详情脱敏(***)；员工无权限；原始报文留档 |

对接云学堂真实环境：在“新增数据源”中填入贵司开放平台地址、令牌/列表接口路径、App Key/Secret，并按贵司接口文档调整字段映射与分类映射即可；没有租约时用“演示数据源”体验全流程。

## 快速开始

### 1. 准备数据库(两种方式任选)

**方式 A:Docker 一键启动(推荐,自动建表+初始化)**

```bash
docker compose up -d
```

**方式 B:已有 MySQL 8.0,手动执行脚本**

```bash
mysql --default-character-set=utf8mb4 -uroot -p < backend/src/main/resources/sql/schema.sql
mysql --default-character-set=utf8mb4 -uroot -p < backend/src/main/resources/sql/data.sql
```

> 数据库连接参数在 `backend/src/main/resources/application.yml` 中调整(默认 `root/root123@localhost:3306/training_db`)。

### 2. 启动后端(端口 8080)

```bash
cd backend
mvn spring-boot:run
# 或打包运行: mvn -DskipTests package && java -jar target/training-backend-1.0.0.jar
```

### 3. 启动前端(端口 5173)

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>。

### 默认账号

| 角色 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 超级管理员 | `admin` | `admin123` | 全部权限(含系统管理) |
| 培训管理员 | `tradmin` | `admin123` | 培训业务全流程 |
| 普通员工 | `zhangsan`(另有 lisi/wangwu/zhaoliu/sunqi/zhouba) | `user123` | 需求申报、报名、评估、我的学时 |

> 演示数据包含:已结束/进行中/报名中三个班次、课表、签到记录、勾稽一致的学时台账、评估问卷与作答,登录后可直接查看统计效果。**生产使用请先修改默认密码。**

## 生产部署要点

1. 前端 `npm run build` 生成 `dist/`,由 Nginx 托管;
2. 后端 `mvn -DskipTests package` 生成可执行 jar,`java -jar` 部署;
3. Nginx 将 `/api` 反代到后端 8080:

```nginx
server {
    listen 80;
    root /opt/training/frontend-dist;
    index index.html;
    location / { try_files $uri $uri/ /index.html; }
    location /api { proxy_pass http://127.0.0.1:8080; proxy_set_header X-Real-IP $remote_addr; }
}
```

4. 修改 `application.yml` 中 JWT 密钥与数据库口令;按企业制度在【参数设置】中调整年度学时要求与迟到折算系数。

## 权限说明

- 系统采用 RBAC:用户 → 角色 → 菜单/按钮权限,权限标识如 `biz:need:audit`、`system:user:add`;
- 侧边栏按角色下发的菜单动态渲染;按钮通过 `v-perm` 指令控制;接口侧双重校验(拦截器 + `@RequirePerm`);
- 内置三角色:超级管理员、培训管理员、普通员工,可在【角色管理】中扩展并分配菜单权限。

## 扩展建议

- 对接企业微信/钉钉/统一身份认证(SSO/CAS)登录;
- 课件附件上传(oss/minio)、在线考试与题库、培训证书生成与归档;
- 讲师课酬计算、培训经费执行跟踪、消息提醒(开班通知/评估催办);
- 移动端(H5/小程序)签到与学习。

详细设计依据与行业对标见 [docs/设计方案.md](docs/设计方案.md)。
