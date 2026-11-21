<template>
  <div class="archive">
    <!-- 头部: 基本信息 + 年度学时 -->
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-head">
          <span>培训档案 · 一人一档</span>
          <el-select v-model="year" style="width: 110px" @change="load">
            <el-option label="全部年度" :value="0" />
            <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </div>
      </template>
      <div class="head-body">
        <el-descriptions :column="3" border size="small" class="desc">
          <el-descriptions-item label="姓名">{{ info.user?.nickname }}</el-descriptions-item>
          <el-descriptions-item label="工号">{{ info.user?.employeeNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ info.user?.deptName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="人员类型">{{ USER_TYPE[info.user?.userType] || info.user?.userType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="岗位">{{ info.user?.position || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录账号">{{ info.user?.username }}</el-descriptions-item>
        </el-descriptions>
        <div class="hours-ring">
          <el-progress
            type="dashboard"
            :percentage="Math.min(Number(hours.percent) || 0, 100)"
            :color="hours.reached ? '#67c23a' : '#e6a23c'"
            :width="120"
          >
            <template #default>
              <div class="ring-value">{{ hours.total ?? 0 }}</div>
              <div class="ring-label">年度学时</div>
            </template>
          </el-progress>
          <p class="ring-tip">
            要求 <b>{{ hours.required ?? '-' }}</b> · {{ yearLabel }}
          </p>
          <p class="ring-src">
            <el-tag v-for="s in hours.bySource || []" :key="s.source" size="small" class="src-tag">
              {{ HOUR_SOURCE[s.source] || s.source }} {{ Number(s.total) }}
            </el-tag>
          </p>
        </div>
      </div>
    </el-card>

    <!-- 履历时间线 -->
    <el-card shadow="never">
      <template #header>培训履历时间线</template>
      <el-empty v-if="!(info.timeline || []).length" description="暂无培训记录" :image-size="80" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="(t, i) in info.timeline"
          :key="i"
          :timestamp="String(t.time || '').slice(0, 10)"
          :type="TL_TYPE[t.type]?.color || 'primary'"
          :icon="TL_TYPE[t.type]?.icon"
        >
          <b>{{ t.title }}</b>
          <span class="tl-detail">{{ t.detail }}</span>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 明细页签 -->
    <el-card shadow="never">
      <el-tabs>
        <el-tab-pane :label="`参训班次(${(info.classes || []).length})`">
          <el-table :data="info.classes || []" size="small" border stripe>
            <el-table-column prop="className" label="班次" min-width="200" show-overflow-tooltip />
            <el-table-column prop="startDate" label="开始" width="100" align="center" />
            <el-table-column prop="endDate" label="结束" width="100" align="center" />
            <el-table-column label="班次状态" width="90" align="center">
              <template #default="{ row }">{{ (CLASS_STATUS[row.status] || {}).label || row.status }}</template>
            </el-table-column>
            <el-table-column label="参训状态" width="90" align="center">
              <template #default="{ row }">{{ STUDENT_STATUS[row.studentStatus] || row.studentStatus }}</template>
            </el-table-column>
            <el-table-column prop="totalHours" label="认定学时" width="90" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`评估记录(${(info.evals || []).length})`">
          <el-table :data="info.evals || []" size="small" border stripe>
            <el-table-column prop="templateName" label="评估问卷" min-width="200" show-overflow-tooltip />
            <el-table-column prop="className" label="班次" min-width="180" show-overflow-tooltip />
            <el-table-column prop="totalScore" label="打分合计" width="90" align="center" />
            <el-table-column prop="submitTime" label="提交时间" width="160" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`结业证书(${(info.certificates || []).length})`">
          <el-table :data="info.certificates || []" size="small" border stripe>
            <el-table-column prop="certNo" label="证书编号" width="170" />
            <el-table-column prop="className" label="班次" min-width="200" show-overflow-tooltip />
            <el-table-column prop="hours" label="学时" width="80" align="center" />
            <el-table-column prop="grade" label="结论" width="80" align="center" />
            <el-table-column prop="issueDate" label="发证日期" width="110" align="center" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`考试成绩(${(info.exams || []).length})`">
          <el-table :data="info.exams || []" size="small" border stripe>
            <el-table-column prop="title" label="考试" min-width="200" show-overflow-tooltip />
            <el-table-column prop="score" label="成绩" width="90" align="center" />
            <el-table-column label="结论" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.passed === 1 ? 'success' : 'danger'" size="small">
                  {{ row.passed === 1 ? '及格' : '不及格' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="交卷时间" width="160" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Notebook, ChatDotRound, Medal, EditPen } from '@element-plus/icons-vue'
import { myArchive, userArchive } from '@/api/optimize'
import { CLASS_STATUS, HOUR_SOURCE, USER_TYPE, yearOptions } from '@/utils/constants'

const props = defineProps({
  /** 查看目标用户ID; self 模式由后端按登录人取 */
  userId: { type: Number, default: null }
})

const info = ref({})
const year = ref(new Date().getFullYear())

const hours = computed(() => info.value.hours || {})
const yearLabel = computed(() => (year.value ? `${year.value}年度` : '全部年度'))

const STUDENT_STATUS = { 0: '已报名', 2: '已完成', 3: '缺勤' }
const TL_TYPE = {
  class: { color: 'primary', icon: Notebook },
  eval: { color: 'warning', icon: ChatDotRound },
  cert: { color: 'success', icon: Medal },
  exam: { color: 'danger', icon: EditPen }
}

async function load() {
  const params = { year: year.value || null }
  const res = props.userId
    ? await userArchive(props.userId, params)
    : await myArchive(params)
  info.value = res.data || {}
}

onMounted(load)
</script>

<style scoped>
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-body {
  display: flex;
  gap: 24px;
  align-items: center;
}

.desc {
  flex: 1;
}

.hours-ring {
  text-align: center;
  min-width: 170px;
}

.ring-value {
  font-size: 22px;
  font-weight: 700;
}

.ring-label {
  color: #909399;
  font-size: 12px;
}

.ring-tip {
  color: #606266;
  font-size: 13px;
  margin: 6px 0 4px;
}

.ring-src {
  margin: 0;
}

.src-tag {
  margin: 0 4px 4px 0;
}

.tl-detail {
  color: #909399;
  margin-left: 8px;
  font-size: 13px;
}
</style>
