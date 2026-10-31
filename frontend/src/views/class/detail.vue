<template>
  <div class="page">
    <!-- 基本信息 -->
    <el-card shadow="never" class="search-card">
      <div class="detail-header">
        <div class="detail-title">
          <el-button :icon="Back" circle @click="$router.back()" />
          <h3>{{ info.className }}</h3>
          <el-tag :type="(CLASS_STATUS[info.status] || {}).type || 'info'">
            {{ (CLASS_STATUS[info.status] || {}).label || info.status }}
          </el-tag>
        </div>
        <div class="detail-actions">
          <el-button v-if="info.status === '0'" v-perm="'biz:class:edit'" type="warning" size="small" @click="changeStatus('1')">发布报名</el-button>
          <el-button v-if="info.status === '1'" v-perm="'biz:class:edit'" type="success" size="small" @click="changeStatus('2')">开始培训</el-button>
          <el-button v-if="info.status === '2'" v-perm="'biz:class:edit'" type="success" size="small" @click="changeStatus('3')">结班</el-button>
          <el-button v-if="info.status === '0' || info.status === '1'" v-perm="'biz:class:edit'" type="danger" size="small" @click="changeStatus('4')">取消班次</el-button>
        </div>
      </div>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="班次类型">{{ CLASS_TYPE[info.classType] || info.classType }}</el-descriptions-item>
        <el-descriptions-item label="主办部门">{{ info.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关联计划">{{ info.planName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="人数">{{ students.length }}{{ info.capacity ? '/' + info.capacity : '' }}</el-descriptions-item>
        <el-descriptions-item label="培训时间">{{ info.startDate }} ~ {{ info.endDate }}</el-descriptions-item>
        <el-descriptions-item label="培训地点">{{ info.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="培训对象">{{ info.targetUsers || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经费(万元)">{{ info.budget ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <!-- 学员管理 -->
        <el-tab-pane label="学员管理" name="students">
          <div class="toolbar">
            <el-button v-perm="'biz:class:edit'" type="primary" :icon="Plus" :disabled="info.status === '3' || info.status === '4'" @click="openAddStudent">添加学员</el-button>
            <el-button v-perm="'biz:class:edit'" :icon="Upload" :disabled="info.status === '3' || info.status === '4'" @click="importVisible = true">批量导入学员</el-button>
            <el-button v-perm="'biz:class:list'" :icon="Download" @click="exportStudents">导出学员名单</el-button>
          </div>
          <el-table :data="students" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="nickname" label="姓名" width="120" />
            <el-table-column prop="employeeNo" label="工号" width="110" />
            <el-table-column prop="deptName" label="部门" min-width="140" />
            <el-table-column prop="enrollTime" label="报名时间" width="170" />
            <el-table-column label="参训状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="(STUDENT_STATUS[row.status] || {}).type || 'info'" size="small">
                  {{ (STUDENT_STATUS[row.status] || {}).label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalHours" label="累计学时" width="100" align="center" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button
                  v-perm="'biz:class:edit'"
                  link
                  type="danger"
                  :disabled="info.status === '3' || (row.totalHours || 0) > 0"
                  @click="removeStudent(row)"
                >移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 课程安排 -->
        <el-tab-pane label="课程安排" name="schedules">
          <div class="toolbar">
            <el-button v-perm="'biz:class:edit'" type="primary" :icon="Plus" :disabled="info.status === '3' || info.status === '4'" @click="openScheduleForm()">添加课节</el-button>
            <el-button v-perm="'biz:class:list'" :icon="Download" @click="exportSchedules">导出课程安排</el-button>
          </div>
          <el-table :data="schedules" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="teachDate" label="日期" width="110" />
            <el-table-column label="时间" width="150" align="center">
              <template #default="{ row }">{{ (row.startTime || '').slice(0, 5) }} ~ {{ (row.endTime || '').slice(0, 5) }}</template>
            </el-table-column>
            <el-table-column prop="courseName" label="课程" min-width="180" show-overflow-tooltip />
            <el-table-column prop="trainerName" label="讲师" width="110" />
            <el-table-column prop="hours" label="学时" width="70" align="center" />
            <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="(SCHEDULE_STATUS[row.status] || {}).type || 'info'" size="small">
                  {{ (SCHEDULE_STATUS[row.status] || {}).label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === '0'"
                  v-perm="'biz:class:sign'"
                  link
                  type="success"
                  @click="doFinishSchedule(row)"
                >结束本节</el-button>
                <el-button v-if="row.status === '0'" v-perm="'biz:class:edit'" link type="primary" @click="openScheduleForm(row)">编辑</el-button>
                <el-button v-if="row.status === '0'" v-perm="'biz:class:edit'" link type="danger" @click="removeSchedule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 签到管理 -->
        <el-tab-pane label="签到管理" name="sign">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="选择课节">
              <el-select v-model="signScheduleId" placeholder="请选择课节" style="width: 360px" @change="loadAttendance">
                <el-option
                  v-for="s in schedules"
                  :key="s.id"
                  :label="`${s.teachDate} ${(s.startTime || '').slice(0, 5)} ${s.courseName || ''} [${(SCHEDULE_STATUS[s.status] || {}).label || ''}]`"
                  :value="s.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-if="currentSchedule && currentSchedule.status === '0'">
              <el-tag type="warning">课节未结束, 可持续登记签到; 结束课节后按规则折算学时</el-tag>
            </el-form-item>
          </el-form>
          <el-table :data="signRows" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="nickname" label="姓名" width="110" />
            <el-table-column prop="employeeNo" label="工号" width="100" />
            <el-table-column prop="deptName" label="部门" min-width="130" />
            <el-table-column label="签到状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.signType" :type="(SIGN_TYPE[row.signType] || {}).type || 'info'" size="small">
                  {{ (SIGN_TYPE[row.signType] || {}).label }}
                </el-tag>
                <span v-else style="color: #c0c4cc">未签到</span>
              </template>
            </el-table-column>
            <el-table-column prop="signTime" label="签到时间" width="170">
              <template #default="{ row }">{{ row.signTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="hoursObtained" label="获得学时" width="90" align="center">
              <template #default="{ row }">{{ row.status === '1' ? (row.hoursObtained ?? '-') : '-' }}</template>
            </el-table-column>
            <el-table-column label="登记签到" align="center">
              <template #default="{ row }">
                <template v-if="currentSchedule && currentSchedule.status === '0'">
                  <el-button size="small" type="success" plain v-perm="'biz:class:sign'" @click="sign(row, 'normal')">正常</el-button>
                  <el-button size="small" type="warning" plain v-perm="'biz:class:sign'" @click="sign(row, 'late')">迟到</el-button>
                  <el-button size="small" type="info" plain v-perm="'biz:class:sign'" @click="sign(row, 'leave')">请假</el-button>
                  <el-button size="small" type="danger" plain v-perm="'biz:class:sign'" @click="sign(row, 'absent')">缺勤</el-button>
                </template>
                <span v-else style="color: #c0c4cc">课节已结束</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 评估汇总 -->
        <el-tab-pane v-if="store.hasPerm('biz:eval:list')" label="评估汇总" name="eval">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="评估模板">
              <el-select v-model="evalTemplateId" style="width: 320px" placeholder="请选择模板" @change="loadSummary">
                <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <template v-if="summary">
            <div class="stat-cards">
              <el-card shadow="never">
                <div class="stat-card">
                  <div class="stat-icon" style="background: #1677ff"><el-icon :size="22"><Star /></el-icon></div>
                  <div>
                    <div class="stat-value">{{ summary.overallAvg }}</div>
                    <div class="stat-label">总体平均分</div>
                  </div>
                </div>
              </el-card>
              <el-card shadow="never">
                <div class="stat-card">
                  <div class="stat-icon" style="background: #52c41a"><el-icon :size="22"><User /></el-icon></div>
                  <div>
                    <div class="stat-value">{{ summary.evalCount }}</div>
                    <div class="stat-label">参评人数</div>
                  </div>
                </div>
              </el-card>
            </div>
            <el-table :data="summaryItems" border size="small" style="margin-bottom: 16px">
              <el-table-column prop="title" label="评估项" min-width="200" />
              <el-table-column label="平均分" width="260">
                <template #default="{ row }">
                  <el-progress :percentage="row.percent" :stroke-width="12" />
                </template>
              </el-table-column>
              <el-table-column prop="avg" label="均分(满分5)" width="120" align="center" />
            </el-table>
            <template v-if="summary.textAnswers && summary.textAnswers.length">
              <h4>开放题作答</h4>
              <el-timeline style="margin-bottom: 16px">
                <el-timeline-item v-for="(a, i) in summary.textAnswers" :key="i" :timestamp="a.user + ' · ' + a.title">
                  {{ a.value }}
                </el-timeline-item>
              </el-timeline>
            </template>
            <template v-if="summary.comments && summary.comments.length">
              <h4>总体评语</h4>
              <el-timeline>
                <el-timeline-item v-for="(c, i) in summary.comments" :key="i" :timestamp="c.user" type="primary">
                  {{ c.comment }}
                </el-timeline-item>
              </el-timeline>
            </template>
            <el-empty v-if="summary.evalCount === 0" description="暂无评估数据" />
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加学员 -->
    <el-dialog v-model="studentVisible" title="添加学员" width="560px">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="studentKeyword" placeholder="姓名/工号" clearable @keyup.enter="loadUserOptions" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadUserOptions">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-select v-model="selectedUserIds" multiple filterable style="width: 100%" placeholder="选择学员(可多选)">
        <el-option v-for="u in userOptions" :key="u.id" :label="`${u.nickname}(${u.employeeNo || '-'})`" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="studentVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStudents">确定添加</el-button>
      </template>
    </el-dialog>

    <!-- 课节表单 -->
    <el-dialog v-model="scheduleVisible" :title="scheduleForm.id ? '编辑课节' : '添加课节'" width="620px">
      <el-form ref="scheduleFormRef" :model="scheduleForm" :rules="scheduleRules" label-width="90px">
        <el-form-item label="课程" prop="courseId">
          <el-select v-model="scheduleForm.courseId" filterable style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="`${c.courseName}(${c.hours}学时)`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课讲师" prop="trainerId">
          <el-select v-model="scheduleForm.trainerId" filterable clearable style="width: 100%">
            <el-option v-for="t in trainers" :key="t.id" :label="t.trainerName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="9">
            <el-form-item label="上课日期" prop="teachDate">
              <el-date-picker v-model="scheduleForm.teachDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="7">
            <el-form-item label="开始" prop="startTime" label-width="52px">
              <el-time-picker v-model="scheduleForm.startTime" value-format="HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束" prop="endTime" label-width="52px">
              <el-time-picker v-model="scheduleForm.endTime" value-format="HH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="10">
            <el-form-item label="本节学时" prop="hours">
              <el-input-number v-model="scheduleForm.hours" :min="0.5" :max="16" :step="0.5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="14">
            <el-form-item label="地点" label-width="52px">
              <el-input v-model="scheduleForm.location" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="scheduleForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>
    <ImportDialog
      v-model="importVisible"
      title="批量导入学员"
      template-type="class-student"
      :action="`/excel/class/${classId}/students/import`"
      @success="loadAll()"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Plus, Upload, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  classDetail, changeClassStatus, addClassStudents, removeClassStudent,
  classSchedules, addSchedule, updateSchedule, deleteSchedule, finishSchedule,
  scheduleAttendance, signAttendance, classStudents, evalSummary, pageEvalTemplate
} from '@/api/train'
import { publishedCourses, allTrainers } from '@/api/biz'
import { simpleUsers } from '@/api/system'
import { CLASS_STATUS, CLASS_TYPE, STUDENT_STATUS, SIGN_TYPE, SCHEDULE_STATUS } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'

const route = useRoute()
const store = useUserStore()
const classId = Number(route.params.id)

const info = ref({})
const students = ref([])
const schedules = ref([])
const activeTab = ref('students')
const saving = ref(false)

const studentVisible = ref(false)
const studentKeyword = ref('')
const userOptions = ref([])
const selectedUserIds = ref([])

const scheduleVisible = ref(false)
const scheduleFormRef = ref()
const emptySchedule = () => ({
  id: null, courseId: null, trainerId: null, teachDate: '',
  startTime: '09:00:00', endTime: '12:00:00', hours: 3, location: '', remark: ''
})
const scheduleForm = reactive(emptySchedule())
const scheduleRules = {
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  teachDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  hours: [{ required: true, message: '请填写学时', trigger: 'blur' }]
}

const courses = ref([])
const trainers = ref([])

const signScheduleId = ref(null)
const signRows = ref([])
const currentSchedule = computed(() => schedules.value.find(s => s.id === signScheduleId.value) || null)

const templates = ref([])
const evalTemplateId = ref(null)
const summary = ref(null)
const summaryItems = computed(() => {
  if (!summary.value || !summary.value.items) return []
  const avg = summary.value.itemAvg || {}
  return summary.value.items
    .filter(i => i.type === 'score')
    .map(i => ({
      title: i.title,
      key: i.key,
      avg: avg[i.key] ?? 0,
      percent: Math.round(((avg[i.key] ?? 0) / 5) * 100)
    }))
})

async function loadAll() {
  const res = await classDetail(classId)
  info.value = res.data.classInfo || {}
  students.value = res.data.students || []
  schedules.value = res.data.schedules || []
}

function changeStatus(status) {
  const actions = { 1: '发布报名', 2: '开始培训', 3: '结班(已结束)', 4: '取消班次' }
  ElMessageBox.confirm(`确认执行【${actions[status]}】?`, '提示', { type: 'warning' })
    .then(async () => {
      await changeClassStatus(classId, status)
      ElMessage.success('操作成功')
      loadAll()
    })
    .catch(() => {})
}

// ---------- 学员 ----------
function openAddStudent() {
  selectedUserIds.value = []
  loadUserOptions()
  studentVisible.value = true
}

async function loadUserOptions() {
  const res = await simpleUsers({ keyword: studentKeyword.value || undefined })
  userOptions.value = res.data
}

async function saveStudents() {
  if (!selectedUserIds.value.length) {
    ElMessage.warning('请选择学员')
    return
  }
  saving.value = true
  try {
    await addClassStudents(classId, selectedUserIds.value)
    ElMessage.success('添加成功')
    studentVisible.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

function removeStudent(row) {
  ElMessageBox.confirm(`确认移除学员「${row.nickname}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await removeClassStudent(classId, row.userId)
      ElMessage.success('已移除')
      loadAll()
    })
    .catch(() => {})
}

// ---------- 课节 ----------
function openScheduleForm(row) {
  Object.assign(scheduleForm, emptySchedule())
  if (row) {
    Object.assign(scheduleForm, row)
  }
  scheduleVisible.value = true
}

async function saveSchedule() {
  await scheduleFormRef.value.validate()
  saving.value = true
  try {
    if (scheduleForm.id) {
      await updateSchedule(scheduleForm.id, scheduleForm)
    } else {
      await addSchedule(classId, scheduleForm)
    }
    ElMessage.success('保存成功')
    scheduleVisible.value = false
    loadAll()
  } finally {
    saving.value = false
  }
}

function removeSchedule(row) {
  ElMessageBox.confirm('确认删除该课节?', '提示', { type: 'warning' })
    .then(async () => {
      await deleteSchedule(row.id)
      ElMessage.success('删除成功')
      loadAll()
    })
    .catch(() => {})
}

function doFinishSchedule(row) {
  ElMessageBox.confirm(
    `确认结束课节「${row.teachDate} ${row.courseName || ''}」?系统将按签到与规则(迟到按系数折算)自动生成学时台账。`,
    '结束课节', { type: 'warning' }
  )
    .then(async () => {
      await finishSchedule(row.id)
      ElMessage.success('课节已结束, 学时已生成')
      loadAll()
    })
    .catch(() => {})
}

// ---------- 签到 ----------
async function loadAttendance() {
  if (!signScheduleId.value) return
  const [stu, att] = await Promise.all([
    classStudents(classId),
    scheduleAttendance(signScheduleId.value)
  ])
  const attMap = {}
  for (const a of att.data) {
    attMap[a.userId] = a
  }
  signRows.value = stu.data.map(s => {
    const a = attMap[s.userId]
    return {
      userId: s.userId,
      nickname: s.nickname,
      employeeNo: s.employeeNo,
      deptName: s.deptName,
      signType: a ? a.signType : null,
      signTime: a ? a.signTime : null,
      hoursObtained: a ? a.hoursObtained : null,
      status: (schedules.value.find(x => x.id === signScheduleId.value) || {}).status
    }
  })
}

function sign(row, signType) {
  signAttendance(signScheduleId.value, { userId: row.userId, signType })
    .then(() => {
      ElMessage.success('已登记')
      loadAttendance()
    })
    .catch(() => {})
}

// ---------- 评估 ----------
async function loadSummary() {
  if (!evalTemplateId.value) return
  const res = await evalSummary({ classId, templateId: evalTemplateId.value })
  summary.value = res.data
}

onMounted(async () => {
  await loadAll()
  try {
    const [c, t, ev] = await Promise.all([
      publishedCourses(),
      allTrainers(),
      store.hasPerm('biz:eval:list') ? pageEvalTemplate({ page: 1, size: 100 }) : Promise.resolve(null)
    ])
    courses.value = c.data
    trainers.value = t.data
    if (ev) {
      templates.value = ev.data.records
      if (templates.value.length) {
        evalTemplateId.value = templates.value[0].id
        loadSummary()
      }
    }
  } catch (e) {
    /* 下拉数据加载失败不阻断页面 */
  }
})

const importVisible = ref(false)
function exportStudents() {
  downloadExcel(`/excel/class/${classId}/students/export`, {}, `学员名单-${info.value.className || classId}`)
}

function exportSchedules() {
  downloadExcel(`/excel/class/${classId}/schedules/export`, {}, `课程安排-${info.value.className || classId}`)
}
</script>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-title h3 {
  margin: 0;
}
</style>
