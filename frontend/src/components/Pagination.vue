<template>
  <div class="pager">
    <el-pagination
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      :current-page="page"
      :page-size="size"
      @update:current-page="v => emit('update:page', v)"
      @update:page-size="v => emit('update:size', v)"
      @change="emit('change')"
    />
  </div>
</template>

<script setup>
defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  total: { type: Number, default: 0 }
})

const emit = defineEmits(['update:page', 'update:size', 'change'])
</script>

<style scoped>
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
