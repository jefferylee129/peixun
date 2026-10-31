<template>
  <div class="login-bg">
    <el-card class="login-card">
      <div class="title">
        <el-icon :size="30" color="#1677ff"><School /></el-icon>
        <h2>国企培训管理系统</h2>
        <p>Training Management System</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="submit">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="tips">
        <div>超级管理员:admin / admin123</div>
        <div>培训管理员:tradmin / admin123</div>
        <div>普通员工:zhangsan / user123</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const formRef = ref()
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form)
    await store.fetchMe()
    ElMessage.success('欢迎, ' + (store.user.nickname || store.user.username))
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-bg {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f2b52 0%, #16509e 55%, #2b7bd4 100%);
}

.login-card {
  width: 400px;
  border-radius: 10px;
}

.title {
  text-align: center;
  margin-bottom: 18px;
}

.title h2 {
  margin: 8px 0 4px;
  font-size: 22px;
}

.title p {
  margin: 0;
  color: #909399;
  font-size: 12px;
  letter-spacing: 1px;
}

.login-btn {
  width: 100%;
}

.tips {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 10px 12px;
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}
</style>
