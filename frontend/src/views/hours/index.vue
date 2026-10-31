<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="年份">
          <el-select v-model="query.year" clearable placeholder="全部" style="width: 100px">
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
        <el-form-item label="来源">
          <el-select v-model="query.source" clearable placeholder="全部" style="width: 110px">
            <el-option v-for="(label, key) in HOUR_SOURCE" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 110px">
            <el-option v-for="(item, key) in HOUR_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/工号"
            clearable
            style="width: 160px"
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
        <el-button v-perm="'biz:hours:add'" type="primary" :icon="Plus" @click="openForm">补录学时</el-button>
        <el-button v-perm="'biz:hours:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'biz:hours:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="year" label="年份" width="70" align="center" />
        <el-table-column prop="userName" label="姓名" width="90" />
        <el-table-column prop="employeeNo" label="工号" width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" show-overflow-tooltip />
        <el-table-column prop="className" label="班次" min-width="160" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" min-width="160" show-overflow-tooltip />
        <el-table-column prop="hours" label="学时" width="80" align="center" />
        <el-table-column label="来源" width="90" align="center">
          <template #default="{ row }">{{ HOUR_SOURCE[row.source] || row.source }}</template>
        </el-table-column>
        <el-table-column prop="recordDate" label="日期" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(HOUR_STATUS[row.status] || {}).type || 'info'">
              {{ (HOUR_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditRemark" label="认定备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === '0'">
              <el-button v-perm="'biz:hours:audit'" link type="success" @click="doAudit(row, true)">通过</el-button>
              <el-button v-perm="'biz:hours:audit'" link type="warning" @click="doAudit(row, false)">驳回</el-button>
              <el-button v-perm="'biz:hours:remove'" link type="danger" @click="del(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" title="补录学时" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="归属人" prop="userId">
          <el-select v-model="form.userId" filterable placeholder="选择员工" style="width: 100%">
            <el-option
              v-for="u in users"
              :key="u.id"
              :label="u.nickname + (u.employeeNo ? `(${u.employeeNo})` : '')"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="学时数" prop="hours">
          <el-input-number v-model="form.hours" :min="0.5" :step="0.5" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-radio-group v-model="form.source">
            <el-radio :value="2">手工补录</el-radio>
            <el-radio :value="3">外部认定</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="补录原因/认定依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入学时" template-type="hours" action="/excel/hours/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { pageHours, addHour, auditHour, deleteHour } from '@/api/train'
import { deptTree, simpleUsers } from '@/api/system'
import { HOUR_SOURCE, HOUR_STATUS, yearOptions } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const depts = ref([])
const users = ref([])
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, year: null, deptId: null, source: null, status: '', keyword: '' })

const emptyForm = () => ({
  userId: null, hours: 4, recordDate: '', source: 2, remark: ''
})
const form = reactive(emptyForm())

const rules = {
  userId: [{ required: true, message: '请选择学时归属人', trigger: 'change' }],
  hours: [{ required: true, message: '请填写学时数', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  source: [{ required: true, message: '请选择来源', trigger: 'change' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageHours(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.year = null
  query.deptId = null
  query.source = null
  query.status = ''
  query.keyword = ''
  load(1)
}

function openForm() {
  Object.assign(form, emptyForm())
  formVisible.value = true
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      await addHour(form)
      ElMessage.success('已提交, 待认定')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function doAudit(row, pass) {
  ElMessageBox.prompt(
    pass ? `通过「${row.userName}」${row.hours} 学时的认定?可填写备注` : `驳回「${row.userName}」的学时认定, 请填写原因`,
    pass ? '认定通过' : '认定驳回',
    { inputPlaceholder: '备注(可空)', type: pass ? 'success' : 'warning' }
  )
    .then(async ({ value }) => {
      await auditHour(row.id, { pass, remark: value || '' })
      ElMessage.success('操作成功')
      load()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除「${row.userName}」${row.recordDate} 的学时记录?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteHour(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  load()
  try {
    const res = await deptTree()
    depts.value = res.data
  } catch (e) {
    depts.value = []
  }
  try {
    const res = await simpleUsers({})
    users.value = res.data
  } catch (e) {
    users.value = []
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/hours/export', { year: query.year, deptId: query.deptId, source: query.source, status: query.status, keyword: query.keyword }, '学时台账')
}
</script>
