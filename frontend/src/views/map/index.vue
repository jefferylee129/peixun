<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <div class="head-title">
            <span>岗位学习地图配置</span>
            <el-select v-model="year" style="width: 110px; margin-left: 12px" @change="load">
              <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
          </div>
          <div class="head-actions">
            <el-button @click="load">重置</el-button>
            <el-button
              v-for="t in TYPES"
              :key="t.key"
              :type="dirty[t.key] ? 'primary' : 'default'"
              :plain="!dirty[t.key]"
              :loading="saving === t.key"
              :disabled="!canEdit"
              @click="save(t.key)"
            >保存{{ t.label }}</el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="tip">
        按人员类型勾选年度必修课程(仅已发布课程可选)。员工在「我的学习地图」查看必修清单与完成情况;
        完成口径与学时台账一致:该课程存在<b>已认定学时</b>即视为完成。
      </el-alert>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column prop="courseCode" label="课程编码" width="130" />
        <el-table-column prop="courseName" label="课程" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">{{ NEED_TYPE[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="学时" width="70" align="center" />
        <el-table-column v-for="t in TYPES" :key="t.key" :label="t.label" width="150" align="center">
          <template #header>
            {{ t.label }}
            <div class="col-count">{{ countOf(t.key) }} 门</div>
          </template>
          <template #default="{ row }">
            <el-checkbox
              v-model="checked[row.courseId][t.key]"
              :disabled="!canEdit"
              @change="markDirty(t.key)"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" v-if="canViewStats" class="completion-card">
      <template #header>各人员类型必修完成率({{ year }}年)</template>
      <el-table :data="completion" border size="small">
        <el-table-column label="人员类型" width="140">
          <template #default="{ row }">{{ (TYPES.find(t => t.key === row.userType) || {}).label || row.userType }}</template>
        </el-table-column>
        <el-table-column prop="people" label="适用人数" width="110" align="center" />
        <el-table-column prop="finished" label="已全部完成" width="110" align="center" />
        <el-table-column label="平均完成率" min-width="260">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.avgPercent) || 0" :stroke-width="14"
              :color="row.avgPercent >= 80 ? '#67c23a' : row.avgPercent >= 50 ? '#e6a23c' : '#f56c6c'" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!completion.length" description="本年度尚未配置必修课" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { mapMatrix, saveMap, mapCompletion } from '@/api/optimize'
import { NEED_TYPE, yearOptions } from '@/utils/constants'

const store = useUserStore()
const canEdit = computed(() => store.hasPerm('biz:map:edit'))
const canViewStats = computed(() => store.hasPerm('biz:map:list'))

const TYPES = [
  { key: 'manager', label: '管理干部' },
  { key: 'professional', label: '专业技术人员' },
  { key: 'staff', label: '一般员工' }
]

const year = ref(new Date().getFullYear())
const loading = ref(false)
const rows = ref([])
const checked = reactive({})
const saved = reactive({ manager: new Set(), professional: new Set(), staff: new Set() })
const dirty = reactive({ manager: false, professional: false, staff: false })
const saving = ref('')
const completion = ref([])

async function load() {
  loading.value = true
  try {
    const res = await mapMatrix({ year: year.value })
    rows.value = res.data.rows || []
    for (const t of TYPES) {
      saved[t.key] = new Set()
    }
    for (const r of rows.value) {
      checked[r.courseId] = {}
      for (const t of TYPES) {
        const on = (r.required || []).includes(t.key)
        checked[r.courseId][t.key] = on
        if (on) saved[t.key].add(r.courseId)
        dirty[t.key] = false
      }
    }
    loadCompletion()
  } finally {
    loading.value = false
  }
}

async function loadCompletion() {
  const res = await mapCompletion({ year: year.value })
  completion.value = res.data || []
}

function countOf(type) {
  return rows.value.filter(r => checked[r.courseId] && checked[r.courseId][type]).length
}

function markDirty(type) {
  const now = new Set(rows.value.filter(r => checked[r.courseId][type]).map(r => r.courseId))
  dirty[type] = now.size !== saved[type].size || [...now].some(id => !saved[type].has(id))
}

async function save(type) {
  saving.value = type
  try {
    const courseIds = rows.value.filter(r => checked[r.courseId][type]).map(r => r.courseId)
    const n = await saveMap({ year: year.value, userType: type, courseIds })
    saved[type] = new Set(courseIds)
    dirty[type] = false
    ElMessage.success(`已保存「${(TYPES.find(t => t.key === type) || {}).label}」必修课 ${n.data} 门`)
    loadCompletion()
  } finally {
    saving.value = ''
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.head-title {
  display: flex;
  align-items: center;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.tip {
  margin-bottom: 12px;
}

.col-count {
  font-size: 12px;
  color: #909399;
  font-weight: 400;
  line-height: 1.2;
}

.completion-card {
  margin-top: 12px;
}
</style>
