<template>
  <div class="page-head">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1 class="page-title">{{ pageTitle }}</h1>
      <span v-if="description" class="page-description">{{ description }}</span>
    </div>
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
  eyebrow: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
})
</script>

<style lang="scss" scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;

  .eyebrow {
    margin-bottom: 8px;
    color: #169b86;
    font-size: 12px;
    font-weight: 900;
    text-transform: uppercase;
  }

  .page-title {
    margin: 0;
    color: #1f2937;
    font-size: 24px;
    font-weight: 850;
  }

  .page-description {
    display: block;
    margin-top: 8px;
    color: #64748b;
    line-height: 1.6;
  }

  .action-btn {
    flex-shrink: 0;
  }
}
</style>
