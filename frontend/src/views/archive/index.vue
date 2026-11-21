<template>
  <div class="page">
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="选择员工">
          <el-select
            v-model="userId"
            filterable
            remote
            clearable
            :remote-method="searchUsers"
            :loading="searching"
            placeholder="输入姓名/工号检索"
            style="width: 260px"
            @change="onPick"
          >
            <el-option v-for="u in users" :key="u.id" :label="`${u.nickname}(${u.employeeNo || u.username})`" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>
    <ArchiveView v-if="userId" :key="userId" :user-id="userId" />
    <el-card v-else shadow="never">
      <el-empty description="请选择员工查看培训档案" />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ArchiveView from '@/components/ArchiveView.vue'
import { simpleUsers } from '@/api/system'

const userId = ref(null)
const users = ref([])
const searching = ref(false)

async function searchUsers(kw) {
  if (!kw || !kw.trim()) {
    users.value = []
    return
  }
  searching.value = true
  try {
    const res = await simpleUsers({ keyword: kw.trim() })
    users.value = res.data || []
  } finally {
    searching.value = false
  }
}

function onPick() {
  /* 选择后由 :key 触发 ArchiveView 重新加载 */
}
</script>
