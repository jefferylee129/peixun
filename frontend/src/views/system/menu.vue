<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="菜单名称">
          <el-input v-model="keyword" placeholder="输入名称即时过滤" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-perm="'system:menu:add'" type="primary" :icon="Plus" @click="openForm()">新增菜单</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredList"
        row-key="id"
        border
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="200" />
        <el-table-column prop="icon" label="图标" width="110" align="center" show-overflow-tooltip />
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="(MENU_TYPE[row.menuType] || {}).type || 'info'">
              {{ (MENU_TYPE[row.menuType] || {}).label || row.menuType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="perms" label="权限标识" width="180" show-overflow-tooltip />
        <el-table-column prop="path" label="路由地址" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="orderNum" label="排序" width="70" align="center" />
        <el-table-column label="可见" width="80" align="center">
          <template #default="{ row }">{{ row.visible === '0' ? '显示' : '隐藏' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.menuType !== 'F'"
              v-perm="'system:menu:add'"
              link
              type="primary"
              @click="openForm(null, row.id)"
            >新增下级</el-button>
            <el-button v-perm="'system:menu:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:menu:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', children: 'children' }"
            node-key="id"
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType" @change="onTypeChange">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名, 如 User(可空)" />
        </el-form-item>
        <el-row v-if="form.menuType !== 'F'" :gutter="16">
          <el-col :span="12">
            <el-form-item
              label="路由地址"
              prop="path"
              :rules="[{ required: true, message: '请输入路由地址', trigger: 'blur' }]"
            >
              <el-input v-model="form.path" placeholder="如 system/user" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType === 'C'" :span="12">
            <el-form-item
              label="组件路径"
              prop="component"
              :rules="[{ required: true, message: '请输入组件路径', trigger: 'blur' }]"
            >
              <el-input v-model="form.component" placeholder="如 system/user" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item
          v-if="form.menuType !== 'M'"
          label="权限标识"
          prop="perms"
          :rules="[{ required: form.menuType === 'F', message: '请输入权限标识', trigger: 'blur' }]"
        >
          <el-input v-model="form.perms" placeholder="如 system:user:list" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="是否显示" prop="visible">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
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
import { menuTree, addMenu, updateMenu, deleteMenu } from '@/api/system'

const MENU_TYPE = {
  M: { label: '目录', type: 'warning' },
  C: { label: '菜单', type: 'success' },
  F: { label: '按钮', type: 'info' }
}

const loading = ref(false)
const saving = ref(false)
const treeData = ref([])
const keyword = ref('')
const formVisible = ref(false)
const formRef = ref()

const emptyForm = () => ({
  id: null, parentId: 0, menuType: 'C', menuName: '', path: '', component: '',
  icon: '', perms: '', orderNum: 0, visible: '0', status: '0'
})
const form = reactive(emptyForm())

const rules = {
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
}

const filteredList = computed(() => filterTree(treeData.value, keyword.value))

/** 上级菜单选项: 根节点(0) + 现有菜单树; 编辑时排除自身及下级, 避免形成环 */
const parentOptions = computed(() => {
  const tree = form.id ? removeSubtree(treeData.value, form.id) : treeData.value
  return [{ id: 0, menuName: '顶级菜单', children: tree }]
})

function filterTree(nodes, kw) {
  const k = (kw || '').trim().toLowerCase()
  if (!k) return nodes
  const walk = list =>
    list
      .map(node => {
        const children = walk(node.children || [])
        if (node.menuName.toLowerCase().includes(k) || children.length) {
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
    const res = await menuTree()
    treeData.value = res.data
  } finally {
    loading.value = false
  }
}

/** 类型切换时清空不再适用的字段, 避免把隐藏字段的残留值提交给后端 */
function onTypeChange() {
  if (form.menuType === 'F') {
    form.path = ''
    form.component = ''
  } else if (form.menuType === 'M') {
    form.component = ''
    form.perms = ''
  }
}

function openForm(row, parentId) {
  Object.assign(form, emptyForm())
  if (row) {
    form.id = row.id
    form.parentId = row.parentId
    form.menuType = row.menuType
    form.menuName = row.menuName
    form.path = row.path
    form.component = row.component
    form.icon = row.icon
    form.perms = row.perms
    form.orderNum = row.orderNum
    form.visible = row.visible
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
        await updateMenu(form.id, form)
      } else {
        await addMenu(form)
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
  ElMessageBox.confirm(`确认删除菜单「${row.menuName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteMenu(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>
