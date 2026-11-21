<template>
  <div class="page" v-loading="loading">
    <template v-if="paper.exam">
      <el-card shadow="never" class="exam-head">
        <div class="exam-title">
          <h3>{{ paper.exam.title }}</h3>
          <div class="timer" :class="{ danger: remainSeconds <= 60 }">
            <el-icon><AlarmClock /></el-icon>
            剩余 {{ fmtRemain }}
          </div>
        </div>
        <p class="exam-meta">
          共 {{ paper.questions.length }} 题 · 总分 {{ paper.exam.totalScore }} ·
          及格线 {{ paper.exam.passScore }} · 答题须在 {{ fmtTime(paper.deadline) }} 前交卷
        </p>
      </el-card>

      <el-card shadow="never" v-for="(q, idx) in paper.questions" :key="q.id" class="q-card">
        <div class="q-stem">
          <el-tag size="small" :type="q.qType === 'multiple' ? 'warning' : q.qType === 'judge' ? 'success' : 'primary'">
            {{ Q_TYPE[q.qType] }}
          </el-tag>
          <b class="q-index">{{ idx + 1 }}.</b> {{ q.stem }}
        </div>
        <div class="q-body">
          <el-radio-group v-if="q.qType === 'single'" v-model="answers[String(q.id)]">
            <el-radio v-for="opt in q.options" :key="opt.key" :value="opt.key" class="opt">
              {{ opt.key }}. {{ opt.text }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="q.qType === 'multiple'" v-model="multiAnswers[String(q.id)]">
            <el-checkbox v-for="opt in q.options" :key="opt.key" :value="opt.key" class="opt">
              {{ opt.key }}. {{ opt.text }}
            </el-checkbox>
          </el-checkbox-group>
          <el-radio-group v-else v-model="answers[String(q.id)]">
            <el-radio v-for="opt in q.options" :key="opt" :value="opt" class="opt">{{ opt }}</el-radio>
          </el-radio-group>
        </div>
      </el-card>

      <div class="submit-bar">
        <el-button @click="$router.push('/my-exam')">放弃</el-button>
        <el-button type="primary" :loading="submitting" @click="doSubmit">交卷</el-button>
      </div>
    </template>

    <el-dialog v-model="resultVisible" title="交卷成功" width="420px" :close-on-click-modal="false">
      <div class="result">
        <p class="score" :class="result.passed ? 'pass' : 'fail'">{{ result.score }}</p>
        <p>
          <el-tag :type="result.passed ? 'success' : 'danger'">{{ result.passed ? '及格' : '不及格' }}</el-tag>
          <el-tag v-if="Number(result.hourReward) > 0" type="success" class="reward">
            认定学时 +{{ result.hourReward }}
          </el-tag>
        </p>
        <p v-if="result.timeout" class="timeout-tip">本次为超时自动交卷</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="$router.push('/my-exam')">返回考试列表</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examPaper, submitExam } from '@/api/optimize'

const route = useRoute()
const router = useRouter()
const Q_TYPE = { single: '单选题', multiple: '多选题', judge: '判断题' }

const loading = ref(false)
const submitting = ref(false)
const paper = ref({})
const answers = reactive({})          // single/judge: {qid: 'A'|'对'}
const multiAnswers = reactive({})     // multiple: {qid: ['A','B']}
const remainSeconds = ref(0)
const resultVisible = ref(false)
const result = ref({})
let timer = null
let submitted = false

const fmtRemain = computed(() => {
  const s = Math.max(remainSeconds.value, 0)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

function fmtTime(t) {
  return String(t || '').slice(0, 19).replace('T', ' ')
}

async function load() {
  loading.value = true
  try {
    const res = await examPaper(route.params.id)
    paper.value = res.data || {}
    remainSeconds.value = Number(paper.value.remainingSeconds) || 0
    Object.keys(answers).forEach(k => delete answers[k])
    Object.keys(multiAnswers).forEach(k => delete multiAnswers[k])
    for (const q of paper.value.questions || []) {
      const key = String(q.id)
      if (q.qType === 'multiple') {
        multiAnswers[key] = (paper.value.savedAnswers || {})[key] || []
      } else {
        answers[key] = (paper.value.savedAnswers || {})[key] || null
      }
    }
    timer = setInterval(() => {
      remainSeconds.value -= 1
      if (remainSeconds.value <= 0) {
        clearInterval(timer)
        ElMessage.warning('考试时间到, 自动交卷')
        doSubmit(true)
      }
    }, 1000)
  } finally {
    loading.value = false
  }
}

function collectAnswers() {
  const data = {}
  for (const q of paper.value.questions || []) {
    const key = String(q.id)
    if (q.qType === 'multiple') {
      if ((multiAnswers[key] || []).length) {
        data[key] = [...multiAnswers[key]].sort().join('')
      }
    } else if (answers[key]) {
      data[key] = answers[key]
    }
  }
  return data
}

async function doSubmit(auto = false) {
  if (submitted) return
  if (!auto) {
    const unanswered = (paper.value.questions || []).filter(q => {
      const key = String(q.id)
      return q.qType === 'multiple'
        ? !(multiAnswers[key] || []).length
        : !answers[key]
    })
    if (unanswered.length) {
      await ElMessageBox.confirm(
        `还有 ${unanswered.length} 题未作答, 确定交卷吗?`, '提示', { type: 'warning' }
      )
    } else {
      await ElMessageBox.confirm('确定交卷吗? 交卷后不能修改。', '提示', { type: 'warning' })
    }
  }
  submitted = true
  submitting.value = true
  try {
    const res = await submitExam(route.params.id, collectAnswers())
    result.value = res.data || {}
    resultVisible.value = true
  } catch (e) {
    submitted = false
    // 已交卷/超时等错误返回列表
    router.replace('/my-exam')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.exam-head {
  margin-bottom: 12px;
}

.exam-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.timer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #1677ff;
}

.timer.danger {
  color: #f56c6c;
}

.exam-meta {
  color: #909399;
  font-size: 13px;
  margin: 6px 0 0;
}

.q-card {
  margin-bottom: 12px;
}

.q-stem {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.6;
}

.q-index {
  flex-shrink: 0;
}

.q-body {
  padding: 12px 4px 0 8px;
}

.opt {
  display: block;
  margin: 0 0 8px;
}

.submit-bar {
  text-align: center;
  padding: 12px 0 24px;
}

.result {
  text-align: center;
}

.score {
  font-size: 44px;
  font-weight: 700;
}

.score.pass {
  color: #67c23a;
}

.score.fail {
  color: #f56c6c;
}

.reward {
  margin-left: 8px;
}

.timeout-tip {
  color: #e6a23c;
  font-size: 13px;
}
</style>
