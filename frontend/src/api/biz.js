import request from '@/utils/request'

// ---------- 培训需求 ----------
export const pageNeed = params => request.get('/biz/needs', { params })
export const myNeeds = params => request.get('/biz/needs/my', { params })
export const addNeed = data => request.post('/biz/needs', data)
export const updateNeed = (id, data) => request.put(`/biz/needs/${id}`, data)
export const deleteNeed = id => request.delete(`/biz/needs/${id}`)
export const submitNeed = id => request.put(`/biz/needs/${id}/submit`)
export const auditNeed = (id, data) => request.put(`/biz/needs/${id}/audit`, data)

// ---------- 需求征集 ----------
export const collections = params => request.get('/biz/needs/collections', { params })
export const activeCollections = () => request.get('/biz/needs/collections/active')
export const addCollection = data => request.post('/biz/needs/collections', data)
export const updateCollection = (id, data) => request.put(`/biz/needs/collections/${id}`, data)
export const closeCollection = id => request.put(`/biz/needs/collections/${id}/close`)
export const deleteCollection = id => request.delete(`/biz/needs/collections/${id}`)

// ---------- 培训计划 ----------
export const pagePlan = params => request.get('/biz/plans', { params })
export const planOptions = params => request.get('/biz/plans/options', { params })
export const addPlan = data => request.post('/biz/plans', data)
export const updatePlan = (id, data) => request.put(`/biz/plans/${id}`, data)
export const deletePlan = id => request.delete(`/biz/plans/${id}`)
export const submitPlan = id => request.put(`/biz/plans/${id}/submit`)
export const approvePlan = (id, data) => request.put(`/biz/plans/${id}/approve`, data)

// ---------- 培训课程 ----------
export const pageCourse = params => request.get('/biz/courses', { params })
export const publishedCourses = () => request.get('/biz/courses/published')
export const addCourse = data => request.post('/biz/courses', data)
export const updateCourse = (id, data) => request.put(`/biz/courses/${id}`, data)
export const deleteCourse = id => request.delete(`/biz/courses/${id}`)
export const submitCourse = id => request.put(`/biz/courses/${id}/submit`)
export const auditCourse = (id, data) => request.put(`/biz/courses/${id}/audit`, data)
export const offlineCourse = id => request.put(`/biz/courses/${id}/offline`)

// ---------- 培训师资 ----------
export const pageTrainer = params => request.get('/biz/trainers', { params })
export const allTrainers = () => request.get('/biz/trainers/all')
export const trainerTeaching = id => request.get(`/biz/trainers/${id}/teaching`)
export const addTrainer = data => request.post('/biz/trainers', data)
export const updateTrainer = (id, data) => request.put(`/biz/trainers/${id}`, data)
export const deleteTrainer = id => request.delete(`/biz/trainers/${id}`)
