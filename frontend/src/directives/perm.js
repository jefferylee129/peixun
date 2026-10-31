import { useUserStore } from '@/store/user'

/**
 * v-perm="'biz:need:audit'" — 无权限时移除元素
 */
export default {
  mounted(el, binding) {
    const store = useUserStore()
    const code = binding.value
    if (code && !store.hasPerm(code)) {
      if (el.parentNode) {
        el.parentNode.removeChild(el)
      }
    }
  }
}
