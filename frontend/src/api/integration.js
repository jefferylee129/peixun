import request from '@/utils/request'

export const pageSources = params => request.get('/biz/integrations/sources', { params })
export const getSource = id => request.get(`/biz/integrations/sources/${id}`)
export const addSource = data => request.post('/biz/integrations/sources', data)
export const updateSource = (id, data) => request.put(`/biz/integrations/sources/${id}`, data)
export const deleteSource = id => request.delete(`/biz/integrations/sources/${id}`)
export const changeSourceStatus = (id, status) =>
  request.put(`/biz/integrations/sources/${id}/status`, null, { params: { status } })
export const testSource = id => request.post(`/biz/integrations/sources/${id}/test`)
export const syncSource = id => request.post(`/biz/integrations/sources/${id}/sync`)

export const pageExternal = params => request.get('/biz/integrations/external-courses', { params })
export const importToCourse = data => request.post('/biz/integrations/external-courses/import-to-course', data)
export const ignoreExternal = ids => request.post('/biz/integrations/external-courses/ignore', { ids })

export const pageSyncLogs = params => request.get('/biz/integrations/logs', { params })
export const syncLogDetail = id => request.get(`/biz/integrations/logs/${id}`)
