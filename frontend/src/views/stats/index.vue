<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="年度">
          <el-select v-model="year" style="width: 110px" @change="loadAll">
            <el-option v-for="y in yearOptions()" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Refresh" @click="loadAll">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 总览卡片 -->
    <div class="stat-cards">
      <el-card v-for="card in cards" :key="card.label" shadow="never">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div>
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <el-card shadow="never">
        <template #header>月度学时趋势</template>
        <div ref="monthRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>各部门培训学时</template>
        <div ref="deptRef" class="chart-box"></div>
      </el-card>
    </div>
    <div class="chart-row">
      <el-card shadow="never">
        <template #header>班次状态分布</template>
        <div ref="classRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>计划状态分布</template>
        <div ref="planRef" class="chart-box"></div>
      </el-card>
    </div>
    <div class="chart-row">
      <el-card shadow="never">
        <template #header>培训需求类别分布</template>
        <div ref="needRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>课程分类分布</template>
        <div ref="courseRef" class="chart-box"></div>
      </el-card>
    </div>

    <!-- 培训经费预算执行 -->
    <el-card shadow="never" class="budget-card">
      <template #header>
        <div class="table-header">
          <span>培训经费预算执行({{ year }}年 · 万元)</span>
          <el-tooltip content="计划预算 = 当年已批准培训计划预算合计; 班次经费 = 当年开班且未取消的班次经费合计(按开班日期归年)。执行率 = 班次经费 / 计划预算" placement="bottom">
            <el-icon class="hint"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </template>
      <div class="budget-figures">
        <div class="fig">
          <div class="fig-value">{{ budget.planBudget ?? '-' }}</div>
          <div class="fig-label">计划预算</div>
        </div>
        <div class="fig">
          <div class="fig-value">{{ budget.classBudget ?? '-' }}</div>
          <div class="fig-label">班次经费</div>
        </div>
        <div class="fig">
          <div class="fig-value" :class="{ over: isOver }">{{ budget.usageRate == null ? (isOver ? '超预算' : '—') : budget.usageRate + '%' }}</div>
          <div class="fig-label">预算执行率</div>
        </div>
        <div class="fig">
          <div class="fig-value">{{ budget.unplannedBudget ?? 0 }}</div>
          <div class="fig-label">计划外班次经费</div>
        </div>
      </div>
      <div ref="budgetRef" class="chart-box"></div>
      <el-table :data="budget.byPlan || []" border size="small" max-height="260" class="plan-table">
        <el-table-column prop="planName" label="培训计划" min-width="200" show-overflow-tooltip />
        <el-table-column prop="classCount" label="班次数" width="80" align="center" />
        <el-table-column prop="planBudget" label="计划预算" width="100" align="center" />
        <el-table-column prop="classBudget" label="班次经费" width="100" align="center" />
        <el-table-column label="执行率" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.over" type="danger" size="small">超预算</el-tag>
            <span v-else>{{ row.usageRate == null ? '—' : row.usageRate + '%' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 员工学时达标 -->
    <el-card shadow="never">
      <template #header>
        <div class="table-header">
          <span>员工年度学时达标情况</span>
          <el-button type="primary" size="small" :icon="Download" @click="exportRows">导出CSV</el-button>
        </div>
      </template>
      <div style="margin-bottom: 12px">
        <el-input
          v-model="keyword"
          placeholder="姓名/工号"
          clearable
          style="width: 200px; margin-right: 8px"
          @keyup.enter="loadUserStats"
        />
        <el-button type="primary" @click="loadUserStats">查询</el-button>
      </div>
      <el-table v-loading="userLoading" :data="userRows" border stripe max-height="520">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="userName" label="姓名" width="110" />
        <el-table-column prop="employeeNo" label="工号" width="110" />
        <el-table-column prop="deptName" label="部门" min-width="130" />
        <el-table-column label="人员类型" width="130" align="center">
          <template #default="{ row }">{{ USER_TYPE[row.userType] || row.userType }}</template>
        </el-table-column>
        <el-table-column prop="required" label="年度要求" width="90" align="center" />
        <el-table-column prop="totalHours" label="累计学时" width="90" align="center" />
        <el-table-column label="完成进度" min-width="220">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :color="row.reached ? '#67c23a' : '#e6a23c'" />
          </template>
        </el-table-column>
        <el-table-column label="达标" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.reached ? 'success' : 'warning'" size="small">
              {{ row.reached ? '已达标' : '未达标' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh, Download, QuestionFilled } from '@element-plus/icons-vue'
import {
  statsOverview, statsMonthHours, statsDeptHours, statsClassStatus,
  statsPlanStatus, statsNeedType, statsCourseCategory, hourUserStats
} from '@/api/train'
import { NEED_TYPE, CLASS_STATUS, PLAN_STATUS, USER_TYPE, yearOptions } from '@/utils/constants'
import { exportCsv } from '@/utils/export'
import { statsBudget } from '@/api/optimize'

const year = ref(new Date().getFullYear())
const keyword = ref('')
const overview = ref({})
const userRows = ref([])
const userLoading = ref(false)
const isOver = computed(() => Number(budget.value.classBudget || 0) > Number(budget.value.planBudget || 0))

const monthRef = ref()
const deptRef = ref()
const classRef = ref()
const planRef = ref()
const needRef = ref()
const budgetRef = ref()
const budget = ref({})
const courseRef = ref()
const charts = {}

const cards = computed(() => [
  { label: '培训班次总数', value: overview.value.classCount ?? '-', icon: 'School', color: '#1677ff' },
  { label: '进行中班次', value: overview.value.classOngoing ?? '-', icon: 'Loading', color: '#13c2c2' },
  { label: '已批准计划', value: overview.value.planApproved ?? '-', icon: 'Calendar', color: '#722ed1' },
  { label: '计划完成率(%)', value: overview.value.planCompletionRate ?? '-', icon: 'DataLine', color: '#faad14' },
  { label: '年度学时总量', value: overview.value.yearHours ?? '-', icon: 'AlarmClock', color: '#52c41a' },
  { label: '人均学时', value: overview.value.avgUserHours ?? '-', icon: 'User', color: '#eb2f96' },
  { label: '培训覆盖率(%)', value: overview.value.coverage ?? '-', icon: 'PieChart', color: '#2f54eb' },
  { label: '待认定学时', value: overview.value.pendingHours ?? '-', icon: 'Clock', color: '#fa541c' }
])

function initChart(key, refEl) {
  if (!refEl.value) return null
  if (!charts[key]) {
    charts[key] = echarts.init(refEl.value)
  }
  return charts[key]
}

function pieOption(rows, nameMap) {
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['35%', '60%'],
      data: rows.map(r => ({ name: nameMap[r.status ?? r.category ?? r.needType] || r.status || r.category || r.needType, value: r.cnt }))
    }]
  }
}

async function loadAll() {
  const [ov, month, dept, cls, plan, need, course] = await Promise.all([
    statsOverview({ year: year.value }),
    statsMonthHours({ year: year.value }),
    statsDeptHours({ year: year.value }),
    statsClassStatus(),
    statsPlanStatus({ year: year.value }),
    statsNeedType({ year: year.value }),
    statsCourseCategory()
  ])
  overview.value = ov.data
  await nextTick()

  const m = initChart('month', monthRef)
  if (m) {
    const map = {}
    for (const r of month.data) map[r.month] = Number(r.hours)
    const months = Array.from({ length: 12 }, (_, i) => i + 1)
    m.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 44, right: 16, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: months.map(x => x + '月') },
      yAxis: { type: 'value', name: '学时' },
      series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: months.map(x => map[x] || 0), itemStyle: { color: '#1677ff' } }]
    })
  }

  const d = initChart('dept', deptRef)
  if (d) {
    d.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 44, right: 16, top: 24, bottom: 60 },
      xAxis: { type: 'category', data: dept.data.map(r => r.deptName), axisLabel: { rotate: 20 } },
      yAxis: { type: 'value', name: '学时' },
      series: [{ type: 'bar', barMaxWidth: 34, data: dept.data.map(r => Number(r.totalHours)), itemStyle: { color: '#52c41a' } }]
    })
  }

  const c = initChart('class', classRef)
  if (c) c.setOption(pieOption(cls.data, CLASS_STATUS))
  const p = initChart('plan', planRef)
  if (p) p.setOption(pieOption(plan.data, PLAN_STATUS))
  const n = initChart('need', needRef)
  if (n) n.setOption(pieOption(need.data, NEED_TYPE))
  const co = initChart('course', courseRef)
  if (co) co.setOption(pieOption(course.data, NEED_TYPE))

  loadUserStats()
  loadBudget()
}

async function loadBudget() {
  const res = await statsBudget({ year: year.value })
  budget.value = res.data || {}
  await nextTick()
  const b = initChart('budget', budgetRef)
  if (!b) return
  const rows = budget.value.byDept || []
  b.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0 },
    grid: { left: 44, right: 16, top: 24, bottom: 44 },
    xAxis: { type: 'category', data: rows.map(r => r.deptName), axisLabel: { rotate: 20 } },
    yAxis: { type: 'value', name: '万元' },
    series: [
      { name: '计划预算', type: 'bar', barMaxWidth: 30, data: rows.map(r => Number(r.planBudget)), itemStyle: { color: '#1677ff' } },
      { name: '班次经费', type: 'bar', barMaxWidth: 30, data: rows.map(r => Number(r.classBudget)), itemStyle: { color: '#fa8c16' } }
    ]
  })
}

async function loadUserStats() {
  userLoading.value = true
  try {
    const res = await hourUserStats({ year: year.value, keyword: keyword.value || undefined })
    userRows.value = res.data
  } finally {
    userLoading.value = false
  }
}

function exportRows() {
  exportCsv(`员工学时达标统计${year.value}.csv`, [
    { label: '姓名', prop: 'userName' },
    { label: '工号', prop: 'employeeNo' },
    { label: '部门', prop: 'deptName' },
    { label: '人员类型', prop: 'userTypeName' },
    { label: '年度要求', prop: 'required' },
    { label: '累计学时', prop: 'totalHours' },
    { label: '是否达标', prop: 'reachedText' }
  ], userRows.value.map(r => ({
    ...r,
    userTypeName: USER_TYPE[r.userType] || r.userType,
    reachedText: r.reached ? '已达标' : '未达标'
  })))
}

function resize() {
  Object.values(charts).forEach(c => c.resize())
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.budget-card {
  margin-bottom: 12px;
}

.hint {
  color: #909399;
  cursor: help;
  vertical-align: -2px;
}

.budget-figures {
  display: flex;
  justify-content: space-around;
  margin-bottom: 8px;
}

.fig {
  text-align: center;
}

.fig-value {
  font-size: 24px;
  font-weight: 700;
}

.fig-value.over {
  color: #f56c6c;
}

.fig-label {
  color: #909399;
  font-size: 12px;
}

.plan-table {
  margin-top: 8px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
