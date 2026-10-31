/** 业务常量与字典映射 */

export const NEED_TYPE = {
  party: '党性教育',
  mgmt: '管理能力',
  skill: '岗位技能',
  safety: '安全合规',
  general: '通用素质'
}

export const COURSE_CATEGORY = NEED_TYPE

export const COURSE_TYPE = {
  internal: '内训',
  external: '外训',
  online: '线上'
}

export const COURSE_STATUS = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '已发布', type: 'success' },
  3: { label: '已下架', type: 'danger' }
}

export const PLAN_TYPE = {
  year: '年度计划',
  quarter: '季度计划',
  month: '月度计划',
  special: '专项计划'
}

export const PLAN_STATUS = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审批', type: 'warning' },
  2: { label: '已批准', type: 'success' },
  3: { label: '已驳回', type: 'danger' }
}

export const NEED_STATUS = {
  0: { label: '草稿', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '已采纳', type: 'success' },
  3: { label: '已驳回', type: 'danger' }
}

export const COLLECTION_STATUS = {
  0: { label: '进行中', type: 'success' },
  1: { label: '已结束', type: 'info' }
}

export const CLASS_STATUS = {
  0: { label: '筹备中', type: 'info' },
  1: { label: '报名中', type: 'warning' },
  2: { label: '进行中', type: 'primary' },
  3: { label: '已结束', type: 'success' },
  4: { label: '已取消', type: 'danger' }
}

export const CLASS_TYPE = COURSE_TYPE

export const STUDENT_STATUS = {
  0: { label: '已报名', type: 'info' },
  2: { label: '已完成', type: 'success' },
  3: { label: '缺勤', type: 'danger' }
}

export const SIGN_TYPE = {
  normal: { label: '正常', type: 'success' },
  late: { label: '迟到', type: 'warning' },
  leave: { label: '请假', type: 'info' },
  absent: { label: '缺勤', type: 'danger' }
}

export const HOUR_SOURCE = {
  1: '签到自动',
  2: '手工补录',
  3: '外部认定',
  4: '线上学习'
}

export const HOUR_STATUS = {
  0: { label: '待认定', type: 'warning' },
  1: { label: '已认定', type: 'success' },
  2: { label: '已驳回', type: 'danger' }
}

export const USER_TYPE = {
  manager: '管理干部',
  professional: '专业技术人员',
  staff: '一般员工'
}

export const EVAL_LEVEL = {
  1: '一级评估(反应层)',
  2: '二级评估(学习层)',
  3: '三级评估(行为层)',
  4: '四级评估(结果层)'
}

export const TRAINER_TYPE = {
  internal: '内部讲师',
  external: '外部专家'
}

export const SCHEDULE_STATUS = {
  0: { label: '待上课', type: 'info' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'danger' }
}

/** 年份选项: 近5年 + 明年 */
export function yearOptions() {
  const now = new Date().getFullYear()
  const list = []
  for (let y = now + 1; y >= now - 4; y--) {
    list.push(y)
  }
  return list
}
