<template>
  <div class="page-container">
    <div class="catalog-hero">
      <div class="hero-main">
        <div class="hero-badge">普通用户观影区</div>
        <h2 class="page-title">影片浏览</h2>
        <p class="hero-desc">
          按分类、导演或主演快速筛选影片，优先查看库存充足和当前热门的内容。
        </p>
        <div class="hero-metrics">
          <div class="hero-metric">
            <strong>{{ rows.length }}</strong>
            <span>全部影片</span>
          </div>
          <div class="hero-metric">
            <strong>{{ inStockCount }}</strong>
            <span>可租影片</span>
          </div>
          <div class="hero-metric">
            <strong>{{ filteredRows.length }}</strong>
            <span>当前结果</span>
          </div>
          <div class="hero-metric">
            <strong>{{ filteredCategoryCount }}</strong>
            <span>覆盖分类</span>
          </div>
        </div>
      </div>

      <div class="hero-filters">
        <el-input
          v-model="keyword"
          placeholder="搜索片名 / 导演 / 主演"
          clearable
          class="filter-input"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="categoryId" clearable placeholder="筛选分类" class="filter-select">
          <el-option
            v-for="item in categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-button plain @click="resetFilters">重置筛选</el-button>
      </div>
    </div>

    <div v-if="featuredRows.length" class="section-block">
      <div class="section-header">
        <div>
          <div class="section-title">优先推荐</div>
          <div class="section-subtitle">优先展示库存较好、租借门槛更友好的影片</div>
        </div>
      </div>

      <div class="featured-grid">
        <div v-for="item in featuredRows" :key="item.id" class="featured-card">
          <div class="poster-badge">荐</div>
          <div class="poster-cover">
            <span>{{ coverInitial(item.title) }}</span>
          </div>
          <div class="featured-body">
            <div class="featured-top">
              <h3 class="featured-title">{{ item.title }}</h3>
              <el-tag size="small" :type="stockTagType(item.id)">
                {{ stockStatusText(item.id) }}
              </el-tag>
            </div>
            <div class="featured-meta">
              <span>{{ item.category?.name || '未分类' }}</span>
              <span>导演：{{ item.director || '-' }}</span>
            </div>
            <div class="featured-actors">主演：{{ item.actor || '暂无信息' }}</div>
            <div class="featured-footer">
              <div class="price-block">
                <span class="price-label">租价</span>
                <strong>¥{{ formatPrice(item.rentPrice) }}</strong>
              </div>
              <el-button
                type="primary"
                round
                :disabled="availableStock(item.id) <= 0"
                @click="goRental(item)"
              >
                {{ availableStock(item.id) > 0 ? '去租借' : '暂时无库存' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="section-block">
      <div class="section-header">
        <div>
          <div class="section-title">卡片浏览</div>
          <div class="section-subtitle">更适合快速比对库存、价格和分类信息</div>
        </div>
        <el-tag type="info" size="small">共 {{ filteredRows.length }} 部</el-tag>
      </div>

      <div v-loading="loading" class="catalog-grid">
        <div v-for="item in filteredRows" :key="item.id" class="catalog-card">
          <div class="catalog-cover">
            <span>{{ coverInitial(item.title) }}</span>
          </div>
          <div class="catalog-content">
            <div class="catalog-top">
              <h3 class="catalog-title">{{ item.title }}</h3>
              <el-tag size="small" effect="light">{{ item.category?.name || '未分类' }}</el-tag>
            </div>
            <div class="catalog-line">导演：{{ item.director || '-' }}</div>
            <div class="catalog-line">主演：{{ item.actor || '-' }}</div>
            <div class="catalog-bottom">
              <div class="catalog-price">
                <span>租价</span>
                <strong>¥{{ formatPrice(item.rentPrice) }}</strong>
              </div>
              <div class="catalog-stock">
                <el-tag :type="stockTagType(item.id)">{{ stockStatusText(item.id) }}</el-tag>
              </div>
            </div>
            <el-button
              class="catalog-action"
              type="primary"
              plain
              :disabled="availableStock(item.id) <= 0"
              @click="goRental(item)"
            >
              立即租借
            </el-button>
          </div>
        </div>

        <el-empty
          v-if="!loading && filteredRows.length === 0"
          description="没有找到符合条件的影片"
          :image-size="90"
          class="catalog-empty"
        />
      </div>
    </div>

    <div class="section-block">
      <div class="section-header">
        <div>
          <div class="section-title">明细列表</div>
          <div class="section-subtitle">适合按字段快速查看影片信息与库存情况</div>
        </div>
      </div>

      <el-table :data="filteredRows" stripe border class="data-table">
        <el-table-column prop="title" label="片名" min-width="170" />
        <el-table-column prop="director" label="导演" min-width="120" />
        <el-table-column prop="actor" label="主演" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="110" align="center">
          <template #default="{ row }">{{ row.category?.name || '-' }}</template>
        </el-table-column>
        <el-table-column prop="rentPrice" label="租价(元)" width="95" align="center" />
        <el-table-column label="可用库存" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="stockTagType(row.id)">
              {{ availableStock(row.id) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="availableStock(row.id) <= 0"
              @click="goRental(row)"
            >
              去租借
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { categoryApi, inventoryApi, vcdApi } from '../api/index.js'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const categoryId = ref()
const rows = ref([])
const categories = ref([])
const stockMap = ref({})

function availableStock(vcdId) {
  return stockMap.value[vcdId] || 0
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2)
}

function coverInitial(title) {
  return String(title || 'V').trim().charAt(0).toUpperCase() || 'V'
}

function stockTagType(vcdId) {
  const stock = availableStock(vcdId)
  if (stock <= 0) return 'danger'
  if (stock <= 2) return 'warning'
  return 'success'
}

function stockStatusText(vcdId) {
  const stock = availableStock(vcdId)
  if (stock <= 0) return '无库存'
  if (stock <= 2) return `库存紧张 ${stock}`
  return `可租 ${stock}`
}

const filteredRows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return rows.value.filter((item) => {
    const categoryMatch = !categoryId.value || item.category?.id === categoryId.value
    if (!categoryMatch) return false
    if (!q) return true
    const text = [item.title, item.director, item.actor].filter(Boolean).join(' ').toLowerCase()
    return text.includes(q)
  })
})

const featuredRows = computed(() => {
  return [...filteredRows.value]
    .sort((a, b) => {
      const stockDiff = availableStock(b.id) - availableStock(a.id)
      if (stockDiff !== 0) return stockDiff
      return Number(a.rentPrice || 0) - Number(b.rentPrice || 0)
    })
    .slice(0, 3)
})

const inStockCount = computed(() => rows.value.filter((item) => availableStock(item.id) > 0).length)

const filteredCategoryCount = computed(() => {
  return new Set(filteredRows.value.map((item) => item.category?.name).filter(Boolean)).size
})

function resetFilters() {
  keyword.value = ''
  categoryId.value = undefined
}

function goRental() {
  router.push('/rental')
}

async function loadData() {
  loading.value = true
  try {
    const [vcdRes, categoryRes, inventoryRes] = await Promise.all([
      vcdApi.getAll(),
      categoryApi.getAll(),
      inventoryApi.getAll()
    ])
    rows.value = Array.isArray(vcdRes.data) ? vcdRes.data : []
    categories.value = Array.isArray(categoryRes.data) ? categoryRes.data : []

    const map = {}
    const inventoryRows = Array.isArray(inventoryRes.data) ? inventoryRes.data : []
    inventoryRows.forEach((item) => {
      const vcdId = item.vcd?.id
      if (vcdId == null) return
      map[vcdId] = Math.max((item.stock || 0) - (item.rentCount || 0), 0)
    })
    stockMap.value = map
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.catalog-hero,
.section-block {
  border-radius: 20px;
  overflow: hidden;
}

.catalog-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.95fr);
  gap: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at right top, rgba(255, 255, 255, 0.3), transparent 32%),
    linear-gradient(135deg, #0f172a 0%, #1d4ed8 52%, #4f46e5 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(37, 99, 235, 0.18);
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
  background: rgba(255, 255, 255, 0.15);
  font-size: 12px;
}

.page-title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 6px;
}

.hero-metric {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
}

.hero-metric strong {
  display: block;
  font-size: 28px;
  line-height: 1;
}

.hero-metric span {
  display: block;
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
}

.hero-filters {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.filter-input,
.filter-select {
  width: 100%;
}

.section-block {
  padding: 22px;
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.featured-card {
  position: relative;
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: #fff;
}

.poster-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
}

.poster-cover,
.catalog-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background:
    linear-gradient(160deg, rgba(79, 70, 229, 0.18), rgba(37, 99, 235, 0.08)),
    #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.poster-cover {
  min-height: 148px;
  font-size: 42px;
}

.featured-body,
.catalog-content {
  display: flex;
  flex-direction: column;
}

.featured-top,
.catalog-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.featured-title,
.catalog-title {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.4;
}

.featured-meta,
.featured-actors,
.catalog-line {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.featured-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.featured-actors {
  margin-top: 4px;
}

.featured-footer,
.catalog-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: auto;
  padding-top: 16px;
}

.price-block,
.catalog-price {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price-block span,
.catalog-price span {
  font-size: 12px;
  color: #94a3b8;
}

.price-block strong,
.catalog-price strong {
  color: #0f172a;
  font-size: 24px;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 120px;
}

.catalog-card {
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fff;
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.catalog-card:hover {
  transform: translateY(-2px);
  border-color: #c7d2fe;
  box-shadow: 0 14px 28px rgba(79, 70, 229, 0.12);
}

.catalog-cover {
  min-height: 160px;
  font-size: 48px;
}

.catalog-content {
  gap: 8px;
  padding: 16px;
  flex: 1;
}

.catalog-title {
  font-size: 17px;
}

.catalog-action {
  width: 100%;
  margin-top: 4px;
}

.catalog-empty {
  grid-column: 1 / -1;
  border-radius: 18px;
  background: #fff;
}

.data-table {
  border-radius: 12px;
  overflow: hidden;
}

@media (max-width: 1200px) {
  .catalog-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .catalog-hero,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .catalog-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .catalog-hero,
  .section-block {
    padding: 18px;
  }

  .page-title {
    font-size: 24px;
  }

  .catalog-grid,
  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .featured-card {
    grid-template-columns: 1fr;
  }
}
</style>
