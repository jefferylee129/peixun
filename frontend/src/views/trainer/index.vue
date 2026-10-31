<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/专业领域/所在单位"
            clearable
            style="width: 200px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item label="讲师类型">
          <el-select v-model="query.trainerType" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="(label, key) in TRAINER_TYPE" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(item, key) in TRAINER_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-perm="'biz:trainer:add'" type="primary" :icon="Plus" @click="openForm()">新增师资</el-button>
        <el-button v-perm="'biz:trainer:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'biz:trainer:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="trainerName" label="姓名" width="100" />
        <el-table-column label="性别" width="60" align="center">
          <template #default="{ row }">{{ row.gender === '1' ? '女' : '男' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ TRAINER_TYPE[row.trainerType] || row.trainerType }}</template>
        </el-table-column>
        <el-table-column prop="orgName" label="所在单位" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.orgName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="title" label="职称/职务" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '-' }}</template>
        </el-table-column>
        <el-table-column prop="specialty" label="专业领域" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.specialty || '-' }}</template>
        </el-table-column>
        <el-table-column prop="level" label="讲师等级" width="90" align="center">
          <template #default="{ row }">{{ row.level || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="(TRAINER_STATUS[row.status] || {}).type || 'info'">
              {{ (TRAINER_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-perm="'biz:trainer:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'biz:trainer:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑师资' : '新增师资'" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="trainerName">
              <el-input v-model="form.trainerName" placeholder="请输入讲师姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio value="0">男</el-radio>
                <el-radio value="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="讲师类型" prop="trainerType">
              <el-select v-model="form.trainerType" style="width: 100%">
                <el-option v-for="(label, key) in TRAINER_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="讲师等级" prop="level">
              <el-select v-model="form.level" clearable style="width: 100%">
                <el-option v-for="l in LEVELS" :key="l" :label="l" :value="l" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所在单位" prop="orgName" :rules="orgRules">
              <el-input v-model="form.orgName" placeholder="外部专家必填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称/职务" prop="title">
              <el-input v-model="form.title" placeholder="如: 高级工程师" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="专业领域" prop="specialty">
          <el-input v-model="form.specialty" placeholder="如: 安全管理、财务分析" />
        </el-form-item>
        <el-form-item label="启用状态" prop="status">
          <el-switch v-model="form.status" active-value="0" inactive-value="1" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="师资简介" prop="intro">
          <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="讲师经历与擅长方向(可空)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" :title="`${current.trainerName || ''} - 师资详情`" width="760px" top="6vh">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ current.trainerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ TRAINER_TYPE[current.trainerType] || current.trainerType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所在单位">{{ current.orgName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="职称/职务">{{ current.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="专业领域">{{ current.specialty || '-' }}</el-descriptions-item>
        <el-descriptions-item label="讲师等级">{{ current.level || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ current.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="师资简介" :span="2">{{ current.intro || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="detail">
        <el-descriptions :column="2" border size="small" class="teach-stats">
          <el-descriptions-item label="已完成授课">{{ detail.teachCount }} 次</el-descriptions-item>
          <el-descriptions-item label="累计学时">{{ detail.totalHours }}</el-descriptions-item>
        </el-descriptions>
        <el-table
          v-if="detail.teachingRecords && detail.teachingRecords.length"
          :data="detail.teachingRecords"
          border
          size="small"
        >
          <el-table-column prop="className" label="班次" min-width="140" show-overflow-tooltip />
          <el-table-column prop="courseName" label="课程" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.courseName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="teachDate" label="上课日期" width="100" align="center" />
          <el-table-column prop="hours" label="学时" width="70" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              {{ (SCHEDULE_STATUS[row.scheduleStatus] || {}).label || row.scheduleStatus }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无授课记录" :image-size="60" />
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入师资" template-type="trainer" action="/excel/trainer/import" @success="load()" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { pageTrainer, addTrainer, updateTrainer, deleteTrainer, trainerTeaching } from '@/api/biz'
import { TRAINER_TYPE, SCHEDULE_STATUS } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const LEVELS = ['初级讲师', '中级讲师', '高级讲师', '特邀专家']
const TRAINER_STATUS = {
  0: { label: '启用', type: 'success' },
  1: { label: '停用', type: 'info' }
}

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formRef = ref()

const detailVisible = ref(false)
const current = ref({})
const detail = ref(null)

const query = reactive({ page: 1, size: 10, keyword: '', trainerType: '', status: '' })

const emptyForm = () => ({
  id: null, trainerName: '', gender: '0', phone: '', email: '', trainerType: 'internal',
  orgName: '', title: '', specialty: '', level: '', intro: '', status: '0'
})
const form = reactive(emptyForm())

const rules = {
  trainerName: [{ required: true, message: '请输入讲师姓名', trigger: 'blur' }],
  trainerType: [{ required: true, message: '请选择讲师类型', trigger: 'change' }]
}

// 外部专家必填所在单位
const orgRules = computed(() =>
  form.trainerType === 'external'
    ? [{ required: true, message: '外部专家必填所在单位', trigger: 'blur' }]
    : []
)

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageTrainer(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.trainerType = ''
  query.status = ''
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
        orgName: form.orgName || null,
        title: form.title || null,
        specialty: form.specialty || null,
        level: form.level || null,
        intro: form.intro || null,
        phone: form.phone || null,
        email: form.email || null
      }
      if (form.id) {
        await updateTrainer(form.id, payload)
      } else {
        await addTrainer(payload)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

async function openDetail(row) {
  current.value = row
  detail.value = null
  detailVisible.value = true
  try {
    const res = await trainerTeaching(row.id)
    detail.value = res.data
  } catch (e) {
    detail.value = null
  }
}

function del(row) {
  ElMessageBox.confirm(
    `确认删除讲师「${row.trainerName}」?已有授课安排的讲师无法删除。`,
    '提示',
    { type: 'warning' }
  )
    .then(async () => {
      await deleteTrainer(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(() => {
  load()
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/trainer/export', { keyword: query.keyword, trainerType: query.trainerType, status: query.status }, '培训师资台账')
}
</script>

<style scoped>
.teach-stats {
  margin: 12px 0;
}
</style>
