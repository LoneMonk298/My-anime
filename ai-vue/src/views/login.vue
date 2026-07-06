<template>
  <div class="login-wrapper">
    <video
      class="login-bg-video"
      autoplay
      muted
      loop
      playsinline
      preload="metadata"
      poster="/anime-assets/background.png"
      aria-hidden="true"
    >
      <source src="/anime-assets/skystar.mp4" type="video/mp4" />
    </video>
    <div class="login-bg-mask" aria-hidden="true"></div>

    <div class="login-page">
      <div class="container" :class="{ active: isRegister }">
        <section class="welcome-box">
          <span class="brand-mark">ZG</span>
          <h2>{{ isRegister ? 'Hello Friend!' : 'Welcome!' }}</h2>
          <p>
            {{
              isRegister
                ? '已有账号？切回登录后即可进入你的记录空间。'
                : '登录后台管理文章、封面、分类与发布状态；普通账号用于 AI 角色对话和资源下载。'
            }}
          </p>
          <button type="button" @click="toggleMode">
            {{ isRegister ? '去登录' : '注册账号' }}
          </button>
        </section>

        <section class="form-box">
          <button class="close-btn" type="button" aria-label="返回前台" @click="goHome">×</button>

          <Transition name="fade" mode="out-in">
            <div v-if="!isRegister" key="login" class="form-content">
              <p class="eyebrow">Anime Record Console</p>
              <h1>登录账号</h1>
              <p v-if="authError" class="auth-error">{{ authError }}</p>
              <el-form
                ref="loginFormRef"
                :model="loginData"
                :rules="loginRules"
                label-position="top"
                @submit.prevent="handleLogin"
              >
                <el-form-item prop="username">
                  <el-input v-model="loginData.username" size="large" placeholder="Account" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="loginData.password"
                    size="large"
                    type="password"
                    show-password
                    placeholder="Password"
                  />
                </el-form-item>
                <div class="forgot-row">
                  <button type="button" @click="forgotDialogVisible = true">忘记密码？邮箱重置</button>
                </div>
                <el-form-item prop="captcha">
                  <div class="captcha-group">
                    <el-input
                      v-model="loginData.captcha"
                      size="large"
                      placeholder="Verification Code"
                      @keyup.enter="handleLogin"
                    />
                    <button type="button" class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <button type="submit" class="submit-btn" :disabled="loginLoading">
                    {{ loginLoading ? '登录中...' : 'Login' }}
                  </button>
                </el-form-item>
              </el-form>
            </div>

            <div v-else key="register" class="form-content">
              <p class="eyebrow">Create Account</p>
              <h1>注册账号</h1>
              <el-form
                ref="registerFormRef"
                :model="registerData"
                :rules="registerRules"
                label-position="top"
                @submit.prevent="handleRegister"
              >
                <el-form-item prop="username">
                  <el-input v-model="registerData.username" size="large" placeholder="用户名" />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input v-model="registerData.email" size="large" placeholder="邮箱" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input
                    v-model="registerData.password"
                    size="large"
                    type="password"
                    show-password
                    placeholder="密码"
                  />
                </el-form-item>
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="registerData.confirmPassword"
                    size="large"
                    type="password"
                    show-password
                    placeholder="确认密码"
                  />
                </el-form-item>
                <el-form-item prop="captcha">
                  <div class="captcha-group">
                    <el-input v-model="registerData.captcha" size="large" placeholder="验证码" />
                    <button type="button" class="captcha-img" @click="refreshCaptcha">{{ captchaCode }}</button>
                  </div>
                </el-form-item>
                <el-form-item>
                  <button type="submit" class="submit-btn" :disabled="registerLoading">
                    {{ registerLoading ? '注册中...' : 'Register' }}
                  </button>
                </el-form-item>
              </el-form>
            </div>
          </Transition>
        </section>
      </div>
    </div>

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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, resetPasswordByEmail, sendResetPasswordCode } from '@/api/admin'
import { canUseLoginRedirect, resolveHomePath } from '@/utils/authRole'
import { normalizeUserInfo } from '@/utils/userAvatar'

const router = useRouter()
const route = useRoute()

const isRegister = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()
const loginLoading = ref(false)
const registerLoading = ref(false)
const authError = ref('')
const forgotDialogVisible = ref(false)
const sendingCode = ref(false)
const resettingPassword = ref(false)
const resetCodeCountdown = ref(0)
const captchaCode = ref('')
let resetCodeTimer = null

const RESET_CODE_COOLDOWN_KEY = 'passwordResetCodeCooldownUntil'

const loginData = reactive({
  username: '',
  password: '',
  captcha: '',
})

const registerData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: '',
})

const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerData.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const refreshCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTWXY23456789'
  captchaCode.value = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

const toggleMode = () => {
  isRegister.value = !isRegister.value
  authError.value = ''
  refreshCaptcha()
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

const checkCaptcha = (value) => {
  if (value.toUpperCase() === captchaCode.value.toUpperCase()) {
    return true
  }
  ElMessage.warning('验证码错误')
  refreshCaptcha()
  return false
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
  authError.value = ''
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (loginData.captcha.toUpperCase() !== captchaCode.value.toUpperCase()) {
    refreshCaptcha()
    authError.value = '验证码错误，请重新输入'
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
    const redirect = canUseLoginRedirect(route.query.redirect, userInfo)
      ? route.query.redirect
      : resolveHomePath(userInfo)
    router.replace(redirect)
  } catch (error) {
    authError.value = error?.message || '登录失败，请检查用户名或密码'
    refreshCaptcha()
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid || !checkCaptcha(registerData.captcha)) return

  registerLoading.value = true
  try {
    await register({
      username: registerData.username,
      email: registerData.email,
      password: registerData.password,
    })
    ElMessage.success('注册成功，请登录')
    isRegister.value = false
    loginData.username = registerData.username
    registerData.username = ''
    registerData.email = ''
    registerData.password = ''
    registerData.confirmPassword = ''
    registerData.captcha = ''
    refreshCaptcha()
  } finally {
    registerLoading.value = false
  }
}

const goHome = () => {
  router.push('/')
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
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  overflow: auto;
  overflow-x: hidden;
  background:
    linear-gradient(200deg, rgba(243, 231, 233, 0.25), rgba(227, 238, 255, 0.18)),
    radial-gradient(circle at 22% 18%, rgba(100, 255, 218, 0.2), transparent 28%),
    radial-gradient(circle at 78% 80%, rgba(248, 206, 236, 0.2), transparent 30%),
    url('/anime-assets/frontnav.png') center / cover no-repeat;
}

.login-wrapper,
.login-wrapper *,
.login-wrapper *::before,
.login-wrapper *::after {
  box-sizing: border-box;
}

.login-bg-video,
.login-bg-mask {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.login-bg-video {
  z-index: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(0.72) saturate(0.95);
  transform: scale(1.02);
}

.login-bg-mask {
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(6, 16, 31, 0.28), rgba(6, 16, 31, 0.44)),
    radial-gradient(circle at 20% 18%, rgba(100, 255, 218, 0.18), transparent 30%),
    radial-gradient(circle at 78% 82%, rgba(248, 206, 236, 0.22), transparent 34%);
  backdrop-filter: blur(1px);
}

.login-page {
  position: relative;
  z-index: 2;
  width: min(900px, 100%);
  height: min(580px, calc(100vh - 40px));
  min-height: 560px;
  box-sizing: border-box;
  padding: 4px;
  overflow: hidden;
  border-radius: 24px;
  background: rgba(6, 16, 31, 0.52);
  box-shadow:
    0 18px 70px rgba(6, 16, 31, 0.34),
    0 0 28px rgba(100, 255, 218, 0.18),
    0 0 42px rgba(59, 130, 246, 0.14);
  backdrop-filter: blur(15px);
  isolation: isolate;
}

.login-page::before {
  position: absolute;
  inset: -55%;
  z-index: 0;
  display: block;
  background-image: conic-gradient(
    transparent,
    rgba(100, 255, 218, 0.95),
    rgba(59, 130, 246, 0.95),
    rgba(248, 206, 236, 0.9),
    transparent 40%
  );
  content: '';
  animation: rotate 5s linear infinite;
}

.container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
}

.welcome-box,
.form-box {
  width: 50%;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.645, 0.045, 0.355, 1);
}

.welcome-box {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  overflow: hidden;
  color: #fff;
  text-align: center;
  background:
    linear-gradient(180deg, rgba(82, 69, 156, 0.45), rgba(20, 29, 62, 0.86)),
    url('/anime-assets/cover-chunibyo.jpg') center / cover no-repeat;
}

.welcome-box::before {
  position: absolute;
  inset: 0;
  content: '';
  background: rgba(100, 80, 174, 0.46);
}

.brand-mark,
.welcome-box h2,
.welcome-box p,
.welcome-box button {
  position: relative;
  z-index: 1;
}

.brand-mark {
  display: grid;
  width: 58px;
  height: 58px;
  margin-bottom: 18px;
  place-items: center;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 22px;
  font-weight: 950;
  letter-spacing: 1px;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.34);
}

.welcome-box h2 {
  margin: 0 0 1.4rem;
  font-size: 2.45rem;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(255, 180, 255, 0.62), 0 0 20px rgba(255, 120, 255, 0.38);
  animation: textBounce 2s ease-in-out infinite;
}

.welcome-box p {
  max-width: 310px;
  margin: 0 0 2rem;
  font-size: 1rem;
  line-height: 1.75;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
  animation: textBounce 2s ease-in-out infinite 0.3s;
}

.welcome-box button {
  min-width: 128px;
  padding: 12px 34px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 800;
  transition: 0.3s;
  animation: textBounce 2s ease-in-out infinite 0.6s;
}

.welcome-box button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.form-box {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: #f0f4ff;
}

.form-content {
  width: 100%;
  max-width: 340px;
  min-width: 0;
}

.eyebrow {
  margin: 0 0 8px;
  color: #8f6bd8;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.form-box h1 {
  margin: 0 0 2rem;
  color: #332a2a;
  font-size: 2rem;
  letter-spacing: 2px;
}

.auth-error {
  margin: -1rem 0 1rem;
  padding: 10px 12px;
  border: 1px solid rgba(239, 68, 68, 0.25);
  border-radius: 12px;
  background: rgba(254, 226, 226, 0.78);
  color: #b91c1c;
  font-size: 13px;
  font-weight: 800;
  line-height: 1.45;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:deep(.el-form-item) {
  margin-bottom: 1.15rem;
}

:deep(.el-form),
:deep(.el-form-item__content) {
  width: 100%;
  min-width: 0;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  padding: 4px 15px;
}

:deep(.el-input__inner) {
  height: 44px;
  font-size: 0.95rem;
}

.captcha-group,
.reset-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  width: 100%;
  align-items: center;
  gap: 10px;
}

.captcha-group :deep(.el-input),
.reset-code-row :deep(.el-input) {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  min-width: 78px;
  padding: 10px 12px;
  border: 0;
  border-radius: 10px;
  background: #f5e6e8;
  color: #9b59b6;
  cursor: pointer;
  font-size: 1.1rem;
  font-weight: 850;
  letter-spacing: 2px;
  text-align: center;
  transition: 0.3s;
  user-select: none;
}

.captcha-img:hover {
  background: #f0d4d8;
  transform: scale(1.05);
}

.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin: -0.4rem 0 0.75rem;
}

.forgot-row button {
  border: 0;
  background: transparent;
  color: #8f6bd8;
  cursor: pointer;
  font: inherit;
  font-size: 0.9rem;
  font-weight: 700;
}

.forgot-row button:hover {
  color: #6f49c7;
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  min-height: 50px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(45deg, #a88beb, #f8ceec);
  box-shadow: 0 4px 12px rgba(168, 139, 235, 0.3);
  color: #fff;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 900;
  letter-spacing: 1px;
  transition: 0.3s;
}

.submit-btn:hover:not(:disabled) {
  background: linear-gradient(45deg, #a888f1, #f5b8e8);
  box-shadow: 0 6px 20px rgba(168, 139, 235, 0.4);
  transform: translateY(-4px);
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.close-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  z-index: 2;
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  background: #c9cfdd;
  color: #fff;
  cursor: pointer;
  font-size: 1.35rem;
  line-height: 1;
  transition: 0.3s;
}

.close-btn:hover {
  background: #99a1b3;
  transform: rotate(90deg);
}

.container.active .welcome-box {
  transform: translateX(100%);
}

.container.active .form-box {
  transform: translateX(-100%);
}

@keyframes textBounce {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}

@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .login-wrapper {
    align-items: flex-start;
    padding: 16px;
  }

  .login-page {
    width: min(390px, 100%);
    height: auto;
    min-height: 0;
  }

  .container,
  .container.active {
    display: block;
  }

  .welcome-box,
  .form-box,
  .container.active .welcome-box,
  .container.active .form-box {
    width: 100%;
    transform: none;
  }

  .welcome-box {
    min-height: 230px;
    padding: 34px 24px;
  }

  .form-box {
    min-height: 0;
    padding: 40px 24px 34px;
  }

  .form-content {
    max-width: 100%;
  }

  .form-box h1 {
    margin-bottom: 1.5rem;
    font-size: 1.85rem;
  }

  .forgot-row {
    justify-content: flex-start;
    margin: -0.3rem 0 0.7rem;
  }

  .captcha-group,
  .reset-code-row {
    grid-template-columns: minmax(0, 1fr) minmax(72px, auto);
    gap: 8px;
  }

  .captcha-img {
    min-width: 72px;
    padding: 10px 8px;
    font-size: 1rem;
    letter-spacing: 1px;
  }

  .close-btn {
    top: 12px;
    right: 12px;
  }
}

@media (max-width: 420px) {
  .login-wrapper {
    padding: 10px;
  }

  .login-page {
    border-radius: 18px;
    padding: 3px;
  }

  .container {
    border-radius: 15px;
  }

  .welcome-box {
    min-height: 210px;
    padding: 30px 22px;
  }

  .welcome-box h2 {
    margin-bottom: 1rem;
    font-size: 2.05rem;
  }

  .welcome-box p {
    margin-bottom: 1.35rem;
    font-size: 0.94rem;
    line-height: 1.65;
  }

  .form-box {
    padding: 34px 22px 30px;
  }

  :deep(.el-form-item) {
    margin-bottom: 1rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-bg-video {
    display: none;
  }

  .login-page::before,
  .welcome-box h2,
  .welcome-box p,
  .welcome-box button {
    animation: none;
  }
}
</style>
