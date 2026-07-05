<template>
  <div class="navbar">
    <div class="left-area">
      <el-button circle @click="handleExpandClick">
        <el-icon><Expand /></el-icon>
      </el-button>
      <p class="page-title">{{ route.meta.title || '后台管理' }}</p>
    </div>

    <div class="right-area">
      <el-button text @click="router.push('/')">返回前台</el-button>
      <el-dropdown @command="handleCommand">
        <div class="admin-entry">
          <el-avatar>ZG</el-avatar>
          <p class="user-name">{{ adminName }}</p>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item command="createAdmin">新增管理员</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="460px" destroy-on-close>
      <el-form :model="passwordForm" label-width="90px">
        <el-form-item label="原密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPassword" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminDialogVisible" title="新增管理员" width="520px" destroy-on-close>
      <el-alert
        class="admin-tip"
        title="该操作仅限超级管理员执行，后端会校验当前账号权限。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form :model="adminForm" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="adminForm.username" placeholder="请输入管理员用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="adminForm.email" placeholder="请输入邮箱，可选" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="adminForm.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="adminForm.confirmPassword" type="password" show-password placeholder="请再次输入初始密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAdmin" @click="submitAdmin">新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Expand } from '@element-plus/icons-vue'
import { createAdmin, logout, updateAdminPassword } from '@/api/admin'

const router = useRouter()
const route = useRoute()

const passwordDialogVisible = ref(false)
const adminDialogVisible = ref(false)
const savingPassword = ref(false)
const savingAdmin = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const adminForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const adminName = computed(() => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return userInfo.username || userInfo.nickname || 'admin'
  } catch {
    return 'admin'
  }
})

const clearLoginState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('privacyAccepted')
}

const handleCommand = (command) => {
  if (command === 'password') {
    passwordDialogVisible.value = true
    return
  }

  if (command === 'createAdmin') {
    adminDialogVisible.value = true
    return
  }

  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
      .then(() => {
        logout()
          .catch(() => {})
          .finally(() => {
            clearLoginState()
            router.replace('/auth/login')
          })
      })
      .catch(() => {})
  }
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写原密码和新密码')
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  savingPassword.value = true
  try {
    await updateAdminPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    clearLoginState()
    router.replace('/auth/login')
  } finally {
    savingPassword.value = false
  }
}

const submitAdmin = async () => {
  if (!adminForm.username || !adminForm.password) {
    ElMessage.warning('请填写用户名和初始密码')
    return
  }

  if (adminForm.password !== adminForm.confirmPassword) {
    ElMessage.warning('两次输入的初始密码不一致')
    return
  }

  savingAdmin.value = true
  try {
    await createAdmin({
      username: adminForm.username,
      email: adminForm.email,
      password: adminForm.password,
    })
    ElMessage.success('管理员新增成功')
    adminDialogVisible.value = false
    adminForm.username = ''
    adminForm.email = ''
    adminForm.password = ''
    adminForm.confirmPassword = ''
  } finally {
    savingAdmin.value = false
  }
}

const handleExpandClick = () => {
  useAdminStore().toggleAdminStatus()
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 22px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;

  .left-area,
  .right-area,
  .admin-entry {
    display: flex;
    align-items: center;
  }

  .left-area {
    gap: 16px;
  }

  .right-area {
    gap: 14px;
  }

  .page-title {
    color: #111827;
    font-size: 24px;
    font-weight: 850;
  }

  .admin-entry {
    gap: 8px;
    padding: 6px 8px;
    border-radius: 999px;
    cursor: pointer;
    transition: background-color 0.2s ease;

    &:hover {
      background: #f5f7fb;
    }
  }

  .user-name {
    max-width: 120px;
    overflow: hidden;
    color: #1f2937;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.admin-tip {
  margin-bottom: 16px;
}
</style>
