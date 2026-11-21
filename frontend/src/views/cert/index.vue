<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>结业证书台账</span>
          <div class="head-actions">
            <el-select v-model="year" clearable placeholder="年度" style="width: 110px" @change="load(1)">
              <el-option v-for="y in yearOptions()" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
            <el-input
              v-model="keyword"
              placeholder="姓名检索"
              clearable
              style="width: 180px"
              @keyup.enter="load(1)"
              @clear="load(1)"
            />
            <el-button type="primary" @click="load(1)">查询</el-button>
            <el-button v-if="store.hasPerm('biz:cert:issue')" @click="issueVisible = true">班次补发证书</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="certNo" label="证书编号" width="170" />
        <el-table-column prop="userName" label="姓名" width="110" />
        <el-table-column prop="employeeNo" label="工号" width="110" />
        <el-table-column prop="className" label="班次" min-width="200" show-overflow-tooltip />
        <el-table-column prop="hours" label="结业学时" width="90" align="center" />
        <el-table-column prop="grade" label="结论" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="small">{{ row.grade }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issueDate" label="发证日期" width="110" align="center" />
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="show(row)">证书预览</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadList" />
    </el-card>

    <!-- 证书预览 -->
    <el-dialog v-model="previewVisible" title="结业证书" width="640px" destroy-on-close>
      <div class="cert-paper">
        <div class="cert-title">培 训 结 业 证 书</div>
        <div class="cert-no">{{ current.certNo }}</div>
        <div class="cert-body">
          <p><b>{{ current.userName }}</b> 同志(工号 {{ current.employeeNo }})</p>
          <p>
            于 {{ current.classStartDate }} 至 {{ current.classEndDate }} 参加
            「<b>{{ current.className }}</b>」培训,
          </p>
          <p>完成规定培训内容, 认定学时 <b>{{ current.hours }}</b> 学时, 考核结论: <b>{{ current.grade }}</b>。</p>
          <p>特发此证。</p>
        </div>
        <div class="cert-foot">
          <span>发证日期: {{ current.issueDate }}</span>
          <span>国企培训管理系统</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="printCert">打印</el-button>
      </template>
    </el-dialog>

    <!-- 补发 -->
    <el-dialog v-model="issueVisible" title="班次补发证书(幂等, 已发不再重发)" width="460px">
      <el-form label-width="90px">
        <el-form-item label="班次ID">
          <el-input v-model.number="issueClassId" placeholder="输入已结业班次的ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="issuing" @click="doIssue">补发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pageCertificates, issueCertificates } from '@/api/optimize'
import { yearOptions } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const store = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const year = ref(null)
const keyword = ref('')
const query = reactive({ page: 1, size: 10 })

async function loadList(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageCertificates({
      ...query,
      year: year.value || null,
      keyword: keyword.value || null
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

const previewVisible = ref(false)
const current = ref({})

function show(row) {
  current.value = row
  previewVisible.value = true
}

function printCert() {
  window.print()
}

const issueVisible = ref(false)
const issuing = ref(false)
const issueClassId = ref(null)

async function doIssue() {
  if (!issueClassId.value) {
    ElMessage.warning('请输入班次ID')
    return
  }
  issuing.value = true
  try {
    const res = await issueCertificates(issueClassId.value)
    ElMessage.success(`本次补发 ${res.data} 本证书(已有证书自动跳过)`)
    issueVisible.value = false
    load(1)
  } finally {
    issuing.value = false
  }
}

onMounted(() => load())
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

.cert-paper {
  border: 3px double #b8860b;
  padding: 36px 40px;
  text-align: center;
  background: #fffdf5;
}

.cert-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #b8860b;
}

.cert-no {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

.cert-body {
  margin: 24px 0;
  font-size: 15px;
  line-height: 2;
  text-align: left;
  text-indent: 2em;
}

.cert-foot {
  display: flex;
  justify-content: space-between;
  color: #606266;
  font-size: 13px;
}

@media print {
  body * {
    visibility: hidden;
  }

  .cert-paper,
  .cert-paper * {
    visibility: visible;
  }

  .cert-paper {
    position: fixed;
    left: 50%;
    top: 80px;
    transform: translateX(-50%);
    width: 600px;
  }
}
</style>
