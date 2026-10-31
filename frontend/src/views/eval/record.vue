<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="培训班次">
          <el-select v-model="classId" filterable clearable placeholder="选择班次" style="width: 240px">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估模板">
          <el-select v-model="templateId" filterable clearable placeholder="选择模板" style="width: 220px">
            <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-perm="'biz:eval:list'" :icon="Download" :disabled="!classId || !templateId" @click="exportRows">导出Excel</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="classId && templateId">
      <el-card shadow="never">
        <template #header>
          评估汇总: {{ className || classId }} / {{ templateName || templateId }}
        </template>

        <div class="stat-cards">
          <el-card shadow="never">
            <div class="stat">
              <div class="stat-num">{{ summary.evalCount ?? 0 }}</div>
              <div class="stat-label">参评人数</div>
            </div>
          </el-card>
          <el-card shadow="never">
            <div class="stat">
              <div class="stat-num">{{ summary.overallAvg ?? '-' }}</div>
              <div class="stat-label">总体平均分</div>
            </div>
          </el-card>
        </div>

        <el-table :data="scoreItems" border size="small">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="title" label="评分条目" min-width="220" />
          <el-table-column label="平均分" width="120" align="center">
            <template #default="{ row }">{{ (summary.itemAvg || {})[row.key] ?? '-' }}</template>
          </el-table-column>
        </el-table>

        <el-row :gutter="16" class="answer-zone">
          <el-col :span="12">
            <h4 class="answer-title">文本意见</h4>
            <div v-if="(summary.textAnswers || []).length" class="answers">
              <div v-for="(a, i) in summary.textAnswers" :key="i" class="answer">
                <span class="who">{{ a.user }}</span>
                <b>{{ a.title }}:</b>{{ a.value }}
              </div>
            </div>
            <el-empty v-else description="暂无文本意见" :image-size="60" />
          </el-col>
          <el-col :span="12">
            <h4 class="answer-title">学员评语</h4>
            <div v-if="(summary.comments || []).length" class="answers">
              <div v-for="(c, i) in summary.comments" :key="i" class="answer">
                <span class="who">{{ c.user }}</span>{{ c.comment }}
              </div>
            </div>
            <el-empty v-else description="暂无评语" :image-size="60" />
          </el-col>
        </el-row>
      </el-card>

      <el-card shadow="never" style="margin-top: 12px">
        <template #header>评估明细</template>
        <el-table v-loading="recordLoading" :data="records" border stripe>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="nickname" label="学员" width="110" />
          <el-table-column prop="deptName" label="部门" min-width="140" show-overflow-tooltip />
          <el-table-column prop="totalScore" label="总分" width="90" align="center" />
          <el-table-column label="提交时间" width="160" align="center">
            <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column prop="comment" label="评语" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>
    </template>
    <el-card v-else shadow="never">
      <el-empty description="请选择班次和评估模板查看汇总" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { evalRecords, evalSummary, pageClass, pageEvalTemplate } from '@/api/train'
import { downloadExcel } from '@/utils/download'

const classId = ref(null)
const templateId = ref(null)
const classes = ref([])
const templates = ref([])
const summary = ref({})
const records = ref([])
const recordLoading = ref(false)

const className = computed(() => (classes.value.find(c => c.id === classId.value) || {}).className || '')
const templateName = computed(() => (templates.value.find(t => t.id === templateId.value) || {}).templateName || '')
const scoreItems = computed(() => (summary.value.items || []).filter(it => it.type === 'score'))

function fmtTime(t) {
  return t ? String(t).replace('T', ' ') : ''
}

async function loadSummary() {
  const res = await evalSummary({ classId: classId.value, templateId: templateId.value })
  summary.value = res.data
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const res = await evalRecords({ classId: classId.value, templateId: templateId.value })
    records.value = res.data
  } finally {
    recordLoading.value = false
  }
}

function exportRows() {
  if (!classId.value || !templateId.value) return
  downloadExcel('/excel/eval/export', { classId: classId.value, templateId: templateId.value }, `培训评估-${className.value || classId.value}`)
}

watch([classId, templateId], ([c, t]) => {
  summary.value = {}
  records.value = []
  if (c && t) {
    loadSummary().catch(() => {})
    loadRecords().catch(() => {})
  }
})

onMounted(async () => {
  try {
    const res = await pageClass({ page: 1, size: 100 })
    classes.value = res.data.records
  } catch (e) {
    classes.value = []
  }
  try {
    const res = await pageEvalTemplate({ page: 1, size: 100 })
    templates.value = res.data.records
  } catch (e) {
    templates.value = []
  }
})
</script>

<style scoped>
.stat {
  text-align: center;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  color: #909399;
  font-size: 12px;
}

.answer-zone {
  margin-top: 16px;
}

.answer-title {
  margin: 0 0 8px;
  color: #303133;
}

.answers {
  max-height: 260px;
  overflow: auto;
  line-height: 1.9;
}

.answer .who {
  color: #909399;
  margin-right: 8px;
}
</style>
