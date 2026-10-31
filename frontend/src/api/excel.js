import request from '@/utils/request'

/** 上传 Excel 文件执行批量导入, 返回 ImportResult */
export function importExcel(action, file) {
  const fd = new FormData()
  fd.append('file', file)
  return request.post(action, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
