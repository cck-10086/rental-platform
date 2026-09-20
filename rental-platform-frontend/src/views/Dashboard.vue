<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card-inner">
            <div class="stat-icon blue">
              <el-icon :size="20"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">合同总数</div>
              <div class="stat-value">{{ stats.totalContracts }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card-inner">
            <div class="stat-icon amber">
              <el-icon :size="20"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">待审查合同</div>
              <div class="stat-value">{{ stats.pendingContracts }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card-inner">
            <div class="stat-icon red">
              <el-icon :size="20"><CircleCloseFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">高风险条款</div>
              <div class="stat-value">{{ stats.highRiskClauses }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card-inner">
            <div class="stat-icon slate">
              <el-icon :size="20"><Money /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">本月费用(元)</div>
              <div class="stat-value">{{ stats.monthlyExpense.toFixed(2) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <div ref="riskChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="never">
          <div ref="expenseChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下方内容区 -->
    <el-row :gutter="20" class="content-row">
      <!-- 左侧:最近合同列表 -->
      <el-col :span="14">
        <el-card class="recent-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近合同</span>
              <el-button text type="primary" @click="$router.push('/contract/list')">查看全部</el-button>
            </div>
          </template>
          <el-table
            :data="recentContracts"
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
                  effect="light"
                >{{ riskLevelLabel(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag
                  :type="statusType(row.status)"
                  size="small"
                  effect="light"
                >{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧:快捷入口 -->
      <el-col :span="10">
        <el-card class="quick-card" shadow="never">
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="quick-buttons">
            <el-button type="primary" size="large" class="quick-btn" @click="$router.push('/contract/upload')">
              <el-icon><Upload /></el-icon>
              <span>上传合同</span>
            </el-button>
            <el-button size="large" class="quick-btn quick-btn-ghost" @click="$router.push('/ai-chat')">
              <el-icon class="quick-icon-blue"><ChatDotRound /></el-icon>
              <span>AI 顾问</span>
            </el-button>
            <el-button size="large" class="quick-btn quick-btn-ghost" @click="$router.push('/house/list')">
              <el-icon class="quick-icon-teal"><OfficeBuilding /></el-icon>
              <span>房源管理</span>
            </el-button>
            <el-button size="large" class="quick-btn quick-btn-ghost" @click="$router.push('/expense/list')">
              <el-icon class="quick-icon-slate"><Money /></el-icon>
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

  // 合同风险分布(饼图):红/琥珀/绿 + 灰阶,与状态标签语义一致
  const riskMap = { '高风险': 0, '中风险': 0, '低风险': 0, '未审查': 0 }
  contracts.forEach((c) => {
    if (c.status === 'completed' && c.riskLevel && riskMap[c.riskLevel] !== undefined) {
      riskMap[c.riskLevel]++
    } else {
      riskMap['未审查']++
    }
  })
  riskChart.setOption({
    title: { text: '合同风险分布', left: 'center', top: 12, textStyle: { fontSize: 14, fontWeight: 500, color: '#1E293B' } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#64748B', fontSize: 12 } },
    color: ['#DC2626', '#D97706', '#059669', '#CBD5E1'],
    series: [{
      type: 'pie',
      radius: ['42%', '65%'],
      center: ['50%', '48%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
      label: { color: '#64748B' },
      data: Object.entries(riskMap).map(([name, value]) => ({ name, value }))
    }]
  })

  // 近 6 个月费用趋势(柱状图):主色蓝
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
    title: { text: '近 6 个月费用(元)', left: 'center', top: 12, textStyle: { fontSize: 14, fontWeight: 500, color: '#1E293B' } },
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 54, bottom: 30 },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11, color: '#64748B' } },
    yAxis: { type: 'value', axisLabel: { color: '#64748B' }, splitLine: { lineStyle: { color: '#F1F5F9' } } },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 32,
      itemStyle: { color: '#2563EB', borderRadius: [6, 6, 0, 0] }
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

.chart-box {
  height: 300px;
}

/* —— 统计卡:白卡 + 圆角图标块 + 大数字,去掉彩色粗边 —— */
.stat-card {
  transition: box-shadow 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.stat-card:hover {
  box-shadow: 0 8px 24px -12px rgba(37, 99, 235, 0.12);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 2px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.blue { background: #EFF6FF; color: #2563EB; }
.stat-icon.amber { background: #FFFBEB; color: #D97706; }
.stat-icon.red { background: #FEF2F2; color: #DC2626; }
.stat-icon.slate { background: #F1F5F9; color: #475569; }

.stat-label {
  font-size: 12px;
  color: #64748B;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #0F172A;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
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
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.recent-card :deep(.el-table__row) {
  cursor: pointer;
}

.quick-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-btn {
  width: 100%;
  height: 52px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 10px;
}

/* 幽灵样式快捷按钮:白底细边框 + 彩色图标,符合蓝白简约基调 */
.quick-btn-ghost {
  background: #fff;
  border: 1px solid #E2E8F0;
  color: #334155;
}

.quick-btn-ghost:hover {
  border-color: rgba(37, 99, 235, 0.35);
  color: #1E293B;
  background: #F8FAFC;
}

.quick-icon-blue { color: #2563EB; }
.quick-icon-teal { color: #0F766E; }
.quick-icon-slate { color: #475569; }
</style>
