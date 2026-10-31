import { ElMessage } from 'element-plus'

/**
 * 下载后端导出的 Excel(Blob), 自动处理业务错误
 */
export async function downloadExcel(url, params, filename) {
  const { default: request } = await import('@/utils/request')
  const blob = await request.get(url, { params, responseType: 'blob' })
  if (blob && blob.type && blob.type.includes('json')) {
    try {
      const text = await blob.text()
      const j = JSON.parse(text)
      ElMessage.error(j.msg || '导出失败')
    } catch (e) {
      ElMessage.error('导出失败')
    }
    return
  }
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename.endsWith('.xlsx') ? filename : filename + '.xlsx'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(objectUrl)
}
