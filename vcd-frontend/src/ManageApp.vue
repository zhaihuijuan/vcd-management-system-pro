<template>
  <div class="manage-layout">
    <!-- 固定背景图层，不随页面切换移动 -->
    <div class="bg-fixed-layer"></div>
    <!-- 侧边栏 -->
    <aside class="side-nav">
      <div class="logo-area">
        <div class="logo-icon">V</div>
        <span class="logo-text">VCD管理系统</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :default-openeds="menuDefaultOpeneds"
        class="modern-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/inventory">
          <el-icon><OfficeBuilding /></el-icon>
          <span>实时库存</span>
        </el-menu-item>

        <template v-if="isAdmin">
          <el-sub-menu index="sub-vcd">
            <template #title>
              <el-icon><VideoCamera /></el-icon>
              <span>资源库</span>
            </template>
            <el-menu-item index="/vcd">影片列表</el-menu-item>
            <el-menu-item index="/categories">分类管理</el-menu-item>
          </el-sub-menu>
        </template>

        <el-menu-item v-if="!isAdmin" index="/catalog">
          <el-icon><VideoCamera /></el-icon>
          <span>影片浏览</span>
        </el-menu-item>

        <el-menu-item v-if="!isAdmin" index="/purchase">
          <el-icon><Box /></el-icon>
          <span>采购入库</span>
        </el-menu-item>

        <el-sub-menu v-if="isAdmin" index="sub-sales">
          <template #title>
            <el-icon><ShoppingBag /></el-icon>
            <span>销售中心</span>
          </template>
          <el-menu-item index="/sales">零售记录</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="!isAdmin" index="sub-rental">
          <template #title>
            <el-icon><Key /></el-icon>
            <span>租赁中心</span>
          </template>
          <el-menu-item index="/rental">租借流水</el-menu-item>
          <el-menu-item index="/return">归还清算</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="isAdmin" index="/customers">
          <el-icon><User /></el-icon>
          <span>客户画像</span>
        </el-menu-item>

        <el-menu-item v-if="isAdmin" index="/users">
          <el-icon><Setting /></el-icon>
          <span>权限设置</span>
        </el-menu-item>
      </el-menu>

      <div class="side-footer">
        <div class="user-brief">
          <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          <span class="username">{{ currentUserName }}</span>
        </div>
        <el-button link class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </aside>

    <!-- 主体区 -->
    <div class="main-wrapper">
      <header class="top-header">
        <div class="breadcrumb">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>管理中心</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-actions">
          <el-tooltip :content="isDark ? '切换为日间模式' : '切换为夜间模式'" placement="bottom">
            <el-button circle class="theme-toggle-btn" @click="handleToggleTheme">
              <el-icon><Sunny v-if="isDark" /><Moon v-else /></el-icon>
            </el-button>
          </el-tooltip>
          <div class="header-clock">{{ currentTime }}</div>
          <el-tooltip content="通知" placement="bottom">
            <el-badge is-dot class="action-item">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-tooltip>
        </div>
      </header>

      <main class="main-content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { isAdmin as checkAdmin } from './utils/role'
import { getStoredTheme, toggleTheme as flipTheme } from './utils/theme'
import {
  House,
  VideoCamera,
  Box,
  OfficeBuilding,
  ShoppingBag,
  Key,
  User,
  Setting,
  SwitchButton,
  Bell,
  Moon,
  Sunny
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const currentTime = ref('')
const themeMode = ref(getStoredTheme())
const isDark = computed(() => themeMode.value === 'dark')
let timer = null

const handleToggleTheme = () => {
  themeMode.value = flipTheme()
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleDateString() + ' ' + now.toLocaleTimeString('zh-CN', { hour12: false })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})

const isAdmin = computed(() => checkAdmin())

const menuDefaultOpeneds = computed(() =>
  isAdmin.value ? ['sub-vcd', 'sub-sales'] : ['sub-rental']
)

const activeMenu = computed(() => {
  const p = route.path
  if (p === '/' || p === '/home') return '/home'
  return p
})

const currentPageTitle = computed(() => {
  const titles = {
    '/home': '数据看板',
    '/vcd': '影片列表',
    '/categories': '分类管理',
    '/catalog': '影片浏览',
    '/purchase': '采购入库',
    '/inventory': '库存管理',
    '/sales': '销售记录',
    '/rental': '租赁记录',
    '/return': '归还管理',
    '/customers': '客户管理',
    '/users': '用户管理'
  }
  return titles[route.path] || '概览'
})

const currentUserName = computed(() => {
  try {
    const token = localStorage.getItem('jwt_token')
    if (!token) return '管理员'
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub || '管理员'
  } catch {
    return '管理员'
  }
})

const handleMenuSelect = (key) => {
  router.push(key)
}

const handleLogout = () => {
  localStorage.removeItem('jwt_token')
  localStorage.removeItem('user_role')
  ElMessage.success('已安全退出')
  window.location.href = '/login.html'
}
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family:
    'Inter',
    'SF Pro Text',
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
}
#app {
  height: 100%;
}

.bg-fixed-layer {
  position: fixed;
  top: 0;
  left: 260px;
  right: 0;
  bottom: 0;
  background: url('/images/dabeijing.jpg') center / cover no-repeat;
  z-index: 0;
  pointer-events: none;
}

.bg-fixed-layer::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.72);
}

</style>

<style scoped>
.manage-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

/* 侧边栏 */
.side-nav {
  width: 260px;
  background: var(--vcd-sidebar-bg);
  color: var(--vcd-sidebar-item);
  display: flex;
  flex-direction: column;
  transition: background 0.35s ease, box-shadow 0.35s ease;
  flex-shrink: 0;
  z-index: 100;
  box-shadow: var(--vcd-sidebar-elev-shadow);
}

.logo-area {
  min-height: 72px;
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 12px;
  background: var(--vcd-sidebar-header);
  border-bottom: 1px solid var(--vcd-sidebar-border);
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(145deg, #c59434 0%, #a07820 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
  color: #fff;
  box-shadow: 0 4px 14px rgba(197, 148, 52, 0.4);
}

.logo-text {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--vcd-sidebar-logo-fg);
}

.modern-menu {
  flex: 1;
  border-right: none !important;
  background: transparent !important;
  padding: 12px 0;
}

.modern-menu :deep(.el-menu) {
  background: transparent !important;
}
.modern-menu :deep(.el-menu-item),
.modern-menu :deep(.el-sub-menu__title) {
  color: var(--vcd-sidebar-item) !important;
  height: 48px;
  margin: 3px 10px;
  border-radius: 10px;
}

.modern-menu :deep(.el-menu-item:hover),
.modern-menu :deep(.el-sub-menu__title:hover) {
  background: var(--vcd-sidebar-item-hover-bg) !important;
  color: var(--vcd-sidebar-item-hover-fg) !important;
}

.modern-menu :deep(.el-menu-item.is-active) {
  background: #efe6dc !important;
  color: #5d4037 !important;
  border-left: 3px solid #c59434;
  padding-left: 17px !important;
  box-shadow: none;
}

.side-footer {
  padding: 14px 18px;
  background: var(--vcd-sidebar-footer);
  border-top: 1px solid var(--vcd-sidebar-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
}

.user-brief {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  font-size: 13px;
  color: var(--vcd-sidebar-user-fg);
  font-weight: 500;
}

.logout-btn {
  color: var(--vcd-text-muted);
  font-size: 18px;
}

.logout-btn:hover {
  color: #f87171;
}

/* 主体内容 */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}





.top-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px 0 32px;
  flex-shrink: 0;
  background: #fdf5e6;
  border-bottom: 1px solid rgba(93, 64, 55, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-clock {
  font-family: 'SF Mono', 'DIN Alternate', 'Inter', ui-monospace, sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.02em;
  color: var(--vcd-text-muted);
}

.action-item {
  font-size: 20px;
  color: var(--vcd-text-muted);
  cursor: pointer;
  transition: color 0.2s;
}

.action-item:hover {
  color: var(--vcd-primary);
}

.main-content {
  flex: 1;
  padding: 22px 26px;
  overflow-y: auto;
  position: relative;
  z-index: 1;
}

/* 动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
