<template>
  <div class="page">
    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="我的班级" name="mine">
          <el-table v-loading="mineLoading" :data="mineList" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="className" label="班次名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">{{ CLASS_TYPE[row.classType] || row.classType }}</template>
            </el-table-column>
            <el-table-column prop="deptName" label="主办部门" width="130" show-overflow-tooltip />
            <el-table-column prop="startDate" label="开始日期" width="110" align="center" />
            <el-table-column prop="endDate" label="结束日期" width="110" align="center" />
            <el-table-column prop="myTotalHours" label="我的学时" width="90" align="center" />
            <el-table-column label="我的状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="(STUDENT_STATUS[row.myStudentStatus] || {}).type || 'info'" size="small">
                  {{ (STUDENT_STATUS[row.myStudentStatus] || {}).label || row.myStudentStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评估" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.evalFilled ? 'success' : 'info'" size="small">
                  {{ row.evalFilled ? '已评估' : '未评估' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  v-if="['2', '3'].includes(row.status) && !row.evalFilled"
                  link
                  type="primary"
                  @click="openEval(row)"
                >填写评估</el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination v-model:page="mineQuery.page" v-model:size="mineQuery.size" :total="mineTotal" @change="loadMine" />
        </el-tab-pane>

        <el-tab-pane label="可报名班次" name="enrollable">
          <el-table v-loading="enrLoading" :data="enrList" border stripe>
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column prop="className" label="班次名称" min-width="200" show-overflow-tooltip />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">{{ CLASS_TYPE[row.classType] || row.classType }}</template>
            </el-table-column>
            <el-table-column prop="deptName" label="主办部门" width="130" show-overflow-tooltip />
            <el-table-column prop="startDate" label="开始日期" width="110" align="center" />
            <el-table-column prop="endDate" label="结束日期" width="110" align="center" />
            <el-table-column prop="location" label="地点" min-width="120" show-overflow-tooltip />
            <el-table-column label="报名情况" width="110" align="center">
              <template #default="{ row }">{{ row.studentCount ?? 0 }}/{{ row.capacity ?? '不限' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="enroll(row)">报名</el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination v-model:page="enrQuery.page" v-model:size="enrQuery.size" :total="enrTotal" @change="loadEnrollable" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 培训评估填写 -->
    <el-dialog v-model="evalVisible" title="培训评估填写" width="560px">
      <el-form label-width="120px">
        <el-form-item v-for="item in evalItems" :key="item.key" :label="item.title">
          <el-rate
            v-if="item.type === 'score'"
            v-model="evalScores[item.key]"
            :max="5"
            show-score
            score-template="{value} 分"
          />
          <el-input
            v-else
            v-model="evalScores[item.key]"
            type="textarea"
            :rows="2"
            placeholder="请填写您的意见"
          />
        </el-form-item>
        <el-form-item label="总体评语">
          <el-input v-model="evalComment" type="textarea" :rows="3" placeholder="对本次培训的整体评价(可空)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalVisible = false">取消</el-button>
        <el-button type="primary" :loading="evalSaving" @click="submitEval">提交评估</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myClasses, enrollableClasses, enrollClass, evalPending, evalTemplateFillView, myEvalRecord, fillEval } from '@/api/train'
import { CLASS_TYPE, STUDENT_STATUS } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const tab = ref('mine')
const mineLoading = ref(false)
const enrLoading = ref(false)
const mineList = ref([])
const mineTotal = ref(0)
const enrList = ref([])
const enrTotal = ref(0)
const mineQuery = reactive({ page: 1, size: 10 })
const enrQuery = reactive({ page: 1, size: 10 })

async function loadMine() {
  mineLoading.value = true
  try {
    const res = await myClasses(mineQuery)
    mineList.value = res.data.records
    mineTotal.value = res.data.total
  } finally {
    mineLoading.value = false
  }
}

async function loadEnrollable() {
  enrLoading.value = true
  try {
    const res = await enrollableClasses(enrQuery)
    enrList.value = res.data.records
    enrTotal.value = res.data.total
  } finally {
    enrLoading.value = false
  }
}

function enroll(row) {
  ElMessageBox.confirm(`确认报名班次「${row.className}」?`, '提示', { type: 'info' })
    .then(async () => {
      await enrollClass(row.id)
      ElMessage.success('报名成功')
      loadMine()
      loadEnrollable()
    })
    .catch(() => {})
}

onMounted(() => {
  loadMine()
  loadEnrollable()
})

// ---------- 评估填写 ----------
const evalVisible = ref(false)
const evalSaving = ref(false)
const evalItems = ref([])
const evalScores = reactive({})
const evalComment = ref('')
const evalTarget = reactive({ classId: null, templateId: null })

async function openEval(row) {
  try {
    const pendRes = await evalPending()
    const pend = (pendRes.data || []).find(p => p.classId === row.id)
    if (!pend) {
      ElMessage.warning('未找到可用的评估模板')
      return
    }
    evalTarget.classId = row.id
    evalTarget.templateId = pend.templateId
    const tplRes = await evalTemplateFillView(pend.templateId)
    evalItems.value = tplRes.data.itemList || []
    for (const k of Object.keys(evalScores)) delete evalScores[k]
    for (const item of evalItems.value) {
      evalScores[item.key] = item.type === 'score' ? 0 : ''
    }
    evalComment.value = ''
    try {
      const rec = await myEvalRecord({ classId: row.id, templateId: pend.templateId })
      if (rec.data && rec.data.scores) {
        const scores = JSON.parse(rec.data.scores)
        Object.assign(evalScores, scores)
        evalComment.value = rec.data.comment || ''
      }
    } catch (e) {
      /* 无历史作答 */
    }
    evalVisible.value = true
  } catch (e) {
    /* 接口错误已提示 */
  }
}

async function submitEval() {
  for (const item of evalItems.value) {
    if (item.required && item.type === 'score' && !evalScores[item.key]) {
      ElMessage.warning(`请对「${item.title}」评分`)
      return
    }
  }
  evalSaving.value = true
  try {
    await fillEval({
      templateId: evalTarget.templateId,
      classId: evalTarget.classId,
      scores: { ...evalScores },
      comment: evalComment.value
    })
    ElMessage.success('评估已提交, 感谢反馈!')
    evalVisible.value = false
    loadMine()
  } finally {
    evalSaving.value = false
  }
}
</script>
