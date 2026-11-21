<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>在线考试管理</span>
          <div class="head-actions">
            <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px" @change="load(1)">
              <el-option label="草稿" value="0" />
              <el-option label="已发布" value="1" />
              <el-option label="已截止" value="2" />
            </el-select>
            <el-input
              v-model="query.keyword"
              placeholder="考试名称"
              clearable
              style="width: 180px"
              @keyup.enter="load(1)"
              @clear="load(1)"
            />
            <el-button type="primary" @click="load(1)">查询</el-button>
            <el-button v-if="store.hasPerm('biz:exam:edit')" type="success" @click="openAdd">新建考试</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="考试名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="className" label="关联班次" min-width="160" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题数" width="70" align="center" />
        <el-table-column label="总分/及格线" width="110" align="center">
          <template #default="{ row }">{{ row.totalScore }} / {{ row.passScore }}</template>
        </el-table-column>
        <el-table-column prop="durationMin" label="时长(分)" width="90" align="center" />
        <el-table-column prop="hourReward" label="及格计学时" width="100" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="EXAM_STATUS[row.status]?.tag" size="small">{{ EXAM_STATUS[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="交卷/及格" width="100" align="center">
          <template #default="{ row }">{{ row.submittedCount }} / {{ row.passedCount }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '0' && store.hasPerm('biz:exam:edit')"
              link type="primary" size="small"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button
              v-if="row.status === '0' && store.hasPerm('biz:exam:publish')"
              link type="success" size="small"
              @click="publish(row)"
            >发布</el-button>
            <el-button
              v-if="row.status === '1' && store.hasPerm('biz:exam:publish')"
              link type="warning" size="small"
              @click="close(row)"
            >截止</el-button>
            <el-button
              v-if="row.status !== '0'"
              link type="primary" size="small"
              @click="showRecords(row)"
            >成绩单</el-button>
            <el-button
              v-if="row.status === '0' && store.hasPerm('biz:exam:edit')"
              link type="danger" size="small"
              @click="remove(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadList" />
    </el-card>

    <!-- 新建/编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑考试' : '新建考试'" width="760px" destroy-on-close>
      <el-form label-width="110px">
        <el-form-item label="考试名称" required>
          <el-input v-model="form.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="关联班次" required>
          <el-select v-model="form.classId" filterable style="width: 100%">
            <el-option
              v-for="c in classes"
              :key="c.id"
              :label="`${c.className}(${(CLASS_STATUS[c.status] || {}).label || c.status})`"
              :value="c.id"
              :disabled="c.status === '3' || c.status === '4'"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="form.durationMin" :min="5" :max="240" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总分">
              <el-input-number v-model="form.totalScore" :min="10" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格线">
              <el-input-number v-model="form.passScore" :min="1" :max="form.totalScore" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="及格计学时">
              <el-input-number v-model="form.hourReward" :min="0" :max="20" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="作答窗口">
              <el-date-picker
                v-model="window"
                type="datetimerange"
                start-placeholder="开始(可空)"
                end-placeholder="截止(可空, 默认发布后7天)"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="选题组卷" required>
          <el-button size="small" @click="showPick">
            已选 {{ form.questionIds.length }} 题, 点击选题
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存草稿</el-button>
      </template>
    </el-dialog>

    <!-- 选题 -->
    <el-dialog v-model="pickVisible" title="从题库选题" width="720px" destroy-on-close>
      <el-table
        ref="pickTableRef"
        :data="questionList"
        border
        size="small"
        @selection-change="onPickChange"
      >
        <el-table-column type="selection" width="45" :selectable="r => r.status === '0'" />
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="题型" width="70" align="center">
          <template #default="{ row }">{{ Q_TYPE[row.qType]?.label }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="280" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" width="70" align="center" />
      </el-table>
      <template #footer>
        <el-button @click="pickVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPick">确定(已选 {{ picked.length }})</el-button>
      </template>
    </el-dialog>

    <!-- 成绩单 -->
    <el-dialog v-model="recordVisible" :title="`成绩单 - ${recordExam.title || ''}`" width="680px" destroy-on-close>
      <el-table :data="recordRows" border size="small">
        <el-table-column prop="employeeNo" label="工号" width="100" />
        <el-table-column prop="userName" label="姓名" width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.submitted ? 'success' : 'info'" size="small">
              {{ row.submitted ? '已交卷' : '未交卷' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="80" align="center" />
        <el-table-column label="结论" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.submitted" :type="row.passed === 1 ? 'success' : 'danger'" size="small">
              {{ row.passed === 1 ? '及格' : '不及格' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="交卷时间" width="160" align="center">
          <template #default="{ row }">{{ String(row.submitTime || '').slice(0, 19).replace('T', ' ') }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pageExams, addExam, updateExam, deleteExam, publishExam, closeExam, examRecords, pageQuestions } from '@/api/optimize'
import { pageClass } from '@/api/train'
import { CLASS_STATUS } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const store = useUserStore()
const EXAM_STATUS = {
  0: { label: '草稿', tag: 'info' },
  1: { label: '已发布', tag: 'success' },
  2: { label: '已截止', tag: 'warning' }
}
const Q_TYPE = { single: '单选', multiple: '多选', judge: '判断' }

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', keyword: '' })

async function loadList(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageExams({
      page: query.page,
      size: query.size,
      status: query.status || null,
      keyword: query.keyword || null
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

const classes = ref([])
async function loadClasses() {
  const res = await pageClass({ page: 1, size: 100 })
  classes.value = (res.data.records || []).filter(c => c.status !== '3' && c.status !== '4')
}

const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({
  id: null, title: '', classId: null, durationMin: 30,
  totalScore: 100, passScore: 60, hourReward: 0, questionIds: []
})
const window = ref(null)

function openAdd() {
  Object.assign(form, {
    id: null, title: '', classId: classes.value[0]?.id || null,
    durationMin: 30, totalScore: 100, passScore: 60, hourReward: 0, questionIds: []
  })
  window.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, title: row.title, classId: row.classId,
    durationMin: row.durationMin, totalScore: Number(row.totalScore),
    passScore: Number(row.passScore), hourReward: Number(row.hourReward),
    questionIds: []
  })
  // 编辑时不改已选题目(后端要求重新保存全部字段, 这里保持简单: 编辑仅改参数)
  window.value = null
  dialogVisible.value = true
}

const pickVisible = ref(false)
const questionList = ref([])
const picked = ref([])
const pickTableRef = ref()

async function openPickIfNeeded() {
  const res = await pageQuestions({ page: 1, size: 200 })
  questionList.value = res.data.records || []
}

async function showPick() {
  await openPickIfNeeded()
  picked.value = []
  pickVisible.value = true
}

function onPickChange(rows) {
  picked.value = rows
}

function confirmPick() {
  form.questionIds = picked.value.map(r => r.id)
  pickVisible.value = false
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写考试名称')
    return
  }
  if (!form.classId) {
    ElMessage.warning('请选择关联班次')
    return
  }
  const payload = {
    title: form.title.trim(),
    classId: form.classId,
    durationMin: form.durationMin,
    totalScore: form.totalScore,
    passScore: form.passScore,
    hourReward: form.hourReward,
    questionIds: form.questionIds.join(','),
    startTime: window.value?.[0] || null,
    endTime: window.value?.[1] || null
  }
  saving.value = true
  try {
    if (form.id) {
      await updateExam(form.id, payload)
    } else {
      await addExam(payload)
    }
    ElMessage.success('已保存草稿, 发布后学员可见')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function publish(row) {
  await ElMessageBox.confirm(`发布后立即通知班次学员, 确定发布「${row.title}」吗?`, '发布考试', { type: 'warning' })
  await publishExam(row.id)
  ElMessage.success('已发布并通知学员')
  load()
}

async function close(row) {
  await ElMessageBox.confirm(`截止后学员不能再作答, 确定截止「${row.title}」吗?`, '截止考试', { type: 'warning' })
  await closeExam(row.id)
  ElMessage.success('已截止')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除草稿「${row.title}」吗?`, '提示', { type: 'warning' })
  await deleteExam(row.id)
  ElMessage.success('已删除')
  load()
}

const recordVisible = ref(false)
const recordExam = ref({})
const recordRows = ref([])

async function showRecords(row) {
  const res = await examRecords(row.id)
  recordExam.value = row
  recordRows.value = res.data.rows || []
  recordVisible.value = true
}

onMounted(() => {
  load()
  loadClasses()
})
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
