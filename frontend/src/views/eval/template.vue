<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="模板名称"
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
        <el-button v-perm="'biz:eval:template'" type="primary" :icon="Plus" @click="openForm()">新增模板</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="评估层级" width="170" align="center">
          <template #default="{ row }">{{ EVAL_LEVEL[row.evalLevel] || row.evalLevel }}</template>
        </el-table-column>
        <el-table-column label="条目数" width="80" align="center">
          <template #default="{ row }">{{ itemCount(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'biz:eval:template'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'biz:eval:template'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑评估模板' : '新增评估模板'" width="720px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="form.templateName" placeholder="请输入模板名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评估层级" prop="evalLevel">
              <el-select v-model="form.evalLevel" style="width: 100%">
                <el-option v-for="(label, key) in EVAL_LEVEL" :key="key" :label="label" :value="Number(key)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="模板用途说明(可空)" />
        </el-form-item>
        <el-form-item label="评估条目" required>
          <div class="item-list">
            <div v-for="(item, index) in form.itemList" :key="index" class="item-row">
              <span class="item-key">{{ item.key }}</span>
              <el-input v-model="item.title" placeholder="条目名称, 如: 课程内容实用性" />
              <el-select v-model="item.type" style="width: 130px">
                <el-option label="打分(1-5分)" value="score" />
                <el-option label="文本" value="text" />
              </el-select>
              <el-checkbox v-model="item.required">必填</el-checkbox>
              <el-button link type="danger" :icon="Delete" @click="form.itemList.splice(index, 1)" />
            </div>
            <el-button :icon="Plus" @click="addItem">添加条目</el-button>
          </div>
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
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import {
  pageEvalTemplate, evalTemplateDetail, addEvalTemplate, updateEvalTemplate, deleteEvalTemplate
} from '@/api/train'
import { EVAL_LEVEL } from '@/utils/constants'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '' })

const emptyForm = () => ({
  id: null, templateName: '', evalLevel: 1, status: '0', description: '', itemList: []
})
const form = reactive(emptyForm())

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  evalLevel: [{ required: true, message: '请选择评估层级', trigger: 'change' }]
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ') : ''
}

function itemCount(row) {
  try {
    return JSON.parse(row.items || '[]').length
  } catch (e) {
    return '-'
  }
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageEvalTemplate(query)
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

function addItem() {
  let max = 0
  for (const it of form.itemList) {
    const m = /^q(\d+)$/.exec(it.key || '')
    if (m) max = Math.max(max, Number(m[1]))
  }
  form.itemList.push({ key: `q${max + 1}`, title: '', type: 'score', required: true })
}

async function openForm(row) {
  Object.assign(form, emptyForm())
  if (row) {
    const res = await evalTemplateDetail(row.id)
    const d = res.data
    Object.assign(form, {
      id: d.id,
      templateName: d.templateName,
      evalLevel: d.evalLevel,
      status: d.status,
      description: d.description,
      itemList: (d.itemList || []).map(it => ({
        key: it.key, title: it.title, type: it.type || 'score', required: !!it.required
      }))
    })
  }
  formVisible.value = true
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    const items = form.itemList.filter(it => it.title && it.title.trim())
    if (!items.length) {
      ElMessage.warning('请至少添加一个评估条目')
      return
    }
    if (items.length !== form.itemList.length) {
      ElMessage.warning('存在未填写名称的条目, 请补全或删除')
      return
    }
    saving.value = true
    try {
      const data = {
        id: form.id,
        templateName: form.templateName,
        evalLevel: form.evalLevel,
        status: form.status,
        description: form.description,
        items: JSON.stringify(items.map(({ key, title, type, required }) => ({ key, title, type, required })))
      }
      if (form.id) {
        await updateEvalTemplate(form.id, data)
      } else {
        await addEvalTemplate(data)
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
  ElMessageBox.confirm(`确认删除模板「${row.templateName}」?已产生评估记录的模板不可删除`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteEvalTemplate(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(() => load())
</script>

<style scoped>
.item-list {
  width: 100%;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-row :deep(.el-input) {
  flex: 1;
}

.item-key {
  width: 34px;
  color: #909399;
  flex-shrink: 0;
}
</style>
