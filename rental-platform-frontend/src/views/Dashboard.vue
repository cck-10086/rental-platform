<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card stat-card-blue" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon blue">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="合同总数" :value="stats.totalContracts" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-green" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon green">
              <el-icon :size="36"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="待审查合同" :value="stats.pendingContracts" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-orange" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon orange">
              <el-icon :size="36"><CircleCloseFilled /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="高风险条款" :value="stats.highRiskClauses" />
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-card-purple" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon purple">
              <el-icon :size="36"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic title="本月费用（元）" :value="stats.monthlyExpense" :precision="2" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <div ref="riskChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <div ref="expenseChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下方内容区 -->
    <el-row :gutter="20" class="content-row">
      <!-- 左侧：最近合同列表 -->
      <el-col :span="14">
        <el-card class="recent-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近合同</span>
              <el-button text type="primary" @click="$router.push('/contract/list')">查看全部</el-button>
            </div>
          </template>
          <el-table
            :data="recentContracts"
            stripe
            style="width: 100%"
            @row-click="handleContractClick"
            highlight-current-row
          >
            <el-table-column prop="title" label="合同标题" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="上传时间" width="160" />
            <el-table-column label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="riskLevelType(row.riskLevel)"
                  size="small"
                >{{ riskLevelLabel(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="statusType(row.status)"
                  size="small"
                >{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：快捷入口 -->
      <el-col :span="10">
        <el-card class="quick-card" shadow="hover">
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="quick-buttons">
            <el-button type="primary" size="large" class="quick-btn" @click="$router.push('/contract/upload')">
              <el-icon><Upload /></el-icon>
              <span>上传合同</span>
            </el-button>
            <el-button type="success" size="large" class="quick-btn" @click="$router.push('/ai-chat')">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI顾问</span>
            </el-button>
            <el-button type="warning" size="large" class="quick-btn" @click="$router.push('/house/list')">
              <el-icon><OfficeBuilding /></el-icon>
              <span>房源管理</span>
            </el-button>
            <el-button type="danger" size="large" class="quick-btn" @click="$router.push('/expense/list')">
              <el-icon><Money /></el-icon>
              <span>费用管理</span>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Document, Warning, CircleCloseFilled, Money,
  Upload, ChatDotRound, OfficeBuilding
} from '@element-plus/icons-vue'
import { getContractList } from '@/api/contract'
import { getExpenseList } from '@/api/expense'

const router = useRouter()

const stats = reactive({
  totalContracts: 0,
  pendingContracts: 0,
  highRiskClauses: 0,
  monthlyExpense: 0
})

const recentContracts = ref([])
const riskChartRef = ref(null)
const expenseChartRef = ref(null)
let riskChart = null
let expenseChart = null

const riskLevelType = (level) => {
  const map = {
    '高风险': 'danger', '中风险': 'warning', '低风险': 'success',
    high: 'danger', medium: 'warning', low: 'success'
  }
  return map[level] || 'info'
}

const riskLevelLabel = (level) => {
  const map = {
    '高风险': '高风险', '中风险': '中风险', '低风险': '低风险',
    high: '高风险', medium: '中风险', low: '低风险'
  }
  return map[level] || '未知'
}

const statusType = (status) => {
  const map = { pending: 'warning', processing: '', completed: 'success', failed: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { pending: '待审查', processing: '审查中', completed: '已审查', failed: '审查失败' }
  return map[status] || '未知'
}

const handleContractClick = (row) => {
  router.push(`/contract/review/${row.id}`)
}

const fetchData = async () => {
  try {
    // 获取合同列表
    const contractRes = await getContractList({ size: 1000 })
    const contracts = contractRes.data.records || contractRes.data || []
    stats.totalContracts = contracts.length
    stats.pendingContracts = contracts.filter(c => c.status === 'pending').length

    // 从已审查合同中统计高风险条款数
    let highRiskCount = 0
    const reviewedContracts = contracts.filter(c => c.status === 'completed')
    for (const c of reviewedContracts) {
      if (c.riskLevel === '高风险') highRiskCount++
    }
    stats.highRiskClauses = highRiskCount

    // 最近5条合同
    const sorted = [...contracts].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    recentContracts.value = sorted.slice(0, 5)

    // 获取本月费用
    const expenseRes = await getExpenseList({ size: 1000 })
    const expenses = expenseRes.data.records || expenseRes.data || []
    const now = new Date()
    const currentMonth = now.getMonth()
    const currentYear = now.getFullYear()
    const monthlyTotal = expenses
      .filter(e => {
        const d = new Date(e.createdAt || e.date)
        return d.getMonth() === currentMonth && d.getFullYear() === currentYear
      })
      .reduce((sum, e) => sum + (parseFloat(e.amount) || 0), 0)
    stats.monthlyExpense = monthlyTotal

    renderCharts(contracts, expenses)
  } catch {
    // 错误已在拦截器中处理
  }
}

const initCharts = () => {
  riskChart = echarts.init(riskChartRef.value)
  expenseChart = echarts.init(expenseChartRef.value)
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  riskChart?.resize()
  expenseChart?.resize()
}

const renderCharts = (contracts, expenses) => {
  if (!riskChart || !expenseChart) return

  // 合同风险分布（饼图）
  const riskMap = { '高风险': 0, '中风险': 0, '低风险': 0, '未审查': 0 }
  contracts.forEach((c) => {
    if (c.status === 'completed' && c.riskLevel && riskMap[c.riskLevel] !== undefined) {
      riskMap[c.riskLevel]++
    } else {
      riskMap['未审查']++
    }
  })
  riskChart.setOption({
    title: { text: '合同风险分布', left: 'center', textStyle: { fontSize: 15, fontWeight: 600 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
    legend: { bottom: 0 },
    color: ['#f56c6c', '#e6a23c', '#67c23a', '#909399'],
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '48%'],
      data: Object.entries(riskMap).map(([name, value]) => ({ name, value }))
    }]
  })

  // 近 6 个月费用趋势（柱状图）
  const now = new Date()
  const labels = []
  const values = []
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    labels.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
    values.push(0)
  }
  expenses.forEach((e) => {
    const dt = new Date(e.createdAt)
    const key = `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}`
    const idx = labels.indexOf(key)
    if (idx >= 0) values[idx] += Number(e.amount) || 0
  })
  expenseChart.setOption({
    title: { text: '近 6 个月费用（元）', left: 'center', textStyle: { fontSize: 15, fontWeight: 600 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 40,
      itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
    }]
  })
}

onMounted(() => {
  initCharts()
  fetchData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  riskChart?.dispose()
  expenseChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  max-width: 1400px;
  margin: 0 auto;
}

.stat-row {
  margin-bottom: 20px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 8px;
}

.chart-box {
  height: 300px;
}

.stat-card {
  border-radius: 8px;
  border-left: 4px solid;
}

.stat-card-blue { border-left-color: #409eff; }
.stat-card-green { border-left-color: #67c23a; }
.stat-card-orange { border-left-color: #e6a23c; }
.stat-card-purple { border-left-color: #a855f7; }

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.blue { background: rgba(64, 158, 255, 0.1); color: #409eff; }
.stat-icon.green { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.stat-icon.orange { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.stat-icon.purple { background: rgba(168, 85, 247, 0.1); color: #a855f7; }

.stat-info {
  flex: 1;
}

.content-row {
  align-items: flex-start;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.recent-card, .quick-card {
  border-radius: 8px;
}

.recent-card :deep(.el-table__row) {
  cursor: pointer;
}

.quick-buttons {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-btn {
  width: 100%;
  height: 56px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
}
</style>
