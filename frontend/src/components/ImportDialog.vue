<template>
  <el-dialog :model-value="modelValue" :title="title" width="600px" @update:model-value="v => emit('update:modelValue', v)" @closed="reset">
    <el-alert type="info" :closable="false" style="margin-bottom: 12px">
      <template #title>
        <span>第一步: </span>
        <el-button link type="primary" @click="downloadTemplate">下载导入模板</el-button>
        <span>, 按模板格式填写(首行为表头勿改); 第二步: 选择文件导入, 系统逐行校验并反馈结果。</span>
      </template>
    </el-alert>

    <el-upload
      drag
      :auto-upload="false"
      :limit="1"
      accept=".xlsx"
      :on-change="onFileChange"
      :on-remove="() => clearFile()"
      :file-list="fileList"
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将 .xlsx 文件拖到此处, 或 <em>点击选择文件</em></div>
    </el-upload>

    <div v-if="result" class="import-result">
      <el-alert :type="result.fail === 0 ? 'success' : 'warning'" :closable="false">
        <template #title>
          共 {{ result.total }} 行: 成功 {{ result.success }}, 失败 {{ result.fail }}, 跳过 {{ result.skip }}
        </template>
      </el-alert>
      <div v-if="result.errors && result.errors.length" class="err-list">
        <div v-for="(e, i) in result.errors" :key="i" class="err-item">{{ e }}</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" :loading="uploading" :disabled="!file" @click="submit">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { importExcel } from '@/api/excel'
import { downloadExcel } from '@/utils/download'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '批量导入' },
  /** 模板类型, 对应后端 /excel/template/{templateType} */
  templateType: { type: String, required: true },
  /** 导入接口地址, 如 /excel/course/import */
  action: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'success'])

const file = ref(null)
const fileList = ref([])
const uploading = ref(false)
const result = ref(null)

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
  fileList.value = [uploadFile]
  result.value = null
}

function clearFile() {
  file.value = null
  fileList.value = []
}

function reset() {
  clearFile()
  result.value = null
  uploading.value = false
}

function downloadTemplate() {
  downloadExcel(`/excel/template/${props.templateType}`, {}, `导入模板-${props.templateType}`)
}

async function submit() {
  if (!file.value) return
  uploading.value = true
  try {
    const res = await importExcel(props.action, file.value)
    result.value = res.data
    if (res.data && res.data.success > 0) {
      ElMessage.success(`成功导入 ${res.data.success} 条`)
      emit('success', res.data)
    } else {
      ElMessage.warning('没有数据被导入, 请查看失败原因')
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.import-result {
  margin-top: 12px;
}

.err-list {
  max-height: 180px;
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
</style>
