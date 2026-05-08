<template>
  <div class="user-dashboard">
    <el-card shadow="hover" class="hero-card">
      <div class="hero-panel">
        <div class="hero-main">
          <div class="hero-badge">普通用户工作台</div>
          <h2 class="hero-title">{{ currentUserName }}，欢迎回来</h2>
          <p class="hero-desc">
            这里集中展示库存、待归还和近期租借情况，方便你快速进入日常操作。
          </p>

          <div class="hero-status">
            <span class="hero-status-label">当前状态</span>
            <strong :class="['hero-status-value', dashboardTone.type]">{{ dashboardTone.text }}</strong>
          </div>

          <div class="hero-actions">
            <el-button type="primary" round @click="go('/catalog')">开始选片</el-button>
            <el-button plain round @click="go('/rental')">查看租借</el-button>
          </div>
        </div>

        <div class="hero-side">
          <div class="hero-side-title">今日关注</div>
          <div class="hero-highlight-grid">
            <div v-for="item in heroHighlights" :key="item.label" class="hero-highlight">
              <div class="hero-highlight-value">{{ item.value }}</div>
              <div class="hero-highlight-label">{{ item.label }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="18" class="kpi-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="kpi-card soft-card">
          <div class="kpi-inner">
            <div class="kpi-icon" :class="item.type">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-label">{{ item.label }}</div>
              <div class="kpi-value" :class="{ danger: item.type === 'danger' }">{{ item.value }}</div>
              <div class="kpi-helper">{{ item.helper }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="panel-row">
      <el-col :xs="24" :lg="10">
        <el-card class="panel-card soft-card" shadow="hover">
          <template #header>
            <div class="panel-header">
              <div>
                <div class="panel-title">快捷操作</div>
                <div class="panel-subtitle">常用入口集中在这里，减少切换成本</div>
              </div>
            </div>
          </template>
          <div class="action-grid">
            <button
              v-for="item in actionCards"
              :key="item.title"
              type="button"
              class="action-card"
              @click="go(item.path)"
            >
              <div class="action-card-top">
                <span class="action-card-title">{{ item.title }}</span>
                <span class="action-card-link">进入</span>
              </div>
              <div class="action-card-desc">{{ item.desc }}</div>
            </button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="14">
        <el-card class="panel-card soft-card" shadow="hover">
          <template #header>
            <div class="panel-header">
              <div>
                <div class="panel-title">即将到期提醒</div>
                <div class="panel-subtitle">未来 7 天内需要重点关注的租借记录</div>
              </div>
              <el-tag size="small" :type="dueSoonRows.length ? 'warning' : 'success'">
                {{ dueSoonRows.length ? `${dueSoonRows.length} 条提醒` : '状态良好' }}
              </el-tag>
            </div>
          </template>

          <el-empty
            v-if="dueSoonRows.length === 0"
            description="暂无即将到期记录"
            :image-size="80"
          />

          <el-table v-else :data="dueSoonRows" size="small" stripe class="dashboard-table">
            <el-table-column label="影片" min-width="140">
              <template #default="{ row }">{{ row.vcdTitle }}</template>
            </el-table-column>
            <el-table-column label="客户" min-width="100">
              <template #default="{ row }">{{ row.customerName }}</template>
            </el-table-column>
            <el-table-column prop="expectedReturnDate" label="预计归还" width="120" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isOverdue ? 'danger' : 'warning'">
                  {{ row.isOverdue ? '已逾期' : '即将到期' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="panel-card soft-card" shadow="hover">
      <template #header>
        <div class="panel-header">
          <div>
            <div class="panel-title">最近租借记录</div>
            <div class="panel-subtitle">帮助你快速回看最近处理过的业务流水</div>
          </div>
          <el-tag size="small" type="info">最近 {{ recentRows.length }} 条</el-tag>
        </div>
      </template>

      <el-table
        :data="recentRows"
        v-loading="loading"
        stripe
        class="dashboard-table"
        empty-text="暂无租借记录"
      >
        <el-table-column prop="id" label="单号" width="86" />
        <el-table-column label="影片" min-width="150">
          <template #default="{ row }">{{ row.vcd?.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="客户" min-width="120">
          <template #default="{ row }">{{ row.customer?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="rentalDate" label="租借日期" width="120" />
        <el-table-column prop="expectedReturnDate" label="预计归还" width="120" />
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <el-tag :type="isReturned(row) ? 'success' : 'warning'">
              {{ isReturned(row) ? '已归还' : '未归还' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, Timer, VideoPlay, Warning } from '@element-plus/icons-vue'
import { inventoryApi, rentalApi, vcdApi } from '../api/index.js'

const router = useRouter()
const loading = ref(false)
const rentals = ref([])
const stats = reactive({
  totalVcd: 0,
  availableStock: 0,
  pendingCount: 0,
  overdueCount: 0
})

const currentUserName = computed(() => {
  try {
    const token = localStorage.getItem('jwt_token')
    if (!token) return '用户'
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub || '用户'
  } catch {
    return '用户'
  }
})

const returnedCount = computed(() => rentals.value.filter((item) => isReturned(item)).length)

const dashboardTone = computed(() => {
  if (stats.overdueCount > 0) {
    return { text: `有 ${stats.overdueCount} 条逾期记录待处理`, type: 'danger' }
  }
  if (stats.pendingCount > 0) {
    return { text: `当前有 ${stats.pendingCount} 条租借进行中`, type: 'warning' }
  }
  return { text: '当前租借状态稳定，可优先浏览新片', type: 'success' }
})

const heroHighlights = computed(() => [
  { label: '可租库存', value: stats.availableStock },
  { label: '7日提醒', value: dueSoonRows.value.length },
  { label: '已归还', value: returnedCount.value }
])

const statCards = computed(() => [
  {
    label: '影片总数',
    value: stats.totalVcd,
    helper: '可浏览的影片资源总量',
    icon: VideoPlay,
    type: 'primary'
  },
  {
    label: '可用库存',
    value: stats.availableStock,
    helper: '当前仍可继续租借的库存',
    icon: Box,
    type: 'success'
  },
  {
    label: '待归还单据',
    value: stats.pendingCount,
    helper: '仍在租借中的业务记录',
    icon: Timer,
    type: 'warning'
  },
  {
    label: '逾期未还',
    value: stats.overdueCount,
    helper: '建议优先跟进处理',
    icon: Warning,
    type: 'danger'
  }
])

const actionCards = [
  {
    title: '浏览影片',
    desc: '先看看当前可选影片和库存情况，再决定租借安排。',
    path: '/catalog'
  },
  {
    title: '租借流水',
    desc: '快速进入租借流程，处理新增租借与业务登记。',
    path: '/rental'
  },
  {
    title: '归还清算',
    desc: '集中处理归还、逾期与费用结算相关操作。',
    path: '/return'
  },
  {
    title: '采购入库',
    desc: '补充热门片源，及时改善库存紧张问题。',
    path: '/purchase'
  }
]

const recentRows = computed(() => {
  return [...rentals.value]
    .sort((a, b) => String(b.rentalDate || '').localeCompare(String(a.rentalDate || '')))
    .slice(0, 8)
})

const dueSoonRows = computed(() => {
  const now = new Date()
  const maxDate = new Date(now)
  maxDate.setDate(maxDate.getDate() + 7)

  return rentals.value
    .filter((item) => !isReturned(item) && item.expectedReturnDate)
    .map((item) => {
      const due = new Date(item.expectedReturnDate)
      return {
        id: item.id,
        vcdTitle: item.vcd?.title || '-',
        customerName: item.customer?.name || '-',
        expectedReturnDate: item.expectedReturnDate,
        isOverdue: due < now,
        dueDate: due
      }
    })
    .filter((item) => item.dueDate <= maxDate)
    .sort((a, b) => a.dueDate - b.dueDate)
    .slice(0, 6)
})

function isReturned(record) {
  return record?.returned === true || record?.isReturned === true
}

function go(path) {
  router.push(path)
}

async function loadData() {
  loading.value = true
  try {
    const [vcdRes, inventoryRes, rentalRes] = await Promise.all([
      vcdApi.getAll(),
      inventoryApi.getAll(),
      rentalApi.getAll()
    ])

    const inventoryRows = Array.isArray(inventoryRes.data) ? inventoryRes.data : []
    const rentalRows = Array.isArray(rentalRes.data) ? rentalRes.data : []

    stats.totalVcd = Array.isArray(vcdRes.data) ? vcdRes.data.length : 0
    stats.availableStock = inventoryRows.reduce(
      (sum, row) => sum + Math.max((row.stock || 0) - (row.rentCount || 0), 0),
      0
    )

    const now = new Date()
    const unreturnedRows = rentalRows.filter((item) => !isReturned(item))
    stats.pendingCount = unreturnedRows.length
    stats.overdueCount = unreturnedRows.filter(
      (item) => item.expectedReturnDate && new Date(item.expectedReturnDate) < now
    ).length
    rentals.value = rentalRows
  } catch (error) {
    ElMessage.error('加载用户工作台失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.user-dashboard {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-card,
.soft-card {
  border: none;
  border-radius: 18px;
  overflow: hidden;
}

.hero-card {
  background:
    radial-gradient(circle at right top, rgba(255, 255, 255, 0.32), transparent 28%),
    linear-gradient(135deg, #4f46e5 0%, #7c3aed 55%, #2563eb 100%);
  box-shadow: 0 18px 44px rgba(79, 70, 229, 0.22);
}

.hero-card :deep(.el-card__body) {
  padding: 0;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(240px, 1fr);
  gap: 20px;
  padding: 16px;
  color: #fff;
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.88);
}

.hero-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-status-label {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.hero-status-value {
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.14);
}

.hero-status-value.success {
  color: #dcfce7;
}

.hero-status-value.warning {
  color: #fef3c7;
}

.hero-status-value.danger {
  color: #ffe4e6;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding-top: 4px;
}

.hero-side {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  padding: 16px;
  backdrop-filter: blur(10px);
}

.hero-side-title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.hero-highlight-grid {
  display: grid;
  grid-template-columns: repeat(1, minmax(0, 1fr));
  gap: 12px;
}

.hero-highlight {
  padding: 10px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-highlight-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.hero-highlight-label {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.75);
}

.kpi-row,
.panel-row {
  margin-bottom: 2px;
}

.kpi-card {
  height: 100%;
}

.kpi-card :deep(.el-card__body),
.panel-card :deep(.el-card__body) {
  padding: 20px;
}

.panel-card :deep(.el-card__header) {
  padding: 18px 20px 0;
  border-bottom: none;
}

.soft-card {
  background: linear-gradient(180deg, #ffffff 0%, #fafbff 100%);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.05);
}

.kpi-inner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.kpi-icon {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 20px;
}

.kpi-icon.primary {
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.12);
}

.kpi-icon.success {
  color: #059669;
  background: rgba(16, 185, 129, 0.12);
}

.kpi-icon.warning {
  color: #d97706;
  background: rgba(245, 158, 11, 0.16);
}

.kpi-icon.danger {
  color: #e11d48;
  background: rgba(244, 63, 94, 0.12);
}

.kpi-content {
  flex: 1;
}

.kpi-label {
  color: #71717a;
  font-size: 13px;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
  color: #18181b;
}

.kpi-value.danger {
  color: #e11d48;
}

.kpi-helper {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  color: #18181b;
  font-weight: 600;
}

.panel-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.action-card {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: #c7d2fe;
  box-shadow: 0 12px 24px rgba(99, 102, 241, 0.12);
}

.action-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.action-card-title {
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.action-card-link {
  color: #6366f1;
  font-size: 12px;
}

.action-card-desc {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.dashboard-table :deep(.el-table__header th) {
  color: #475569;
  background: #f8fafc;
}

@media (max-width: 1080px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .action-grid {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    padding: 22px;
  }

  .hero-title {
    font-size: 24px;
  }

  .kpi-card :deep(.el-card__body),
  .panel-card :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>
