<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/账号/工号"
            clearable
            style="width: 200px"
            @keyup.enter="load(1)"
          />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="query.deptId"
            :data="deptTreeData"
            :props="{ label: 'deptName', children: 'children' }"
            node-key="id"
            clearable
            default-expand-all
            placeholder="全部"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="正常" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="toolbar">
        <el-button v-perm="'system:user:add'" type="primary" :icon="Plus" @click="openForm()">新增用户</el-button>
        <el-button v-perm="'system:user:add'" :icon="Upload" @click="importVisible = true">批量导入</el-button>
        <el-button v-perm="'system:user:list'" :icon="Download" @click="exportRows">导出Excel</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="nickname" label="姓名" width="100" />
        <el-table-column prop="username" label="登录账号" width="120" />
        <el-table-column prop="employeeNo" label="工号" width="100" />
        <el-table-column prop="deptName" label="部门" min-width="140" show-overflow-tooltip />
        <el-table-column prop="position" label="岗位" width="120" show-overflow-tooltip />
        <el-table-column label="人员类型" width="120" align="center">
          <template #default="{ row }">{{ USER_TYPE[row.userType] || row.userType }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">
              {{ row.status === '0' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160" align="center">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-perm="'system:user:edit'" link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-perm="'system:user:resetPwd'" link type="warning" @click="resetPwd(row)">重置密码</el-button>
            <el-button
              v-if="row.id !== 1"
              v-perm="'system:user:edit'"
              link
              :type="row.status === '0' ? 'info' : 'success'"
              @click="toggleStatus(row)"
            >{{ row.status === '0' ? '停用' : '启用' }}</el-button>
            <el-button
              v-if="row.id !== 1"
              v-perm="'system:user:remove'"
              link
              type="danger"
              @click="del(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <Pagination v-model:page="query.page" v-model:size="query.size" :total="total" @change="load()" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="760px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="form.username" placeholder="请输入登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="deptId">
              <el-tree-select
                v-model="form.deptId"
                :data="deptTreeData"
                :props="{ label: 'deptName', children: 'children' }"
                node-key="id"
                clearable
                default-expand-all
                placeholder="请选择部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人员类型" prop="userType">
              <el-select v-model="form.userType" style="width: 100%">
                <el-option v-for="(label, key) in USER_TYPE" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeNo">
              <el-input v-model="form.employeeNo" placeholder="请输入工号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="岗位" prop="position">
              <el-input v-model="form.position" placeholder="请输入岗位" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio value="0">男</el-radio>
                <el-radio value="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status" :disabled="form.id === 1">
                <el-radio value="0">正常</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="form.id ? '留空则不修改密码' : '默认 123456'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
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
    <ImportDialog v-model="importVisible" title="批量导入用户" template-type="user" action="/excel/user/import" @success="load()" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import {
  pageUser, getUser, addUser, updateUser, deleteUser, resetUserPassword, changeUserStatus,
  deptTree, allRoles
} from '@/api/system'
import { USER_TYPE } from '@/utils/constants'
import ImportDialog from '@/components/ImportDialog.vue'
import { downloadExcel } from '@/utils/download'
import Pagination from '@/components/Pagination.vue'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const deptTreeData = ref([])
const roleOptions = ref([])
const formVisible = ref(false)
const formRef = ref()

const query = reactive({ page: 1, size: 10, keyword: '', deptId: '', status: '' })

const emptyForm = () => ({
  id: null, username: '', nickname: '', password: '', deptId: null, employeeNo: '',
  gender: '0', phone: '', email: '', position: '', userType: 'staff',
  roleIds: [], status: '0', remark: ''
})
const form = reactive(emptyForm())

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择人员类型', trigger: 'change' }]
}

async function load(page) {
  if (page) query.page = page
  loading.value = true
  try {
    const res = await pageUser(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.deptId = ''
  query.status = ''
  load(1)
}

async function openForm(row) {
  Object.assign(form, emptyForm())
  formVisible.value = true
  if (!row) return
  try {
    // 编辑时取详情, 行数据不含 roleIds
    const res = await getUser(row.id)
    Object.assign(form, res.data)
  } catch (e) {
    // 详情获取失败时保持空白表单, request.js 已提示错误
  }
}

function save() {
  formRef.value.validate(async valid => {
    if (!valid) return
    saving.value = true
    try {
      if (form.id) {
        await updateUser(form.id, form)
      } else {
        await addUser(form)
      }
      ElMessage.success('保存成功')
      formVisible.value = false
      load()
    } finally {
      saving.value = false
    }
  })
}

function resetPwd(row) {
  ElMessageBox.confirm(`确认重置用户「${row.nickname}」的密码?`, '提示', { type: 'warning' })
    .then(async () => {
      const res = await resetUserPassword(row.id)
      ElMessageBox.alert(`新密码为: ${res.data}, 请尽快通知用户修改`, '重置成功', { type: 'success' })
    })
    .catch(() => {})
}

function toggleStatus(row) {
  const target = row.status === '0' ? '1' : '0'
  const action = target === '1' ? '停用' : '启用'
  ElMessageBox.confirm(`确认${action}用户「${row.nickname}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await changeUserStatus(row.id, target)
      ElMessage.success(`${action}成功`)
      load()
    })
    .catch(() => {})
}

function del(row) {
  ElMessageBox.confirm(`确认删除用户「${row.nickname}」?`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

/** 后端 LocalDateTime 兼容展示: 去掉 ISO 格式中的 T */
function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(async () => {
  load()
  try {
    const res = await deptTree()
    deptTreeData.value = res.data
  } catch (e) {
    deptTreeData.value = []
  }
  try {
    const res = await allRoles()
    roleOptions.value = res.data
  } catch (e) {
    roleOptions.value = []
  }
})

const importVisible = ref(false)
function exportRows() {
  downloadExcel('/excel/user/export', { keyword: query.keyword, deptId: query.deptId, status: query.status }, '用户台账')
}
</script>
