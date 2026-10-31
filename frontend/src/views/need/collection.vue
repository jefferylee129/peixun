<template>
  <div class="page">
    <el-card shadow="never">
      <el-tabs v-model="tab">
        <!-- 征集任务维护 -->
        <el-tab-pane label="征集任务" name="task">
          <div class="toolbar">
            <el-button v-perm="'biz:need:audit'" type="primary" :icon="Plus" @click="openForm()">新增征集任务</el-button>
            <el-select v-model="taskStatus" clearable placeholder="全部状态" style="width: 120px" @change="loadTasks">
              <el-option v-for="(item, key) in COLLECTION_STATUS" :key="key" :label="item.label" :value="key" />
            </el-select>
          </div>

          <el-table v-loading="taskLoading" :data="tasks" border stripe>
            <el-table-column prop="title" label="征集标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="year" label="年度" width="70" align="center" />
            <el-table-column label="开始时间" width="150" align="center">
              <template #default="{ row }">{{ fmtTime(row.startTime) }}</template>
            </el-table-column>
            <el-table-column label="结束时间" width="150" align="center">
              <template #default="{ row }">{{ fmtTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="(COLLECTION_STATUS[row.status] || {}).type || 'info'">
                  {{ (COLLECTION_STATUS[row.status] || {}).label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-perm="'biz:need:audit'" link type="primary" @click="openForm(row)">编辑</el-button>
                <el-button
                  v-if="row.status === '0'"
                  v-perm="'biz:need:audit'"
                  link
                  type="warning"
                  @click="close(row)"
                >结束征集</el-button>
                <el-button v-perm="'biz:need:audit'" link type="danger" @click="del(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 需求审核 -->
        <el-tab-pane label="需求审核" name="audit">
          <el-form :inline="true" :model="query" class="audit-search" @submit.prevent>
            <el-form-item label="年度">
              <el-select v-model="query.year" clearable placeholder="全部" style="width: 110px">
                <el-option v-for="y in years" :key="y" :label="y" :value="y" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
                <el-option v-for="(item, key) in NEED_STATUS" :key="key" :label="item.label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item label="类别">
              <el-select v-model="query.needType" clearable placeholder="全部" style="width: 130px">
                <el-option v-for="(label, key) in NEED_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input
                v-model="query.keyword"
                placeholder="需求标题"
                clearable
                style="width: 180px"
                @keyup.enter="loadAudit(1)"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Search" @click="loadAudit(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetAudit">重置</el-button>
              <el-button v-perm="'biz:need:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="loading" :data="list" border stripe>
            <el-table-column type="expand">
              <template #default="{ row }">
                <el-descriptions :column="2" border size="small" class="expand-desc">
                  <el-descriptions-item label="需求描述" :span="2">{{ row.content || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="申报理由" :span="2">{{ row.reason || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="期望时间">{{ row.expectedTime || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="建议参训人数">{{ row.participants || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="审核意见" :span="2">{{ row.auditRemark || '-' }}</el-descriptions-item>
                </el-descriptions>
              </template>
            </el-table-column>
            <el-table-column prop="year" label="年度" width="70" align="center" />
            <el-table-column prop="title" label="需求标题" min-width="180" show-overflow-tooltip />
            <el-table-column label="类别" width="90" align="center">
              <template #default="{ row }">{{ NEED_TYPE[row.needType] || row.needType }}</template>
            </el-table-column>
            <el-table-column prop="deptName" label="申报部门" width="130" show-overflow-tooltip />
            <el-table-column prop="userName" label="申报人" width="90" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="(NEED_STATUS[row.status] || {}).type || 'info'">
                  {{ (NEED_STATUS[row.status] || {}).label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申报时间" width="110" align="center">
              <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <template v-if="row.status === '1'">
                  <el-button v-perm="'biz:need:audit'" link type="success" @click="doAudit(row, true)">通过</el-button>
                  <el-button v-perm="'biz:need:audit'" link type="danger" @click="doAudit(row, false)">驳回</el-button>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadAudit()" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑征集任务' : '新增征集任务'" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="征集标题" prop="title">
          <el-input v-model="form.title" placeholder="如: 2027年度培训需求征集" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="年度" prop="year">
              <el-select v-model="form.year" style="width: 100%">
                <el-option v-for="y in years" :key="y" :label="y" :value="y" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="征集说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="征集范围与填报要求(可空)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Download } from '@element-plus/icons-vue'
import {
  pageNeed, auditNeed,
  collections, addCollection, updateCollection, closeCollection, deleteCollection
} from '@/api/biz'
import { NEED_TYPE, NEED_STATUS, COLLECTION_STATUS, yearOptions } from '@/utils/constants'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const years = yearOptions()
const tab = ref('task')

// ---- 征集任务 ----
const taskLoading = ref(false)
const tasks = ref([])
const taskStatus = ref('')
const saving = ref(false)
const formVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null, title: '', year: new Date().getFullYear(), startTime: '', endTime: '', description: ''
})
const form = reactive(emptyForm())

const rules = {
  title: [{ required: true, message: '请输入征集标题', trigger: 'blur' }],
  year: [{ required: true, message: '请选择年度', trigger: 'change' }]
}

// ---- 需求审核 ----
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, year: '', status: '', needType: '', keyword: '' })

function fmtTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : '-'
}

async function loadTasks() {
  taskLoading.value = true
  try {
    const res = await collections(taskStatus ? { status: taskStatus } : {})
    tasks.value = res.data
  } finally {
    taskLoading.value = false
  }
}

async function loadAudit(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageNeed(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function resetAudit() {
  query.year = ''
  query.status = ''
  query.needType = ''
  query.keyword = ''
  loadAudit(1)
}

function openForm(row) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, row)
  }
  formVisible.value = true
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        ...form,
        startTime: form.startTime || null,
        endTime: form.endTime || null
      }
      if (form.id) {
        await updateCollection(form.id, payload)
      } else {
        await addCollection(payload)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      loadTasks()
    } finally {
      saving.value = false
    }
  })
}

function close(row) {
  ElMessageBox.confirm(`确认结束征集任务「${row.title}」?结束后不再展示给申报人。`, '提示', { type: 'warning' })
    .then(async () => {
      await closeCollection(row.id)
      ElMessage.success('已结束征集')
      loadTasks()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除征集任务「${row.title}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCollection(row.id)
      ElMessage.success('删除成功')
      loadTasks()
    })
    .catch(() => {})
}

function doAudit(row, adopt) {
  ElMessageBox.prompt(
    adopt ? `通过「${row.title}」的审核?可填写备注` : `驳回「${row.title}」, 请填写驳回原因`,
    adopt ? '审核通过' : '审核驳回',
    { inputPlaceholder: '备注(可空)', type: adopt ? 'success' : 'warning' }
  )
    .then(async ({ value }) => {
      await auditNeed(row.id, { adopt, remark: value || '' })
      ElMessage.success('操作成功')
      loadAudit()
    })
    .catch(() => {})
}

onMounted(() => {
  loadTasks()
  loadAudit()
})

function exportRows() {
  downloadExcel('/excel/need/export', { year: query.year, status: query.status, needType: query.needType, keyword: query.keyword }, '培训需求台账')
}
</script>

<style scoped>
.audit-search {
  margin-bottom: 12px;
}

.expand-desc {
  margin: 8px 12px;
}
</style>
