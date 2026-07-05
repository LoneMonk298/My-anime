<template>
  <div class="login-wrapper">
    <button class="back-home" type="button" @click="router.push('/')">返回前台</button>

    <section class="login-card">
      <div class="visual-panel">
        <span class="brand-mark">ZG</span>
        <h1>二次元记录站</h1>
        <p>登录后台后可以新增文章、上传封面、编辑记录并控制发布状态。</p>
      </div>

      <div class="form-panel">
        <span class="eyebrow">Admin console</span>
        <h2>后台登录</h2>
        <el-form ref="loginFormRef" :model="loginData" :rules="loginRules" label-position="top" @submit.prevent>
          <el-form-item label="账号" prop="username">
            <el-input v-model="loginData.username" size="large" placeholder="请输入管理员账号" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginData.password" size="large" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-form-item label="验证码" prop="captcha">
            <div class="captcha-row">
              <el-input v-model="loginData.captcha" size="large" placeholder="请输入验证码" @keyup.enter="handleLogin" />
              <button type="button" class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</button>
            </div>
          </el-form-item>
          <button type="button" class="submit-btn" :disabled="loginLoading" @click="handleLogin">
            {{ loginLoading ? '登录中...' : '登录后台' }}
          </button>
        </el-form>

        <button type="button" class="forgot-link" @click="forgotDialogVisible = true">忘记密码？邮箱重置</button>
      </div>
    </section>

    <el-dialog v-model="forgotDialogVisible" title="邮箱重置密码" width="460px" destroy-on-close>
      <el-form :model="forgotForm" label-width="86px">
        <el-form-item label="邮箱">
          <el-input v-model="forgotForm.email" placeholder="请输入绑定邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="reset-code-row">
            <el-input v-model="forgotForm.code" placeholder="请输入邮箱验证码" />
            <el-button
              type="button"
              :loading="sendingCode"
              :disabled="resetCodeCountdown > 0"
              @click.prevent="handleSendResetCode"
            >
              {{ resetCodeCountdown > 0 ? `${resetCodeCountdown}s 后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="forgotForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, resetPasswordByEmail, sendResetPasswordCode } from '@/api/admin'
import { normalizeUserInfo } from '@/utils/userAvatar'

const router = useRouter()
const loginFormRef = ref()
const loginLoading = ref(false)
const captchaCode = ref('')
const forgotDialogVisible = ref(false)
const sendingCode = ref(false)
const resettingPassword = ref(false)
const resetCodeCountdown = ref(0)
let resetCodeTimer = null

const RESET_CODE_COOLDOWN_KEY = 'passwordResetCodeCooldownUntil'

const loginData = reactive({
  username: '',
  password: '',
  captcha: '',
})

const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const refreshCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTWXY23456789'
  captchaCode.value = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

const startResetCodeCountdown = () => {
  const cooldownUntil = Date.now() + 300 * 1000
  localStorage.setItem(RESET_CODE_COOLDOWN_KEY, String(cooldownUntil))
  resetCodeCountdown.value = 300
  if (resetCodeTimer) window.clearInterval(resetCodeTimer)

  resetCodeTimer = window.setInterval(() => {
    const savedUntil = Number(localStorage.getItem(RESET_CODE_COOLDOWN_KEY) || 0)
    resetCodeCountdown.value = Math.max(0, Math.ceil((savedUntil - Date.now()) / 1000))
    if (resetCodeCountdown.value <= 0) {
      localStorage.removeItem(RESET_CODE_COOLDOWN_KEY)
      window.clearInterval(resetCodeTimer)
      resetCodeTimer = null
    }
  }, 1000)
}

const restoreResetCodeCountdown = () => {
  const savedUntil = Number(localStorage.getItem(RESET_CODE_COOLDOWN_KEY) || 0)
  const remain = Math.max(0, Math.ceil((savedUntil - Date.now()) / 1000))
  if (remain > 0) {
    resetCodeCountdown.value = remain
    startResetCodeCountdown()
  }
}

const handleSendResetCode = async () => {
  if (resetCodeCountdown.value > 0) return

  const email = forgotForm.email.trim()
  if (!email) {
    ElMessage.warning('请先输入绑定邮箱')
    return
  }

  sendingCode.value = true
  try {
    await sendResetPasswordCode(email)
    ElMessage.success('验证码已发送，请查看邮箱')
    startResetCodeCountdown()
  } finally {
    sendingCode.value = false
  }
}

const handleResetPassword = async () => {
  if (!forgotForm.email || !forgotForm.code || !forgotForm.newPassword) {
    ElMessage.warning('请完整填写邮箱、验证码和新密码')
    return
  }

  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  resettingPassword.value = true
  try {
    await resetPasswordByEmail({
      email: forgotForm.email,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword,
    })
    ElMessage.success('密码已重置，请重新登录')
    forgotDialogVisible.value = false
    forgotForm.email = ''
    forgotForm.code = ''
    forgotForm.newPassword = ''
    forgotForm.confirmPassword = ''
  } finally {
    resettingPassword.value = false
  }
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (loginData.captcha.toUpperCase() !== captchaCode.value.toUpperCase()) {
    ElMessage.warning('验证码错误')
    refreshCaptcha()
    return
  }

  loginLoading.value = true
  try {
    const res = await login({
      username: loginData.username,
      password: loginData.password,
    })
    const data = res?.data || res || {}
    const token = data.token
    const userInfo = normalizeUserInfo(data.userInfo || data)

    if (!token) {
      ElMessage.error('登录失败，未获取到 token')
      return
    }

    localStorage.setItem('token', token)
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    router.replace(userInfo.roleType === 2 || userInfo.userType === 2 ? '/user/articles' : '/')
  } finally {
    loginLoading.value = false
  }
}

refreshCaptcha()
restoreResetCodeCountdown()

onBeforeUnmount(() => {
  if (resetCodeTimer) {
    window.clearInterval(resetCodeTimer)
  }
})
</script>

<style lang="scss" scoped>
.login-wrapper {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 22px;
  background:
    radial-gradient(circle at 18% 12%, rgba(100, 255, 218, 0.16), transparent 32%),
    radial-gradient(circle at 82% 18%, rgba(59, 130, 246, 0.16), transparent 30%),
    linear-gradient(135deg, #0d1117 0%, #112240 100%);
}

.back-home {
  position: fixed;
  top: 20px;
  left: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(10, 25, 47, 0.62);
  color: #e6f1ff;
  cursor: pointer;
  padding: 10px 14px;
}

.login-card {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(340px, 1fr);
  width: min(920px, 100%);
  min-height: 560px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.28);
}

.visual-panel {
  display: grid;
  align-content: center;
  padding: 48px;
  background:
    linear-gradient(180deg, rgba(10, 25, 47, 0.42), rgba(10, 25, 47, 0.92)),
    url('/anime-assets/frontnav.png') center / cover;
  color: #fff;

  .brand-mark {
    display: grid;
    width: 62px;
    height: 62px;
    place-items: center;
    border-radius: 12px;
    background: linear-gradient(135deg, #64ffda, #3b82f6);
    color: #07111f;
    font-size: 22px;
    font-weight: 950;
  }

  h1 {
    margin: 24px 0 16px;
    font-size: 42px;
    line-height: 1.12;
  }

  p {
    color: #d6e6f8;
    line-height: 1.8;
  }
}

.form-panel {
  display: grid;
  align-content: center;
  padding: 48px;

  .eyebrow {
    color: #169b86;
    font-size: 13px;
    font-weight: 850;
    text-transform: uppercase;
  }

  h2 {
    margin: 8px 0 28px;
    color: #111827;
    font-size: 34px;
  }
}

.captcha-row,
.reset-code-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.captcha-row :deep(.el-input),
.reset-code-row :deep(.el-input) {
  flex: 1;
}

.captcha-img {
  min-width: 86px;
  border: 0;
  border-radius: 8px;
  background: #e6fffa;
  color: #16806f;
  cursor: pointer;
  font-size: 18px;
  font-weight: 850;
  letter-spacing: 2px;
}

.submit-btn {
  width: 100%;
  min-height: 46px;
  margin-top: 8px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, #64ffda, #3b82f6);
  color: #07111f;
  cursor: pointer;
  font-size: 16px;
  font-weight: 900;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.submit-btn:hover {
  filter: saturate(1.06);
  transform: translateY(-2px);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.forgot-link {
  width: fit-content;
  margin-top: 18px;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font: inherit;
  font-weight: 760;
}

@media (max-width: 760px) {
  .login-card {
    grid-template-columns: 1fr;
  }

  .visual-panel {
    min-height: 220px;
  }
}
</style>
