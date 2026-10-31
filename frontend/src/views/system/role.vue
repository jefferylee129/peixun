<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="角色名称/角色标识"
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
        <el-button v-perm="'system:role:add'" type="primary" :icon="Plus" @click="openForm()">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="roleKey" label="角色标识" width="160" />
        <el-table-column prop="orderNum" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'system:role:edit'" link type="primary" @click="openAssign(row)">分配权限</el-button>
            <el-button v-perm="'system:role:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.id !== 1" v-perm="'system:role:remove'" link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如 deptAdmin, 与权限标识前缀对应" />
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
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可空" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" :title="`分配菜单权限 - ${assignRole.roleName}`" width="480px" top="8vh">
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        :props="{ label: 'menuName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
        :check-strictly="menuCheckStrictly"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignSaving" @click="saveAssign">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  pageRole, addRole, updateRole, deleteRole, roleMenuIds, assignRoleMenus, menuTree
} from '@/api/system'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formRef = ref()

const assignVisible = ref(false)
const assignSaving = ref(false)
const menuTreeData = ref([])
const menuTreeRef = ref()
// 回显已选菜单时先开启严格模式, 避免勾选父节点级联勾选全部子节点
const menuCheckStrictly = ref(true)
const assignRole = reactive({ id: null, roleName: '' })

const query = reactive({ page: 1, size: 10, keyword: '' })

const emptyForm = () => ({
  id: null, roleName: '', roleKey: '', orderNum: 0, status: '0', remark: ''
})
const form = reactive(emptyForm())

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageRole(query)
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
        await updateRole(form.id, form)
      } else {
        await addRole(form)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

async function openAssign(row) {
  assignRole.id = row.id
  assignRole.roleName = row.roleName
  assignVisible.value = true
  menuCheckStrictly.value = true
  const treeRes = await menuTree()
  menuTreeData.value = treeRes.data
  const res = await roleMenuIds(row.id)
  await nextTick()
  menuTreeRef.value.setCheckedKeys(res.data || [])
  await nextTick()
  menuCheckStrictly.value = false
}

async function saveAssign() {
  const tree = menuTreeRef.value
  // 半选的父节点一并保存, 保证父级菜单在侧边栏可见
  const menuIds = [...tree.getCheckedKeys(), ...tree.getHalfCheckedKeys()]
  assignSaving.value = true
  try {
    await assignRoleMenus(assignRole.id, menuIds)
    ElMessage.success('分配成功')
    assignVisible.value = false
  } finally {
    assignSaving.value = false
  }
}

function del(row) {
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteRole(row.id)
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
