<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <el-tabs v-model="type" class="type-tabs" @tab-change="load(1)">
            <el-tab-pane label="全部" name="" />
            <el-tab-pane label="达标预警" name="warn" />
            <el-tab-pane label="报名/开班" name="enroll" />
            <el-tab-pane label="评估催办" name="eval" />
            <el-tab-pane label="证书" name="cert" />
            <el-tab-pane label="考试" name="exam" />
            <el-tab-pane label="系统公告" name="system" />
          </el-tabs>
          <div class="head-actions">
            <el-checkbox v-model="onlyUnread" @change="load(1)">只看未读</el-checkbox>
            <el-button size="small" @click="markAll">全部已读</el-button>
            <el-button v-if="canAnnounce" size="small" type="primary" @click="annVisible = true">
              发布公告
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column width="60" align="center">
          <template #default="{ row }">
            <el-badge is-dot :hidden="!!row.isRead" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="MSG_TYPE[row.msgType]?.tag || 'info'" size="small">
              {{ MSG_TYPE[row.msgType]?.label || '消息' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" align="center">
          <template #default="{ row }">{{ String(row.createTime || '').slice(0, 19).replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" link type="primary" size="small" @click="mark(row)">已读</el-button>
            <span v-else class="read-mark">已读</span>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadList" />
    </el-card>

    <el-dialog v-model="annVisible" title="发布系统公告" width="520px">
      <el-form label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="ann.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="ann.content" type="textarea" :rows="4" maxlength="900" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="annVisible = false">取消</el-button>
        <el-button type="primary" :loading="annSending" @click="sendAnnounce">发送全员</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { myMessages, readMessage, readAllMessages, announce } from '@/api/optimize'
import Pagination from '@/components/Pagination.vue'

const store = useUserStore()
const canAnnounce = computed(() => store.hasPerm('biz:archive:other'))

const MSG_TYPE = {
  warn: { label: '达标预警', tag: 'danger' },
  enroll: { label: '报名开班', tag: 'primary' },
  eval: { label: '评估催办', tag: 'warning' },
  cert: { label: '证书', tag: 'success' },
  exam: { label: '考试', tag: 'danger' },
  system: { label: '公告', tag: 'info' }
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const type = ref('')
const onlyUnread = ref(false)
const query = reactive({ page: 1, size: 10 })

async function loadList(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await myMessages({
      ...query,
      type: type.value || null,
      unread: onlyUnread.value ? 1 : null
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function load(page) {
  loadList(page)
}

async function mark(row) {
  await readMessage(row.id)
  row.isRead = 1
}

async function markAll() {
  await readAllMessages()
  ElMessage.success('已全部标记已读')
  load(1)
}

const annVisible = ref(false)
const annSending = ref(false)
const ann = reactive({ title: '', content: '' })

async function sendAnnounce() {
  if (!ann.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  annSending.value = true
  try {
    const res = await announce({ title: ann.title.trim(), content: ann.content })
    ElMessage.success(`公告已发送给 ${res.data} 名在册用户`)
    annVisible.value = false
    ann.title = ''
    ann.content = ''
    load(1)
  } finally {
    annSending.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.type-tabs {
  flex: 1;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.read-mark {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
