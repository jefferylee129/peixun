<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>在线考试(柯氏二级 · 学习层)</template>
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="title" label="考试名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="className" label="班次" min-width="160" show-overflow-tooltip />
        <el-table-column label="作答窗口" width="320" align="center">
          <template #default="{ row }">
            {{ fmt(row.startTime) }} ~ {{ fmt(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMin" label="时长(分)" width="90" align="center" />
        <el-table-column prop="hourReward" label="及格计学时" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.submitted" :type="row.passed === 1 ? 'success' : 'danger'" size="small">
              {{ row.passed === 1 ? '及格' : '不及格' }}
            </el-tag>
            <el-tag v-else-if="row.status === '2'" type="info" size="small">已截止</el-tag>
            <el-tag v-else-if="expired(row)" type="info" size="small">已过期</el-tag>
            <el-tag v-else type="primary" size="small">待作答</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="80" align="center">
          <template #default="{ row }">{{ row.submitted ? row.score : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button
              v-if="!row.submitted && row.status === '1' && !expired(row)"
              type="primary" size="small"
              @click="$router.push(`/exam-do/${row.id}`)"
            >开始考试</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无考试, 参加班次后管理员发布考试即可在线作答" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { myExams } from '@/api/optimize'

const loading = ref(false)
const list = ref([])

function fmt(t) {
  return String(t || '').slice(0, 16).replace('T', ' ')
}

function expired(row) {
  return row.endTime && new Date(String(row.endTime).replace('T', ' ')) < new Date()
}

async function load() {
  loading.value = true
  try {
    const res = await myExams()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
