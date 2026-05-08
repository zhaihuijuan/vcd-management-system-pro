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
              <div class="reg-hint">店员名不能重复，密码必须同时有字母和数字，且密码长度大于等于6</div>
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
  username: [
    { required: true, message: '请输入店员名', trigger: 'blur' },
    { min: 1, max: 50, message: '店员名长度不能超过50个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => {
        if (!v || v.length < 6) return cb(new Error('密码长度必须大于等于6位'))
        const hasLetter = /[a-zA-Z]/.test(v)
        const hasDigit  = /[0-9]/.test(v)
        if (!hasLetter || !hasDigit) return cb(new Error('密码必须同时包含字母和数字'))
        cb()
      },
      trigger: 'blur'
    }
  ],
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
    const msg = e.response?.data?.message || e.response?.data || '注册失败'
    ElMessage({
      message: typeof msg === 'string' ? msg : '注册失败',
      type: 'error',
      duration: 4000,
      showClose: true
    })
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
  background: #fdf5e6;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Source Han Serif SC', 'SimSun', serif, sans-serif;
}

.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 48px;
  height: 56px;
  background: #fdf5e6;
  border-bottom: 1px solid rgba(93, 64, 55, 0.08);
  flex-shrink: 0;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.login-theme-btn {
  border: 1px solid rgba(93, 64, 55, 0.2);
  background: transparent;
  color: #5d4037;
  transition: border-color 0.2s, color 0.2s;
}

.login-theme-btn:hover {
  border-color: #c59434;
  color: #c59434;
}

.brand {
  font-size: 22px;
  font-weight: 600;
  color: #5d4037;
  letter-spacing: 0.05em;
}

.nav-links {
  display: flex;
  gap: 32px;
}

.nav-item {
  color: #5d4037;
  text-decoration: none;
  font-size: 15px;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
}

.nav-item:hover {
  color: #3e2723;
}

.nav-item.active {
  border-bottom-color: #c59434;
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
    linear-gradient(rgba(45, 36, 28, 0.55), rgba(45, 36, 28, 0.55)),
    url('/images/beijing.jpg') center / cover no-repeat;
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
  background: linear-gradient(90deg, #d4a017, #f0d060);
  border-radius: 2px;
  margin-top: 24px;
}

.login-card {
  width: 380px;
  flex-shrink: 0;
  padding: 40px 36px 36px;
  background: url('/images/zhuzi.jpg') center / cover no-repeat;
  border-radius: 16px;
  border: 1px solid rgba(93, 64, 55, 0.12);
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.25);
}

.card-title {
  margin: 0;
  font-size: 28px;
  color: #5d4037;
  font-weight: 700;
  font-family: 'Source Han Serif SC', 'SimSun', serif;
}

.card-sub {
  margin: 10px 0 28px;
  font-size: 14px;
  color: #8d6e63;
  font-weight: 600;
}

.card-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 22px;
}

.field-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  background: #f0f7ff;
  box-shadow: none;
  border: 1px solid rgba(93, 64, 55, 0.12);
  padding: 6px 16px;
}

.field-input :deep(.el-input__wrapper:hover),
.field-input :deep(.el-input__wrapper.is-focus) {
  border-color: #c59434;
  background: #fff;
}

.btn-gold {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  border: none;
  background: linear-gradient(180deg, #e8b923, #c59434);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(197, 148, 52, 0.4);
  transition: filter 0.2s;
}

.btn-gold:hover {
  filter: brightness(1.05);
}

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: #a1887f;
  font-size: 13px;
  font-weight: 600;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(93, 64, 55, 0.2);
}

.divider span {
  padding: 0 12px;
}

.btn-outline {
  width: 100%;
  height: 44px;
  border-radius: 22px;
  background: transparent;
  border: 1px solid #c59434;
  color: #5d4037;
  font-size: 15px;
  font-weight: 500;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}

.btn-outline:hover {
  background: rgba(197, 148, 52, 0.12);
  border-color: #b8860b;
  color: #3e2723;
}

.reg-hint {
  font-size: 11px;
  color: #ef4444;
  line-height: 1.5;
  margin-top: 8px;
  padding: 0 2px;
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
/* 修复白边和弹窗抖动 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow-x: hidden;
}

/* 禁止Element Plus弹窗打开时给body加padding-right导致背景抖动 */
body.el-popup-parent--hidden {
  padding-right: 0 !important;
}

/* 创建新账户弹窗背景图 */
.el-overlay .el-dialog {
  background: url('/images/xinzhanghu.jpg') center / cover no-repeat !important;
}

.el-overlay .el-dialog .el-dialog__header {
  background: transparent;
}

.el-overlay .el-dialog .el-dialog__body {
  background: transparent;
}

.el-overlay .el-dialog .el-dialog__footer {
  background: transparent;
}


/* 创建新账户弹窗字体加粗 */
.el-overlay .el-dialog .el-form-item__label {
  font-weight: 600;
}

.el-overlay .el-dialog .el-dialog__title {
  font-weight: 700;
}

.el-overlay .el-dialog .el-input__inner {
  font-weight: 500;
}

</style>