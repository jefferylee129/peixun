<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="班次名称" clearable style="width: 180px" @keyup.enter="load(1)" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(item, key) in CLASS_STATUS" :key="key" :label="item.label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="年度">
          <el-select v-model="query.year" clearable placeholder="全部" style="width: 110px">
            <el-option v-for="y in yearOptions()" :key="y" :label="y + '年'" :value="y" />
          </el-select>
        </el-form-item>
        <el-form-item label="主办部门">
          <el-tree-select
            v-model="query.deptId"
            :data="depts"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="全部"
            style="width: 160px"
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
        <el-button v-perm="'biz:class:add'" type="primary" :icon="Plus" @click="openForm()">新增班次</el-button>
        <el-button v-perm="'biz:class:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'biz:class:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="className" label="班次名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">{{ CLASS_TYPE[row.classType] || row.classType }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="主办部门" width="120" />
        <el-table-column prop="planName" label="关联计划" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.planName || '-' }}</template>
        </el-table-column>
        <el-table-column label="培训时间" width="200" align="center">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="人数" width="90" align="center">
          <template #default="{ row }">{{ row.studentCount || 0 }}{{ row.capacity ? '/' + row.capacity : '' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(CLASS_STATUS[row.status] || {}).type || 'info'">
              {{ (CLASS_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === '0'"
              v-perm="'biz:class:edit'"
              link
              type="warning"
              @click="changeStatus(row, '1')"
            >发布报名</el-button>
            <el-button
              v-if="row.status === '1'"
              v-perm="'biz:class:edit'"
              link
              type="success"
              @click="changeStatus(row, '2')"
            >开始培训</el-button>
            <el-button
              v-if="row.status === '2'"
              v-perm="'biz:class:edit'"
              link
              type="success"
              @click="changeStatus(row, '3')"
            >结班</el-button>
            <el-button
              v-if="row.status === '0' || row.status === '1'"
              v-perm="'biz:class:edit'"
              link
              type="danger"
              @click="changeStatus(row, '4')"
            >取消</el-button>
            <el-button v-if="row.status === '0'" v-perm="'biz:class:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.status === '0' || row.status === '4'" v-perm="'biz:class:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑班次' : '新增班次'" width="700px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="班次名称" prop="className">
          <el-input v-model="form.className" placeholder="请输入班次名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关联计划" prop="planId">
              <el-select v-model="form.planId" clearable filterable placeholder="可不关联" style="width: 100%">
                <el-option v-for="p in plans" :key="p.id" :label="p.planName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次类型" prop="classType">
              <el-select v-model="form.classType" style="width: 100%">
                <el-option v-for="(label, key) in CLASS_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主办部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="depts"
                :props="{ label: 'deptName', value: 'id', children: 'children' }"
                node-key="id"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人数上限" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="9999" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="培训地点" prop="location">
              <el-input v-model="form.location" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经费(万元)" prop="budget">
              <el-input-number v-model="form.budget" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训对象" prop="targetUsers">
          <el-input v-model="form.targetUsers" />
        </el-form-item>
        <el-form-item label="班次说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入班次" template-type="class" action="/excel/class/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import { planOptions } from '@/api/biz'
import { deptTree } from '@/api/system'
import { pageClass, addClass, updateClass, deleteClass, changeClassStatus } from '@/api/train'
import { CLASS_STATUS, CLASS_TYPE, yearOptions } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const depts = ref([])
const plans = ref([])
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', status: '', year: null, deptId: null })

const emptyForm = () => ({
  id: null, className: '', planId: null, classType: 'internal', deptId: null,
  startDate: '', endDate: '', location: '', capacity: 40, budget: 0,
  targetUsers: '', description: ''
})
const form = reactive(emptyForm())

const rules = {
  className: [{ required: true, message: '请输入班次名称', trigger: 'blur' }],
  classType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageClass(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.status = ''
  query.year = null
  query.deptId = null
  load(1)
}

function goDetail(row) {
  router.push(`/classes/${row.id}`)
}

const statusActions = {
  1: '发布报名',
  2: '开始培训',
  3: '结班(已结束)',
  4: '取消班次'
}

function changeStatus(row, status) {
  ElMessageBox.confirm(`确认对「${row.className}」执行【${statusActions[status]}】?`, '提示', { type: 'warning' })
    .then(async () => {
      await changeClassStatus(row.id, status)
      ElMessage.success('操作成功')
      load()
    })
    .catch(() => {})
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
      if (form.id) {
        await updateClass(form.id, form)
      } else {
        await addClass(form)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function del(row) {
  ElMessageBox.confirm(`确认删除班次「${row.className}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteClass(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  load()
  try {
    const [d, p] = await Promise.all([deptTree(), planOptions({})])
    depts.value = d.data
    plans.value = p.data
  } catch (e) {
    depts.value = []
    plans.value = []
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/class/export', { status: query.status, year: query.year, deptId: query.deptId, keyword: query.keyword }, '培训班次台账')
}
</script>
