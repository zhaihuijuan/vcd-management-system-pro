<template>
  <div class="login-page">
    <header class="top-nav">
      <span class="brand">VCD管理系统</span>
      <div class="nav-right">
      <nav class="nav-links">
        <a href="#" class="nav-item active">首页</a>
        <a href="#" class="nav-item">关于我们</a>
        <a href="#" class="nav-item">帮助中心</a>
        <a href="#" class="nav-item">联系我们</a>
      </nav>
      <el-tooltip :content="isDark ? '切换为日间模式' : '切换为夜间模式'" placement="bottom">
        <el-button circle class="login-theme-btn" @click="handleToggleTheme">
          <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
      </el-tooltip>
      </div>
    </header>

    <div class="hero">
      <div class="hero-bg" />
      <div class="hero-inner">
        <div class="hero-copy">
          <h1>探索影像世界</h1>
          <p>书与影像记录过去</p>
          <p>登录即是探索未来的开始</p>
          <div class="hero-line" />
        </div>

        <div class="login-card">
          <h2 class="card-title">欢迎回来</h2>
          <p class="card-sub">请登录您的账户</p>

          <div class="card-fields">
            <el-input
              v-model="form.username"
              class="field-input"
              placeholder="用户名"
              clearable
              @keyup.enter="login"
            />
            <el-input
              v-model="form.password"
              class="field-input"
              type="password"
              placeholder="密码"
              show-password
              @keyup.enter="login"
            />
          </div>

          <el-button class="btn-gold" :loading="loading" @click="login">登录</el-button>

          <div class="divider">
            <span>或</span>
          </div>

          <el-button class="btn-outline" @click="registerVisible = true">创建新账户</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="registerVisible" title="创建新账户" width="420px" destroy-on-close>
      <el-form :model="regForm" :rules="regRules" ref="regRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="regForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="regForm.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="regForm.confirm" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">取消</el-button>
        <el-button type="primary" :loading="regLoading" @click="submitRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Moon, Sunny } from '@element-plus/icons-vue'
import axios from 'axios'
import { ROLE, HARDCODED_ADMIN_USERNAME } from './utils/role'
import { getStoredTheme, toggleTheme as flipTheme } from './utils/theme'

const themeMode = ref(getStoredTheme())
const isDark = computed(() => themeMode.value === 'dark')

const handleToggleTheme = () => {
  themeMode.value = flipTheme()
}

const form = ref({ username: '', password: '' })
const loading = ref(false)
const registerVisible = ref(false)
const regLoading = ref(false)
const regRef = ref(null)
const regForm = ref({ username: '', password: '', confirm: '' })

const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => {
        if (v !== regForm.value.password) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

const login = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await axios.post('/api/users/login', {
      username: form.value.username,
      password: form.value.password
    })
    localStorage.setItem('jwt_token', res.data.token)
    const isHardcodedAdmin =
      form.value.username === HARDCODED_ADMIN_USERNAME && form.value.password === HARDCODED_ADMIN_USERNAME
    const role = isHardcodedAdmin ? ROLE.ADMIN : (res.data.user?.role ?? ROLE.USER)
    localStorage.setItem('user_role', role)
    ElMessage.success('登录成功')
    const baseUrl = import.meta.env.BASE_URL || '/'
    window.location.href = `${baseUrl}manage.html`
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}

const submitRegister = async () => {
  await regRef.value.validate()
  regLoading.value = true
  try {
    await axios.post('/api/users/register', {
      username: regForm.value.username,
      password: regForm.value.password
    })
    ElMessage.success('注册成功，请登录')
    registerVisible.value = false
    regForm.value = { username: '', password: '', confirm: '' }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '注册失败')
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(165deg, #eef2ff 0%, #f4f4f5 42%, #fafafa 100%);
  font-family:
    'Inter',
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 58px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid #e4e4e7;
  flex-shrink: 0;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.login-theme-btn {
  border: 1px solid #e4e4e7;
  background: #ffffff;
  color: #52525b;
  transition:
    border-color 0.2s,
    color 0.2s,
    background 0.2s;
}

.login-theme-btn:hover {
  border-color: #6366f1;
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.08);
}

.brand {
  font-size: 20px;
  font-weight: 700;
  color: #18181b;
  letter-spacing: 0.04em;
}

.nav-links {
  display: flex;
  gap: 28px;
}

.nav-item {
  color: #52525b;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #18181b;
}

.nav-item.active {
  color: #4f46e5;
  border-bottom-color: #6366f1;
}

.hero {
  position: relative;
  flex: 1;
  min-height: calc(100vh - 56px);
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(
      125deg,
      rgba(15, 23, 42, 0.82) 0%,
      rgba(218, 226, 199, 0.55) 45%,
      rgba(99, 102, 241, 0.35) 100%
    ),
    url('/images/login-library.jpg') center / cover no-repeat;
}

.hero-inner {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 48px 40px 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
  min-height: 100%;
  box-sizing: border-box;
}

.hero-copy {
  flex: 1;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
}

.hero-copy h1 {
  font-size: 42px;
  font-weight: 700;
  margin: 0 0 20px;
  font-family: 'Source Han Serif SC', 'SimSun', 'Times New Roman', serif;
}

.hero-copy p {
  margin: 0 0 10px;
  font-size: 17px;
  opacity: 0.95;
}

.hero-line {
  width: 56px;
  height: 3px;
  background: linear-gradient(90deg, #a5b4fc, #6366f1);
  border-radius: 2px;
  margin-top: 24px;
}

.login-card {
  width: 380px;
  flex-shrink: 0;
  padding: 40px 36px 36px;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 4px 6px -1px rgba(15, 23, 42, 0.06),
    0 24px 48px -12px rgba(79, 70, 229, 0.18);
  backdrop-filter: blur(12px);
}

.card-title {
  margin: 0;
  font-size: 26px;
  color: #18181b;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.card-sub {
  margin: 10px 0 28px;
  font-size: 14px;
  color: #71717a;
}

.card-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 22px;
}

.field-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #fafafa;
  box-shadow: none;
  border: 1px solid #e4e4e7;
  padding: 6px 16px;
  transition:
    border-color 0.2s,
    background 0.2s;
}

.field-input :deep(.el-input__wrapper:hover),
.field-input :deep(.el-input__wrapper.is-focus) {
  border-color: #a5b4fc;
  background: #ffffff;
}

.btn-gold {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 8px 20px -4px rgba(79, 70, 229, 0.45);
  transition:
    filter 0.2s,
    box-shadow 0.2s;
}

.btn-gold:hover {
  filter: brightness(1.06);
  box-shadow: 0 10px 24px -4px rgba(79, 70, 229, 0.55);
}

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: #a1a1aa;
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e4e4e7;
}

.divider span {
  padding: 0 12px;
}

.btn-outline {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  background: transparent;
  border: 1px solid #d4d4d8;
  color: #52525b;
  font-size: 15px;
  font-weight: 500;
  transition:
    border-color 0.2s,
    color 0.2s,
    background 0.2s;
}

.btn-outline:hover {
  background: rgba(99, 102, 241, 0.06);
  border-color: #818cf8;
  color: #4f46e5;
}

@media (max-width: 900px) {
  .hero-inner {
    flex-direction: column;
    align-items: stretch;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
    margin: 0 auto;
  }
  .top-nav {
    padding: 0 16px;
  }
  .nav-links {
    gap: 16px;
    font-size: 13px;
  }
}
</style>

<style>
/* 登录页夜间：Indigo + Zinc，与全局 html.dark 一致 */
html.dark .login-page {
  background: linear-gradient(165deg, #0c0c0f 0%, #09090b 50%, #000000 100%);
}

html.dark .login-page .top-nav {
  background: rgba(9, 9, 11, 0.85);
  border-bottom-color: #27272a;
}

html.dark .login-page .brand {
  color: #fafafa;
}

html.dark .login-page .nav-item {
  color: #a1a1aa;
}

html.dark .login-page .nav-item:hover {
  color: #fafafa;
}

html.dark .login-page .nav-item.active {
  color: #a5b4fc;
  border-bottom-color: #818cf8;
}

html.dark .login-page .login-theme-btn {
  border-color: #3f3f46;
  background: #18181b;
  color: #d4d4d8;
}

html.dark .login-page .login-theme-btn:hover {
  border-color: #818cf8;
  color: #a5b4fc;
  background: rgba(129, 140, 248, 0.12);
}

html.dark .login-page .hero-bg {
  filter: saturate(0.85) brightness(0.5);
}

html.dark .login-page .login-card {
  background: rgba(24, 24, 27, 0.92);
  border-color: #27272a;
  box-shadow:
    0 4px 6px -1px rgba(0, 0, 0, 0.3),
    0 24px 48px -8px rgba(0, 0, 0, 0.55);
}

html.dark .login-page .card-title {
  color: #fafafa;
}

html.dark .login-page .card-sub {
  color: #a1a1aa;
}

html.dark .login-page .field-input :deep(.el-input__wrapper) {
  background: #18181b;
  border-color: #3f3f46;
}

html.dark .login-page .field-input :deep(.el-input__wrapper.is-focus) {
  border-color: #818cf8;
}

html.dark .login-page .divider {
  color: #71717a;
}

html.dark .login-page .divider::before,
html.dark .login-page .divider::after {
  background: #3f3f46;
}

html.dark .login-page .btn-outline {
  border-color: #52525b;
  color: #d4d4d8;
}

html.dark .login-page .btn-outline:hover {
  background: rgba(129, 140, 248, 0.12);
  border-color: #818cf8;
  color: #a5b4fc;
}
</style>
