<template>
  <div class="page">
    <el-tabs v-model="tab" @tab-change="onTab">
      <!-- 数据源 -->
      <el-tab-pane label="数据源" name="sources">
        <div class="toolbar">
          <el-button v-perm="'biz:integration:edit'" type="primary" :icon="Plus" @click="openSourceForm()">新增数据源</el-button>
          <el-button :icon="Refresh" @click="loadSources">刷新</el-button>
        </div>
        <el-table v-loading="srcLoading" :data="sources" border stripe>
          <el-table-column prop="sourceName" label="名称" min-width="160" />
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }">{{ TYPE_CN[row.sourceType] || row.sourceType }}</template>
          </el-table-column>
          <el-table-column prop="baseUrl" label="开放平台地址" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
                {{ row.status === '0' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastSyncTime" label="最近同步" width="170" />
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-perm="'biz:integration:sync'" link type="success" @click="doSync(row)">立即同步</el-button>
              <el-button v-perm="'biz:integration:sync'" link type="primary" @click="doTest(row)">测试连接</el-button>
              <el-button v-perm="'biz:integration:edit'" link type="primary" @click="openSourceForm(row)">编辑</el-button>
              <el-button
                v-perm="'biz:integration:edit'"
                link
                :type="row.status === '0' ? 'warning' : 'success'"
                @click="toggleStatus(row)"
              >{{ row.status === '0' ? '停用' : '启用' }}</el-button>
              <el-button v-perm="'biz:integration:edit'" link type="danger" @click="delSource(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 待入库课程 -->
      <el-tab-pane label="待入库课程" name="ext">
        <el-card shadow="never" class="search-card">
          <el-form :inline="true" @submit.prevent>
            <el-form-item label="数据源">
              <el-select v-model="extQuery.sourceId" clearable placeholder="全部" style="width: 180px" @change="loadExt(1)">
                <el-option v-for="s in sources" :key="s.id" :label="s.sourceName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="extQuery.status" clearable placeholder="全部" style="width: 120px" @change="loadExt(1)">
                <el-option label="待入库" value="0" />
                <el-option label="已入库" value="1" />
                <el-option label="已忽略" value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="extQuery.keyword" placeholder="课程名称" clearable style="width: 160px" @keyup.enter="loadExt(1)" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadExt(1)">查询</el-button>
            </el-form-item>
            <el-form-item>
              <el-button v-perm="'biz:integration:sync'" type="success" :disabled="!extSelected.length" @click="importSelected">
                入库所选({{ extSelected.length }})
              </el-button>
              <el-button v-perm="'biz:integration:sync'" type="warning" plain @click="importAllPending">入库全部待入库</el-button>
              <el-button v-perm="'biz:integration:sync'" :disabled="!extSelected.length" @click="ignoreSelected">忽略所选</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <el-table v-loading="extLoading" :data="extList" border stripe @selection-change="v => extSelected = v">
          <el-table-column type="selection" width="45" :selectable="row => row.status !== '2'" />
          <el-table-column prop="courseName" label="课程名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="categoryRaw" label="源分类" width="110" />
          <el-table-column label="映射分类" width="90" align="center">
            <template #default="{ row }">{{ CATEGORY_CN[row.category] || row.category || '未映射' }}</template>
          </el-table-column>
          <el-table-column prop="hours" label="学时" width="70" align="center" />
          <el-table-column prop="url" label="学习链接" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <a v-if="row.url" :href="row.url" target="_blank" rel="noopener" class="link">{{ row.url }}</a>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="({0:'warning',1:'success',2:'info'})[row.status]" size="small">
                {{ ({0:'待入库',1:'已入库',2:'已忽略'})[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="syncTime" label="同步时间" width="170" />
          <el-table-column label="操作" width="90" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="row.status !== '1'" v-perm="'biz:integration:sync'" link type="success" @click="importSelected([row.id])">入库</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination v-model:page="extQuery.page" v-model:size="extQuery.size" :total="extTotal" @change="loadExt()" />
      </el-tab-pane>

      <!-- 同步日志 -->
      <el-tab-pane label="同步日志" name="logs">
        <el-table v-loading="logLoading" :data="logs" border stripe>
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="sourceName" label="数据源" width="140" />
          <el-table-column label="动作" width="90" align="center">
            <template #default="{ row }">{{ ({pull:'拉取',import:'入库',test:'测试连接'})[row.action] || row.action }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="({0:'success',1:'danger',2:'warning'})[row.status]" size="small">
                {{ ({0:'成功',1:'失败',2:'部分成功'})[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="结果" min-width="220" show-overflow-tooltip />
          <el-table-column label="总/成/败/跳" width="130" align="center">
            <template #default="{ row }">{{ row.total }} / {{ row.success }} / {{ row.fail }} / {{ row.skip }}</template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="170" />
          <el-table-column label="操作" width="80" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="showLog(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination v-model:page="logQuery.page" v-model:size="logQuery.size" :total="logTotal" @change="loadLogs()" />
      </el-tab-pane>
    </el-tabs>

    <!-- 数据源表单 -->
    <el-dialog v-model="srcVisible" :title="srcForm.id ? '编辑数据源' : '新增数据源'" width="680px" top="6vh">
      <el-form ref="srcFormRef" :model="srcForm" :rules="srcRules" label-width="130px">
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="srcForm.sourceName" placeholder="如: 云学堂-集团账号" />
        </el-form-item>
        <el-form-item label="数据源类型" prop="sourceType">
          <el-radio-group v-model="srcForm.sourceType" :disabled="!!srcForm.id">
            <el-radio-button value="yunxuetang">云学堂</el-radio-button>
            <el-radio-button value="api">通用 REST API</el-radio-button>
            <el-radio-button value="mock">演示数据源</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <template v-if="srcForm.sourceType !== 'mock'">
          <el-form-item label="开放平台地址" prop="baseUrl">
            <el-input v-model="srcForm.baseUrl" placeholder="https://openapi.yunxuetang.cn 或贵司租约地址" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="令牌接口路径" label-width="130px">
                <el-input v-model="srcForm.tokenPath" placeholder="如 /oauth/token(通用API可留空)" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="列表接口路径" label-width="130px">
                <el-input v-model="srcForm.listPath" placeholder="如 /openapi/v1/course/list" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="App Key">
                <el-input v-model="srcForm.appKey" placeholder="开放平台应用 Key" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="App Secret">
                <el-input v-model="srcForm.appSecret" type="password" show-password placeholder="编辑时留空或******表示不变" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="列表数据路径">
            <el-input v-model="srcForm.dataPath" :placeholder="srcForm.sourceType === 'api' ? 'data' : 'data.list'" />
          </el-form-item>
        </template>
        <el-form-item v-if="srcForm.sourceType !== 'mock'" label="字段映射(JSON)">
          <el-input v-model="srcForm.fieldMapping" type="textarea" :rows="3"
            placeholder='可选。默认: {"externalId":"id","name":"name","category":"categoryName","hours":"credit","url":"url"}' />
        </el-form-item>
        <el-form-item label="分类映射(JSON)">
          <el-input v-model="srcForm.categoryMapping" type="textarea" :rows="3"
            placeholder='可选。源分类→本系统分类, 如 {"新员工培训":"general","管理课程":"mgmt","安全课程":"safety","党建课程":"party"}' />
        </el-form-item>
        <el-form-item label="其他配置(JSON)">
          <el-input v-model="srcForm.extraConfig" type="textarea" :rows="2"
            placeholder='可选。如 {"defaultCategory":"general","maxPages":5,"pageSize":50,"tokenBody":"form"}' />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="srcForm.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="srcForm.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="srcVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSource">保存</el-button>
      </template>
    </el-dialog>

    <!-- 日志详情 -->
    <el-dialog v-model="logVisible" title="同步日志详情" width="560px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="数据源">{{ logDetail?.sourceName }}</el-descriptions-item>
        <el-descriptions-item label="动作">{{ logDetail?.action }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ ({0:'成功',1:'失败',2:'部分成功'})[logDetail?.status] }}</el-descriptions-item>
        <el-descriptions-item label="总/成/败/跳">{{ logDetail?.total }} / {{ logDetail?.success }} / {{ logDetail?.fail }} / {{ logDetail?.skip }}</el-descriptions-item>
        <el-descriptions-item label="开始" :span="2">{{ logDetail?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结果" :span="2">{{ logDetail?.message }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="logDetail?.detail" class="err-list">
        <div v-for="(line, i) in (logDetail.detail || '').split('\n')" :key="i" class="err-item">{{ line }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import {
  pageSources, addSource, updateSource, deleteSource, changeSourceStatus,
  testSource, syncSource, pageExternal, importToCourse, ignoreExternal, pageSyncLogs
} from '@/api/integration'
import Pagination from '@/components/Pagination.vue'

const TYPE_CN = { yunxuetang: '云学堂', api: '通用 REST API', mock: '演示数据源' }
const CATEGORY_CN = { party: '党性教育', mgmt: '管理能力', skill: '岗位技能', safety: '安全合规', general: '通用素质' }

const tab = ref('sources')
const sources = ref([])
const srcLoading = ref(false)
const srcVisible = ref(false)
const saving = ref(false)
const srcFormRef = ref()

const emptySrc = () => ({
  id: null, sourceName: '', sourceType: 'yunxuetang', baseUrl: '', tokenPath: '', listPath: '',
  appKey: '', appSecret: '', dataPath: '', fieldMapping: '', categoryMapping: '', extraConfig: '',
  status: '0', remark: ''
})
const srcForm = reactive(emptySrc())
const srcRules = {
  sourceName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const extQuery = reactive({ page: 1, size: 10, sourceId: null, status: '0', keyword: '' })
const extList = ref([])
const extTotal = ref(0)
const extLoading = ref(false)
const extSelected = ref([])

const logQuery = reactive({ page: 1, size: 10 })
const logs = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logVisible = ref(false)
const logDetail = ref(null)

async function loadSources() {
  srcLoading.value = true
  try {
    const res = await pageSources({ page: 1, size: 50 })
    sources.value = res.data.records
  } finally {
    srcLoading.value = false
  }
}

function openSourceForm(row) {
  Object.assign(srcForm, emptySrc())
  if (row) Object.assign(srcForm, row)
  srcVisible.value = true
}

function saveSource() {
  srcFormRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      if (srcForm.id) {
        await updateSource(srcForm.id, srcForm)
      } else {
        await addSource(srcForm)
      }
      ElMessage.success('保存成功')
      srcVisible.value = false
      loadSources()
    } finally {
      saving.value = false
    }
  })
}

function delSource(row) {
  ElMessageBox.confirm(`确认删除数据源「${row.sourceName}」? 暂存课程将保留`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteSource(row.id)
      ElMessage.success('已删除')
      loadSources()
    })
    .catch(() => {})
}

function toggleStatus(row) {
  const to = row.status === '0' ? '1' : '0'
  changeSourceStatus(row.id, to).then(() => {
    ElMessage.success(to === '0' ? '已启用' : '已停用')
    loadSources()
  })
}

async function doTest(row) {
  const res = await testSource(row.id)
  const d = res.data
  if (d.ok) {
    ElMessage.success(d.message)
  } else {
    ElMessage.error(d.message)
  }
}

async function doSync(row) {
  const res = await syncSource(row.id)
  const d = res.data
  ElMessage.success(d.message || '同步完成')
  loadSources()
  loadExt(1)
  loadLogs()
}

function onTab(name) {
  if (name === 'ext') loadExt(1)
  if (name === 'logs') loadLogs()
}

async function loadExt(page) {
  if (page) extQuery.page = page
  extLoading.value = true
  try {
    const res = await pageExternal(extQuery)
    extList.value = res.data.records
    extTotal.value = res.data.total
  } finally {
    extLoading.value = false
  }
}

function importSelected(ids) {
  const list = ids || extSelected.value.map(v => v.id)
  if (!list.length) return
  ElMessageBox.confirm(`确认将所选 ${list.length} 门课程入库到课程库? 同名外部课程将更新`, '入库', { type: 'info' })
    .then(async () => {
      const res = await importToCourse({ ids: list })
      ElMessage.success(res.data.message)
      loadExt()
    })
    .catch(() => {})
}

function importAllPending() {
  ElMessageBox.confirm('确认将当前数据源下全部"待入库"课程入库? ', '入库', { type: 'info' })
    .then(async () => {
      const res = await importToCourse({ sourceId: extQuery.sourceId, all: false })
      ElMessage.success(res.data.message)
      loadExt()
    })
    .catch(() => {})
}

function ignoreSelected() {
  const ids = extSelected.value.map(v => v.id)
  if (!ids.length) return
  ElMessageBox.confirm(`确认忽略所选 ${ids.length} 门课程(不再入库)?`, '忽略', { type: 'warning' })
    .then(async () => {
      await ignoreExternal(ids)
      ElMessage.success('已忽略')
      loadExt()
    })
    .catch(() => {})
}

async function loadLogs() {
  logLoading.value = true
  try {
    const res = await pageSyncLogs(logQuery)
    logs.value = res.data.records
    logTotal.value = res.data.total
  } finally {
    logLoading.value = false
  }
}

function showLog(row) {
  logDetail.value = row
  logVisible.value = true
}

onMounted(() => {
  loadSources()
})
</script>

<style scoped>
.err-list {
  max-height: 200px;
  overflow-y: auto;
  background: #fef0f0;
  border-radius: 4px;
  padding: 8px 12px;
  margin-top: 8px;
}
.err-item {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.8;
}
.link {
  color: #1677ff;
}
</style>
