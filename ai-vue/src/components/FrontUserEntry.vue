<template>
  <div class="front-user-entry">
    <button v-if="!isLoggedIn" class="login-link" type="button" @click="goLogin">登录</button>

    <el-dropdown v-else trigger="click" @command="handleCommand">
      <button class="user-chip" type="button">
        <el-avatar :size="24" :src="avatarUrl">{{ userInitial }}</el-avatar>
        <span>{{ displayName }}</span>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">个人中心</el-dropdown-item>
          <el-dropdown-item command="admin" v-if="isAdmin">管理后台</el-dropdown-item>
          <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <UserProfileDialog v-model="profileVisible" @updated="loadUser" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { logout } from '@/api/frontend'
import UserProfileDialog from '@/components/UserProfileDialog.vue'
import { isAdminRole } from '@/utils/authRole'
import { normalizeUserInfo, resolveUserAvatarUrl } from '@/utils/userAvatar'

const router = useRouter()
const route = useRoute()

const profileVisible = ref(false)
const userInfo = ref({})

const loadUser = () => {
  try {
    userInfo.value = normalizeUserInfo(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  } catch {
    userInfo.value = {}
  }
}

loadUser()
window.addEventListener('storage', loadUser)

onBeforeUnmount(() => {
  window.removeEventListener('storage', loadUser)
})

const isLoggedIn = computed(() => Boolean(localStorage.getItem('token')))
const isAdmin = computed(() => isAdminRole(userInfo.value))
const displayName = computed(() => userInfo.value.nickname || userInfo.value.username || '用户')
const avatarUrl = computed(() => resolveUserAvatarUrl(userInfo.value))
const userInitial = computed(() => String(displayName.value || 'U').slice(0, 1).toUpperCase())

const clearLoginState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('privacyAccepted')
  loadUser()
}

const goLogin = () => {
  router.push({ path: '/auth/login', query: { redirect: route.fullPath } })
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    profileVisible.value = true
    return
  }
  if (command === 'admin') {
    router.push('/user/dashboard')
    return
  }
  if (command === 'logout') {
    const confirmed = await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).catch(() => false)
    if (!confirmed) return
    await logout().catch(() => {})
    clearLoginState()
    router.push('/')
  }
}
</script>

<style scoped>
.front-user-entry {
  margin-left: auto;
  padding-right: var(--gutter, 40px);
}

.login-link,
.user-chip {
  display: inline-flex;
  height: 30px;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 4px;
  background: rgba(5, 20, 42, 0.42);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.login-link:hover,
.user-chip:hover {
  border-color: rgba(100, 255, 218, 0.7);
  background: rgba(13, 110, 253, 0.35);
}

@media (max-width: 640px) {
  .front-user-entry {
    padding-right: 10px;
  }

  .user-chip {
    width: 32px;
    justify-content: center;
    padding: 0;
  }

  .user-chip span {
    display: none;
  }

  .login-link {
    height: 28px;
    padding: 0 8px;
    font-size: 12px;
  }
}
</style>
