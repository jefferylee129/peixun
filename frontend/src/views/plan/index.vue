<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="年度">
          <el-select v-model="query.year" clearable placeholder="全部" style="width: 110px">
            <el-option v-for="y in years" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划类型">
          <el-select v-model="query.planType" clearable placeholder="全部" style="width: 130px">
            <el-option v-for="(label, key) in PLAN_TYPE" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(item, key) in PLAN_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="deptAvailable" label="主办部门">
          <el-tree-select
            v-model="query.deptId"
            :data="depts"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="全部"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="计划名称"
            clearable
            style="width: 180px"
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
        <el-button v-perm="'biz:plan:add'" type="primary" :icon="Plus" @click="openForm()">新增计划</el-button>
        <el-button v-perm="'biz:plan:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'biz:plan:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="planName" label="计划名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="year" label="年度" width="70" align="center" />
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">{{ PLAN_TYPE[row.planType] || row.planType }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="主办部门" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column label="起止日期" width="200" align="center">
          <template #default="{ row }">{{ row.startDate || '-' }} ~ {{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="targetUsers" label="培训对象" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.targetUsers || '-' }}</template>
        </el-table-column>
        <el-table-column prop="budget" label="预算(万元)" width="100" align="center">
          <template #default="{ row }">{{ row.budget ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(PLAN_STATUS[row.status] || {}).type || 'info'">
              {{ (PLAN_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批意见" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.approveRemark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === '0' || row.status === '3'">
              <el-button v-perm="'biz:plan:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
              <el-button v-perm="'biz:plan:edit'" link type="warning" @click="submit(row)">提交审批</el-button>
              <el-button v-perm="'biz:plan:remove'" link type="danger" @click="del(row)">删除</el-button>
            </template>
            <template v-else-if="row.status === '1'">
              <el-button v-perm="'biz:plan:approve'" link type="success" @click="doApprove(row, true)">通过</el-button>
              <el-button v-perm="'biz:plan:approve'" link type="danger" @click="doApprove(row, false)">驳回</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑培训计划' : '新增培训计划'" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="form.planName" placeholder="如: 2027年度安全生产培训计划" />
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
            <el-form-item label="计划类型" prop="planType">
              <el-select v-model="form.planType" style="width: 100%">
                <el-option v-for="(label, key) in PLAN_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预算(万元)" prop="budget">
              <el-input-number v-model="form.budget" :min="0" :precision="2" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col v-if="deptAvailable" :span="8">
            <el-form-item label="主办部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="depts"
                :props="{ label: 'deptName', children: 'children' }"
                node-key="id"
                check-strictly
                clearable
                placeholder="请选择"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="deptAvailable ? 8 : 12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="开始日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="deptAvailable ? 8 : 12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训对象" prop="targetUsers">
          <el-input v-model="form.targetUsers" placeholder="如: 安全管理人员、特种作业人员" />
        </el-form-item>
        <el-form-item label="计划内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="培训安排与主要内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入培训计划" template-type="plan" action="/excel/plan/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { pagePlan, addPlan, updatePlan, deletePlan, submitPlan, approvePlan } from '@/api/biz'
import { deptTree } from '@/api/system'
import { PLAN_TYPE, PLAN_STATUS, yearOptions } from '@/utils/constants'
import { useUserStore } from '@/store/user'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const userStore = useUserStore()
const years = yearOptions()

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formRef = ref()

// 部门树接口需要 system:dept:list 权限, 无权限时不加载、不展示部门选择
const depts = ref([])
const deptAvailable = ref(false)

const query = reactive({ page: 1, size: 10, year: '', status: '', planType: '', deptId: null, keyword: '' })

const emptyForm = () => ({
  id: null, planName: '', year: new Date().getFullYear(), planType: 'year', deptId: null,
  startDate: '', endDate: '', targetUsers: '', content: '', budget: null
})
const form = reactive(emptyForm())

const rules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  year: [{ required: true, message: '请选择年度', trigger: 'change' }],
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pagePlan(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.year = ''
  query.status = ''
  query.planType = ''
  query.deptId = null
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
        deptId: form.deptId || null,
        startDate: form.startDate || null,
        endDate: form.endDate || null,
        budget: form.budget ?? null
      }
      if (form.id) {
        await updatePlan(form.id, payload)
      } else {
        await addPlan(payload)
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
  ElMessageBox.confirm(`确认提交计划「${row.planName}」进入审批?`, '提示', { type: 'warning' })
    .then(async () => {
      await submitPlan(row.id)
      ElMessage.success('已提交审批')
      load()
    })
    .catch(() => {})
}

function doApprove(row, pass) {
  ElMessageBox.prompt(
    pass ? `通过「${row.planName}」的审批?可填写意见` : `驳回「${row.planName}」, 请填写驳回原因`,
    pass ? '审批通过' : '审批驳回',
    { inputPlaceholder: '审批意见(可空)', type: pass ? 'success' : 'warning' }
  )
    .then(async ({ value }) => {
      await approvePlan(row.id, { pass, remark: value || '' })
      ElMessage.success('操作成功')
      load()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除计划「${row.planName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deletePlan(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  load()
  if (userStore.hasPerm('system:dept:list')) {
    try {
      const res = await deptTree()
      depts.value = res.data
      deptAvailable.value = true
    } catch (e) {
      deptAvailable.value = false
    }
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/plan/export', { year: query.year, status: query.status, planType: query.planType, deptId: query.deptId, keyword: query.keyword }, '培训计划台账')
}
</script>
