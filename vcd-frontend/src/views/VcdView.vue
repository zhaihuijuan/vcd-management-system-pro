<template>
  <div class="page-container">
    <!-- 顶部：标题 + 统计卡片 + 操作按钮 -->
    <div class="page-header">
      <h2 class="page-title">VCD管理</h2>

      <!-- 三个统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-icon icon-primary">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">影片总数</div>
            <div class="stat-value">{{ tableData.length }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon icon-success">
            <el-icon><Sell /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">平均售价</div>
            <div class="stat-value">¥{{ avgSalesPrice }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon icon-warning">
            <el-icon><Key /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">平均租价</div>
            <div class="stat-value">¥{{ avgRentPrice }}</div>
          </div>
        </div>
      </div>

      <!-- 搜索和新增 -->
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索片名..."
          clearable
          style="width: 200px"
          @clear="loadData"
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button type="success" :icon="Plus" @click="openAddDialog">新增VCD</el-button>
      </div>
    </div>

    <el-table :data="tableData" stripe border v-loading="loading" class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="title" label="片名" min-width="120" />
      <el-table-column prop="director" label="导演" min-width="100" />
      <el-table-column prop="actor" label="主演" min-width="150" show-overflow-tooltip />
      <el-table-column prop="publishYear" label="年份" width="80" align="center" />
      <el-table-column prop="time" label="时长" width="90" align="center" />
      <el-table-column label="分类" width="90" align="center">
        <template #default="{ row }">{{ row.category?.name || '-' }}</template>
      </el-table-column>
      <el-table-column prop="rentPrice" label="租价(元)" width="90" align="center" />
      <el-table-column prop="salesPrice" label="售价(元)" width="90" align="center" />
      <el-table-column prop="description" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑VCD' : '新增VCD'"
      width="560px"
      destroy-on-close
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="片名" prop="title">
          <el-input v-model="form.title" placeholder="请输入片名" />
        </el-form-item>
        <el-form-item label="导演" prop="director">
          <el-input v-model="form.director" placeholder="请输入导演" />
        </el-form-item>
        <el-form-item label="主演" prop="actor">
          <el-input v-model="form.actor" placeholder="请输入主演" />
        </el-form-item>
        <el-form-item label="出版年份" prop="publishYear">
          <el-input v-model="form.publishYear" placeholder="如：2024" />
        </el-form-item>
        <el-form-item label="时长" prop="time">
          <el-input v-model="form.time" placeholder="如：120分钟" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="租赁价格" prop="rentPrice">
          <el-input-number v-model="form.rentPrice" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="销售价格" prop="salesPrice">
          <el-input-number v-model="form.salesPrice" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入简介" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, VideoPlay, Sell, Key } from '@element-plus/icons-vue'
import { vcdApi, categoryApi } from '../api/index.js'

const tableData = ref([])
const categories = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchKeyword = ref('')
const formRef = ref(null)

// 统计计算
const avgSalesPrice = computed(() => {
  if (!tableData.value.length) return '0.00'
  const avg = tableData.value.reduce((s, r) => s + (Number(r.salesPrice) || 0), 0) / tableData.value.length
  return avg.toFixed(2)
})

const avgRentPrice = computed(() => {
  if (!tableData.value.length) return '0.00'
  const avg = tableData.value.reduce((s, r) => s + (Number(r.rentPrice) || 0), 0) / tableData.value.length
  return avg.toFixed(2)
})

const defaultForm = () => ({
  id: null, title: '', director: '', actor: '',
  publishYear: '', time: '', categoryId: null,
  rentPrice: 0, salesPrice: 0, description: ''
})
const form = ref(defaultForm())

const rules = {
  title: [{ required: true, message: '请输入片名', trigger: 'blur' }],
  director: [{ required: true, message: '请输入导演', trigger: 'blur' }],
  actor: [{ required: true, message: '请输入主演', trigger: 'blur' }],
  publishYear: [{ required: true, message: '请输入出版年份', trigger: 'blur' }],
  time: [{ required: true, message: '请输入时长', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入简介', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await vcdApi.getAll()
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  const res = await categoryApi.getAll()
  categories.value = res.data
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) { loadData(); return }
  loading.value = true
  try {
    const res = await vcdApi.search(searchKeyword.value)
    tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  isEdit.value = false
  form.value = defaultForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id, title: row.title, director: row.director,
    actor: row.actor, publishYear: row.publishYear, time: row.time,
    categoryId: row.category?.id || null,
    rentPrice: row.rentPrice, salesPrice: row.salesPrice,
    description: row.description
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      title: form.value.title, director: form.value.director,
      actor: form.value.actor, publishYear: form.value.publishYear,
      time: form.value.time, rentPrice: form.value.rentPrice,
      salesPrice: form.value.salesPrice, description: form.value.description,
      category: { id: form.value.categoryId }
    }
    if (isEdit.value) {
      await vcdApi.update(form.value.id, payload)
      ElMessage.success('修改成功')
    } else {
      await vcdApi.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(isEdit.value ? '修改失败' : '新增失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该VCD吗？', '提示', { type: 'warning' })
    .then(async () => {
      await vcdApi.delete(id)
      ElMessage.success('删除成功')
      loadData()
    }).catch(() => {})
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped>
.page-container { padding: 4px; }

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: nowrap;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--vcd-page-title);
  white-space: nowrap;
  margin: 0;
  flex-shrink: 0;
}

/* 统计卡片组：均匀填满中间空白 */
.stat-cards {
  display: flex;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--vcd-card-bg);
  border: 1px solid var(--vcd-border);
  border-radius: 10px;
  padding: 10px 18px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  white-space: nowrap;
  min-width: 0;
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.icon-primary { background: rgba(99,102,241,0.12); color: #6366f1; }
.icon-success { background: rgba(34,197,94,0.12);  color: #16a34a; }
.icon-warning { background: rgba(234,179,8,0.12);  color: #ca8a04; }

/* 数字和标签横排 */
.stat-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.stat-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--vcd-stat-text);
  white-space: nowrap;
}

.stat-value {
  font-size: 20px;
  font-weight: 800;
  color: var(--vcd-stat-text);
  white-space: nowrap;
  flex: 1;
  text-align: center;
}

.value-primary { color: #4f46e5; }
.value-success { color: #15803d; }
.value-warning { color: #b45309; }

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.data-table { border-radius: 8px; overflow: hidden; }
</style>