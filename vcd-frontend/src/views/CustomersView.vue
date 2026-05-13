<template>
  <div class="page-container">
    <div class="analytics-header">
      <div>
        <div class="analytics-badge">Customer Insight</div>
        <h2 class="page-title">客户管理</h2>
        <p class="page-desc">从客户行为、风险记录和活跃度三个角度查看客户画像。</p>
      </div>
      <div class="header-actions top-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索客户姓名或电话"
          clearable
          class="filter-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增客户</el-button>
      </div>
    </div>

    <section class="summary-strip">
      <div class="summary-card accent-teal">
        <span>客户总数</span>
        <strong>{{ customerProfiles.length }}</strong>
        <small>当前客户资产规模</small>
      </div>
      <div class="summary-card accent-blue">
        <span>高活跃客户</span>
        <strong>{{ stats.activeCount }}</strong>
        <small>可优先推荐热门片单</small>
      </div>
      <div class="summary-card accent-rose">
        <span>风险关注客户</span>
        <strong>{{ stats.riskCount }}</strong>
        <small>建议重点跟进逾期与损坏记录</small>
      </div>
      <div class="summary-card accent-amber">
        <span>待激活客户</span>
        <strong>{{ stats.newCount }}</strong>
        <small>适合做首单或优惠触达</small>
      </div>
      <div class="summary-card accent-slate">
        <span>累计租借次数</span>
        <strong>{{ stats.totalRentals }}</strong>
        <small>反映整体客户活跃度</small>
      </div>
    </section>

    <div class="customer-layout">
      <section class="main-column">
        <div class="table-panel">
          <div class="table-toolbar">
            <div>
              <div class="panel-title">客户主列表</div>
              <div class="panel-subtitle">支持按客户分层筛选，并查看关键画像字段</div>
            </div>
            <div class="header-actions">
              <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
              <el-select v-model="segmentFilter" class="filter-select" placeholder="客户分层">
                <el-option label="全部客户" value="all" />
                <el-option label="高活跃" value="active" />
                <el-option label="风险关注" value="risk" />
                <el-option label="待激活" value="new" />
                <el-option label="普通客户" value="normal" />
              </el-select>
            </div>
          </div>

          <el-table :data="filteredProfiles" stripe border v-loading="loading" class="data-table">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="name" label="姓名" min-width="120" />
            <el-table-column prop="phoneNumber" label="电话号码" min-width="150" />
            <el-table-column label="客户分层" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="segmentTagType(row.segment)" effect="light">
                  {{ segmentText(row.segment) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="租借次数" width="95" align="center">
              <template #default="{ row }">{{ row.rentalCount }}</template>
            </el-table-column>
            <el-table-column label="归还次数" width="95" align="center">
              <template #default="{ row }">{{ row.returnCount }}</template>
            </el-table-column>
            <el-table-column label="逾期次数" width="95" align="center">
              <template #default="{ row }">{{ row.overdueCount }}</template>
            </el-table-column>
            <el-table-column label="画像说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.note }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <aside class="side-column">
        <div class="insight-panel featured-panel">
          <div class="panel-title">重点客户</div>
          <div class="panel-subtitle">优先查看高活跃与风险客户</div>

          <div v-if="featuredProfiles.length" class="featured-list">
            <div
              v-for="item in featuredProfiles"
              :key="item.id"
              class="featured-card"
              :class="`featured-card--${item.segment}`"
            >
              <div class="featured-top">
                <div class="featured-identity">
                  <div class="featured-avatar">{{ (item.name || '?').slice(0, 1) }}</div>
                  <div>
                    <div class="featured-name">{{ item.name }}</div>
                    <div class="featured-phone">{{ item.phoneNumber || '暂无电话' }}</div>
                  </div>
                </div>
                <el-tag :type="segmentTagType(item.segment)">
                  {{ segmentText(item.segment) }}
                </el-tag>
              </div>
              <div class="featured-stats">
                <span>租借 {{ item.rentalCount }}</span>
                <span>归还 {{ item.returnCount }}</span>
                <span>逾期 {{ item.overdueCount }}</span>
              </div>
              <div class="featured-note">{{ item.note }}</div>
            </div>
          </div>

          <el-empty v-else description="暂无客户画像数据" :image-size="70" />
        </div>
      </aside>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户' : '新增客户'" width="440px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话号码" prop="phoneNumber">
          <el-input v-model="form.phoneNumber" placeholder="请输入电话号码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { customerApi, rentalApi, returnApi } from '../api/index.js'

const customers = ref([])
const rentalRows = ref([])
const returnRows = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const searchKeyword = ref('')
const segmentFilter = ref('all')
const form = ref({ id: null, name: '', phoneNumber: '' })

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phoneNumber: [{ required: true, message: '请输入电话号码', trigger: 'blur' }]
}

function segmentText(segment) {
  if (segment === 'active') return '高活跃'
  if (segment === 'risk') return '风险关注'
  if (segment === 'new') return '待激活'
  return '普通客户'
}

function segmentTagType(segment) {
  if (segment === 'active') return 'success'
  if (segment === 'risk') return 'danger'
  if (segment === 'new') return 'warning'
  return 'info'
}

const customerProfiles = computed(() => {
  return customers.value.map((customer) => {
    const customerRentals = rentalRows.value.filter((row) => row.customer?.id === customer.id)
    const customerReturns = returnRows.value.filter((row) => row.customer?.id === customer.id)
    const overdueCount = customerReturns.filter((row) => row.status === 'OVERDUE').length
    const damagedCount = customerReturns.filter((row) => row.status === 'DAMAGED').length
    const pendingCount = customerRentals.filter((row) => !(row.returned === true || row.isReturned === true)).length

    let segment = 'normal'
    if (overdueCount > 0 || damagedCount > 0) {
      segment = 'risk'
    } else if (customerRentals.length >= 3) {
      segment = 'active'
    } else if (customerRentals.length === 0) {
      segment = 'new'
    }

    let note = '租借行为稳定，可持续跟进偏好内容。'
    if (segment === 'risk') {
      note = `存在 ${overdueCount} 次逾期${damagedCount ? `、${damagedCount} 次损坏` : ''} 记录，建议重点关注。`
    } else if (segment === 'active') {
      note = `累计租借 ${customerRentals.length} 次，当前仍有 ${pendingCount} 笔进行中。`
    } else if (segment === 'new') {
      note = '尚未产生租借记录，可作为待激活客户进行运营。'
    }

    return {
      ...customer,
      rentalCount: customerRentals.length,
      returnCount: customerReturns.length,
      overdueCount,
      damagedCount,
      pendingCount,
      segment,
      note
    }
  })
})

const filteredProfiles = computed(() => {
  const q = searchKeyword.value.trim().toLowerCase()
  return customerProfiles.value.filter((item) => {
    const text = [item.name, item.phoneNumber].filter(Boolean).join(' ').toLowerCase()
    const keywordMatch = !q || text.includes(q)
    const segmentMatch = segmentFilter.value === 'all' || item.segment === segmentFilter.value
    return keywordMatch && segmentMatch
  })
})

const featuredProfiles = computed(() => {
  return [...customerProfiles.value]
    .sort((a, b) => {
      const scoreA = a.segment === 'risk' ? 3 : a.segment === 'active' ? 2 : a.segment === 'new' ? 1 : 0
      const scoreB = b.segment === 'risk' ? 3 : b.segment === 'active' ? 2 : b.segment === 'new' ? 1 : 0
      if (scoreB !== scoreA) return scoreB - scoreA
      return b.rentalCount - a.rentalCount
    })
    .slice(0, 4)
})

const stats = computed(() => {
  const profiles = customerProfiles.value
  return {
    activeCount: profiles.filter((item) => item.segment === 'active').length,
    riskCount: profiles.filter((item) => item.segment === 'risk').length,
    newCount: profiles.filter((item) => item.segment === 'new').length,
    totalRentals: profiles.reduce((sum, item) => sum + item.rentalCount, 0)
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const [customerRes, rentalRes, returnRes] = await Promise.all([
      customerApi.getAll(),
      rentalApi.getAll(),
      returnApi.getAll()
    ])
    customers.value = Array.isArray(customerRes.data) ? customerRes.data : []
    rentalRows.value = Array.isArray(rentalRes.data) ? rentalRes.data : []
    returnRows.value = Array.isArray(returnRes.data) ? returnRes.data : []
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    await loadData()
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, name: '', phoneNumber: '' }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, phoneNumber: row.phoneNumber }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (isEdit.value) {
      await customerApi.update(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await customerApi.create(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该客户吗？', '提示', { type: 'warning' })
    .then(async () => {
      await customerApi.delete(id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.analytics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--vcd-card-border-alt);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
}

.analytics-badge {
  width: fit-content;
  padding: 4px 8px;
  border-radius: 6px;
  background: #f0fdfa;
  color: #0f766e;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.page-title {
  margin: 0;
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--vcd-page-title-alt);
}

.page-desc {
  margin: 0;
  margin-top: 6px;
  color: var(--vcd-page-desc);
  line-height: 1.6;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--vcd-card-border);
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.summary-card::after {
  content: '';
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 3px;
  background: currentColor;
  opacity: 0.9;
}

.summary-card span,
.summary-card small {
  display: block;
}

.summary-card span {
  color: var(--vcd-page-desc);
  font-size: 12px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  color: var(--vcd-page-title-alt);
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
}

.summary-card small {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.accent-teal {
  color: #0f766e;
}

.accent-blue {
  color: #2563eb;
}

.accent-rose {
  color: #e11d48;
}

.accent-amber {
  color: #d97706;
}

.accent-slate {
  color: #475569;
}

.customer-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
}

.main-column,
.side-column {
  min-width: 0;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 16px;
  align-self: start;
}

.table-panel,
.insight-panel {
  background: #ffffff;
  border: 1px solid var(--vcd-card-border);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
  padding: 20px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--vcd-page-title-alt);
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: #0f766e;
}

.panel-subtitle {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.featured-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.top-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 160px;
}

.featured-card {
  position: relative;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.featured-card::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 10px 0 0 10px;
  background: #94a3b8;
}

.featured-card--active::before {
  background: #16a34a;
}

.featured-card--risk::before {
  background: #e11d48;
}

.featured-card--new::before {
  background: #d97706;
}

.featured-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.featured-identity {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.featured-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #ecfeff;
  color: #0f766e;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.featured-name {
  color: var(--vcd-page-title-alt);
  font-size: 14px;
  font-weight: 600;
}

.featured-phone {
  margin-top: 2px;
  color: var(--vcd-page-desc);
  font-size: 12px;
}

.featured-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
  color: #475569;
  font-size: 12px;
}

.featured-note {
  margin-top: 8px;
  color: var(--vcd-page-desc);
  font-size: 12px;
  line-height: 1.5;
}

.data-table {
  margin-top: 18px;
  border-radius: 10px;
  overflow: hidden;
}

.data-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
}

.data-table :deep(.el-table__row:hover > td) {
  background: #f7fffc;
}

@media (max-width: 1100px) {
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .customer-layout {
    grid-template-columns: 1fr;
  }

  .side-column {
    position: static;
  }
}

@media (max-width: 760px) {
  .analytics-header,
  .table-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .filter-input,
  .filter-select {
    width: 100%;
  }

  .summary-strip {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 20px;
  }
}
</style>
