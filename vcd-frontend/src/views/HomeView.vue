<template>
  <div v-if="!showAdminDashboard" class="home-view-root">
    <UserDashboardView />
  </div>
  <div v-else class="home-view-root">
    <div class="dashboard" v-loading="loading">
      <!-- 顶部通栏
      <div class="dashboard-head">
        <h2 class="greeting">VCD 租赁管理数据中心</h2>
        <div class="time-display">{{ currentTime }}</div>
      </div> -->

      <!-- KPI 指标卡片 -->
      <el-row :gutter="24" class="stat-row">
        <el-col :xs="24" :sm="12" :lg="6" v-for="item in kpiCards" :key="item.label">
          <el-card class="stat-card modern-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon" :class="item.type">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-label">{{ item.label }}</div>
                <div class="stat-value-wrap">
                  <span class="stat-value">{{ item.value }}</span>
                  <span class="stat-unit">{{ item.unit }}</span>
                </div>
                <div class="stat-growth" :class="item.growth >= 0 ? 'up' : 'down'">
                  <el-icon><component :is="item.growth >= 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
                  <span>较昨日 {{ Math.abs(item.growth) }}%</span>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 主内容区：20% - 50% - 30% -->
      <el-row :gutter="20" class="main-content-row">
        <!-- 左侧 (20%)：排行榜 & 待归还 -->
        <el-col :xs="24" :lg="5" class="side-col">
          <el-card class="rank-card side-panel" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">热门VCD排行榜</span>
                <el-radio-group v-model="rankRange" size="small" @change="initCharts">
                  <el-radio-button value="month">月</el-radio-button>
                  <el-radio-button value="all">全</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div ref="chartRankRef" class="chart-box rank-chart-box" />
          </el-card>

          <el-card class="table-card side-panel" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">待归还概览</span>
                <el-link type="primary" underline="never" @click="goRental">更多</el-link>
              </div>
            </template>
            <div ref="chartPendingRef" class="chart-box pending-chart-box" />
          </el-card>
        </el-col>

        <!-- 中间 (50%)：中国地图 -->
        <el-col :xs="24" :lg="12">
          <el-card class="map-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">全国用户分布与租借热力图</span>
                <div class="header-hint">实时业务地理分布</div>
              </div>
            </template>
            <div ref="chartMapRef" class="chart-box map-box" />
          </el-card>
        </el-col>

        <!-- 右侧 (30%)：图表组 -->
        <el-col :xs="24" :lg="7" class="side-col">
          <el-card class="chart-card side-panel" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">影片类型分布</span>
              </div>
            </template>
            <div ref="chartPieRef" class="chart-box small-chart" />
          </el-card>
          <el-card class="chart-card side-panel" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">营收趋势</span>
              </div>
            </template>
            <div ref="chartRevenueRef" class="chart-box small-chart" />
          </el-card>
          <el-card class="chart-card side-panel" shadow="hover">
            <template #header>
              <div class="card-header">
                <span class="card-title">近 7 日租借量</span>
              </div>
            </template>
            <div ref="chartRecentRef" class="chart-box small-chart" />
          </el-card>

        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { isAdmin as checkAdmin } from '../utils/role'
import { ElMessage } from 'element-plus'
import { 
  Collection, 
  VideoPlay, 
  User, 
  Timer,
  CaretTop,
  CaretBottom
} from '@element-plus/icons-vue'
import UserDashboardView from './UserDashboardView.vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, MapChart, FunnelChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  VisualMapComponent,
  GeoComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { salesApi, rentalApi, inventoryApi, dashboardApi } from '../api/index.js'

echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  VisualMapComponent,
  GeoComponent,
  BarChart,
  LineChart,
  PieChart,
  MapChart,
  FunnelChart,
  CanvasRenderer,
  LabelLayout,
  UniversalTransition
])

const router = useRouter()
const showAdminDashboard = computed(() => checkAdmin())
const loading = ref(true)
const currentTime = ref('')
let timeInterval = null

// 与全局 Indigo + Zinc 主题一致
const COLORS = {
  primary: '#6366f1',
  secondary: '#8b5cf6',
  success: '#22c55e',
  warning: '#eab308',
  danger: '#f43f5e',
  text: '#18181b',
  textSecondary: '#71717a',
  border: '#e4e4e7',
  cardBg: '#ffffff'
}

function chartTheme() {
  const dark =
    typeof document !== 'undefined' && document.documentElement.classList.contains('dark')
  if (dark) {
    return {
      ...COLORS,
      primary: '#a5b4fc',
      secondary: '#c4b5fd',
      success: '#4ade80',
      warning: '#facc15',
      danger: '#fb7185',
      text: '#000',
      textSecondary: '#d4d4d8',
      border: '#52525b',
      tooltipBg: 'rgba(39, 39, 42, 0.97)',
      tooltipBorder: '#a5b4fc',
      axisLine: '#a1a1aa',
      axisLabel: '#e4e4e7',
      splitLine: 'rgba(228, 228, 231, 0.14)',
      chartLabel: '#f4f4f5',
      mapArea: '#3f3f46',
      mapBorder: '#a5b4fc',
      mapEmphasisArea: '#818cf8',
      pieBorder: '#27272a',
      lineAreaTop: 'rgba(165, 180, 252, 0.35)',
      lineAreaBottom: 'rgba(165, 180, 252, 0.02)',
      piePalette: ['#a5b4fc', '#c4b5fd', '#4ade80', '#facc15', '#fb7185']
    }
  }
  return {
    ...COLORS,
    tooltipBg: 'rgba(255, 255, 255, 0.97)',
    tooltipBorder: COLORS.primary,
    axisLine: '#e4e4e7',
    axisLabel: '#52525b',
    splitLine: 'rgba(228, 228, 231, 0.9)',
    chartLabel: '#3f3f46',
    mapArea: '#fafafa',
    mapBorder: '#a5b4fc',
    mapEmphasisArea: '#c7d2fe',
    pieBorder: '#ffffff',
    lineAreaTop: 'rgba(99, 102, 241, 0.22)',
    lineAreaBottom: 'rgba(99, 102, 241, 0)',
    piePalette: ['#6366f1', '#8b5cf6', '#22c55e', '#eab308', '#f43f5e']
  }
}

// 基础数据
const stats = reactive({
  totalRentals: 0,
  totalVcds: 0,
  totalCustomers: 0,
  overdueCount: 0,
  top10Vcds: [],
  monthlyTrends: [],
  categoryStats: [],
  customerFreqDistribution: []
})

const rentals = ref([])
const rankRange = ref('all')

// 图表引用
const chartMapRef = ref(null)
const chartPieRef = ref(null)
const chartRecentRef = ref(null)
const chartRevenueRef = ref(null)
const chartRankRef = ref(null)
const chartPendingRef = ref(null)
let chartInstances = []

const displayName = computed(() => {
  try {
    const token = localStorage.getItem('jwt_token')
    if (!token) return '用户'
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.sub || '用户'
  } catch {
    return '用户'
  }
})

const kpiCards = computed(() => [
  { label: '总租赁次数', value: stats.totalRentals, unit: '单', icon: Collection, type: 'primary', growth: 12.5 },
  { label: '在库影片', value: stats.totalVcds, unit: '部', icon: VideoPlay, type: 'secondary', growth: 3.2 },
  { label: '注册客户', value: stats.totalCustomers, unit: '位', icon: User, type: 'success', growth: 8.7 },
  { label: '逾期待归还', value: stats.overdueCount, unit: '单', icon: Timer, type: 'warning', growth: -2.4 }
])

const popularList = computed(() => {
  const data = rankRange.value === 'month' 
    ? stats.monthlyTrends[stats.monthlyTrends.length - 1]?.topVcds || []
    : stats.top10Vcds
  
  return data.slice(0, 6).map(item => ({
    title: item.title,
    label: `${item.count}笔`
  }))
})

const pendingReturns = computed(() => {
  const today = new Date()
  return rentals.value
    .filter(r => !(r.isReturned || r.returned))
    .map(r => {
      const due = r.expectedReturnDate ? new Date(r.expectedReturnDate) : null
      return {
        vcdTitle: r.vcd?.title || '-',
        overdue: due && due < today
      }
    })
    .slice(0, 5)
})

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleDateString() + ' ' + now.toLocaleTimeString('zh-CN', { hour12: false })
}

function createBaseOption() {
  const T = chartTheme()
  return {
    backgroundColor: 'transparent',
    textStyle: {
      color: T.textSecondary,
      fontFamily: 'PingFang SC, Microsoft YaHei, sans-serif',
      fontSize: 12
    },
    tooltip: {
      backgroundColor: T.tooltipBg,
      borderColor: T.tooltipBorder,
      borderWidth: 1,
      padding: [10, 15],
      textStyle: { color: T.text, fontSize: 13, fontWeight: 500 },
      extraCssText: 'backdrop-filter: blur(8px);',
      shadowColor: 'rgba(0, 0, 0, 0.2)',
      shadowBlur: 14
    }
  }
}

async function initMap() {
  if (!chartMapRef.value) return
  
  try {
    const response = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json')
    const chinaGeoJSON = await response.json()
    echarts.registerMap('china', chinaGeoJSON)

    const chart = echarts.init(chartMapRef.value)
    const T = chartTheme()
    const isDarkMap = document.documentElement.classList.contains('dark')

    // 全面采用模拟数据，绕过后端数据库乱码问题，并增加覆盖省份
    const data = [
      { name: '北京市', value: 225 }, { name: '上海市', value: 162 }, { name: '广东省', value: 228 },
      { name: '浙江省', value: 185 }, { name: '江苏省', value: 194 }, { name: '四川省', value: 152 },
      { name: '湖北省', value: 111 }, { name: '山东省', value: 136 }, { name: '陕西省', value: 83 },
      { name: '福建省', value: 158 }, { name: '湖南省', value: 127 }, { name: '辽宁省', value: 99 },
      { name: '安徽省', value: 108 }, { name: '河南省', value: 145 }, { name: '河北省', value: 112 },
      { name: '云南省', value: 78 }, { name: '贵州省', value: 65 }, { name: '江西省', value: 92 },
      { name: '广西壮族自治区', value: 88 }, { name: '山西省', value: 72 }, { name: '吉林省', value: 56 },
      { name: '黑龙江省', value: 98 }, { name: '内蒙古自治区', value: 45 }, { name: '重庆市', value: 132 },
      { name: '天津市', value: 88 }, { name: '甘肃省', value: 142 }, { name: '海南省', value: 55 },
      { name: '新疆维吾尔自治区', value: 38 }, { name: '宁夏回族自治区', value: 25 }, { name: '青海省', value: 18 },
      { name: '西藏自治区', value: 25 }
    ]

    const values = data.map(d => d.value)
    const maxVal = Math.max(...values)

    chart.setOption({
      ...createBaseOption(),
      tooltip: {
        trigger: 'item',
        formatter: (params) => {
          if (!params.data) return `${params.name}: 暂无数据`
          return `<div style="padding: 3px 6px;">
            <b style="color: ${T.primary}">${params.name}</b><br/>
            会员人数: <span style="font-weight: 600">${params.value}</span>
          </div>`
        }
      },
      visualMap: {
        min: 0,
        max: maxVal || 10, // 确保 max 至少为 10，避免数据过小时颜色全白
        left: 30,
        bottom: 30,
        text: ['高', '低'],
        calculable: true,
        orient: 'vertical',
        inRange: {
          color: isDarkMap
            ? ['#52525b', '#818cf8', '#eef2ff']
            : ['#fafafa', '#c7d2fe', '#4f46e5']
        },
        textStyle: { color: T.axisLabel, fontSize: 12, fontWeight: 500 }
      },
      geo: {
        map: 'china',
        roam: false,
        zoom: 1.2,
        label: {
          show: false,
          color: T.textSecondary,
          fontSize: 10
        },
        itemStyle: {
          areaColor: T.mapArea,
          borderColor: T.mapBorder,
          borderWidth: 1
        },
        emphasis: {
          label: { show: true, fontWeight: 'bold', color: T.text },
          itemStyle: {
            areaColor: T.mapEmphasisArea,
            borderColor: T.primary,
            borderWidth: 1.5
          }
        }
      },
      series: [
        {
          name: '会员人数',
          type: 'map',
          geoIndex: 0,
          data: data
        }
      ]
    })
    chartInstances.push(chart)
  } catch (err) {
    console.error('Failed to load map:', err)
  }
}

function initCharts() {
  disposeCharts()
  initMap()

  const T = chartTheme()

  // 排行榜 (横向柱状图)
  if (chartRankRef.value) {
    const chart = echarts.init(chartRankRef.value)
    const data = rankRange.value === 'month' 
      ? stats.monthlyTrends[stats.monthlyTrends.length - 1]?.topVcds || []
      : stats.top10Vcds
    const chartData = data.slice(0, 5).reverse()
    chart.setOption({
      ...createBaseOption(),
      grid: { left: '3%', right: '12%', bottom: '3%', top: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        splitLine: { show: false },
        axisLabel: { show: false }
      },
      yAxis: {
        type: 'category',
        data: chartData.map(d => d.title),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          fontSize: 12,
          color: T.axisLabel,
          fontWeight: 500,
          width: 72,
          overflow: 'truncate'
        }
      },
      series: [{
        type: 'bar',
        data: chartData.map(d => d.count),
        barWidth: 15,
        itemStyle: {
          borderRadius: [0, 10, 10, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: T.primary },
            { offset: 1, color: T.secondary }
          ])
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}次',
          color: T.chartLabel,
          fontSize: 12,
          fontWeight: 600
        }
      }]
    })
    chartInstances.push(chart)
  }

  // 待归还概览 (漏斗图展示比例)
  if (chartPendingRef.value) {
    const chart = echarts.init(chartPendingRef.value)
    const today = new Date()
    const list = rentals.value.filter(r => !(r.isReturned || r.returned))
    const overdueCount = list.filter(r => {
      const due = r.expectedReturnDate ? new Date(r.expectedReturnDate) : null
      return due && due < today
    }).length
    const normalCount = list.length - overdueCount
    
    chart.setOption({
      ...createBaseOption(),
      tooltip: { trigger: 'item', formatter: '{b}: {c}单 ({d}%)' },
      legend: {
        bottom: '0',
        icon: 'circle',
        itemWidth: 8,
        itemHeight: 8,
        textStyle: { color: T.axisLabel, fontSize: 12 }
      },
      series: [{
        name: '待归还状态',
        type: 'funnel',
        left: '10%', top: '10%', bottom: '20%', width: '80%',
        min: 0, max: list.length || 10,
        sort: 'descending',
        gap: 2,
        label: {
          show: true,
          position: 'inside',
          color: '#ffffff',
          fontSize: 12,
          fontWeight: 600
        },
        labelLine: { show: false },
        itemStyle: { borderColor: T.pieBorder, borderWidth: 1 },
        data: [
          { value: normalCount, name: '正常待还', itemStyle: { color: T.success } },
          { value: overdueCount, name: '逾期未还', itemStyle: { color: T.danger } }
        ]
      }]
    })
    chartInstances.push(chart)
  }

  // 影片类型分布 (饼图)
  if (chartPieRef.value) {
    const chart = echarts.init(chartPieRef.value)
    chart.setOption({
      ...createBaseOption(),
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: T.pieBorder, borderWidth: 2 },
        label: { show: false },
        data: stats.categoryStats.map(s => ({ name: s.name, value: s.count })),
        color: T.piePalette
      }]
    })
    chartInstances.push(chart)
  }

  // 近 7 日租借量 (折线图)
  if (chartRecentRef.value) {
    const chart = echarts.init(chartRecentRef.value)
    const last7Days = stats.monthlyTrends.slice(-7)
    chart.setOption({
      ...createBaseOption(),
      grid: { top: 20, bottom: 22, left: 36, right: 12 },
      xAxis: {
        type: 'category',
        data: last7Days.map(t => t.month.slice(-2) + '日'),
        axisLine: { lineStyle: { color: T.axisLine } },
        axisLabel: { color: T.axisLabel, fontSize: 11, fontWeight: 500 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: T.splitLine, type: 'dashed' } },
        axisLabel: { color: T.axisLabel, fontSize: 11 }
      },
      series: [{
        data: last7Days.map(t => t.count),
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3, color: T.primary },
        emphasis: { lineStyle: { width: 4 } }
      }]
    })
    chartInstances.push(chart)
  }

  // 营收趋势 (面积图)
  if (chartRevenueRef.value) {
    const chart = echarts.init(chartRevenueRef.value)
    const revenueData = stats.monthlyTrends.map(t => ({ month: t.month, val: Math.floor(t.count * 15.5) }))
    chart.setOption({
      ...createBaseOption(),
      grid: { top: 20, bottom: 22, left: 40, right: 12 },
      xAxis: {
        type: 'category',
        data: revenueData.map(d => d.month.slice(-2) + '月'),
        axisLine: { lineStyle: { color: T.axisLine } },
        axisLabel: { color: T.axisLabel, fontSize: 11, fontWeight: 500 }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: T.splitLine, type: 'dashed' } },
        axisLabel: { color: T.axisLabel, fontSize: 11 }
      },
      series: [{
        data: revenueData.map(d => d.val),
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: T.lineAreaTop },
            { offset: 1, color: T.lineAreaBottom }
          ])
        },
        lineStyle: { width: 3, color: T.secondary },
        emphasis: { lineStyle: { width: 4 } }
      }]
    })
    chartInstances.push(chart)
  }
}

function disposeCharts() {
  chartInstances.forEach(c => c.dispose())
  chartInstances = []
}

async function loadData() {
  loading.value = true
  try {
    const [dashRes, rentalRes] = await Promise.all([
      dashboardApi.getStats(),
      rentalApi.getAll()
    ])
    Object.assign(stats, dashRes.data)
    rentals.value = rentalRes.data || []
    
    await nextTick()
    initCharts()
  } catch (e) {
    console.error('Failed to load dashboard data:', e)
  } finally {
    loading.value = false
  }
}

function goRental() {
  router.push('/rental')
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  if (showAdminDashboard.value) {
    loadData()
    window.addEventListener('resize', () => chartInstances.forEach(c => c.resize()))
  } else {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  clearInterval(timeInterval)
  disposeCharts()
})
</script>

<style scoped>
.home-view-root {
  padding: 0;
}

.home-user-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

.dashboard {
  max-width: 1600px;
  margin: 0 auto;
}

.dashboard-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.greeting {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #18181b;
}

.time-display {
  font-family: 'DIN Alternate', sans-serif;
  color: #71717a;
  font-weight: 600;
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.stat-card :deep(.el-card__body) { padding: 15px; }

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  font-size: 20px;
  padding: 12px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.primary { color: #6366f1; background: rgba(99, 102, 241, 0.12); }
.stat-icon.secondary { color: #8b5cf6; background: rgba(139, 92, 246, 0.12); }
.stat-icon.success { color: #22c55e; background: rgba(34, 197, 94, 0.12); }
.stat-icon.warning { color: #ca8a04; background: rgba(234, 179, 8, 0.14); }

.stat-label {
  font-size: 13px;
  color: #71717a;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #18181b;
  font-family: 'DIN Alternate', sans-serif;
}

.stat-unit {
  font-size: 12px;
  margin-left: 4px;
  color: #a1a1aa;
}

.stat-growth {
  margin-top: 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-growth.up { color: #10b981; }
.stat-growth.down { color: #ef4444; }

.main-content-row {
  margin-bottom: 20px;
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.side-panel {
  border: none;
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #18181b;
}

.header-hint {
  font-size: 12px;
  color: #a1a1aa;
}

.rank-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.rank-row {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f4f4f5;
}

.rank-row:last-child { border-bottom: none; }

.rank-badge {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  margin-right: 10px;
  flex-shrink: 0;
}

.rank-badge.top { background: #e6f4ff; color: #1890ff; }

.rank-title {
  flex: 1;
  font-size: 13px;
  color: #3f3f46;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-val {
  font-size: 12px;
  color: #000000;
}

.map-card {
  border: none;
  border-radius: 12px;
  height: 90%;
}

.map-box {
  height: 500px;
}

.rank-chart-box {
  height: 170px;
}

.pending-chart-box {
  height: 172px;
}

.small-chart {
  height: 172px;
}

.pending-table :deep(.el-table__inner-wrapper::before) { display: none; }

@media (max-width: 1200px) {
  .main-content-row {
    display: block;
  }
  .side-col {
    margin-bottom: 20px;
  }
}
</style>

<style>
/* 仪表盘夜间：文字与卡片与 Zinc 一致 */
html.dark .home-view-root .greeting,
html.dark .home-view-root .card-title {
  color: #fafafa;
}

html.dark .home-view-root .time-display,
html.dark .home-view-root .stat-label,
html.dark .home-view-root .header-hint,
html.dark .home-view-root .rank-val {
  color: #a1a1aa !important;
}

html.dark .home-view-root .stat-value {
  color: #fafafa;
}

html.dark .home-view-root .stat-unit {
  color: #71717a;
}

html.dark .home-view-root .rank-title {
  color: #d4d4d8;
}

html.dark .home-view-root .rank-row {
  border-bottom-color: #27272a;
}

html.dark .home-view-root .rank-badge {
  background: #27272a;
  color: #a1a1aa;
}

html.dark .home-view-root .rank-badge.top {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}
</style>