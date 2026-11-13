#!/bin/bash
# =====================================================================
# 国企培训管理系统 E2E 接口测试
# 用法: 后端启动后执行  BASE=http://localhost:18080/api bash tests/e2e-test.sh
# 说明: 依赖初始化演示数据(用户 zhangsan/lisi/wangwu 等), 测试会创建 TEST-* 前缀数据
# =====================================================================
BASE=${BASE:-http://localhost:18080/api}
R=/tmp/e2e-resp.json
PASS=0; FAIL=0
TS=$(date +%s)

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

assert() { # assert 名称 python表达式 期望值
  local actual
  actual=$(jget "$2")
  if [ "$actual" == "$3" ]; then PASS=$((PASS+1)); echo "  PASS $1"
  else FAIL=$((FAIL+1)); echo "  FAIL $1 expect=[$3] actual=[$actual] body=$(head -c 150 "$R" 2>/dev/null)"; fi
}

assertc() { # assertc 名称 期望HTTP状态码
  if [ "$CODE" == "$2" ]; then PASS=$((PASS+1)); echo "  PASS $1"
  else FAIL=$((FAIL+1)); echo "  FAIL $1 http=[$CODE] expect=[$2] body=$(head -c 150 "$R" 2>/dev/null)"; fi
}

tok() { curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$1\",\"password\":\"$2\"}" -o "$R"; jget "d['data']['token']"; }

section() { echo; echo "== $1 =="; }

# ---------- 准备 token ----------
ADMIN=$(tok admin admin123)
TA=$(tok tradmin admin123)
U1=$(tok zhangsan user123)
U2=$(tok lisi user123)
if [ -z "$ADMIN" ] || [ -z "$U1" ]; then echo "登录失败, 请确认后端已启动"; exit 1; fi

# ============ 1. 认证与越权 ============
section "1. 认证与越权"
req GET /system/users "" ; assertc "1.1 无token访问管理接口返回401" 401
req GET /system/users "fake.token.x" ; assertc "1.2 伪造token返回401" 401
curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong!"}' -o "$R"
assert "1.3 错误密码返回业务错误400" "d['code']" 400
req GET /system/users "$U1" ; assert "1.4 员工访问用户管理被拒" "d['code']" 403
req PUT /biz/needs/1/audit "$U1" '{"adopt":true}' ; assert "1.5 员工审核需求被拒" "d['code']" 403
req GET /biz/hours?page=1\&size=10 "$U1" ; assert "1.6 员工查全量学时台账被拒" "d['code']" 403
req GET /system/users?page=1\&size=10 "$TA" ; assert "1.7 培训管理员无系统管理权限" "d['code']" 403
req GET /biz/classes/enrollable "$U1" ; assert "1.8 员工查询可报名班次(自助接口)放行" "d['code']" 200
curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"'"'"' OR '"'"'1'"'"'='"'"'1","password":"x"}' -o "$R"
assert "1.9 SQL注入登录被拒" "d['code']" 400
CODE=$(curl -s -o "$R" -w "%{http_code}" -G "$BASE/system/users" -H "Authorization: Bearer $ADMIN" --data-urlencode "keyword=' OR '1'='1")
assert "1.10 SQL注入查询不炸库" "d['code']" 200

# ============ 2. 培训需求流 ============
section "2. 培训需求申报与审核"
req POST /biz/needs "$U1" "{\"year\":2026,\"title\":\"TESTN-$TS\",\"needType\":\"skill\",\"content\":\"测试需求\"}"
assert "2.1 员工新增需求草稿" "d['code']" 200
req GET "/biz/needs/my?page=1&size=5&keyword=TESTN-$TS" "$U1"
assert "2.2 草稿状态为0" "d['data']['records'][0]['status']" 0
NID=$(jget "d['data']['records'][0]['id']")
req POST /biz/needs "$U2" "{\"year\":2026,\"title\":\"TESTN2-$TS\",\"needType\":\"safety\",\"content\":\"他人测试需求\"}"
req GET "/biz/needs/my?page=1&size=5&keyword=TESTN2-$TS" "$U2"
NID2=$(jget "d['data']['records'][0]['id']")
req PUT "/biz/needs/$NID2/submit" "$U1" ; assert "2.3 提交他人需求被拒(越权修复)" "d['code']" 400
req PUT "/biz/needs/$NID/submit" "$U1" ; assert "2.4 提交本人需求" "d['code']" 200
req PUT "/biz/needs/$NID/audit" "$U1" '{"adopt":true}' ; assert "2.5 员工不能审核(含本人)" "d['code']" 403
req PUT "/biz/needs/$NID/audit" "$TA" '{"adopt":true,"remark":"采纳"}' ; assert "2.6 培训管理员审核采纳" "d['code']" 200
req GET "/biz/needs/my?page=1&size=5&keyword=TESTN-$TS" "$U1"
assert "2.7 需求状态变为已采纳" "d['data']['records'][0]['status']" 2

# ============ 3. 培训计划流 ============
section "3. 培训计划编制与审批"
req POST /biz/plans "$TA" "{\"planName\":\"TESTP-$TS\",\"year\":2026,\"planType\":\"special\",\"deptId\":3,\"budget\":1.5,\"content\":\"测试计划\"}"
assert "3.1 新增计划草稿" "d['code']" 200
req GET "/biz/plans?page=1&size=5&keyword=TESTP-$TS" "$TA"
PID=$(jget "d['data']['records'][0]['id']")
req PUT "/biz/plans/$PID/submit" "$TA" ; assert "3.2 提交审批" "d['code']" 200
req PUT "/biz/plans/$PID/approve" "$TA" '{"pass":true,"remark":"同意"}' ; assert "3.3 审批通过" "d['code']" 200
req GET "/biz/plans?page=1&size=5&keyword=TESTP-$TS" "$TA"
assert "3.4 计划状态为已批准" "d['data']['records'][0]['status']" 2

# ============ 4. 课程与师资 ============
section "4. 培训课程与师资"
req POST /biz/courses "$TA" "{\"courseName\":\"TESTC-$TS\",\"category\":\"skill\",\"courseType\":\"internal\",\"hours\":3,\"level\":\"初级\"}"
assert "4.1 新增课程草稿" "d['code']" 200
req GET "/biz/courses?page=1&size=5&keyword=TESTC-$TS" "$TA"
CID=$(jget "d['data']['records'][0]['id']")
req PUT "/biz/courses/$CID/submit" "$TA" ; assert "4.2 提交课程审核" "d['code']" 200
req PUT "/biz/courses/$CID/audit" "$TA" '{"pass":true}' ; assert "4.3 审核发布" "d['code']" 200
req POST /biz/trainers "$TA" "{\"trainerName\":\"TESTT-$TS\",\"trainerType\":\"internal\",\"specialty\":\"测试\"}"
assert "4.4 新增师资" "d['code']" 200
req GET "/biz/trainers?page=1&size=5&keyword=TESTT-$TS" "$TA"
TID=$(jget "d['data']['records'][0]['id']")
req DELETE "/biz/courses/1" "$ADMIN" ; assert "4.5 已被课表引用的课程删除被拒(修复项)" "d['code']" 400
req POST /biz/courses "$ADMIN" "{\"courseName\":\"TESTC2-$TS\",\"category\":\"general\",\"courseType\":\"internal\",\"hours\":1}"
req GET "/biz/courses?page=1&size=5&keyword=TESTC2-$TS" "$ADMIN"
CID2=$(jget "d['data']['records'][0]['id']")
req DELETE "/biz/courses/$CID2" "$ADMIN" ; assert "4.6 未引用课程可删除" "d['code']" 200

# ============ 5. 班次全流程(开班/报名/签到/学时) ============
section "5. 培训班次全流程"
req POST /biz/classes "$TA" "{\"className\":\"TESTCL-$TS\",\"planId\":$PID,\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"2026-11-01\",\"endDate\":\"2026-11-02\",\"capacity\":10,\"location\":\"测试教室\"}"
assert "5.1 新增班次" "d['code']" 200
req GET "/biz/classes?page=1&size=5&keyword=TESTCL-$TS" "$TA"
CLID=$(jget "d['data']['records'][0]['id']")
req PUT "/biz/classes/$CLID/status?status=1" "$TA" ; assert "5.2 发布报名" "d['code']" 200
req POST "/biz/classes/$CLID/enroll" "$U1" ; assert "5.3 员工自主报名" "d['code']" 200
req POST "/biz/classes/$CLID/enroll" "$U1" ; assert "5.4 重复报名被拒" "d['code']" 400
req POST "/biz/classes/$CLID/students" "$TA" '{"userIds":[4,5]}' ; assert "5.5 批量添加学员" "d['code']" 200
req PUT "/biz/classes/$CLID/status?status=2" "$ADMIN" ; assert "5.6 开始培训" "d['code']" 200
TODAY=$(date +%F)
req POST "/biz/classes/$CLID/schedules" "$TA" "{\"courseId\":$CID,\"trainerId\":$TID,\"teachDate\":\"$TODAY\",\"startTime\":\"09:00:00\",\"endTime\":\"12:00:00\",\"hours\":3,\"location\":\"测试教室\"}"
assert "5.7 添加课节" "d['code']" 200
req GET "/biz/classes/$CLID/schedules" "$TA"
SID=$(jget "d['data'][0]['id']")
req POST "/biz/classes/schedules/$SID/attendance" "$TA" '{"userId":3,"signType":"normal"}' ; assert "5.8 学员1正常签到" "d['code']" 200
req POST "/biz/classes/schedules/$SID/attendance" "$TA" '{"userId":4,"signType":"late"}' ; assert "5.9 学员2迟到签到" "d['code']" 200
req PUT "/biz/classes/schedules/$SID/finish" "$TA" ; assert "5.10 结束课节计学时" "d['code']" 200
req GET "/biz/classes/$CLID/students" "$TA"
assert "5.11 正常签到学时=3" "d['data'][0]['totalHours']" 3.0
assert "5.12 迟到学时=1.5(系数0.5)" "d['data'][1]['totalHours']" 1.5
req POST "/biz/classes/schedules/$SID/attendance" "$TA" '{"userId":5,"signType":"normal"}'
assert "5.13 课节结束后班次进行中允许补签(修复项)" "d['code']" 200
req GET "/biz/classes/$CLID/students" "$TA"
assert "5.13a 补签学员即时获得学时" "[s['totalHours'] for s in d['data'] if s['userId']==5][0]" 3.0
# 原未参训学员已被补签, 追加一名从未签到的学员用于缺勤断言
req POST "/biz/classes/$CLID/students" "$TA" '{"userIds":[6]}' ; assert "5.13b 追加未参训学员" "d['code']" 200
req PUT "/biz/classes/$CLID/status?status=3" "$ADMIN" ; assert "5.14 结班" "d['code']" 200
req GET "/biz/classes/$CLID/students" "$TA"
assert "5.15 未参训学员标记缺勤(修复项)" "[s['status'] for s in d['data'] if s['userId']==6][0]" 3

# ============ 6. 学时台账 ============
section "6. 学时台账与认定"
req POST /biz/hours "$TA" "{\"userId\":3,\"hours\":2,\"recordDate\":\"2026-10-31\",\"source\":1,\"remark\":\"测试补录\"}"
assert "6.1 手工补录入口" "d['code']" 200
req GET "/biz/hours?page=1&size=3&userId=3&year=2026&status=0" "$TA"
assert "6.2 非法来源被规范为手工补录(修复项)" "d['data']['records'][0]['source']" 2
HID=$(jget "d['data']['records'][0]['id']")
req PUT "/biz/hours/$HID/audit" "$TA" '{"pass":true}' ; assert "6.3 学时认定通过" "d['code']" 200
req GET "/biz/hours/my/summary?year=2026" "$U1"
jget "d['data']['total'] >= 13" > /tmp/e2e-flag.txt
if [ "$(cat /tmp/e2e-flag.txt)" == "True" ]; then PASS=$((PASS+1)); echo "  PASS 6.4 张三年度学时累计>=13"
else FAIL=$((FAIL+1)); echo "  FAIL 6.4 学时汇总不符: $(jget "d['data']['total']")"; fi

# ============ 7. 培训评估 ============
section "7. 培训评估"
ITEMS='[{"key":"q1","title":"实用性","type":"score","required":true},{"key":"q2","title":"讲师水平","type":"score","required":true},{"key":"t1","title":"建议","type":"text","required":false}]'
req POST /biz/evals/templates "$ADMIN" "{\"templateName\":\"TESTE-$TS\",\"evalLevel\":1,\"items\":$ITEMS,\"status\":\"0\"}"
assert "7.1 新增评估模板" "d['code']" 200
req GET "/biz/evals/templates?page=1&size=5&keyword=TESTE-$TS" "$ADMIN"
EID=$(jget "d['data']['records'][0]['id']")
req POST /biz/evals/records "$U1" "{\"templateId\":$EID,\"classId\":$CLID,\"scores\":{\"q1\":5,\"t1\":\"好\"},\"comment\":\"\"}"
assert "7.2 缺必填评分被拒(修复项)" "d['code']" 400
req POST /biz/evals/records "$U1" "{\"templateId\":$EID,\"classId\":$CLID,\"scores\":{\"q1\":5,\"q2\":4,\"t1\":\"好\"},\"comment\":\"整体不错\"}"
assert "7.3 完整作答提交" "d['code']" 200
req POST /biz/evals/records "$U1" "{\"templateId\":$EID,\"classId\":$CLID,\"scores\":{\"q1\":4,\"q2\":4},\"comment\":\"修改后\"}"
assert "7.4 重复提交为覆盖更新" "d['code']" 200
req GET "/biz/evals/records?classId=$CLID&templateId=$EID" "$ADMIN"
assert "7.5 作答仅一条(未重复)" "len(d['data'])" 1
NEWITEMS='[{"key":"q1","title":"改过的条目","type":"score","required":true}]'
req PUT "/biz/evals/templates/$EID" "$ADMIN" "{\"templateName\":\"TESTE-$TS\",\"evalLevel\":1,\"items\":$NEWITEMS,\"status\":\"0\"}"
assert "7.6 已有作答的模板条目冻结(修复项)" "d['code']" 400
req GET "/biz/evals/summary?classId=$CLID&templateId=$EID" "$ADMIN"
assert "7.7 逐项均分正确" "d['data']['itemAvg']['q1']" 4.0
assert "7.8 参评人数正确" "d['data']['evalCount']" 1

# ============ 8. 统计分析 ============
section "8. 统计分析"
req GET "/biz/stats/overview?year=2026" "$ADMIN"
assert "8.1 总览班次数>=4" "d['data']['classCount'] >= 4" True
req GET "/biz/stats/user-hours?year=2026" "$ADMIN"
assert "8.2 员工学时统计含全员" "len(d['data']['rows']) >= 8" True
req GET "/biz/stats/month-hours?year=2026" "$ADMIN"
assert "8.3 月度学时趋势" "d['code']" 200

# ============ 9. 用户管理 ============
section "9. 用户管理与参数"
req POST /system/users "$ADMIN" "{\"username\":\"tlock$TS\",\"nickname\":\"锁定测试\",\"password\":\"test123\",\"deptId\":4,\"userType\":\"staff\",\"roleIds\":[3]}"
assert "9.1 新增用户(含角色)" "d['code']" 200
req GET "/system/users?page=1&size=5&keyword=tlock$TS" "$ADMIN"
UID1=$(jget "d['data']['records'][0]['id']")
tok "tlock$TS" test123 > /dev/null; jget "d['code']" > /tmp/e2e-flag.txt
if [ "$(cat /tmp/e2e-flag.txt)" == "200" ]; then PASS=$((PASS+1)); echo "  PASS 9.2 新用户按指定密码登录"
else FAIL=$((FAIL+1)); echo "  FAIL 9.2 新用户登录失败"; fi
req PUT "/system/users/$UID1" "$ADMIN" "{\"username\":\"tlock$TS\",\"nickname\":\"锁定测试改\",\"deptId\":4,\"userType\":\"staff\"}"
assert "9.3 不带roleIds的更新不丢角色(修复项)" "d['code']" 200
req GET "/system/users/$UID1" "$ADMIN"
assert "9.4 角色仍保留" "d['data']['roleIds']" '[3]'
req PUT "/system/users/$UID1/resetPassword" "$ADMIN" ; assert "9.5 重置密码" "d['code']" 200
tok "tlock$TS" 123456 > /dev/null; jget "d['code']" > /tmp/e2e-flag.txt
if [ "$(cat /tmp/e2e-flag.txt)" == "200" ]; then PASS=$((PASS+1)); echo "  PASS 9.6 重置后可用123456登录"
else FAIL=$((FAIL+1)); echo "  FAIL 9.6 重置密码后登录失败"; fi
CODE=$(curl -s -o "$R" -w "%{http_code}" "$BASE/system/users?page=1&size=10000" -H "Authorization: Bearer $ADMIN")
assertc "9.7 超大分页安全处理" 200

# ============ 10. 登录爆破锁定(放最后) ============
section "10. 登录失败锁定"
for i in 1 2 3 4 5; do curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"tlock$TS\",\"password\":\"bad$i\"}" -o /dev/null; done
curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"tlock$TS\",\"password\":\"123456\"}" -o "$R"
assert "10.1 连续失败5次后正确密码也被锁(修复项)" "d['code']" 429

echo
echo "======================"
echo "通过: $PASS  失败: $FAIL"
[ $FAIL -eq 0 ] && echo "全部通过 ✔" || { echo "存在失败用例:"; exit 2; }
