import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Layout.vue'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台' } },
      { path: 'need-collection', component: () => import('@/views/need/collection.vue'), meta: { title: '需求征集' } },
      { path: 'need', component: () => import('@/views/need/index.vue'), meta: { title: '需求申报' } },
      { path: 'plan', component: () => import('@/views/plan/index.vue'), meta: { title: '培训计划' } },
      { path: 'course', component: () => import('@/views/course/index.vue'), meta: { title: '培训课程' } },
      { path: 'trainer', component: () => import('@/views/trainer/index.vue'), meta: { title: '培训师资' } },
      { path: 'classes', component: () => import('@/views/class/index.vue'), meta: { title: '培训班次' } },
      { path: 'classes/:id', component: () => import('@/views/class/detail.vue'), meta: { title: '班次详情' } },
      { path: 'hours', component: () => import('@/views/hours/index.vue'), meta: { title: '学时台账' } },
      { path: 'hours-stats', component: () => import('@/views/hours/stats.vue'), meta: { title: '达标统计' } },
      { path: 'eval-template', component: () => import('@/views/eval/template.vue'), meta: { title: '评估模板' } },
      { path: 'eval-record', component: () => import('@/views/eval/record.vue'), meta: { title: '评估汇总' } },
      { path: 'stats', component: () => import('@/views/stats/index.vue'), meta: { title: '统计分析' } },
      { path: 'system/user', component: () => import('@/views/system/user.vue'), meta: { title: '用户管理' } },
      { path: 'system/dept', component: () => import('@/views/system/dept.vue'), meta: { title: '部门管理' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', component: () => import('@/views/system/menu.vue'), meta: { title: '菜单管理' } },
      { path: 'system/config', component: () => import('@/views/system/config.vue'), meta: { title: '参数设置' } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { title: '操作日志' } },
      { path: 'my-class', component: () => import('@/views/mine/class.vue'), meta: { title: '我的班级' } },
      { path: 'my-hours', component: () => import('@/views/mine/hours.vue'), meta: { title: '我的学时' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async to => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    return true
  }
  if (!token) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  const userStore = useUserStore()
  if (!userStore.loaded) {
    try {
      await userStore.fetchMe()
    } catch (e) {
      return { path: '/login' }
    }
  }
  document.title = to.meta.title ? `${to.meta.title} - 国企培训管理系统` : '国企培训管理系统'
  return true
})

export default router
