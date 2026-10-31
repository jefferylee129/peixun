<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <div class="summary">
        <el-progress
          type="dashboard"
          :percentage="Number(summary.percent) || 0"
          :color="summary.reached ? '#67c23a' : '#e6a23c'"
          :width="130"
        >
          <template #default>
            <div class="summary-value">{{ summary.total ?? 0 }}</div>
            <div class="summary-label">累计学时</div>
          </template>
        </el-progress>
        <div class="summary-desc">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="年份">
              <el-select v-model="year" style="width: 100px" @change="load">
                <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
              </el-select>
            </el-form-item>
          </el-form>
          <p>年度要求: <b>{{ summary.required ?? '-' }}</b> 学时</p>
          <p>
            达标状态:
            <el-tag :type="summary.reached ? 'success' : 'warning'" size="small">
              {{ summary.reached ? '已达标' : '未达标' }}
            </el-tag>
          </p>
          <p class="tip">学时由班次签到、手工补录与外部认定汇总生成, 仅统计已认定记录</p>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="recordDate" label="日期" width="110" align="center" />
        <el-table-column prop="className" label="班次" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip />
        <el-table-column prop="hours" label="学时" width="80" align="center" />
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">{{ HOUR_SOURCE[row.source] || row.source }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(HOUR_STATUS[row.status] || {}).type || 'info'" size="small">
              {{ (HOUR_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="认定备注" min-width="140" show-overflow-tooltip />
      </el-table>
      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadList" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { myHours, myHourSummary } from '@/api/train'
import { HOUR_SOURCE, HOUR_STATUS, yearOptions } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const summary = ref({})
const year = ref(new Date().getFullYear())
const query = reactive({ page: 1, size: 10 })

async function loadSummary() {
  try {
    const res = await myHourSummary({ year: year.value })
    summary.value = res.data
  } catch (e) {
    summary.value = {}
  }
}

async function loadList(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await myHours({ ...query, year: year.value })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function load() {
  loadSummary()
  loadList(1)
}

onMounted(() => load())
</script>

<style scoped>
.summary {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
}

.summary-label {
  color: #909399;
  font-size: 12px;
}

.summary-desc {
  color: #606266;
  line-height: 2;
}

.summary-desc .tip {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
