<template>
  <div class="page">
    <el-alert
      v-for="c in actives"
      :key="c.id"
      class="collection-alert"
      type="success"
      show-icon
      :closable="false"
    >
      「{{ c.title }}」正在征集培训需求, 截止时间 {{ fmtTime(c.endTime) }}, 欢迎填报。
    </el-alert>

    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
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
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="需求标题"
            clearable
            style="width: 200px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openForm()">新增需求申报</el-button>
        <el-button :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="year" label="年度" width="70" align="center" />
        <el-table-column prop="title" label="需求标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类别" width="90" align="center">
          <template #default="{ row }">{{ NEED_TYPE[row.needType] || row.needType }}</template>
        </el-table-column>
        <el-table-column prop="expectedTime" label="期望时间" width="90" align="center">
          <template #default="{ row }">{{ row.expectedTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="participants" label="参训人数" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(NEED_STATUS[row.status] || {}).type || 'info'">
              {{ (NEED_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.auditRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="申报时间" width="110" align="center">
          <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '0' || row.status === '3'"
              link
              type="primary"
              @click="openForm(row)"
            >编辑</el-button>
            <el-button v-if="row.status === '0'" link type="warning" @click="submit(row)">提交审核</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑需求申报' : '新增需求申报'" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="需求标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入需求标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需求类别" prop="needType">
              <el-select v-model="form.needType" style="width: 100%">
                <el-option v-for="(label, key) in NEED_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="年度" prop="year">
              <el-select v-model="form.year" style="width: 100%">
                <el-option v-for="y in years" :key="y" :label="y" :value="y" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联征集" prop="collectionId">
              <el-select v-model="form.collectionId" clearable placeholder="无" style="width: 100%">
                <el-option v-for="c in actives" :key="c.id" :label="c.title" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="期望时间" prop="expectedTime">
              <el-date-picker
                v-model="form.expectedTime"
                type="month"
                value-format="YYYY-MM"
                placeholder="期望培训时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参训人数" prop="participants">
              <el-input-number v-model="form.participants" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="需求描述" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="希望开展的培训内容或课程" />
        </el-form-item>
        <el-form-item label="申报理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="申报背景与理由(可空)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入培训需求" template-type="need" action="/excel/need/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { myNeeds, addNeed, updateNeed, deleteNeed, submitNeed, activeCollections } from '@/api/biz'
import { NEED_TYPE, NEED_STATUS, yearOptions } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const years = yearOptions()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const actives = ref([])
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, year: '', status: '', keyword: '' })

const emptyForm = () => ({
  id: null, title: '', needType: 'skill', year: years[0], collectionId: null,
  expectedTime: '', participants: null, content: '', reason: ''
})
const form = reactive(emptyForm())

const rules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  needType: [{ required: true, message: '请选择需求类别', trigger: 'change' }],
  year: [{ required: true, message: '请选择年度', trigger: 'change' }],
  content: [{ required: true, message: '请填写需求描述', trigger: 'blur' }]
}

function fmtTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

function fmtDate(v) {
  return v ? String(v).slice(0, 10) : '-'
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await myNeeds(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.year = ''
  query.status = ''
  query.keyword = ''
  load(1)
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
        collectionId: form.collectionId || null,
        expectedTime: form.expectedTime || null,
        participants: form.participants || null
      }
      if (form.id) {
        await updateNeed(form.id, payload)
      } else {
        await addNeed(payload)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function submit(row) {
  ElMessageBox.confirm(`确认提交「${row.title}」进入审核?`, '提示', { type: 'warning' })
    .then(async () => {
      await submitNeed(row.id)
      ElMessage.success('已提交审核')
      load()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除需求「${row.title}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteNeed(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  load()
  try {
    const res = await activeCollections()
    actives.value = res.data
  } catch (e) {
    actives.value = []
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/need-my/export', { year: query.year, status: query.status, keyword: query.keyword }, '我的培训需求')
}
</script>

<style scoped>
.collection-alert {
  margin-bottom: 12px;
}
</style>
