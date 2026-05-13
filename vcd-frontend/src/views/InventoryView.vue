<template>
  <div class="page-container">
    <div class="console-header">
      <div>
        <div class="console-badge">Inventory Console</div>
        <h2 class="page-title">库存管理</h2>
        <p class="page-desc">面向管理员的库存监控与补货决策视图。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">新增库存</el-button>
    </div>

    <section class="overview-strip">
      <div class="overview-card accent-slate">
        <span>库存记录</span>
        <strong>{{ tableData.length }}</strong>
        <small>当前已纳入监控的库存条目</small>
      </div>
      <div class="overview-card accent-blue">
        <span>总可用库存</span>
        <strong>{{ stats.availableTotal }}</strong>
        <small>当前可直接租借的库存总量</small>
      </div>
      <div class="overview-card accent-amber">
        <span>低库存影片</span>
        <strong>{{ stats.lowStockCount }}</strong>
        <small>建议尽快关注补货节奏</small>
      </div>
      <div class="overview-card accent-rose">
        <span>零库存影片</span>
        <strong>{{ stats.outOfStockCount }}</strong>
        <small>已存在业务中断风险</small>
      </div>
      <div class="overview-card progress-card">
        <span>平均租出率</span>
        <div class="progress-value">{{ stats.avgUtilization }}%</div>
        <el-progress
          :percentage="stats.avgUtilization"
          :stroke-width="8"
          :show-text="false"
          status="success"
        />
        <small>用于观察整体库存压力</small>
      </div>
    </section>

    <div class="inventory-layout">
      <section class="main-column">
        <div class="table-panel">
          <div class="table-toolbar">
            <div>
              <div class="panel-title">库存主表</div>
              <div class="panel-subtitle">按状态筛选并查看关键库存指标</div>
            </div>
            <div class="header-actions">
              <el-input
                v-model="keyword"
                clearable
                placeholder="搜索片名"
                class="filter-input"
              />
              <el-select v-model="stockFilter" class="filter-select" placeholder="库存状态">
                <el-option label="全部状态" value="all" />
                <el-option label="正常库存" value="normal" />
                <el-option label="低库存" value="low" />
                <el-option label="零库存" value="out" />
              </el-select>
            </div>
          </div>

          <el-table :data="filteredRows" stripe border v-loading="loading" class="data-table">
            <el-table-column prop="id" label="ID" width="70" align="center" />
            <el-table-column label="VCD片名" min-width="170">
              <template #default="{ row }">{{ row.vcd?.title || '-' }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="总库存" width="100" align="center" />
            <el-table-column prop="rentCount" label="已租出" width="100" align="center" />
            <el-table-column label="可用库存" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="stockTagType(row)">
                  {{ availableStock(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="库存状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="stockTagType(row)" effect="light">
                  {{ stockStatusText(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="租出率" width="110" align="center">
              <template #default="{ row }">
                <div class="utilization-cell">
                  <span>{{ utilization(row) }}%</span>
                  <el-progress
                    :percentage="utilization(row)"
                    :stroke-width="8"
                    :show-text="false"
                    :status="stockLevel(row) === 'out' ? 'exception' : stockLevel(row) === 'low' ? 'warning' : 'success'"
                  />
                </div>
              </template>
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
        <div class="side-panel queue-panel">
          <div class="queue-header">
            <div>
              <div class="panel-title">补货队列</div>
              <div class="panel-subtitle">优先级从高到低排列</div>
            </div>
            <el-tag size="small" :type="restockSuggestions.length ? 'warning' : 'success'">
              {{ restockSuggestions.length ? `${restockSuggestions.length} 条` : '稳定' }}
            </el-tag>
          </div>

          <div v-if="restockSuggestions.length" class="queue-list">
            <div
              v-for="item in restockSuggestions"
              :key="item.id"
              class="queue-item"
              :class="`queue-item--${stockLevel(item)}`"
            >
              <div class="queue-top">
                <strong>{{ item.vcd?.title || '-' }}</strong>
                <el-tag size="small" :type="stockTagType(item)">
                  {{ stockStatusText(item) }}
                </el-tag>
              </div>
              <div class="queue-meta">可用 {{ availableStock(item) }} / 总量 {{ item.stock }}</div>
              <div class="queue-action">
                <span>建议动作</span>
                <strong>{{ stockLevel(item) === 'out' ? '立即补货' : '尽快备货' }}</strong>
              </div>
              <div class="queue-note">{{ restockText(item) }}</div>
            </div>
          </div>

          <el-empty v-else description="暂无紧急补货项" :image-size="70" />
        </div>
      </aside>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑库存' : '新增库存'" width="440px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="VCD" prop="vcdId">
          <el-select v-model="form.vcdId" placeholder="请选择VCD" style="width:100%">
            <el-option v-for="v in vcds" :key="v.id" :label="v.title" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="已租出数量" prop="rentCount">
          <el-input-number v-model="form.rentCount" :min="0" style="width:100%" />
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
import { Plus } from '@element-plus/icons-vue'
import { inventoryApi, vcdApi } from '../api/index.js'

const tableData = ref([])
const vcds = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const keyword = ref('')
const stockFilter = ref('all')
const formRef = ref(null)
const form = ref({ id: null, vcdId: null, stock: 0, rentCount: 0 })

const rules = {
  vcdId: [{ required: true, message: '请选择VCD', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存数量', trigger: 'blur' }]
}

function availableStock(row) {
  return Math.max((row?.stock || 0) - (row?.rentCount || 0), 0)
}

function stockLevel(row) {
  const available = availableStock(row)
  if (available <= 0) return 'out'
  if (available <= 2) return 'low'
  return 'normal'
}

function stockTagType(row) {
  const level = stockLevel(row)
  if (level === 'out') return 'danger'
  if (level === 'low') return 'warning'
  return 'success'
}

function stockStatusText(row) {
  const level = stockLevel(row)
  if (level === 'out') return '零库存'
  if (level === 'low') return '低库存'
  return '库存正常'
}

function utilization(row) {
  const total = Number(row?.stock || 0)
  if (total <= 0) return 0
  return Math.min(Math.round((Number(row?.rentCount || 0) / total) * 100), 100)
}

function restockText(row) {
  if (stockLevel(row) === 'out') return '建议尽快补货，避免业务中断'
  if (utilization(row) >= 70) return '建议补充 2-5 份热门库存'
  return '建议关注租出趋势，提前补货'
}

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tableData.value.filter((row) => {
    const title = String(row.vcd?.title || '').toLowerCase()
    const keywordMatch = !q || title.includes(q)
    const filterMatch = stockFilter.value === 'all' || stockLevel(row) === stockFilter.value
    return keywordMatch && filterMatch
  })
})

const restockSuggestions = computed(() => {
  return [...tableData.value]
    .filter((row) => stockLevel(row) !== 'normal')
    .sort((a, b) => {
      const availableDiff = availableStock(a) - availableStock(b)
      if (availableDiff !== 0) return availableDiff
      return utilization(b) - utilization(a)
    })
    .slice(0, 4)
})

const stats = computed(() => {
  const rows = tableData.value
  const availableTotal = rows.reduce((sum, row) => sum + availableStock(row), 0)
  const lowStockCount = rows.filter((row) => stockLevel(row) === 'low').length
  const outOfStockCount = rows.filter((row) => stockLevel(row) === 'out').length
  const avgUtilization = rows.length
    ? Math.round(rows.reduce((sum, row) => sum + utilization(row), 0) / rows.length)
    : 0
  return {
    availableTotal,
    lowStockCount,
    outOfStockCount,
    avgUtilization
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await inventoryApi.getAll()
    tableData.value = Array.isArray(res.data) ? res.data : []
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = { id: null, vcdId: null, stock: 0, rentCount: 0 }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = { id: row.id, vcdId: row.vcd?.id, stock: row.stock, rentCount: row.rentCount }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { stock: form.value.stock, rentCount: form.value.rentCount, vcd: { id: form.value.vcdId } }
    if (isEdit.value) {
      await inventoryApi.update(form.value.id, payload)
      ElMessage.success('修改成功')
    } else {
      await inventoryApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该库存记录吗？', '提示', { type: 'warning' })
    .then(async () => {
      await inventoryApi.delete(id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

onMounted(async () => {
  loadData()
  const res = await vcdApi.getAll()
  vcds.value = Array.isArray(res.data) ? res.data : []
})
</script>

<style scoped>
.page-container {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border: 1px solid var(--vcd-card-border-alt);
  border-radius: 12px;
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
}

.console-badge {
  width: fit-content;
  padding: 4px 8px;
  border-radius: 6px;
  background: #eff6ff;
  color: #1d4ed8;
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

.overview-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.overview-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--vcd-card-border);
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.overview-card::after {
  content: '';
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 3px;
  background: currentColor;
  opacity: 0.9;
}

.overview-card span,
.overview-card small {
  display: block;
}

.overview-card span {
  color: var(--vcd-page-desc);
  font-size: 12px;
}

.overview-card strong,
.progress-value {
  display: block;
  margin-top: 10px;
  color: var(--vcd-page-title-alt);
  font-size: 30px;
  line-height: 1;
  font-weight: 700;
}

.overview-card small {
  margin-top: 10px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.progress-card {
  color: #16a34a;
}

.progress-card :deep(.el-progress-bar__outer) {
  margin-top: 12px;
}

.accent-slate {
  color: #475569;
}

.accent-blue {
  color: #2563eb;
}

.accent-amber {
  color: #d97706;
}

.accent-rose {
  color: #e11d48;
}

.inventory-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.main-column {
  min-width: 0;
  grid-column: 1;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 16px;
  align-self: start;
  grid-column: 2;
}

.side-panel,
.table-panel {
  background: #ffffff;
  border: 1px solid var(--vcd-card-border);
  border-radius: 12px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.side-panel {
  padding: 16px;
}

.queue-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(148px, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.queue-header,
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
  background: #2563eb;
}

.panel-subtitle {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.queue-item {
  position: relative;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  background: linear-gradient(180deg, var(--vcd-page-bg) 0%, var(--vcd-page-bg-soft) 100%);
}

.queue-item::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: 10px 0 0 10px;
  background: #94a3b8;
}

.queue-item--out::before {
  background: #e11d48;
}

.queue-item--low::before {
  background: #d97706;
}

.queue-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.queue-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  background: rgba(241, 245, 249, 0.9);
}

.queue-action span {
  color: var(--vcd-page-desc);
  font-size: 12px;
}

.queue-action strong {
  color: var(--vcd-page-title-alt);
  font-size: 12px;
}

.queue-meta,
.queue-note {
  margin-top: 8px;
  color: var(--vcd-page-desc);
  font-size: 12px;
  line-height: 1.5;
}

.table-panel {
  padding: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-input {
  width: 240px;
}

.filter-select {
  width: 160px;
}

.utilization-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 72px;
}

.utilization-cell span {
  color: #334155;
  font-size: 12px;
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
  background: #f8fbff;
}

@media (max-width: 1100px) {
  .overview-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inventory-layout {
    grid-template-columns: 1fr;
  }

  .main-column,
  .side-column {
    grid-column: auto;
    position: static;
  }
}

@media (max-width: 760px) {
  .console-header,
  .queue-header,
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

  .overview-strip {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 20px;
  }
}
</style>
