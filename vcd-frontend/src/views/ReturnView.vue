<template>
  <div class="page-container">
    <div class="return-hero">
      <div class="hero-main">
        <div class="hero-badge">归还工作台</div>
        <h2 class="page-title">归还记录</h2>
        <p class="hero-desc">
          集中查看归还状态、逾期情况和待处理租借，减少来回切换页面的成本。
        </p>
      </div>
      <el-button type="success" :icon="Plus" round @click="openAddDialog">新增归还</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">归还总数</div>
        <div class="summary-value">{{ stats.total }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">逾期归还</div>
        <div class="summary-value warning">{{ stats.overdue }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">损坏归还</div>
        <div class="summary-value danger">{{ stats.damaged }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">待归还租借</div>
        <div class="summary-value primary">{{ unreturned.length }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">实收总额</div>
        <div class="summary-value">¥{{ formatMoney(stats.totalIncome) }}</div>
      </div>
    </div>

    <div class="panel-card">
      <div class="panel-header">
        <div>
          <div class="panel-title">待处理提醒</div>
          <div class="panel-subtitle">优先关注已逾期和即将到期的租借</div>
        </div>
        <el-tag size="small" :type="pendingReminders.length ? 'warning' : 'success'">
          {{ pendingReminders.length ? `${pendingReminders.length} 条提醒` : '当前状态良好' }}
        </el-tag>
      </div>

      <div v-if="pendingReminders.length" class="reminder-grid">
        <div v-for="item in pendingReminders" :key="item.id" class="reminder-card">
          <div class="reminder-top">
            <strong>{{ item.vcdTitle }}</strong>
            <el-tag size="small" :type="item.isOverdue ? 'danger' : 'warning'">
              {{ item.isOverdue ? `逾期 ${item.daysText}` : `剩余 ${item.daysText}` }}
            </el-tag>
          </div>
          <div class="reminder-line">客户：{{ item.customerName }}</div>
          <div class="reminder-line">预计归还：{{ item.expectedReturnDate || '-' }}</div>
        </div>
      </div>

      <el-empty v-else description="暂无需要重点跟进的归还事项" :image-size="80" />
    </div>

    <div class="panel-card">
      <div class="page-header">
        <div>
          <div class="panel-title">归还列表</div>
          <div class="panel-subtitle">支持按状态和关键词快速筛选</div>
        </div>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索影片 / 客户 / 单号"
            class="filter-input"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="statusFilter" clearable placeholder="状态筛选" class="filter-select">
            <el-option label="正常归还" value="NORMAL" />
            <el-option label="逾期归还" value="OVERDUE" />
            <el-option label="损坏归还" value="DAMAGED" />
          </el-select>
        </div>
      </div>

      <el-table :data="filteredTableData" stripe border v-loading="loading" class="data-table">
        <el-table-column label="ID" width="90" align="center">
          <template #default="{ row }">
            <template v-if="row.id != null && row.id < 0">
              <el-tooltip content="由租赁记录补全，无独立归还表记录" placement="top">
                <span>{{ row.rental?.id ?? '—' }}</span>
              </el-tooltip>
            </template>
            <template v-else>{{ row.id }}</template>
          </template>
        </el-table-column>
        <el-table-column label="VCD片名" min-width="140">
          <template #default="{ row }">{{ row.vcd?.title || '-' }}</template>
        </el-table-column>
        <el-table-column label="客户姓名" width="110">
          <template #default="{ row }">{{ row.customer?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="returnDate" label="归还日期" width="120" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" width="100" align="center" />
        <el-table-column prop="overdueFee" label="逾期费(元)" width="100" align="center" />
        <el-table-column prop="depositRefund" label="退还押金(元)" width="110" align="center" />
        <el-table-column prop="price" label="实收(元)" width="90" align="center" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.id != null && row.id > 0"
              size="small"
              type="danger"
              @click="handleDelete(row.id)"
            >删除</el-button>
            <span v-else class="muted-op">—</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新增归还记录" width="560px" destroy-on-close>
      <div v-if="selectedRental" class="preview-box">
        <div class="preview-title">归还提示</div>
        <div class="preview-grid">
          <div class="preview-item">
            <span>影片</span>
            <strong>{{ selectedRental.vcd?.title || '-' }}</strong>
          </div>
          <div class="preview-item">
            <span>客户</span>
            <strong>{{ selectedRental.customer?.name || '-' }}</strong>
          </div>
          <div class="preview-item">
            <span>预计归还</span>
            <strong>{{ selectedRental.expectedReturnDate || '-' }}</strong>
          </div>
          <div class="preview-item">
            <span>押金参考</span>
            <strong>¥{{ formatMoney(selectedRental.deposit) }}</strong>
          </div>
        </div>
        <el-alert
          :title="settlementHint"
          :type="selectedRentalOverdueDays > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
        />
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="租赁记录ID" prop="rentalId">
          <el-select v-model="form.rentalId" placeholder="请选择租赁记录" style="width: 100%">
            <el-option
              v-for="r in unreturned"
              :key="r.id"
              :label="`#${r.id} - ${r.vcd?.title} (${r.customer?.name})`"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="VCD" prop="vcdId">
          <el-select v-model="form.vcdId" placeholder="请选择VCD" style="width: 100%">
            <el-option v-for="v in vcds" :key="v.id" :label="v.title" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户" prop="customerId">
          <el-select v-model="form.customerId" placeholder="请选择客户" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="归还日期" prop="returnDate">
          <el-date-picker
            v-model="form.returnDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="归还状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="正常归还" value="NORMAL" />
            <el-option label="逾期归还" value="OVERDUE" />
            <el-option label="损坏归还" value="DAMAGED" />
          </el-select>
        </el-form-item>
        <el-form-item label="逾期天数">
          <el-input-number
            v-model="form.overdueDays"
            :min="0"
            :precision="0"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="逾期费用">
          <el-input-number v-model="form.overdueFee" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退还押金">
          <el-input-number
            v-model="form.depositRefund"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实收金额">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
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
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { returnApi, rentalApi, vcdApi, customerApi } from '../api/index.js'

const tableData = ref([])
const rentalRows = ref([])
const unreturned = ref([])
const vcds = ref([])
const customers = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const formRef = ref(null)

function createDefaultForm() {
  return {
    rentalId: null,
    vcdId: null,
    customerId: null,
    returnDate: todayString(),
    status: 'NORMAL',
    overdueDays: 0,
    overdueFee: 0,
    depositRefund: 0,
    price: 0
  }
}

const form = ref(createDefaultForm())

const rules = {
  rentalId: [{ required: true, message: '请选择租赁记录', trigger: 'change' }],
  vcdId: [{ required: true, message: '请选择VCD', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  status: [{ required: true, message: '请选择归还状态', trigger: 'change' }]
}

function todayString() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toDate(value) {
  if (!value) return null
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? null : date
}

function daysBetween(startValue, endValue) {
  const start = toDate(startValue)
  const end = toDate(endValue)
  if (!start || !end) return 0
  return Math.floor((end.getTime() - start.getTime()) / 86400000)
}

function isReturned(record) {
  return record?.returned === true || record?.isReturned === true
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

function statusText(status) {
  if (status === 'NORMAL') return '正常'
  if (status === 'OVERDUE') return '逾期'
  if (status === 'DAMAGED') return '损坏'
  return '-'
}

function statusTagType(status) {
  if (status === 'NORMAL') return 'success'
  if (status === 'OVERDUE') return 'warning'
  if (status === 'DAMAGED') return 'danger'
  return 'info'
}

const filteredTableData = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return tableData.value.filter((item) => {
    if (statusFilter.value && item.status !== statusFilter.value) return false
    if (!q) return true
    const text = [
      item.vcd?.title,
      item.customer?.name,
      item.id,
      item.rental?.id
    ].join(' ').toLowerCase()
    return text.includes(q)
  })
})

const stats = computed(() => {
  const rows = tableData.value
  return {
    total: rows.length,
    overdue: rows.filter((item) => item.status === 'OVERDUE').length,
    damaged: rows.filter((item) => item.status === 'DAMAGED').length,
    totalIncome: rows.reduce((sum, item) => sum + Number(item.price || 0), 0)
  }
})

const pendingReminders = computed(() => {
  const today = todayString()
  return [...unreturned.value]
    .filter((item) => item.expectedReturnDate)
    .map((item) => {
      const diff = daysBetween(today, item.expectedReturnDate)
      return {
        id: item.id,
        vcdTitle: item.vcd?.title || '-',
        customerName: item.customer?.name || '-',
        expectedReturnDate: item.expectedReturnDate,
        isOverdue: diff < 0,
        daysText: `${Math.abs(diff)} 天`,
        diff
      }
    })
    .filter((item) => item.diff <= 3)
    .sort((a, b) => a.diff - b.diff)
    .slice(0, 4)
})

const selectedRental = computed(() => {
  return unreturned.value.find((item) => item.id === form.value.rentalId) || null
})

const selectedRentalOverdueDays = computed(() => {
  if (!selectedRental.value?.expectedReturnDate || !form.value.returnDate) return 0
  return Math.max(daysBetween(selectedRental.value.expectedReturnDate, form.value.returnDate), 0)
})

const settlementHint = computed(() => {
  if (!selectedRental.value) return ''
  if (selectedRentalOverdueDays.value > 0) {
    return `该租借已超过预计归还日期 ${selectedRentalOverdueDays.value} 天，建议核对逾期费用。`
  }
  return '该租借仍在预计归还周期内，可直接核对押金与实收金额。'
})

watch(
  () => form.value.rentalId,
  () => {
    const rental = selectedRental.value
    if (!rental) return
    form.value.vcdId = rental.vcd?.id ?? null
    form.value.customerId = rental.customer?.id ?? null
    form.value.depositRefund = Number(rental.deposit || 0)
    form.value.status = selectedRentalOverdueDays.value > 0 ? 'OVERDUE' : 'NORMAL'
    syncOverdueDays()
  }
)

watch(
  () => form.value.returnDate,
  () => {
    syncOverdueDays()
  }
)

watch(
  () => form.value.status,
  (status) => {
    if (status === 'NORMAL') {
      form.value.overdueFee = 0
    }
    syncOverdueDays()
  }
)

function syncOverdueDays() {
  if (form.value.status === 'OVERDUE' || form.value.status === 'DAMAGED') {
    form.value.overdueDays = selectedRentalOverdueDays.value
    return
  }
  form.value.overdueDays = 0
}

async function loadReturnData() {
  loading.value = true
  try {
    const res = await returnApi.getAll()
    const raw = res?.data
    tableData.value = Array.isArray(raw) ? raw : []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载归还记录失败：' + (e.response?.data?.message || e.message || '未知错误'))
    tableData.value = []
  } finally {
    loading.value = false
  }
}

async function loadRentalData() {
  const res = await rentalApi.getAll()
  rentalRows.value = Array.isArray(res.data) ? res.data : []
  unreturned.value = rentalRows.value.filter((item) => !isReturned(item))
}

async function loadBaseOptions() {
  const [vRes, cRes] = await Promise.all([vcdApi.getAll(), customerApi.getAll()])
  vcds.value = Array.isArray(vRes.data) ? vRes.data : []
  customers.value = Array.isArray(cRes.data) ? cRes.data : []
}

async function openAddDialog() {
  form.value = createDefaultForm()
  await loadRentalData()
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      rental: { id: form.value.rentalId },
      vcd: { id: form.value.vcdId },
      customer: { id: form.value.customerId },
      returnDate: form.value.returnDate || null,
      status: form.value.status,
      overdueDays: form.value.overdueDays,
      overdueFee: form.value.overdueFee,
      depositRefund: form.value.depositRefund,
      price: form.value.price
    }
    await returnApi.create(payload)
    ElMessage.success('归还成功，库存已恢复')
    dialogVisible.value = false
    await Promise.all([loadReturnData(), loadRentalData()])
  } catch (e) {
    ElMessage.error('归还失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

function handleDelete(id) {
  ElMessageBox.confirm('确定要删除该归还记录吗？', '提示', { type: 'warning' })
    .then(async () => {
      await returnApi.delete(id)
      ElMessage.success('删除成功')
      await Promise.all([loadReturnData(), loadRentalData()])
    })
    .catch(() => {})
}

onMounted(async () => {
  await Promise.all([loadReturnData(), loadRentalData(), loadBaseOptions()])
})
</script>

<style scoped>
.page-container {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.return-hero,
.panel-card {
  border-radius: 18px;
  overflow: hidden;
}

.return-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 24px 26px;
  background:
    radial-gradient(circle at right top, rgba(255, 255, 255, 0.24), transparent 30%),
    linear-gradient(135deg, #14532d 0%, #15803d 55%, #0f766e 100%);
  color: #fff;
  box-shadow: 0 18px 36px rgba(20, 83, 45, 0.18);
}

.hero-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hero-badge {
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
}

.page-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  max-width: 580px;
  color: rgba(255, 255, 255, 0.84);
  line-height: 1.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.summary-card,
.panel-card {
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.05);
}

.summary-card {
  padding: 18px;
  border-radius: 18px;
}

.summary-label {
  color: #64748b;
  font-size: 13px;
}

.summary-value {
  margin-top: 10px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
}

.summary-value.primary {
  color: #2563eb;
}

.summary-value.warning {
  color: #d97706;
}

.summary-value.danger {
  color: #e11d48;
}

.panel-card {
  padding: 22px;
}

.panel-header,
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
}

.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
}

.panel-subtitle {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-input {
  width: 260px;
}

.filter-select {
  width: 160px;
}

.reminder-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.reminder-card {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
}

.reminder-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  color: #0f172a;
}

.reminder-line {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.data-table {
  margin-top: 18px;
  border-radius: 12px;
  overflow: hidden;
}

.preview-box {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.preview-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 15px;
  font-weight: 600;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.preview-item {
  padding: 12px;
  border-radius: 12px;
  background: #fff;
}

.preview-item span {
  display: block;
  color: #94a3b8;
  font-size: 12px;
}

.preview-item strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 15px;
}

.muted-op {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .reminder-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .return-hero,
  .page-header,
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid,
  .reminder-grid,
  .preview-grid {
    grid-template-columns: 1fr;
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

  .page-title {
    font-size: 24px;
  }
}
</style>
