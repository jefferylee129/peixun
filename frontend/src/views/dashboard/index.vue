<template>
  <div class="page">
    <!-- 统计卡片 -->
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

    <!-- 图表 -->
    <div class="chart-row" v-if="store.hasPerm('biz:stats:view')">
      <el-card shadow="never">
        <template #header>本年度月度学时趋势</template>
        <div ref="monthChartRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>课程分类分布</template>
        <div ref="categoryChartRef" class="chart-box"></div>
      </el-card>
    </div>
    <div class="chart-row" v-if="store.hasPerm('biz:stats:view')">
      <el-card shadow="never">
        <template #header>各部门培训学时</template>
        <div ref="deptChartRef" class="chart-box"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>我的年度学时</template>
        <div class="my-hours">
          <el-progress type="dashboard" :percentage="mySummary.percent || 0"
            :color="mySummary.reached ? '#67c23a' : '#e6a23c'" :width="150">
            <template #default>
              <div class="my-hours-value">{{ mySummary.total || 0 }}</div>
              <div class="my-hours-label">累计学时</div>
            </template>
          </el-progress>
          <div class="my-hours-desc">
            <p>年度要求:<b>{{ mySummary.required }}</b> 学时</p>
            <p>
              达标状态:
              <el-tag :type="mySummary.reached ? 'success' : 'warning'" size="small">
                {{ mySummary.reached ? '已达标' : '未达标' }}
              </el-tag>
            </p>
            <p class="tip">学时由签到、补录与外部认定汇总生成</p>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 最近班次 -->
    <el-card shadow="never" v-if="store.hasPerm('biz:class:list')">
      <template #header>最近培训班次</template>
      <el-table :data="recentClasses" size="small">
        <el-table-column prop="className" label="班次名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(CLASS_STATUS[row.status] || {}).type || 'info'" size="small">
              {{ (CLASS_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="endDate" label="结束日期" width="110" />
        <el-table-column prop="studentCount" label="报名人数" width="90" align="center" />
        <el-table-column prop="deptName" label="主办部门" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useUserStore } from '@/store/user'
import { statsOverview, statsMonthHours, statsDeptHours, statsCourseCategory } from '@/api/train'
import { myHourSummary } from '@/api/train'
import { pageClass } from '@/api/train'
import { NEED_TYPE, CLASS_STATUS } from '@/utils/constants'

const store = useUserStore()
const year = new Date().getFullYear()

const overview = ref({})
const mySummary = ref({})
const recentClasses = ref([])
const monthChartRef = ref()
const categoryChartRef = ref()
const deptChartRef = ref()
let monthChart = null
let categoryChart = null
let deptChart = null

const cards = computed(() => [
  { label: '培训班次总数', value: overview.value.classCount ?? '-', icon: 'School', color: '#1677ff' },
  { label: '进行中/报名中班次', value: overview.value.classOngoing ?? '-', icon: 'Loading', color: '#13c2c2' },
  { label: '已发布课程', value: overview.value.courseCount ?? '-', icon: 'Reading', color: '#722ed1' },
  { label: '在聘师资', value: overview.value.trainerCount ?? '-', icon: 'Avatar', color: '#fa8c16' },
  { label: `${year}年学时总量`, value: overview.value.yearHours ?? '-', icon: 'AlarmClock', color: '#52c41a' },
  { label: '人均学时', value: overview.value.avgUserHours ?? '-', icon: 'User', color: '#eb2f96' },
  { label: '培训覆盖率(%)', value: overview.value.coverage ?? '-', icon: 'PieChart', color: '#2f54eb' },
  { label: '计划完成率(%)', value: overview.value.planCompletionRate ?? '-', icon: 'DataLine', color: '#faad14' }
])

function renderMonthChart(rows) {
  if (!monthChartRef.value) return
  monthChart = monthChart || echarts.init(monthChartRef.value)
  const map = {}
  for (const r of rows) map[r.month] = Number(r.hours)
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  monthChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 20, bottom: 28 },
    xAxis: { type: 'category', data: months.map(m => m + '月') },
    yAxis: { type: 'value', name: '学时' },
    series: [{
      type: 'line', smooth: true, areaStyle: { opacity: 0.15 },
      data: months.map(m => map[m] || 0), itemStyle: { color: '#1677ff' }
    }]
  })
}

function renderCategoryChart(rows) {
  if (!categoryChartRef.value) return
  categoryChart = categoryChart || echarts.init(categoryChartRef.value)
  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['38%', '62%'],
      data: rows.map(r => ({ name: NEED_TYPE[r.category] || r.category, value: r.cnt }))
    }]
  })
}

function renderDeptChart(rows) {
  if (!deptChartRef.value) return
  deptChart = deptChart || echarts.init(deptChartRef.value)
  deptChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 16, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: rows.map(r => r.deptName), axisLabel: { rotate: 20 } },
    yAxis: { type: 'value', name: '学时' },
    series: [{
      type: 'bar', barMaxWidth: 36,
      data: rows.map(r => Number(r.totalHours)), itemStyle: { color: '#52c41a' }
    }]
  })
}

function resizeCharts() {
  monthChart && monthChart.resize()
  categoryChart && categoryChart.resize()
  deptChart && deptChart.resize()
}

onMounted(async () => {
  if (store.hasPerm('biz:stats:view')) {
    const [ov, month, dept, cat] = await Promise.all([
      statsOverview({ year }), statsMonthHours({ year }), statsDeptHours({ year }), statsCourseCategory()
    ])
    overview.value = ov.data
    await nextTick()
    renderMonthChart(month.data)
    renderDeptChart(dept.data)
    renderCategoryChart(cat.data)
    window.addEventListener('resize', resizeCharts)
  }
  if (store.hasPerm('biz:class:list')) {
    const res = await pageClass({ page: 1, size: 5 })
    recentClasses.value = res.data.records
  }
  try {
    const res = await myHourSummary({ year })
    mySummary.value = res.data
  } catch (e) {
    mySummary.value = {}
  }
})
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  color: #909399;
  font-size: 12px;
}

.my-hours {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 100%;
}

.my-hours-value {
  font-size: 26px;
  font-weight: 700;
}

.my-hours-label {
  color: #909399;
  font-size: 12px;
}

.my-hours-desc {
  color: #606266;
  line-height: 2;
}

.my-hours-desc .tip {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
