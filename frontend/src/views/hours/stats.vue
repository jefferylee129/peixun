<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="年份">
          <el-select v-model="query.year" style="width: 100px">
            <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptId"
            :data="depts"
            node-key="id"
            clearable
            check-strictly
            :props="{ label: 'deptName', children: 'children' }"
            placeholder="全部"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/工号"
            clearable
            style="width: 160px"
            @keyup.enter="load()"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load()">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
          <el-button :icon="Download" @click="exportUserStats">导出CSV</el-button>
          <el-button :icon="Download" @click="exportStats">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>员工年度学时达标统计</template>
      <el-table v-loading="loading" :data="userRows" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="userName" label="姓名" width="100" />
        <el-table-column prop="employeeNo" label="工号" width="110" />
        <el-table-column prop="deptName" label="部门" min-width="140" show-overflow-tooltip />
        <el-table-column label="人员类别" width="130" align="center">
          <template #default="{ row }">{{ USER_TYPE[row.userType] || '一般员工' }}</template>
        </el-table-column>
        <el-table-column prop="required" label="年度要求" width="90" align="center" />
        <el-table-column prop="totalHours" label="累计学时" width="90" align="center" />
        <el-table-column label="达标进度" min-width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.percent) || 0"
              :color="row.reached ? '#67c23a' : '#e6a23c'"
            />
          </template>
        </el-table-column>
        <el-table-column label="达标状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.reached ? 'success' : 'warning'" size="small">
              {{ row.reached ? '已达标' : '未达标' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 12px">
      <template #header>部门学时统计</template>
      <el-table v-loading="deptLoading" :data="deptRows" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deptName" label="部门" min-width="160" />
        <el-table-column prop="traineeCount" label="参训人数" width="100" align="center" />
        <el-table-column prop="totalHours" label="学时总量" width="110" align="center" />
        <el-table-column label="人均学时" width="110" align="center">
          <template #default="{ row }">{{ avgOf(row) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { hourUserStats, hourDeptStats } from '@/api/train'
import { deptTree } from '@/api/system'
import { USER_TYPE, yearOptions } from '@/utils/constants'
import { exportCsv } from '@/utils/export'
import { downloadExcel } from '@/utils/download'

const loading = ref(false)
const deptLoading = ref(false)
const userRows = ref([])
const deptRows = ref([])
const depts = ref([])

const query = reactive({ year: new Date().getFullYear(), deptId: null, keyword: '' })

async function loadUsers() {
  loading.value = true
  try {
    const res = await hourUserStats({ ...query })
    userRows.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadDepts() {
  deptLoading.value = true
  try {
    const res = await hourDeptStats({ year: query.year })
    deptRows.value = res.data
  } finally {
    deptLoading.value = false
  }
}

function load() {
  loadUsers()
  loadDepts()
}

function reset() {
  query.year = new Date().getFullYear()
  query.deptId = null
  query.keyword = ''
  load()
}

function avgOf(row) {
  return row.traineeCount ? (Number(row.totalHours) / row.traineeCount).toFixed(1) : '-'
}

function exportUserStats() {
  if (!userRows.value.length) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  exportCsv(`员工学时达标统计-${query.year}.csv`, [
    { label: '姓名', prop: 'userName' },
    { label: '工号', prop: 'employeeNo' },
    { label: '部门', prop: 'deptName' },
    { label: '人员类别', prop: 'userTypeLabel' },
    { label: '年度要求', prop: 'required' },
    { label: '累计学时', prop: 'totalHours' },
    { label: '达标进度(%)', prop: 'percent' },
    { label: '达标状态', prop: 'reachedLabel' }
  ], userRows.value.map(r => ({
    ...r,
    userTypeLabel: USER_TYPE[r.userType] || '一般员工',
    reachedLabel: r.reached ? '已达标' : '未达标'
  })))
}

function exportStats() {
  downloadExcel('/excel/hours-stats/export', { year: query.year, deptId: query.deptId, keyword: query.keyword }, `学时达标统计${query.year || ''}`)
}

onMounted(async () => {
  load()
  try {
    const res = await deptTree()
    depts.value = res.data
  } catch (e) {
    depts.value = []
  }
})
</script>
