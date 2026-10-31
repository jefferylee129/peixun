<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="课程名称/编码"
            clearable
            style="width: 200px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="query.category" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="(label, key) in COURSE_CATEGORY" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程形式">
          <el-select v-model="query.courseType" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(label, key) in COURSE_TYPE" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option v-for="(item, key) in COURSE_STATUS" :key="key" :label="item.label" :value="key" />
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
        <el-button v-perm="'biz:course:add'" type="primary" :icon="Plus" @click="openForm()">新增课程</el-button>
        <el-button v-perm="'biz:course:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'biz:course:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="courseCode" label="课程编码" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="90" align="center">
          <template #default="{ row }">{{ COURSE_CATEGORY[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column label="形式" width="80" align="center">
          <template #default="{ row }">{{ COURSE_TYPE[row.courseType] || row.courseType }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="学时" width="70" align="center" />
        <el-table-column prop="level" label="层级" width="70" align="center" />
        <el-table-column prop="trainerName" label="主讲讲师" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="(COURSE_STATUS[row.status] || {}).type || 'info'">
              {{ (COURSE_STATUS[row.status] || {}).label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'biz:course:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.status === '0' || row.status === '3'"
              v-perm="'biz:course:edit'"
              link
              type="warning"
              @click="submitAudit(row)"
            >提交审核</el-button>
            <el-button
              v-if="row.status === '1'"
              v-perm="'biz:course:audit'"
              link
              type="success"
              @click="doAudit(row, true)"
            >通过</el-button>
            <el-button
              v-if="row.status === '1'"
              v-perm="'biz:course:audit'"
              link
              type="danger"
              @click="doAudit(row, false)"
            >驳回</el-button>
            <el-button
              v-if="row.status === '2'"
              v-perm="'biz:course:audit'"
              link
              type="warning"
              @click="doOffline(row)"
            >下架</el-button>
            <el-button v-perm="'biz:course:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑课程' : '新增课程'" width="680px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程编码" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="留空自动生成" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程分类" prop="category">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="(label, key) in COURSE_CATEGORY" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程形式" prop="courseType">
              <el-select v-model="form.courseType" style="width: 100%">
                <el-option v-for="(label, key) in COURSE_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="标准学时" prop="hours">
              <el-input-number v-model="form.hours" :min="0.5" :max="99" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程层级" prop="level">
              <el-select v-model="form.level" clearable style="width: 100%">
                <el-option label="初级" value="初级" />
                <el-option label="中级" value="中级" />
                <el-option label="高级" value="高级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="主讲讲师" prop="trainerId">
              <el-select v-model="form.trainerId" clearable filterable style="width: 100%">
                <el-option v-for="t in trainers" :key="t.id" :label="t.trainerName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="培训对象" prop="targetUsers">
          <el-input v-model="form.targetUsers" placeholder="如: 安全管理人员、特种作业人员" />
        </el-form-item>
        <el-form-item label="课程大纲" prop="outline">
          <el-input v-model="form.outline" type="textarea" :rows="4" placeholder="章节/要点, 每行一项" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog v-model="importVisible" title="批量导入课程" template-type="course" action="/excel/course/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import {
  pageCourse, addCourse, updateCourse, deleteCourse, submitCourse, auditCourse, offlineCourse
} from '@/api/biz'
import { allTrainers } from '@/api/biz'
import { COURSE_CATEGORY, COURSE_TYPE, COURSE_STATUS } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const trainers = ref([])
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', category: '', courseType: '', status: '' })

const emptyForm = () => ({
  id: null, courseCode: '', courseName: '', category: 'general', courseType: 'internal',
  hours: 4, level: '', trainerId: null, targetUsers: '', outline: ''
})
const form = reactive(emptyForm())

const rules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  courseType: [{ required: true, message: '请选择形式', trigger: 'change' }],
  hours: [{ required: true, message: '请填写学时', trigger: 'blur' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageCourse(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.category = ''
  query.courseType = ''
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
      if (form.id) {
        await updateCourse(form.id, form)
      } else {
        await addCourse(form)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function submitAudit(row) {
  ElMessageBox.confirm(`确认提交课程「${row.courseName}」审核?`, '提示', { type: 'warning' })
    .then(async () => {
      await submitCourse(row.id)
      ElMessage.success('已提交审核')
      load()
    })
    .catch(() => {})
}

function doAudit(row, pass) {
  ElMessageBox.prompt(
    pass ? `通过「${row.courseName}」的审核?可填写备注` : `驳回「${row.courseName}」, 请填写驳回原因`,
    pass ? '审核通过' : '审核驳回',
    { inputPlaceholder: '备注(可空)', type: pass ? 'success' : 'warning' }
  )
    .then(async ({ value }) => {
      await auditCourse(row.id, { pass, remark: value || '' })
      ElMessage.success('操作成功')
      load()
    })
    .catch(() => {})
}

function doOffline(row) {
  ElMessageBox.confirm(`确认下架课程「${row.courseName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await offlineCourse(row.id)
      ElMessage.success('已下架')
      load()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除课程「${row.courseName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCourse(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(async () => {
  load()
  try {
    const res = await allTrainers()
    trainers.value = res.data
  } catch (e) {
    trainers.value = []
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/course/export', { ...query }, '培训课程库')
}
</script>
