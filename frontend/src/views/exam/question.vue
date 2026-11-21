<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <span>考试题库(柯氏二级 · 学习层)</span>
          <div class="head-actions">
            <el-select v-model="query.qType" clearable placeholder="题型" style="width: 110px" @change="load(1)">
              <el-option label="单选题" value="single" />
              <el-option label="多选题" value="multiple" />
              <el-option label="判断题" value="judge" />
            </el-select>
            <el-input
              v-model="query.keyword"
              placeholder="题干检索"
              clearable
              style="width: 200px"
              @keyup.enter="load(1)"
              @clear="load(1)"
            />
            <el-button type="primary" @click="load(1)">查询</el-button>
            <el-button v-if="store.hasPerm('biz:question:edit')" type="success" @click="openAdd">新增题目</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="题型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="Q_TYPE[row.qType]?.tag">{{ Q_TYPE[row.qType]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" width="90" align="center" />
        <el-table-column prop="analysis" label="解析" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
              {{ row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="store.hasPerm('biz:question:edit')" label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="loadList" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑题目' : '新增题目'" width="680px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.qType" :disabled="!!form.id">
            <el-radio value="single">单选题</el-radio>
            <el-radio value="multiple">多选题</el-radio>
            <el-radio value="judge">判断题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.stem" type="textarea" :rows="2" maxlength="900" show-word-limit />
        </el-form-item>
        <template v-if="form.qType !== 'judge'">
          <el-form-item
            v-for="(opt, i) in form.options"
            :key="i"
            :label="`选项${opt.key}`"
            required
          >
            <div class="opt-row">
              <el-input v-model="opt.text" :placeholder="`选项 ${opt.key} 内容`" />
              <el-button
                v-if="form.options.length > 2"
                link
                type="danger"
                @click="removeOption(i)"
              >删除</el-button>
            </div>
          </el-form-item>
          <el-form-item label=" ">
            <el-button size="small" @click="addOption">添加选项</el-button>
          </el-form-item>
        </template>
        <el-form-item label="正确答案" required>
          <el-select v-if="form.qType === 'single'" v-model="form.answer" style="width: 120px">
            <el-option v-for="opt in form.options" :key="opt.key" :label="opt.key" :value="opt.key" />
          </el-select>
          <el-select v-else-if="form.qType === 'multiple'" v-model="answerKeys" multiple style="width: 200px">
            <el-option v-for="opt in form.options" :key="opt.key" :label="opt.key" :value="opt.key" />
          </el-select>
          <el-radio-group v-else v-model="form.answer">
            <el-radio value="对">对</el-radio>
            <el-radio value="错">错</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" maxlength="450" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pageQuestions, addQuestion, updateQuestion, deleteQuestion } from '@/api/optimize'
import Pagination from '@/components/Pagination.vue'

const store = useUserStore()
const Q_TYPE = {
  single: { label: '单选', tag: 'primary' },
  multiple: { label: '多选', tag: 'warning' },
  judge: { label: '判断', tag: 'success' }
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, qType: '', keyword: '' })

async function loadList(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageQuestions({
      page: query.page,
      size: query.size,
      qType: query.qType || null,
      keyword: query.keyword || null
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

const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, qType: 'single', stem: '', options: [], answer: '', analysis: '', status: '0' })
const answerKeys = ref([])

function blankOptions(n = 4) {
  return Array.from({ length: n }, (_, i) => ({ key: String.fromCharCode(65 + i), text: '' }))
}

function openAdd() {
  Object.assign(form, { id: null, qType: 'single', stem: '', options: blankOptions(), answer: '', analysis: '', status: '0' })
  answerKeys.value = []
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    qType: row.qType,
    stem: row.stem,
    options: row.qType === 'judge' ? [] : JSON.parse(row.options || '[]'),
    answer: row.answer,
    analysis: row.analysis || '',
    status: row.status
  })
  answerKeys.value = row.qType === 'multiple' ? row.answer.split('') : []
  dialogVisible.value = true
}

watch(() => form.qType, t => {
  if (!form.id) {
    if (t === 'judge') {
      form.answer = '对'
      form.options = []
    } else if ((form.options || []).length === 0) {
      form.options = blankOptions()
    }
    form.answer = t === 'judge' ? '对' : ''
  }
})

function addOption() {
  const next = String.fromCharCode(65 + form.options.length)
  if (form.options.length >= 8) {
    ElMessage.warning('最多 8 个选项')
    return
  }
  form.options.push({ key: next, text: '' })
}

function removeOption(i) {
  form.options.splice(i, 1)
  // 重排选项键
  form.options.forEach((o, idx) => {
    o.key = String.fromCharCode(65 + idx)
  })
}

async function save() {
  if (!form.stem.trim()) {
    ElMessage.warning('请填写题干')
    return
  }
  let answer = form.answer
  if (form.qType === 'multiple') {
    answer = [...answerKeys.value].sort().join('')
    if (!answer) {
      ElMessage.warning('请勾选正确答案')
      return
    }
  }
  const payload = {
    qType: form.qType,
    stem: form.stem.trim(),
    answer,
    analysis: form.analysis,
    status: form.status,
    options: form.qType === 'judge' ? '["对","错"]' : JSON.stringify(form.options)
  }
  saving.value = true
  try {
    if (form.id) {
      await updateQuestion(form.id, payload)
    } else {
      await addQuestion(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除题目「${row.stem.slice(0, 20)}…」吗?`, '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  load()
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

.opt-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>
