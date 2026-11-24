<template>
  <div class="page">
    <el-card shadow="never" class="verify-card">
      <template #header>结业证书防伪查验</template>

      <el-alert type="info" :closable="false" show-icon class="tip">
        请输入证书上的<b>证书编号</b>与<b>防伪码</b>两项信息。编号与防伪码均印于证书上, 缺一不可;
        查询结果已做脱敏处理, 仅用于核验证书真伪。
      </el-alert>

      <el-form :inline="true" :model="form" class="verify-form" @submit.prevent="doVerify">
        <el-form-item label="证书编号">
          <el-input v-model="form.certNo" placeholder="如 QGPX-2026-000001" clearable style="width: 230px" @keyup.enter="doVerify" />
        </el-form-item>
        <el-form-item label="防伪码">
          <el-input v-model="form.code" placeholder="6 位数字" clearable maxlength="6" style="width: 140px" @keyup.enter="doVerify" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="doVerify">查验</el-button>
        </el-form-item>
      </el-form>

      <el-empty v-if="!result" description="请输入证书信息后点击查验" :image-size="90" />

      <el-result v-else-if="result.valid === false" icon="warning" title="未查询到有效证书"
        sub-title="请核对证书编号与防伪码是否与证书一致; 如仍有疑问请联系培训管理部门" />

      <div v-else class="cert-result">
        <el-result icon="success" title="证书有效" sub-title="该证书信息与系统登记一致" />
        <el-descriptions :column="2" border class="desc">
          <el-descriptions-item label="证书编号">{{ result.certNo }}</el-descriptions-item>
          <el-descriptions-item label="持证人">{{ result.userName }}</el-descriptions-item>
          <el-descriptions-item label="工号(后四位)">{{ result.employeeNoTail }}</el-descriptions-item>
          <el-descriptions-item label="发证日期">{{ result.issueDate }}</el-descriptions-item>
          <el-descriptions-item label="培训班次" :span="2">{{ result.className }}</el-descriptions-item>
          <el-descriptions-item label="认定学时">{{ result.hours }}</el-descriptions-item>
          <el-descriptions-item label="考核结论">{{ result.grade }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { verifyCertificate } from '@/api/optimize'

const form = reactive({ certNo: '', code: '' })
const loading = ref(false)
const result = ref(null)

async function doVerify() {
  if (!form.certNo.trim() || !form.code.trim()) {
    result.value = { valid: false }
    return
  }
  loading.value = true
  try {
    const res = await verifyCertificate({ certNo: form.certNo.trim(), code: form.code.trim() })
    result.value = res.data || { valid: false }
  } catch (e) {
    // 限流(429)等错误已由请求拦截器提示, 保留上一次结果
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.verify-card {
  max-width: 860px;
}

.tip {
  margin-bottom: 16px;
}

.verify-form {
  margin-bottom: 8px;
}

.cert-result {
  margin-top: 8px;
}

.desc {
  max-width: 720px;
  margin: 0 auto;
}
</style>
