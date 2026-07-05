<template>
  <el-aside :width="isCollapsed ? '64px' : '264px'">
    <el-menu
      :default-active="activePath"
      :collapse="isCollapsed"
      :collapse-transition="false"
      class="menu-style"
    >
      <div class="brand" @click="router.push('/')">
        <div class="brand-mark">ZG</div>
        <div v-show="!isCollapsed" class="info-card">
          <h1 class="brand-title">二次元记录站</h1>
          <p class="brand-subtitle">内容管理后台</p>
        </div>
      </div>

      <el-menu-item
        v-for="item in adminMenus"
        :key="item.path"
        :index="`/user/${item.path}`"
        @click="router.push(`/user/${item.path}`)"
      >
        <el-icon><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const router = useRouter()
const route = useRoute()
const isCollapsed = computed(() => useAdminStore().isAdmin)
const activePath = computed(() => route.path)
const adminMenus = computed(() => router.options.routes.find((item) => item.path === '/user')?.children || [])
</script>

<style lang="scss" scoped>
.menu-style {
  height: 100%;
  border-right: 1px solid #1f2937;
  background: #0f172a;

  :deep(.el-menu-item) {
    color: #cbd5e1;

    &:hover,
    &.is-active {
      background: rgba(100, 255, 218, 0.12);
      color: #64ffda;
    }
  }

  .brand {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    min-height: 74px;
    padding: 10px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    cursor: pointer;

    .brand-mark {
      display: grid;
      width: 44px;
      height: 44px;
      place-items: center;
      border-radius: 8px;
      background: linear-gradient(135deg, #64ffda, #3b82f6);
      color: #07111f;
      font-weight: 950;
    }

    .brand-title {
      margin-bottom: 5px;
      color: #fff;
      font-size: 20px;
      font-weight: 850;
    }

    .brand-subtitle {
      color: #94a3b8;
      font-size: 13px;
    }
  }
}
</style>
