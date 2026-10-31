import request from '@/utils/request'

// ---------- 培训班次 ----------
export const pageClass = params => request.get('/biz/classes', { params })
export const myClasses = params => request.get('/biz/classes/my', { params })
export const enrollableClasses = params => request.get('/biz/classes/enrollable', { params })
export const classDetail = id => request.get(`/biz/classes/${id}/detail`)
export const addClass = data => request.post('/biz/classes', data)
export const updateClass = (id, data) => request.put(`/biz/classes/${id}`, data)
export const deleteClass = id => request.delete(`/biz/classes/${id}`)
export const changeClassStatus = (id, status) =>
  request.put(`/biz/classes/${id}/status`, null, { params: { status } })
export const classStudents = id => request.get(`/biz/classes/${id}/students`)
export const addClassStudents = (id, userIds) =>
  request.post(`/biz/classes/${id}/students`, { userIds })
export const removeClassStudent = (id, userId) =>
  request.delete(`/biz/classes/${id}/students/${userId}`)
export const enrollClass = id => request.post(`/biz/classes/${id}/enroll`)

// ---------- 课表与签到 ----------
export const classSchedules = id => request.get(`/biz/classes/${id}/schedules`)
export const addSchedule = (id, data) => request.post(`/biz/classes/${id}/schedules`, data)
export const updateSchedule = (sid, data) => request.put(`/biz/classes/schedules/${sid}`, data)
export const deleteSchedule = sid => request.delete(`/biz/classes/schedules/${sid}`)
export const finishSchedule = sid => request.put(`/biz/classes/schedules/${sid}/finish`)
export const scheduleAttendance = sid => request.get(`/biz/classes/schedules/${sid}/attendance`)
export const signAttendance = (sid, data) =>
  request.post(`/biz/classes/schedules/${sid}/attendance`, data)

// ---------- 学时 ----------
export const pageHours = params => request.get('/biz/hours', { params })
export const myHours = params => request.get('/biz/hours/my', { params })
export const myHourSummary = params => request.get('/biz/hours/my/summary', { params })
export const addHour = data => request.post('/biz/hours', data)
export const auditHour = (id, data) => request.put(`/biz/hours/${id}/audit`, data)
export const deleteHour = id => request.delete(`/biz/hours/${id}`)
export const hourUserStats = params => request.get('/biz/hours/stats/user', { params })
export const hourDeptStats = params => request.get('/biz/hours/stats/dept', { params })

// ---------- 评估 ----------
export const pageEvalTemplate = params => request.get('/biz/evals/templates', { params })
export const evalTemplateDetail = id => request.get(`/biz/evals/templates/${id}`)
export const evalTemplateFillView = id => request.get(`/biz/evals/templates/${id}/fill-view`)
export const addEvalTemplate = data => request.post('/biz/evals/templates', data)
export const updateEvalTemplate = (id, data) => request.put(`/biz/evals/templates/${id}`, data)
export const deleteEvalTemplate = id => request.delete(`/biz/evals/templates/${id}`)
export const fillEval = data => request.post('/biz/evals/records', data)
export const evalRecords = params => request.get('/biz/evals/records', { params })
export const myEvalRecord = params => request.get('/biz/evals/records/my', { params })
export const evalSummary = params => request.get('/biz/evals/summary', { params })
export const evalPending = () => request.get('/biz/evals/pending')

// ---------- 统计 ----------
export const statsOverview = params => request.get('/biz/stats/overview', { params })
export const statsMonthHours = params => request.get('/biz/stats/month-hours', { params })
export const statsDeptHours = params => request.get('/biz/stats/dept-hours', { params })
export const statsCourseCategory = () => request.get('/biz/stats/course-category')
export const statsClassStatus = () => request.get('/biz/stats/class-status')
export const statsPlanStatus = params => request.get('/biz/stats/plan-status', { params })
export const statsNeedType = params => request.get('/biz/stats/need-type', { params })
