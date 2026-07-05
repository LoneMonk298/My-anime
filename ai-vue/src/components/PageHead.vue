<template>
  <div class="page-head">
    <p class="page-title">{{ pageTitle }}</p>
    <div class="action-btn">
      <slot name="buttons"></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  pageTitle: {
    type: String,
    default: '页面标题',
  },
})
</script>

<style lang="scss" scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .page-title {
    color: #1f2937;
    font-size: 24px;
    font-weight: 850;
  }
}
</style>
