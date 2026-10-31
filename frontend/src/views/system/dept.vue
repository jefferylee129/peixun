<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="部门名称">
          <el-input v-model="keyword" placeholder="输入名称即时过滤" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-perm="'system:dept:add'" type="primary" :icon="Plus" @click="openForm()">新增部门</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredList"
        row-key="id"
        border
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="220" />
        <el-table-column prop="orderNum" label="排序" width="70" align="center" />
        <el-table-column prop="leader" label="负责人" width="110" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'system:dept:add'" link type="primary" @click="openForm(null, row.id)">新增下级</el-button>
            <el-button v-perm="'system:dept:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:dept:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑部门' : '新增部门'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="负责人" prop="leader">
              <el-input v-model="form.leader" placeholder="可空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="可空" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { deptTree, addDept, updateDept, deleteDept } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const treeData = ref([])
const keyword = ref('')
const formVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null, parentId: 0, deptName: '', orderNum: 0, leader: '', phone: '', status: '0'
})
const form = reactive(emptyForm())

const rules = {
  parentId: [{ required: true, message: '请选择上级部门', trigger: 'change' }],
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const filteredList = computed(() => filterTree(treeData.value, keyword.value))

/** 上级部门选项: 根节点(0) + 现有部门树; 编辑时排除自身及下级, 避免形成环 */
const parentOptions = computed(() => {
  const tree = form.id ? removeSubtree(treeData.value, form.id) : treeData.value
  return [{ id: 0, deptName: '顶级部门', children: tree }]
})

function filterTree(nodes, kw) {
  const k = (kw || '').trim().toLowerCase()
  if (!k) return nodes
  const walk = list =>
    list
      .map(node => {
        const children = walk(node.children || [])
        if (node.deptName.toLowerCase().includes(k) || children.length) {
          return { ...node, children }
        }
        return null
      })
      .filter(Boolean)
  return walk(nodes)
}

function removeSubtree(nodes, id) {
  const walk = list =>
    list
      .filter(node => node.id !== id)
      .map(node => ({ ...node, children: walk(node.children || []) }))
  return walk(nodes)
}

async function load() {
  loading.value = true
  try {
    const res = await deptTree()
    treeData.value = res.data
  } finally {
    loading.value = false
  }
}

function openForm(row, parentId) {
  Object.assign(form, emptyForm())
  if (row) {
    form.id = row.id
    form.parentId = row.parentId
    form.deptName = row.deptName
    form.orderNum = row.orderNum
    form.leader = row.leader
    form.phone = row.phone
    form.status = row.status
  } else if (parentId) {
    form.parentId = parentId
  }
  formVisible.value = true
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateDept(form.id, form)
      } else {
        await addDept(form)
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
  ElMessageBox.confirm(`确认删除部门「${row.deptName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteDept(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>
