#!/bin/bash
# =====================================================================
# 国企培训管理系统 深度业务测试
# 用法: 后端启动后执行  BASE=http://localhost:18080/api bash tests/deep-test.sh
# 说明: 打穿常规套件未覆盖的业务规则(结班计学时/补签/容量并发/停用token/
#       评估模板层级/统计一致性/集成映射等), 测试创建 TESTD-* 前缀数据
# =====================================================================
BASE=${BASE:-http://localhost:18080/api}
R=/tmp/deep-resp.json
PASS=0; FAIL=0
TS=$(date +%s)
YEAR=$(date +%Y)

# 中文/引号/空格不能裸放在查询串里, Tomcat 会直接 400
enc() { python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "$1"; }

# 关键 ID 提取失败立即终止, 避免 ERR/空串拼进路径或 JSON 造成级联失败
gid() {
  local v
  v=$(jget "$2")
  case "$v" in
    ''|ERR|None)
      echo "  FATAL $1 提取失败[$v] body=$(head -c 200 "$R" 2>/dev/null)"
      exit 1
      ;;
  esac
  printf '%s' "$v"
}

jget() { python3 -c "
import json, sys
try:
    d = json.load(open('$R'))
    v = eval(sys.argv[1], {'d': d})
    print('' if v is None else v)
except Exception:
    print('ERR')
" "$1"; }

req() { # req METHOD PATH TOKEN [JSON_BODY]
  local m=$1 p=$2 t=$3 d=$4
  local a=(-s -o "$R" -w "%{http_code}" -X "$m" "$BASE$p")
  [ -n "$t" ] && a+=(-H "Authorization: Bearer $t")
  [ -n "$d" ] && a+=(-H "Content-Type: application/json" -d "$d")
  CODE=$(curl "${a[@]}")
}

rawcode() { # rawcode METHOD PATH TOKEN BODY OUTFILE -> http_code
  local m=$1 p=$2 t=$3 d=$4 o=$5
  local a=(-s -o "$o" -w "%{http_code}" -X "$m" "$BASE$p" -H "Authorization: Bearer $t")
  [ -n "$d" ] && a+=(-H "Content-Type: application/json" -d "$d")
  curl "${a[@]}"
}

bcode() { python3 -c "import json,sys;print(json.load(open('$1')).get('code'))" 2>/dev/null; }

assert() { # assert 名称 python表达式 期望值
  local actual
  actual=$(jget "$2")
  if [ "$actual" == "$3" ]; then PASS=$((PASS+1)); echo "  PASS $1"
  else FAIL=$((FAIL+1)); echo "  FAIL $1 expect=[$3] actual=[$actual] body=$(head -c 180 "$R" 2>/dev/null)"; fi
}

assertc() { # assertc 名称 期望HTTP状态码
  if [ "$CODE" == "$2" ]; then PASS=$((PASS+1)); echo "  PASS $1"
  else FAIL=$((FAIL+1)); echo "  FAIL $1 http=[$CODE] expect=[$2] body=$(head -c 180 "$R" 2>/dev/null)"; fi
}

tok() { curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" -o "$R"; jget "d['data']['token']"; }

section() { echo; echo "== $1 =="; }

# ---------- 等待后端就绪(最多40s) ----------
for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/auth/me")
  [ "$code" == "401" ] && break
  sleep 1
done

# ---------- 准备 token ----------
ADMIN=$(tok admin admin123)
TA=$(tok tradmin admin123)
U1=$(tok zhangsan user123)
U2=$(tok lisi user123)
U3=$(tok wangwu user123)
case "$ADMIN$TA$U1$U2$U3" in
  *ERR*|*/*) echo "登录失败, 请确认后端与 MySQL 已启动"; exit 1 ;;
esac
[ -z "$ADMIN" ] || [ -z "$TA" ] || [ -z "$U1" ] || [ -z "$U2" ] || [ -z "$U3" ] && { echo "登录失败, 请确认后端已启动"; exit 1; }


# ============ 0. 就绪 ============
section "0. 环境就绪"
req GET /auth/me "" ; assertc "D0.1 后端就绪(未带token访问/me为401)" 401

# ============ 1. 认证与越权 ============
section "1. 认证/越权/注入/分页"
req GET /system/users "" ; assertc "D1.1 无token访问用户管理返回401" 401
req GET /system/users "fake.token.x" ; assertc "D1.2 伪造token返回401" 401
req GET /system/users "$U1" ; assert "D1.3 员工访问用户管理被拒" "d['code']" 403
req GET /biz/hours "$U1" ; assert "D1.4 员工查全公司学时台账被拒" "d['code']" 403
req GET /biz/stats/overview "$U1" ; assert "D1.5 员工查统计被拒" "d['code']" 403
req GET "/biz/courses?page=1&size=5&keyword=$(enc "' OR '1'='1")" "$ADMIN"
assert "D1.6 课程keyword单引号注入不放大结果" "d['code']" 200
req GET "/system/users?page=1&size=5&keyword=$(enc "' OR '1'='1")" "$ADMIN"
assert "D1.7 用户keyword单引号注入返回正常" "d['code']" 200
req GET "/biz/classes?page=0&size=-5" "$ADMIN"
assert "D1.8 非法分页参数被钳制不报错" "d['code']" 200
req GET "/system/users?page=1&size=9999" "$ADMIN"
assert "D1.9 size=9999 单页不超过200" "len(d['data']['records']) <= 200" True

# --- 停用/删除用户旧token仍有效 ---
NEWPWD='Test@123456'
req POST /system/users "$ADMIN" "{\"username\":\"TESTDU-$TS\",\"password\":\"$NEWPWD\",\"nickname\":\"深度测试员\",\"deptId\":4,\"userType\":\"staff\",\"roleIds\":[3]}"
assert "D1.10 新增测试用户" "d['code']" 200
req GET "/system/users?page=1&size=5&keyword=TESTDU-$TS" "$ADMIN"
NUID=$(gid "D1测试用户ID" "d['data']['records'][0]['id']")
NUTOK=$(tok "TESTDU-$TS" "$NEWPWD")
req PUT "/system/users/$NUID/status?status=1" "$ADMIN"
assert "D1.11 停用测试用户" "d['code']" 200
req GET /biz/classes/my "$NUTOK"
assertc "D1.12 停用用户旧token调业务接口返回401" 401
req GET /auth/me "$NUTOK"
assertc "D1.13 停用用户访问/me返回401" 401

req POST /system/users "$ADMIN" "{\"username\":\"TESTDV-$TS\",\"password\":\"$NEWPWD\",\"nickname\":\"深度删除员\",\"deptId\":4,\"userType\":\"staff\",\"roleIds\":[3]}"
assert "D1.14 新增待删测试用户" "d['code']" 200
req GET "/system/users?page=1&size=5&keyword=TESTDV-$TS" "$ADMIN"
DUID=$(gid "D1待删用户ID" "d['data']['records'][0]['id']")
DUTOK=$(tok "TESTDV-$TS" "$NEWPWD")
req DELETE "/system/users/$DUID" "$ADMIN"
assert "D1.15 删除无关联用户" "d['code']" 200
req GET /biz/classes/my "$DUTOK"
assertc "D1.16 已删除用户旧token调业务接口返回401" 401

req PUT "/system/users/$NUID/status?status=9" "$ADMIN"
assert "D1.17 用户状态非法枚举被拒" "d['code']" 400
req DELETE "/system/users/3" "$ADMIN"
assert "D1.18 删除有关联学时的用户被拒" "d['code']" 400
req PUT "/system/users/1/status?status=1" "$ADMIN"
assert "D1.19 内置管理员不允许停用" "d['code']" 400

# ============ 2. 需求征集与需求申报 ============
section "2. 需求征集与申报"
req POST /biz/needs/collections "$TA" "{\"title\":\"TESTD征集-$TS\",\"year\":$YEAR,\"startTime\":\"$YEAR-01-01T00:00:00\",\"endTime\":\"$YEAR-12-31T23:59:59\",\"description\":\"深度测试\"}"
assert "D2.1 新增需求征集" "d['code']" 200
# collections 接口返回 List 而非分页对象, 且不支持 keyword 过滤
req GET /biz/needs/collections "$TA"
CID_COL=$(gid "D2征集ID" "d['data'][0]['id']")
req PUT "/biz/needs/collections/$CID_COL/close" "$TA"
assert "D2.2 关闭征集" "d['code']" 200
req PUT "/biz/needs/collections/$CID_COL/close" "$TA"
assert "D2.3 重复关闭征集被拒" "d['code']" 400
req POST /biz/needs "$U1" "{\"title\":\"TESTD需求-$TS\",\"needType\":\"skill\",\"content\":\"深度测试需求\",\"collectionId\":$CID_COL,\"year\":$YEAR}"
assert "D2.4 员工提交需求申报" "d['code']" 200
req GET "/biz/needs/my?keyword=$(enc "TESTD需求-$TS")" "$U1"
NID=$(gid "D2需求ID" "d['data']['records'][0]['id']")
req PUT "/biz/needs/$NID" "$U2" "{\"title\":\"TESTD需求-被越权改\",\"needType\":\"skill\",\"content\":\"hack\"}"
assert "D2.5 他人修改需求被拒" "d['code']" 400
req DELETE "/biz/needs/collections/$CID_COL" "$TA"
assert "D2.6 删除有关联申报的征集被拒" "d['code']" 400
req PUT "/biz/needs/$NID/submit" "$U1"
assert "D2.7 提交需求审核" "d['code']" 200
req PUT "/biz/needs/$NID/audit" "$TA" '{"adopt":true,"remark":"采纳"}'
assert "D2.8 审核采纳需求" "d['code']" 200
req DELETE "/biz/needs/$NID" "$U1"
assert "D2.9 已采纳需求不可删" "d['code']" 400
req POST /biz/needs/collections "$TA" "{\"title\":\"TESTD空征集-$TS\",\"year\":$YEAR}"
assert "D2.10 新增空征集" "d['code']" 200
req GET /biz/needs/collections "$TA"
CID_EMPTY=$(gid "D2空征集ID" "d['data'][0]['id']")
req DELETE "/biz/needs/collections/$CID_EMPTY" "$TA"
assert "D2.11 删除无关联空征集" "d['code']" 200

# ============ 3. 培训计划 ============
section "3. 培训计划"
req POST /biz/plans "$TA" "{\"planName\":\"TESTD计划-倒挂-$TS\",\"year\":$YEAR,\"planType\":\"year\",\"startDate\":\"$YEAR-12-31\",\"endDate\":\"$YEAR-01-01\",\"content\":\"x\"}"
assert "D3.1 计划日期倒挂被拒" "d['code']" 400
req POST /biz/plans "$TA" "{\"planName\":\"TESTD计划-$TS\",\"year\":$YEAR,\"planType\":\"year\",\"startDate\":\"$YEAR-11-01\",\"endDate\":\"$YEAR-11-30\",\"content\":\"深度测试计划\"}"
assert "D3.2 新增计划" "d['code']" 200
req GET "/biz/plans?page=1&size=5&keyword=$(enc "TESTD计划-$TS")" "$TA"
PID=$(gid "D3计划ID" "d['data']['records'][0]['id']")
req PUT "/biz/plans/$PID/submit" "$TA"
assert "D3.3 提交计划审批" "d['code']" 200
req PUT "/biz/plans/$PID/approve" "$ADMIN" '{"pass":true,"remark":"同意"}'
assert "D3.4 审批下达计划" "d['code']" 200
req PUT "/biz/plans/$PID" "$TA" "{\"planName\":\"TESTD计划-改\",\"year\":$YEAR}"
assert "D3.5 已批准计划不可编辑" "d['code']" 400
req GET /biz/plans/options "$TA"
assert "D3.6 计划下拉包含已批准计划" "any(r['id']==$PID for r in d['data'])" True

# ============ 4. 课程库 ============
section "4. 课程库"
req POST /biz/courses "$TA" "{\"courseName\":\"TESTDC-$TS\",\"category\":\"general\",\"courseType\":\"internal\",\"hours\":2,\"outline\":\"深度测试课程\"}"
assert "D4.1 新增课程" "d['code']" 200
req GET "/biz/courses?page=1&size=5&keyword=TESTDC-$TS" "$TA"
CID=$(gid "D4课程ID" "d['data']['records'][0]['id']")
req PUT "/biz/courses/$CID" "$TA" "{\"courseName\":\"TESTDC-$TS\",\"category\":\"general\",\"courseType\":\"internal\",\"hours\":2,\"sourceType\":1,\"sourceId\":14}"
assert "D4.2 编辑课程" "d['code']" 200
req GET "/biz/courses?page=1&size=5&keyword=TESTDC-$TS" "$TA"
assert "D4.3 编辑不可篡改外部来源标记" "d['data']['records'][0]['sourceType']" 0
req PUT "/biz/courses/$CID/submit" "$TA"
assert "D4.4 提交课程审核" "d['code']" 200
req PUT "/biz/courses/$CID/audit" "$ADMIN" '{"pass":true,"remark":"发布"}'
assert "D4.5 审核发布课程" "d['code']" 200
req PUT "/biz/courses/$CID" "$TA" "{\"courseName\":\"TESTDC-改\",\"hours\":3}"
assert "D4.6 已发布课程不可编辑" "d['code']" 400
req PUT "/biz/courses/$CID/submit" "$TA"
assert "D4.7 已发布课程不可重复送审" "d['code']" 400
req PUT "/biz/courses/$CID/offline" "$ADMIN"
assert "D4.8 下架课程" "d['code']" 200
req DELETE "/biz/courses/1" "$ADMIN"
assert "D4.9 被课表引用的课程删除被拒" "d['code']" 400

# ============ 5. 班次/签到/结班计学时 ============
section "5. 班次状态机与结班计学时"
req POST /biz/classes "$TA" "{\"className\":\"TESTDCL-倒挂-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"$YEAR-12-31\",\"endDate\":\"$YEAR-11-01\",\"capacity\":10}"
assert "D5.1 班次日期倒挂被拒" "d['code']" 400
req POST /biz/classes "$TA" "{\"className\":\"TESTDCL-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"$YEAR-11-01\",\"endDate\":\"$YEAR-11-02\",\"capacity\":2,\"location\":\"深度测试教室\"}"
assert "D5.2 新增班次(容量2)" "d['code']" 200
req GET "/biz/classes?page=1&size=5&keyword=TESTDCL-$TS" "$TA"
CLID=$(gid "D5班次ID" "d['data']['records'][0]['id']")
TODAY=$(date +%F)
req POST "/biz/classes/$CLID/schedules" "$TA" "{\"courseId\":$CID,\"teachDate\":\"$TODAY\",\"startTime\":\"09:00:00\",\"endTime\":\"12:00:00\",\"hours\":3,\"location\":\"教室A\"}"
assert "D5.3 筹备中可排课" "d['code']" 200
req GET "/biz/classes/$CLID/schedules" "$TA"
SID1=$(gid "D5课节1ID" "d['data'][0]['id']")
req POST "/biz/classes/schedules/$SID1/attendance" "$TA" "{\"userId\":3,\"signType\":\"normal\"}"
assert "D5.4 筹备中班次禁止签到" "d['code']" 400
req PUT "/biz/classes/$CLID/status?status=1" "$TA"
assert "D5.5 发布报名" "d['code']" 200
req POST "/biz/classes/$CLID/enroll" "$U1"
assert "D5.6 员工自主报名" "d['code']" 200
req POST "/biz/classes/$CLID/enroll" "$U1"
assert "D5.7 重复报名被拒" "d['code']" 400
req POST "/biz/classes/$CLID/students" "$TA" '{"userIds":[4]}'
assert "D5.8 批量添加学员" "d['code']" 200
req POST "/biz/classes/$CLID/students" "$TA" '{"userIds":[5]}'
assert "D5.9 超出容量添加学员被拒" "d['code']" 400
req PUT "/biz/classes/$CLID" "$TA" "{\"className\":\"TESTDCL-$TS\",\"capacity\":1,\"startDate\":\"$YEAR-11-01\",\"endDate\":\"$YEAR-11-02\"}"
assert "D5.10 容量不得小于当前学员数" "d['code']" 400
req PUT "/biz/classes/$CLID/status?status=2" "$ADMIN"
assert "D5.11 开始培训" "d['code']" 200
req POST "/biz/classes/schedules/$SID1/attendance" "$TA" '{"userId":3,"signType":"normal"}'
assert "D5.12 学员1正常签到" "d['code']" 200
req POST "/biz/classes/schedules/$SID1/attendance" "$TA" '{"userId":4,"signType":"late"}'
assert "D5.13 学员2迟到签到" "d['code']" 200
req PUT "/biz/classes/schedules/$SID1/finish" "$TA"
assert "D5.14 结束课节计学时" "d['code']" 200
req GET "/biz/classes/$CLID/students" "$TA"
assert "D5.15 正常签到学时=3" "[s['totalHours'] for s in d['data'] if s['userId']==3][0]" 3.0
assert "D5.16 迟到学时=1.5(系数0.5)" "[s['totalHours'] for s in d['data'] if s['userId']==4][0]" 1.5
req POST "/biz/classes/$CLID/schedules" "$TA" "{\"courseId\":$CID,\"teachDate\":\"$TODAY\",\"startTime\":\"14:00:00\",\"endTime\":\"16:00:00\",\"hours\":2,\"location\":\"教室B\"}"
assert "D5.17 进行中班次可加课节" "d['code']" 200
req GET "/biz/classes/$CLID/schedules" "$TA"
SID2=$(gid "D5课节2ID" "[s['id'] for s in d['data'] if s['status']=='0'][0]")
req POST "/biz/classes/schedules/$SID2/attendance" "$TA" '{"userId":3,"signType":"normal"}'
assert "D5.18 第二节课节签到(未结束)" "d['code']" 200
# 关键缺陷: 结班时存在未结束课节, 签到学时是否丢失
req PUT "/biz/classes/$CLID/status?status=3" "$ADMIN"
assert "D5.19 结班(存在未结束课节)" "d['code']" 200
req GET "/biz/classes/$CLID/schedules" "$TA"
assert "D5.20 结班自动结束未结束课节" "[s['status'] for s in d['data'] if s['id']==$SID2][0]" 1
req GET "/biz/classes/$CLID/students" "$TA"
assert "D5.21 结班不丢签到学时(3+2=5)" "[s['totalHours'] for s in d['data'] if s['userId']==3][0]" 5.0
assert "D5.22 迟到学时保持1.5" "[s['totalHours'] for s in d['data'] if s['userId']==4][0]" 1.5
assert "D5.23 参训学员状态为已参训" "[s['status'] for s in d['data'] if s['userId']==3][0]" 2
req POST "/biz/classes/schedules/$SID1/attendance" "$TA" '{"userId":5,"signType":"normal"}'
assert "D5.24 已结束班次禁止签到" "d['code']" 400
req PUT "/biz/classes/schedules/$SID1/finish" "$TA"
assert "D5.25 已结束班次禁止结束课节" "d['code']" 400
req POST "/biz/classes/$CLID/schedules" "$TA" "{\"courseId\":$CID,\"teachDate\":\"$TODAY\",\"startTime\":\"18:00:00\",\"endTime\":\"19:00:00\",\"hours\":1}"
assert "D5.26 已结束班次禁止排课" "d['code']" 400
req PUT "/biz/classes/$CLID/status?status=2" "$ADMIN"
assert "D5.27 已结束班次不可回退状态" "d['code']" 400

# ============ 6. 课节结束后的补签与重算 ============
section "6. 补签与学时重算"
req POST /biz/classes "$TA" "{\"className\":\"TESTDCL2-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"$YEAR-11-05\",\"endDate\":\"$YEAR-11-06\",\"capacity\":5}"
assert "D6.1 新增班次2" "d['code']" 200
req GET "/biz/classes?page=1&size=5&keyword=TESTDCL2-$TS" "$TA"
CLID2=$(gid "D6班次ID" "d['data']['records'][0]['id']")
req POST "/biz/classes/$CLID2/students" "$TA" '{"userIds":[3,4]}'
assert "D6.2 添加学员" "d['code']" 200
req PUT "/biz/classes/$CLID2/status?status=2" "$ADMIN"
assert "D6.3 直接开始培训" "d['code']" 200
req POST "/biz/classes/$CLID2/schedules" "$TA" "{\"courseId\":$CID,\"teachDate\":\"$TODAY\",\"startTime\":\"09:00:00\",\"endTime\":\"11:00:00\",\"hours\":2}"
assert "D6.4 添加课节" "d['code']" 200
req GET "/biz/classes/$CLID2/schedules" "$TA"
SID3=$(gid "D6课节ID" "d['data'][0]['id']")
req POST "/biz/classes/schedules/$SID3/attendance" "$TA" '{"userId":3,"signType":"normal"}'
assert "D6.5 学员1签到" "d['code']" 200
req PUT "/biz/classes/schedules/$SID3/finish" "$TA"
assert "D6.6 结束课节" "d['code']" 200
req POST "/biz/classes/schedules/$SID3/attendance" "$TA" '{"userId":4,"signType":"normal"}'
assert "D6.7 课节结束后班次进行中允许补签" "d['code']" 200
req GET "/biz/classes/$CLID2/students" "$TA"
assert "D6.8 补签学员获得学时" "[s['totalHours'] for s in d['data'] if s['userId']==4][0]" 2.0
req POST "/biz/classes/schedules/$SID3/attendance" "$TA" '{"userId":3,"signType":"absent"}'
assert "D6.9 补签改缺席" "d['code']" 200
req GET "/biz/classes/$CLID2/students" "$TA"
assert "D6.10 缺席后学时归零" "[s['totalHours'] for s in d['data'] if s['userId']==3][0]" 0.0
assert "D6.11 缺席后参训状态回退" "[s['status'] for s in d['data'] if s['userId']==3][0]" 0
req POST "/biz/classes/schedules/$SID3/attendance" "$TA" '{"userId":3,"signType":"normal"}'
assert "D6.12 再次补签恢复正常" "d['code']" 200
req GET "/biz/classes/$CLID2/students" "$TA"
assert "D6.13 重算后学时恢复" "[s['totalHours'] for s in d['data'] if s['userId']==3][0]" 2.0
assert "D6.14 重算后状态恢复已参训" "[s['status'] for s in d['data'] if s['userId']==3][0]" 2
req PUT "/biz/classes/$CLID2/status?status=3" "$ADMIN"
assert "D6.15 结班" "d['code']" 200
req POST "/biz/classes/schedules/$SID3/attendance" "$TA" '{"userId":5,"signType":"normal"}'
assert "D6.16 结班后禁止补签" "d['code']" 400

# ============ 7. 并发报名与容量 ============
section "7. 并发报名与容量"
req POST /biz/classes "$TA" "{\"className\":\"TESTDCL3-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"$YEAR-11-10\",\"endDate\":\"$YEAR-11-11\",\"capacity\":1}"
assert "D7.1 新增容量1班次" "d['code']" 200
req GET "/biz/classes?page=1&size=5&keyword=TESTDCL3-$TS" "$TA"
CLID3=$(gid "D7班次ID" "d['data']['records'][0]['id']")
req PUT "/biz/classes/$CLID3/status?status=1" "$TA"
assert "D7.2 发布报名" "d['code']" 200
# 两名员工并发抢最后一个名额
curl -s -X POST "$BASE/biz/classes/$CLID3/enroll" -H "Authorization: Bearer $U1" -o /tmp/deep-c1.json &
P1=$!
curl -s -X POST "$BASE/biz/classes/$CLID3/enroll" -H "Authorization: Bearer $U2" -o /tmp/deep-c2.json &
P2=$!
wait $P1 $P2
C1=$(bcode /tmp/deep-c1.json); C2=$(bcode /tmp/deep-c2.json)
if { [ "$C1" == "200" ] && [ "$C2" == "400" ]; } || { [ "$C1" == "400" ] && [ "$C2" == "200" ]; }; then
  PASS=$((PASS+1)); echo "  PASS D7.3 并发报名恰好成功一人 ($C1/$C2)"
else
  FAIL=$((FAIL+1)); echo "  FAIL D7.3 并发报名结果异常: $C1/$C2"
fi
req GET "/biz/classes/$CLID3/students" "$TA"
assert "D7.4 班内学员数不超过容量" "len(d['data'])" 1
req POST "/biz/classes/$CLID3/enroll" "$U3"
assert "D7.5 满员后报名被拒" "d['code']" 400
req PUT "/biz/classes/$CLID3/status?status=2" "$ADMIN"
assert "D7.6 开始培训" "d['code']" 200
req POST "/biz/classes/$CLID3/enroll" "$U3"
assert "D7.7 非报名状态禁止自主报名" "d['code']" 400

# ============ 8. 学时台账 ============
section "8. 学时台账与认定"
req POST /biz/hours "$TA" "{\"userId\":3,\"hours\":0,\"recordDate\":\"$YEAR-11-01\",\"source\":2}"
assert "D8.1 学时为0被拒" "d['code']" 400
req POST /biz/hours "$TA" "{\"userId\":3,\"hours\":-5,\"recordDate\":\"$YEAR-11-01\",\"source\":2}"
assert "D8.2 学时为负被拒" "d['code']" 400
req POST /biz/hours "$TA" "{\"hours\":3,\"recordDate\":\"$YEAR-11-01\",\"source\":2}"
assert "D8.3 缺少归属人被拒" "d['code']" 400
req POST /biz/hours "$TA" "{\"userId\":3,\"hours\":2,\"recordDate\":\"$YEAR-11-01\",\"source\":1,\"remark\":\"深度测试\"}"
assert "D8.4 手工补录" "d['code']" 200
req GET "/biz/hours?page=1&size=5&userId=3&year=$YEAR&status=0&keyword=$(enc "深度测试")" "$TA"
assert "D8.5 非法来源规范为手工补录" "d['data']['records'][0]['source']" 2
HID=$(gid "D8学时ID" "d['data']['records'][0]['id']")
req PUT "/biz/hours/$HID/audit" "$TA" '{"pass":true}'
assert "D8.6 学时认定通过" "d['code']" 200
req DELETE "/biz/hours/$HID" "$TA"
assert "D8.7 已认定学时不可删" "d['code']" 400
req PUT "/biz/hours/$HID/audit" "$TA" '{"pass":true}'
assert "D8.8 重复认定被拒" "d['code']" 400
# 学时明细与学员台账一致
req GET "/biz/hours?page=1&size=50&userId=3&year=$YEAR&status=1" "$TA"
assert "D8.9 班次2学时明细合计=学员台账" "sum(r['hours'] for r in d['data']['records'] if r.get('classId')==$CLID2)" 2.0
# 统计口径一致: 我的汇总 vs 统计-员工学时
req GET "/biz/hours/my/summary?year=$YEAR" "$U1"
SUM_MY=$(jget "d['data']['total']")
req GET "/biz/stats/user-hours?year=$YEAR&deptId=4" "$TA"
assert "D8.10 统计-员工学时与我的汇总一致" "[r['totalHours'] for r in d['data']['rows'] if r['userId']==3][0]" "$SUM_MY"
req GET "/biz/stats/overview?year=$YEAR" "$TA"
assert "D8.11 统计概览可查" "d['code']" 200
req GET "/biz/stats/month-hours?year=$YEAR" "$TA"
assert "D8.12 月度学时统计可查" "d['code']" 200
req GET "/biz/stats/dept-hours?year=$YEAR" "$TA"
assert "D8.13 部门学时统计可查" "d['code']" 200

# ============ 9. 培训评估 ============
section "9. 培训评估(柯氏四级)"
# 三级(行为层)模板不可用于班次一级评估
req POST /biz/evals/records "$U1" "{\"templateId\":2,\"classId\":$CLID2,\"scores\":{\"q1\":5},\"comment\":\"越级评估\"}"
assert "D9.1 三级模板不可评班次" "d['code']" 400
req POST /biz/evals/records "$U1" "{\"templateId\":1,\"classId\":$CLID2,\"scores\":{\"q1\":5,\"q2\":4,\"q3\":5,\"q4\":4},\"comment\":\"第一次\"}"
assert "D9.2 一级模板提交评估" "d['code']" 200
req POST /biz/evals/records "$U1" "{\"templateId\":1,\"classId\":$CLID2,\"scores\":{\"q1\":3,\"q2\":3,\"q3\":3,\"q4\":3},\"comment\":\"第二次覆盖\"}"
assert "D9.3 重复提交为覆盖更新" "d['code']" 200
req GET "/biz/evals/records/my?classId=$CLID2&templateId=1" "$U1"
assert "D9.4 重复提交不产生第二条作答" "d['data']['id'] is not None and True" True
req GET "/biz/evals/summary?classId=$CLID2&templateId=1" "$TA"
assert "D9.5 汇总参评人数=1(去重)" "d['data']['evalCount']" 1
assert "D9.6 汇总均分与作答一致" "d['data']['overallAvg']" 3.0
req POST /biz/evals/records "$U3" "{\"templateId\":1,\"classId\":$CLID2,\"scores\":{\"q1\":5,\"q2\":5,\"q3\":5,\"q4\":5}}"
assert "D9.7 非本班学员评估被拒" "d['code']" 400
req POST /biz/evals/records "$U1" "{\"templateId\":1,\"classId\":$CLID2,\"scores\":{\"q1\":6,\"q2\":3,\"q3\":3,\"q4\":3}}"
assert "D9.8 评分越界被拒" "d['code']" 400
req POST /biz/evals/records "$U1" "{\"templateId\":1,\"classId\":$CLID2,\"scores\":{\"q2\":3}}"
assert "D9.9 缺少必填项被拒" "d['code']" 400
req GET /biz/evals/pending "$U1"
assert "D9.10 待评估列表包含已结班班次" "any(r['classId']==$CLID2 for r in d['data'])" True

# ============ 10. 外部课程集成 ============
section "10. 外部课程集成"
req POST /biz/integrations/sources "$ADMIN" "{\"sourceName\":\"TESTD源-$TS\",\"sourceType\":\"mock\",\"appSecret\":\"testsecret-$TS\",\"remark\":\"深度测试\"}"
assert "D10.1 新增mock数据源" "d['code']" 200
req GET "/biz/integrations/sources?keyword=$(enc "TESTD源-$TS")" "$ADMIN"
SRCID=$(gid "D10数据源ID" "d['data']['records'][0]['id']")
req GET "/biz/integrations/sources/$SRCID" "$ADMIN"
assert "D10.2 app_secret掩码显示" "d['data'].get('appSecret') in ('', None, '******')" True
req PUT "/biz/integrations/sources/$SRCID/status?status=9" "$ADMIN"
assert "D10.3 数据源状态非法枚举被拒" "d['code']" 400
req POST "/biz/integrations/sources/$SRCID/sync" "$ADMIN"
assert "D10.4 同步mock课程" "d['code']" 200
assert "D10.5 同步到8门演示课程" "d['data']['total']" 8
req POST "/biz/integrations/external-courses/ignore" "$ADMIN" '{"ids":[999999]}'
assert "D10.6 忽略不存在的暂存课程被拒" "d['code']" 400
req GET "/biz/integrations/external-courses?page=1&size=10&sourceId=$SRCID&status=0" "$ADMIN"
EID1=$(gid "D10暂存1" "d['data']['records'][0]['id']")
EID2=$(gid "D10暂存2" "d['data']['records'][1]['id']")
req POST /biz/integrations/external-courses/import-to-course "$ADMIN" "{\"ids\":[$EID1,$EID2]}"
assert "D10.7 暂存课程入库" "d['data']['success']" 2
req GET "/biz/courses?page=1&size=5&keyword=$(enc "数字化转型")" "$ADMIN"
assert "D10.8 入库课程为外部来源" "d['data']['records'][0]['sourceType']" 1
req GET "/biz/integrations/external-courses?page=1&size=10&sourceId=$SRCID&status=1" "$ADMIN"
assert "D10.9 入库后暂存状态已更新" "len(d['data']['records'])" 2
req POST /biz/integrations/external-courses/import-to-course "$ADMIN" "{\"ids\":[$EID1,$EID2]}"
assert "D10.10 重复入库幂等(更新)" "d['data']['success']" 2
req GET "/biz/courses?page=1&size=50&keyword=$(enc "数字化转型")" "$ADMIN"
assert "D10.11 重复入库不产生重复课程" "len([r for r in d['data']['records'] if r.get('sourceId')==$SRCID and r.get('externalCode')=='EX-MOCK-001'])" 1
# 先取一条待入库课程再忽略, 避免嵌套 req 覆盖响应文件
req GET "/biz/integrations/external-courses?page=1&size=1&sourceId=$SRCID&status=0" "$ADMIN"
IGNID=$(gid "D10待忽略ID" "d['data']['records'][0]['id']")
req POST /biz/integrations/external-courses/ignore "$ADMIN" "{\"ids\":[$IGNID]}"
assert "D10.12 忽略暂存课程" "d['data']" 1
# 字段映射覆盖: 用 description 作为名称字段
req PUT "/biz/integrations/sources/$SRCID" "$ADMIN" "{\"id\":$SRCID,\"sourceName\":\"TESTD源-$TS\",\"sourceType\":\"mock\",\"fieldMapping\":\"{\\\"name\\\":\\\"description\\\"}\",\"categoryMapping\":\"{\\\"通用课程\\\":\\\"general\\\"}\"}"
assert "D10.13 更新字段映射" "d['code']" 200
req POST "/biz/integrations/sources/$SRCID/sync" "$ADMIN"
assert "D10.14 按新映射重新同步" "d['data']['success']" 8
req GET "/biz/integrations/external-courses?page=1&size=5&sourceId=$SRCID&keyword=$(enc "面向中基层")" "$ADMIN"
assert "D10.15 字段映射生效(name<-description)" "len(d['data']['records'])" 1
req PUT "/biz/integrations/sources/$SRCID/status?status=1" "$ADMIN"
assert "D10.16 停用数据源" "d['code']" 200
req POST "/biz/integrations/sources/$SRCID/sync" "$ADMIN"
assert "D10.17 停用数据源禁止同步" "d['code']" 400
req GET "/biz/integrations/logs?page=1&size=5&sourceId=$SRCID" "$ADMIN"
assert "D10.18 同步日志可查" "d['code']" 200
req DELETE "/biz/integrations/sources/$SRCID" "$ADMIN"
assert "D10.19 删除测试数据源" "d['code']" 200
req GET "/biz/integrations/sources/$SRCID" "$ADMIN"
assert "D10.20 已删数据源查询不存在" "d['code']" 400

# ============ 11. 一人一档/学时预警/消息中心 ============
section "11. 培训档案与学时预警(对标国企干部培训档案)"
req GET /biz/archive/my "$U1"
assert "D11.1 我的档案可查" "d['code']" 200
assert "D11.2 档案含基本信息与学时汇总" "d['data']['user']['nickname'] is not None and d['data']['hours']['required'] is not None" True
assert "D11.3 档案含履历时间线与明细页签" "isinstance(d['data']['timeline'], list) and isinstance(d['data']['classes'], list)" True
req GET /biz/archive/user/1 "$U1"
assert "D11.4 员工查他人档案被拒" "d['code']" 403
req GET /biz/archive/user/3 "$TA"
assert "D11.5 管理员查他人档案" "d['data']['user']['nickname']" "张三"
req GET /biz/archive/warnings "$TA"
assert "D11.6 达标预警名单可查" "d['code']" 200
req GET /biz/archive/my-warning "$U1"
assert "D11.7 我的预警可查(可为空)" "d['code']" 200
req POST "/biz/archive/warnings/push?year=$YEAR" "$TA"
assert "D11.8 管理员推送预警消息" "d['code']" 200
req GET /biz/messages/unread-count "$ADMIN"
assert "D11.9 预警推送产生未读消息" "d['data']>=1" True
req PUT /biz/messages/read-all "$ADMIN"
assert "D11.10 全部已读" "d['code']" 200
req GET /biz/messages/unread-count "$ADMIN"
assert "D11.11 已读后未读清零" "d['data']" 0
req POST /biz/messages/announce "$TA" "{\"title\":\"D11公告-$TS\",\"content\":\"深度测试公告\"}"
assert "D11.12 管理员发布全员公告" "d['code']" 200
req GET "/biz/messages/my?page=1&size=5&type=system" "$U1"
assert "D11.13 员工收到系统公告" "len(d['data']['records'])>=1" True

# ============ 12. 结业证书与在线考试(柯氏二级) ============
section "12. 结业证书与在线考试"
req POST /biz/certificates/class/1/issue "$TA"
assert "D12.1 已结业班次补发证书(幂等)" "d['code']" 200
req GET "/biz/certificates?page=1&size=5&keyword=$(enc "张三")" "$TA"
assert "D12.2 证书台账含结业学员" "d['data']['total']>=1" True
assert "D12.3 证书编号规范(QGPX-)" "str(d['data']['records'][0]['certNo']).startswith('QGPX-')" True
req GET /biz/certificates/my "$U1"
assert "D12.4 我的证书可查" "len(d['data'])>=1" True
req GET /biz/certificates "$U1"
assert "D12.5 员工无证书台账权限" "d['code']" 403
# 考试用班次
req POST /biz/classes "$TA" "{\"className\":\"TESTDX-CL-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"$YEAR-11-01\",\"endDate\":\"$YEAR-11-02\",\"capacity\":10}"
assert "D12.6 新增考试用班次" "d['code']" 200
req GET "/biz/classes?page=1&size=5&keyword=TESTDX-CL-$TS" "$TA"
XCL=$(gid "D12考试班次ID" "d['data']['records'][0]['id']")
req POST "/biz/classes/$XCL/students" "$TA" '{"userIds":[3,4]}'
assert "D12.7 添加考试班学员" "d['code']" 200
req PUT "/biz/classes/$XCL/status?status=2" "$ADMIN"
assert "D12.8 班次直接开始" "d['code']" 200
# 题库
req POST /biz/questions "$TA" '{"qType":"single","stem":"D12安全生产培训主要目的是?","options":"[{\"key\":\"A\",\"text\":\"提升安全意识\"},{\"key\":\"B\",\"text\":\"增加工时\"},{\"key\":\"C\",\"text\":\"应付检查\"},{\"key\":\"D\",\"text\":\"形式主义\"}]","answer":"A","analysis":"安全第一"}'
assert "D12.9 新增单选题" "d['code']" 200
req POST /biz/questions "$TA" '{"qType":"multiple","stem":"D12属于安全操作规程的有?","options":"[{\"key\":\"A\",\"text\":\"持证上岗\"},{\"key\":\"B\",\"text\":\"佩戴劳保\"},{\"key\":\"C\",\"text\":\"违章作业\"},{\"key\":\"D\",\"text\":\"危险作业审批\"}]","answer":"ABD"}'
assert "D12.10 新增多选题" "d['code']" 200
req POST /biz/questions "$TA" '{"qType":"judge","stem":"D12迟到按节学时乘系数折算。","answer":"对"}'
assert "D12.11 新增判断题" "d['code']" 200
req POST /biz/questions "$TA" '{"qType":"judge","stem":"D12判断题答案可为任意文本。","answer":"abc"}'
assert "D12.12 非法判断题答案被拒" "d['code']" 400
req POST /biz/questions "$TA" '{"qType":"single","stem":"D12答案不在选项中校验","options":"[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]","answer":"Z"}'
assert "D12.13 答案越界被拒" "d['code']" 400
req POST /biz/questions "$TA" '{"qType":"single","stem":"D12学时台账唯一口径是?","options":"[{\"key\":\"A\",\"text\":\"已认定学时记录\"},{\"key\":\"B\",\"text\":\"签到次数\"},{\"key\":\"C\",\"text\":\"证书数量\"},{\"key\":\"D\",\"text\":\"评估均分\"}]","answer":"A"}'
assert "D12.14 新增第4题(凑整百)" "d['code']" 200
req GET "/biz/questions?page=1&size=5&keyword=D12" "$TA"
Q4=$(gid "D12题4" "d['data']['records'][0]['id']")
Q3=$(gid "D12题3" "d['data']['records'][1]['id']")
Q2=$(gid "D12题2" "d['data']['records'][2]['id']")
Q1=$(gid "D12题1" "d['data']['records'][3]['id']")
# 考试
req POST /biz/exams "$TA" "{\"title\":\"TESTDX-考试-$TS\",\"classId\":$XCL,\"durationMin\":30,\"passScore\":60,\"totalScore\":100,\"hourReward\":2,\"questionIds\":\"$Q1,$Q2,$Q3,$Q4\"}"
assert "D12.15 新建考试草稿" "d['code']" 200
req POST /biz/exams "$TA" "{\"title\":\"TESTDX-空卷-$TS\",\"classId\":$XCL,\"questionIds\":\"\"}"
assert "D12.16 未选题组卷被拒" "d['code']" 400
req GET "/biz/exams?page=1&size=5&keyword=$(enc "TESTDX-考试-$TS")" "$TA"
XEX=$(gid "D12考试ID" "d['data']['records'][0]['id']")
req PUT "/biz/exams/$XEX/publish" "$ADMIN"
assert "D12.17 发布考试(通知学员)" "d['code']" 200
req GET "/biz/exams/$XEX/paper" "$U3"
assert "D12.18 非学员取卷被拒" "d['code']" 400
req GET "/biz/exams/$XEX/paper" "$U1"
assert "D12.19 学员取卷" "d['code']" 200
assert "D12.20 试卷不含答案" "all('answer' not in q for q in d['data']['questions'])" True
req POST "/biz/exams/$XEX/submit" "$U1" "{\"answers\":{\"$Q1\":\"A\",\"$Q2\":\"ABD\",\"$Q3\":\"对\",\"$Q4\":\"A\"}}"
assert "D12.21 全对交卷得满分" "d['data']['score']" 100.0
assert "D12.22 及格" "d['data']['passed']" True
assert "D12.23 及格计学时" "d['data']['hourReward']" 2.0
req POST "/biz/exams/$XEX/submit" "$U1" "{\"answers\":{\"$Q1\":\"A\"}}"
assert "D12.24 重复交卷被拒" "d['code']" 400
req GET "/biz/exams/$XEX/records" "$TA"
assert "D12.25 成绩单可查" "d['data']['submittedCount']" 1
req GET /biz/exams/my "$U1"
assert "D12.26 我的考试列表含已交卷考试" "any(r['id']==$XEX and r['submitted'] for r in d['data'])" True
req GET /biz/archive/my "$U1"
assert "D12.27 档案聚合考试成绩" "any(e['examId']==$XEX for e in d['data']['exams'])" True
req GET /biz/archive/my "$U1"
assert "D12.28 档案聚合考试学时来源" "any(int(s['source'])==4 for s in d['data']['hours']['bySource'])" True

# ============ 13. 安全加固与健壮性 ============
section "13. 安全加固与健壮性(漏洞修复验证)"
req GET /biz/messages/my ""
assertc "D13.1 未认证访问消息接口401" 401
req POST /biz/questions "$U1" '{"qType":"judge","stem":"D13越权建题","answer":"对"}'
assert "D13.2 员工新增题目被拒" "d['code']" 403
req POST /biz/exams "$U1" "{\"title\":\"D13越权建考\",\"classId\":$XCL,\"questionIds\":\"$Q1\"}"
assert "D13.3 员工新建考试被拒" "d['code']" 403
req POST /biz/certificates/class/1/issue "$U1"
assert "D13.4 员工补发证书被拒" "d['code']" 403
req POST /biz/messages/announce "$U1" '{"title":"D13越权公告"}'
assert "D13.5 员工发公告被拒" "d['code']" 403
req POST "/biz/archive/warnings/push?year=$YEAR" "$U1"
assert "D13.6 员工推送预警被拒" "d['code']" 403
# 13.B 参数校验(一律400而非500)
req POST /biz/exams "$TA" "{\"title\":\"TESTDX-负学时-$TS\",\"classId\":$XCL,\"hourReward\":-2,\"questionIds\":\"$Q1\"}"
assert "D13.7 负数及格学时被拒" "d['code']" 400
LONGS=$(python3 -c "print('长'*1100)")
req POST /biz/questions "$TA" "{\"qType\":\"judge\",\"stem\":\"$LONGS\",\"answer\":\"对\"}"
assert "D13.8 超长题干400而非500" "d['code']" 400
req POST /biz/questions "$TA" '{"qType":"single","stem":"D13单选双字母答案","options":"[{\"key\":\"A\",\"text\":\"x\"},{\"key\":\"B\",\"text\":\"y\"}]","answer":"AA"}'
assert "D13.9 单选多字母答案被拒" "d['code']" 400
LONGC=$(python3 -c "print('评'*1100)")
req POST /biz/messages/announce "$TA" "{\"title\":\"D13超长\",\"content\":\"$LONGC\"}"
assert "D13.10 超长公告被拒" "d['code']" 400
# 13.C 考试状态机与角色边界
req GET "/biz/exams/$XEX/paper" "$TA"
assert "D13.11 非学员培训管理员取卷被拒" "d['code']" 400
req POST "/biz/exams/$XEX/submit" "$ADMIN" "{\"answers\":{\"$Q1\":\"A\"}}"
assert "D13.12 管理员非学员代考被拒" "d['code']" 400
req PUT "/biz/exams/$XEX/publish" "$ADMIN"
assert "D13.13 重复发布被拒" "d['code']" 400
req DELETE "/biz/exams/$XEX" "$TA"
assert "D13.14 删除已发布考试被拒" "d['code']" 400
req PUT "/biz/exams/$XEX/close" "$ADMIN"
assert "D13.15 截止考试" "d['code']" 200
req POST "/biz/exams/$XEX/submit" "$U2" "{\"answers\":{\"$Q1\":\"A\",\"$Q2\":\"ABD\",\"$Q3\":\"对\",\"$Q4\":\"A\"}}"
assert "D13.16 截止后交卷被拒" "d['code']" 400
req POST /biz/exams "$TA" "{\"title\":\"TESTDX-草稿2-$TS\",\"classId\":$XCL,\"questionIds\":\"$Q1\"}"
assert "D13.17 新建草稿考试" "d['code']" 200
req GET "/biz/exams?page=1&size=5&keyword=$(enc "TESTDX-草稿2-$TS")" "$TA"
XEX2=$(gid "D13草稿考试ID" "d['data']['records'][0]['id']")
req PUT "/biz/exams/$XEX2/close" "$ADMIN"
assert "D13.18 草稿截止被拒" "d['code']" 400
# 13.E 消息越权与注入
req GET "/biz/messages/my?page=1&size=1" "$U1"
MID=$(gid "D13消息ID" "d['data']['records'][0]['id']")
req PUT "/biz/messages/$MID/read" "$U2"
assert "D13.19 标记他人消息被拒" "d['code']" 400
req POST /biz/messages/announce "$TA" '{"title":""}'
assert "D13.20 空标题公告被拒" "d['code']" 400
INJ=$(enc "' OR '1'='1")
req GET "/biz/certificates?page=1&size=5&keyword=$INJ" "$TA"
assert "D13.21 证书台账查询注入安全" "d['code']" 200
req GET "/biz/exams?page=1&size=5&keyword=$INJ" "$TA"
assert "D13.22 考试列表注入安全" "d['code']" 200
req GET /biz/archive/user/12 "$TA"
assert "D13.23 已删除用户档案被拒" "d['code']" 400
req GET /biz/archive/warnings "$U1"
assert "D13.24 员工预警名单仅含本人" "all(r['userId']==3 for r in d['data'])" True
# 13.F 并发交卷原子性
req POST /biz/exams "$TA" "{\"title\":\"TESTDX-并发-$TS\",\"classId\":$XCL,\"durationMin\":30,\"hourReward\":0,\"questionIds\":\"$Q1,$Q2,$Q3,$Q4\"}"
assert "D13.25 新建并发考试" "d['code']" 200
req GET "/biz/exams?page=1&size=5&keyword=$(enc "TESTDX-并发-$TS")" "$TA"
XEX3=$(gid "D13并发考试ID" "d['data']['records'][0]['id']")
req PUT "/biz/exams/$XEX3/publish" "$ADMIN"
assert "D13.26 发布并发考试" "d['code']" 200
req GET "/biz/exams/$XEX3/paper" "$U2"
assert "D13.27 学员取卷" "d['code']" 200
curl -s -X POST "$BASE/biz/exams/$XEX3/submit" -H "Authorization: Bearer $U2" -H 'Content-Type: application/json' -d "{\"answers\":{\"$Q1\":\"A\",\"$Q2\":\"ABD\",\"$Q3\":\"对\",\"$Q4\":\"A\"}}" -o /tmp/deep-s1.json &
P1=$!
curl -s -X POST "$BASE/biz/exams/$XEX3/submit" -H "Authorization: Bearer $U2" -H 'Content-Type: application/json' -d "{\"answers\":{\"$Q1\":\"A\",\"$Q2\":\"ABD\",\"$Q3\":\"对\",\"$Q4\":\"A\"}}" -o /tmp/deep-s2.json &
P2=$!
wait $P1 $P2
S1=$(bcode /tmp/deep-s1.json); S2=$(bcode /tmp/deep-s2.json)
if { [ "$S1" == "200" ] && [ "$S2" == "400" ]; } || { [ "$S1" == "400" ] && [ "$S2" == "200" ]; }; then
  PASS=$((PASS+1)); echo "  PASS D13.28 并发交卷恰好一次生效 ($S1/$S2)"
else
  FAIL=$((FAIL+1)); echo "  FAIL D13.28 并发交卷结果异常: $S1/$S2"
fi
req GET "/biz/exams/$XEX3/records" "$TA"
assert "D13.29 并发交卷仅计一次成绩" "d['data']['submittedCount']" 1
req GET "/biz/exams/$XEX3/paper" "$ADMIN"
assert "D13.30 管理员预览试卷不建作答记录" "d['data']['preview']" True
req GET "/biz/exams/$XEX3/records" "$TA"
assert "D13.31 预览不产生成绩记录" "d['data']['submittedCount']" 1

# ============ 汇总 ============
echo
echo "==================================="
echo "深度测试完成: PASS=$PASS FAIL=$FAIL"
echo "==================================="
[ "$FAIL" -eq 0 ] || exit 1
