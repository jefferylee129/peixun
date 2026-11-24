<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>我的学习地图(年度必修课)</span>
          <el-select v-model="year" style="width: 110px" @change="load">
            <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </div>
      </template>

      <div class="overview">
        <el-progress
          type="dashboard"
          :percentage="Number(map.percent) || 0"
          :width="150"
          :color="barColor"
        >
          <template #default>
            <div class="ov-value">{{ map.done ?? 0 }}/{{ map.total ?? 0 }}</div>
            <div class="ov-label">必修课完成</div>
          </template>
        </el-progress>
        <div class="ov-desc">
          <p>人员类型:<el-tag size="small">{{ typeLabel }}</el-tag></p>
          <p>
            完成状态:
            <el-tag :type="(map.percent || 0) >= 100 ? 'success' : (map.percent || 0) > 0 ? 'warning' : 'info'" size="small">
              {{ (map.percent || 0) >= 100 ? '必修课已全部完成' : `已完成 ${map.percent || 0}%` }}
            </el-tag>
          </p>
          <p class="tip">完成口径:该课程存在已认定学时(与学时台账一致)。可通过参加相关班次或外部认定完成。</p>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="map.items || []" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="courseCode" label="课程编码" width="140" />
        <el-table-column prop="courseName" label="必修课程" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">{{ NEED_TYPE[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="标准学时" width="90" align="center" />
        <el-table-column label="完成情况" width="130" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.done" type="success" size="small">已完成</el-tag>
            <el-tag v-else-if="!row.courseAvailable" type="info" size="small">课程已下架</el-tag>
            <el-tag v-else type="warning" size="small">待完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="findClass(row)">找班次</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && map.configured === false" description="本年度学习地图尚未配置, 请联系培训管理员" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myMap } from '@/api/optimize'
import { NEED_TYPE, yearOptions } from '@/utils/constants'

const router = useRouter()
const year = ref(new Date().getFullYear())
const loading = ref(false)
const map = ref({})

const TYPE_LABEL = { manager: '管理干部', professional: '专业技术人员', staff: '一般员工' }
const typeLabel = computed(() => TYPE_LABEL[map.value.userType] || map.value.userType || '-')
const barColor = computed(() => (map.value.percent || 0) >= 100 ? '#67c23a' : '#e6a23c')

async function load() {
  loading.value = true
  try {
    const res = await myMap({ year: year.value })
    map.value = res.data || {}
  } finally {
    loading.value = false
  }
}

function findClass(row) {
  // 引导员工到可报名班次, 按课程名检索
  router.push({ path: '/my-class', query: { keyword: row.courseName } })
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview {
  display: flex;
  align-items: center;
  justify-content: space-around;
}

.ov-value {
  font-size: 24px;
  font-weight: 700;
}

.ov-label {
  color: #909399;
  font-size: 12px;
}

.ov-desc {
  color: #606266;
  line-height: 2;
}

.tip {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
