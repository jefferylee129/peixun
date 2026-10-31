<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="操作人/模块/操作内容"
            clearable
            style="width: 220px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" value="0" />
            <el-option label="失败" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="130" show-overflow-tooltip />
        <el-table-column prop="operation" label="操作内容" min-width="160" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'">
              {{ row.status === '0' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作时间" width="160" align="center">
          <template #default="{ row }">{{ fmtTime(row.operTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="680px" top="8vh">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="操作人">{{ current.username }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="current.status === '0' ? 'success' : 'danger'">
            {{ current.status === '0' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作内容" :span="2">{{ current.operation }}</el-descriptions-item>
        <el-descriptions-item label="方法" :span="2">{{ current.method }}</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ fmtTime(current.operTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="错误信息" :span="2">
          {{ current.errorMsg }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ prettyParams }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { pageLog } from '@/api/system'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const current = ref({})

const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const prettyParams = computed(() => {
  const p = current.value.params
  if (!p) return ''
  try {
    return JSON.stringify(JSON.parse(p), null, 2)
  } catch (e) {
    return p
  }
})

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageLog(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.status = ''
  load(1)
}

function showDetail(row) {
  current.value = row
  detailVisible.value = true
}

/** 后端 LocalDateTime 兼容展示: 去掉 ISO 格式中的 T */
function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(() => load())
</script>

<style scoped>
.params-pre {
  margin: 0;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.6;
}
</style>
