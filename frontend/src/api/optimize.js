import request from '@/utils/request'

// ---------- 一人一档培训档案 ----------
export const myArchive = params => request.get('/biz/archive/my', { params })
export const userArchive = (userId, params) => request.get(`/biz/archive/user/${userId}`, { params })
export const myWarning = params => request.get('/biz/archive/my-warning', { params })
export const warnings = params => request.get('/biz/archive/warnings', { params })
export const pushWarnings = params => request.post('/biz/archive/warnings/push', null, { params })

// ---------- 消息中心 ----------
export const myMessages = params => request.get('/biz/messages/my', { params })
export const unreadCount = () => request.get('/biz/messages/unread-count')
export const readMessage = id => request.put(`/biz/messages/${id}/read`)
export const readAllMessages = () => request.put('/biz/messages/read-all')
export const announce = data => request.post('/biz/messages/announce', data)

// ---------- 结业证书 ----------
export const pageCertificates = params => request.get('/biz/certificates', { params })
export const myCertificates = () => request.get('/biz/certificates/my')
export const certificateDetail = id => request.get(`/biz/certificates/${id}`)
export const issueCertificates = classId => request.post(`/biz/certificates/class/${classId}/issue`)

// ---------- 在线考试(题库/考试/作答) ----------
export const pageQuestions = params => request.get('/biz/questions', { params })
export const addQuestion = data => request.post('/biz/questions', data)
export const updateQuestion = (id, data) => request.put(`/biz/questions/${id}`, data)
export const deleteQuestion = id => request.delete(`/biz/questions/${id}`)

export const pageExams = params => request.get('/biz/exams', { params })
export const addExam = data => request.post('/biz/exams', data)
export const updateExam = (id, data) => request.put(`/biz/exams/${id}`, data)
export const deleteExam = id => request.delete(`/biz/exams/${id}`)
export const publishExam = id => request.put(`/biz/exams/${id}/publish`)
export const closeExam = id => request.put(`/biz/exams/${id}/close`)
export const examRecords = id => request.get(`/biz/exams/${id}/records`)
export const myExams = () => request.get('/biz/exams/my')
export const examPaper = id => request.get(`/biz/exams/${id}/paper`)
export const submitExam = (id, answers) => request.post(`/biz/exams/${id}/submit`, { answers })

// ---------- v1.5 岗位学习地图 ----------
export const mapMatrix = params => request.get('/biz/learning-map/matrix', { params })
export const saveMap = data => request.post('/biz/learning-map/save', data)
export const myMap = params => request.get('/biz/learning-map/my', { params })
export const mapCompletion = params => request.get('/biz/learning-map/completion', { params })

// ---------- v1.5 自助签到码 ----------
export const selfSignByClass = (classId, code) => request.put(`/biz/classes/${classId}/self-sign`, { code })
export const refreshSignCode = sid => request.put(`/biz/classes/schedules/${sid}/refresh-code`)

// ---------- v1.5 自动提醒 / 经费 ----------
export const runNotify = () => request.post('/biz/notify/run')
export const statsBudget = params => request.get('/biz/stats/budget', { params })

// ---------- v1.5 证书防伪公开查验(无需登录) ----------
export const verifyCertificate = params => request.get('/public/certificates/verify', { params })
