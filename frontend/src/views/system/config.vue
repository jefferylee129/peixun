<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="参数名称/参数键名"
            clearable
            style="width: 200px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-perm="'system:config:add'" type="primary" :icon="Plus" @click="openForm()">新增参数</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="configName" label="参数名称" width="200" show-overflow-tooltip />
        <el-table-column prop="configKey" label="参数键名" width="220" show-overflow-tooltip />
        <el-table-column prop="configValue" label="参数值" min-width="160" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'system:config:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:config:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑参数' : '新增参数'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="参数名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如: 年度学时要求" />
        </el-form-item>
        <el-form-item label="参数键名" prop="configKey">
          <el-input v-model="form.configKey" placeholder="如: hours.year.required" />
        </el-form-item>
        <el-form-item label="参数值" prop="configValue">
          <el-input v-model="form.configValue" placeholder="请输入参数值" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { pageConfig, addConfig, updateConfig, deleteConfig } from '@/api/system'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '' })

const emptyForm = () => ({
  id: null, configName: '', configKey: '', configValue: '', remark: ''
})
const form = reactive(emptyForm())

const rules = {
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageConfig(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  load(1)
}

function openForm(row) {
  Object.assign(form, emptyForm())
  if (row) {
    Object.assign(form, row)
  }
  formVisible.value = true
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateConfig(form.id, form)
      } else {
        await addConfig(form)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function del(row) {
  ElMessageBox.confirm(`确认删除参数「${row.configName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteConfig(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

/** 后端 LocalDateTime 兼容展示: 去掉 ISO 格式中的 T */
function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(() => load())
</script>
