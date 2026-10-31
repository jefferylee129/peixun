#!/bin/bash
# =====================================================================
# Excel 导入导出接口回环测试(需后端 18080 已启动)
# 策略: 导出端点校验 HTTP 200 + xlsx 魔数(PK) + 体积;
#       模板下载后原样回传导入端点(模板示例行即合法数据)验证导入链路
# =====================================================================
BASE=${BASE:-http://localhost:18080/api}
T=/tmp/excel-test
mkdir -p $T
PASS=0; FAIL=0

check() { if [ "$1" == "$2" ]; then PASS=$((PASS+1)); echo "  PASS $3"; else FAIL=$((FAIL+1)); echo "  FAIL $3 expect=[$2] actual=[$1] $4"; fi; }

ADMIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
U1=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"zhangsan","password":"user123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
A="Authorization: Bearer $ADMIN"

exp() { # exp URL 文件名 用例名
  local code size magic
  code=$(curl -s -o "$T/out.xlsx" -w "%{http_code}" -H "$A" "$BASE$1")
  size=$(stat -f%z "$T/out.xlsx" 2>/dev/null || echo 0)
  magic=$(xxd -p -l 2 "$T/out.xlsx" 2>/dev/null)
  if [ "$code" == "200" ] && [ "$magic" == "504b" ] && [ "$size" -gt 2000 ]; then
    PASS=$((PASS+1)); echo "  PASS $3"
  else
    FAIL=$((FAIL+1)); echo "  FAIL $3 code=$code size=$size magic=$magic"
  fi
}

imp() { # imp URL 模板类型 用例名  (模板仅1行示例, 成功+跳过==1 即链路正确; 跳过=重复导入幂等)
  curl -s -o "$T/out.xlsx" -H "$A" "$BASE/excel/template/$2"
  local res ok
  res=$(curl -s -X POST -H "$A" -F "file=@$T/out.xlsx" "$BASE$1")
  ok=$(echo "$res" | python3 -c "import sys,json;d=json.load(sys.stdin);r=d.get('data') or {};print(r.get('success',0)+r.get('skip',0) if d.get('code')==200 and r else 'ERR:'+str(d.get('msg')))")
  if [ "$ok" == "1" ]; then PASS=$((PASS+1)); echo "  PASS $3"
  else FAIL=$((FAIL+1)); echo "  FAIL $3 resp=$res"; fi
}

echo "== 导出端点 =="
exp "/excel/need/export?year=2026"                     need.xlsx      "1.1 需求导出"
exp "/excel/plan/export?year=2026"                     plan.xlsx      "1.2 计划导出"
exp "/excel/course/export"                             course.xlsx    "1.3 课程导出"
exp "/excel/trainer/export"                            trainer.xlsx   "1.4 师资导出"
exp "/excel/class/export?year=2026"                    class.xlsx     "1.5 班次导出"
exp "/excel/hours/export?year=2026"                    hours.xlsx     "1.6 学时导出"
exp "/excel/hours-stats/export?year=2026"              stats.xlsx     "1.7 达标统计导出"
exp "/excel/user/export"                               user.xlsx      "1.8 用户导出"
exp "/excel/need-my/export"                            myneed.xlsx    "1.9 我的需求导出(员工自助)"
CODE=$(curl -s -o "$T/out.xlsx" -w "%{http_code}" -H "Authorization: Bearer $U1" "$BASE/excel/user/export")
BCODE=$(python3 -c "import json;print(json.load(open('$T/out.xlsx'))['code'])" 2>/dev/null || echo none)
check "$CODE/$BCODE" "200/403" "1.10 员工导出全量用户被拒"
CODE=$(curl -s -o "$T/out.xlsx" -w "%{http_code}" "$BASE/excel/course/export")
check "$CODE" 401 "1.11 未登录导出被拒"

echo "== 模板下载 =="
for t in need plan course trainer class hours class-student user; do
  code=$(curl -s -o "$T/tpl-$t.xlsx" -w "%{http_code}" -H "$A" "$BASE/excel/template/$t")
  magic=$(xxd -p -l 2 "$T/tpl-$t.xlsx" 2>/dev/null)
  check "$code/$magic" "200/504b" "2.$t 模板下载"
done

echo "== 导入回环(模板示例行即合法数据) =="
imp "/excel/need/import"        need          "3.1 需求导入(员工自助)"    1
imp "/excel/plan/import"        plan          "3.2 计划导入"              1
imp "/excel/course/import"      course        "3.3 课程导入(重复自动跳过)" any
imp "/excel/trainer/import"     trainer       "3.4 师资导入(重复自动跳过)" any
imp "/excel/class/import"       class         "3.5 班次导入"              1
imp "/excel/hours/import"       hours         "3.6 学时导入(重复自动跳过)" any
imp "/excel/user/import"        user          "3.7 用户导入(重复自动跳过)" any

echo "== 班级学员导入回环 =="
CLID=$(curl -s -X POST -H "$A" -H 'Content-Type: application/json' \
  -d "{\"className\":\"EXCEL测试班$RANDOM\",\"classType\":\"internal\",\"deptId\":3,\"startDate\":\"2026-11-01\",\"endDate\":\"2026-11-02\",\"capacity\":40}" \
  "$BASE/biz/classes" > /dev/null; curl -s -H "$A" -G "$BASE/biz/classes" --data-urlencode "page=1" --data-urlencode "size=1" --data-urlencode "keyword=EXCEL测试班" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['records'][0]['id'])")
curl -s -o "$T/stu1.xlsx" -H "$A" "$BASE/excel/template/class-student"
res=$(curl -s -X POST -H "$A" -F "file=@$T/stu1.xlsx" "$BASE/excel/class/$CLID/students/import")
ok1=$(echo "$res" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['success'])")
check "$ok1" 1 "4.1 学员导入成功1条"
res=$(curl -s -X POST -H "$A" -F "file=@$T/stu1.xlsx" "$BASE/excel/class/$CLID/students/import")
ok2=$(echo "$res" | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];print(d['skip'])")
check "$ok2" 1 "4.2 重复导入被跳过"
code=$(curl -s -o "$T/out.xlsx" -w "%{http_code}" -H "$A" "$BASE/excel/class/$CLID/students/export")
magic=$(xxd -p -l 2 "$T/out.xlsx")
check "$code/$magic" "200/504b" "4.3 学员名单导出"

echo "== 评估导出(含双Sheet) =="
CLID2=$(curl -s -H "$A" "$BASE/biz/classes?page=1&size=1&status=3" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['records'][0]['id'])")
EID=$(curl -s -H "$A" "$BASE/biz/evals/templates?page=1&size=1" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['records'][0]['id'])")
code=$(curl -s -o "$T/out.xlsx" -w "%{http_code}" -H "$A" "$BASE/excel/eval/export?classId=$CLID2&templateId=$EID")
magic=$(xxd -p -l 2 "$T/out.xlsx")
check "$code/$magic" "200/504b" "5.1 评估导出(汇总+明细)"

echo
echo "通过: $PASS  失败: $FAIL"
[ $FAIL -eq 0 ] && echo "全部通过 ✔" || exit 2
