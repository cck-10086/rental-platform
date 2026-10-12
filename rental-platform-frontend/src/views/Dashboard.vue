<template>
  <div class="dashboard-container">
    <PageHeader
      title="租住工作台"
      description="集中查看合同风险、近期费用与待处理事项。"
    >
      <template #actions>
        <el-button type="primary" @click="$router.push('/contract/upload')">
          <el-icon><Upload /></el-icon>上传合同
        </el-button>
      </template>
    </PageHeader>

    <!-- 统计数据行:细边框 + 小图标 + 低对比背景,一格一指标 -->
    <section class="stat-strip" aria-label="核心统计">
      <div class="stat-cell">
        <el-icon class="stat-cell__icon"><FileText /></el-icon>
        <div class="stat-cell__info">
          <div class="stat-cell__label">合同总数</div>
          <div class="stat-cell__value">{{ stats.totalContracts }}</div>
        </div>
      </div>
      <div class="stat-cell">
        <el-icon class="stat-cell__icon"><FileSearch /></el-icon>
        <div class="stat-cell__info">
          <div class="stat-cell__label">待审查合同</div>
          <div class="stat-cell__value">{{ stats.pendingContracts }}</div>
        </div>
      </div>
      <div class="stat-cell">
        <el-icon class="stat-cell__icon stat-cell__icon--risk"><ShieldCheck /></el-icon>
        <div class="stat-cell__info">
          <div class="stat-cell__label">高风险条款</div>
          <div class="stat-cell__value">{{ stats.highRiskClauses }}</div>
        </div>
      </div>
      <div class="stat-cell">
        <el-icon class="stat-cell__icon"><Receipt /></el-icon>
        <div class="stat-cell__info">
          <div class="stat-cell__label">本月费用(元)</div>
          <div class="stat-cell__value">{{ stats.monthlyExpense.toFixed(2) }}</div>
        </div>
      </div>
    </section>

    <!-- 图表区 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span class="card-title">合同风险分布</span>
          </template>
          <div ref="riskChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="never">
          <template #header>
            <span class="card-title">费用趋势(近 6 个月,元)</span>
          </template>
          <div ref="expenseChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 下方内容区 -->
    <el-row :gutter="16" class="content-row">
      <!-- 左侧:最近合同列表 -->
      <el-col :xs="24" :xl="14">
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
                <StatusTag :status="riskKey(row.riskLevel)" :label="riskLevelLabel(row.riskLevel)" />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :status="row.status" :label="statusLabel(row.status)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧:待处理事项 -->
      <el-col :xs="24" :xl="10">
        <el-card class="todo-card" shadow="never">
          <template #header>
            <span class="card-title">待处理事项</span>
          </template>
          <nav class="todo-list" aria-label="待处理事项">
            <button
              v-for="item in todoItems"
              :key="item.label"
              type="button"
              class="todo-item"
              @click="$router.push(item.path)"
            >
              <el-icon class="todo-item__icon"><component :is="item.icon" /></el-icon>
              <span class="todo-item__label">{{ item.label }}</span>
              <span class="todo-item__count">{{ item.count }}</span>
              <el-icon class="todo-item__arrow"><ChevronRight /></el-icon>
            </button>
          </nav>
          <p v-if="todoItems.length === 0" class="todo-empty">暂无待处理事项</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileText, FileSearch, ShieldCheck, Receipt, Upload,
  Wrench, DoorOpen, ChevronRight
} from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'

import { getContractList } from '@/api/contract'
import { getExpenseList } from '@/api/expense'
import { getRepairList } from '@/api/repair'
import { getMoveOutList } from '@/api/moveOut'

const router = useRouter()

const stats = reactive({
  totalContracts: 0,
  pendingContracts: 0,
  highRiskClauses: 0,
  monthlyExpense: 0
})

const pendingRepairs = ref(0)
const pendingMoveOuts = ref(0)

const recentContracts = ref([])
const riskChartRef = ref(null)
const expenseChartRef = ref(null)
let riskChart = null
let expenseChart = null

/* 待处理事项:由真实业务数据推导,不做虚构指标 */
const todoItems = computed(() => {
  const items = []
  if (stats.pendingContracts > 0) {
    items.push({ label: '待审查合同', count: `${stats.pendingContracts} 份`, icon: FileSearch, path: '/contract/list' })
  }
  if (pendingRepairs.value > 0) {
    items.push({ label: '维修待处理', count: `${pendingRepairs.value} 项`, icon: Wrench, path: '/repair/list' })
  }
  if (pendingMoveOuts.value > 0) {
    items.push({ label: '退租待处理', count: `${pendingMoveOuts.value} 项`, icon: DoorOpen, path: '/move-out' })
  }
  return items
})

const riskKey = (level) => {
  const map = { '高风险': 'high', '中风险': 'medium', '低风险': 'low', high: 'high', medium: 'medium', low: 'low' }
  return map[level] || 'default'
}

const riskLevelLabel = (level) => {
  const map = {
    '高风险': '高风险', '中风险': '中风险', '低风险': '低风险',
    high: '高风险', medium: '中风险', low: '低风险'
  }
  return map[level] || '未知'
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

    // 并行获取费用 / 维修 / 退租数据
    const [expenseRes, repairRes, moveOutRes] = await Promise.all([
      getExpenseList({ size: 1000 }),
      getRepairList({ size: 1000 }).catch(() => null),
      getMoveOutList({ size: 1000 }).catch(() => null)
    ])

    // 获取本月费用
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

    // 待处理维修(待处理 + 处理中)与待处理退租
    if (repairRes) {
      const repairs = repairRes.data.records || repairRes.data || []
      pendingRepairs.value = repairs.filter(r => r.status === 'pending' || r.status === 'processing').length
    }
    if (moveOutRes) {
      const moveOuts = moveOutRes.data.records || moveOutRes.data || []
      pendingMoveOuts.value = moveOuts.filter(m => m.status === 'pending').length
    }

    renderCharts(contracts, expenses)
  } catch {
    // 错误已在拦截器中处理
  }
}

const initCharts = async () => {
  const [{ init, use }, { CanvasRenderer }, { PieChart, BarChart }, components] = await Promise.all([
    import('echarts/core'),
    import('echarts/renderers'),
    import('echarts/charts'),
    import('echarts/components')
  ])
  const { TooltipComponent, LegendComponent, GridComponent } = components
  use([CanvasRenderer, PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent])
  riskChart = init(riskChartRef.value)
  expenseChart = init(expenseChartRef.value)
  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  riskChart?.resize()
  expenseChart?.resize()
}

const renderCharts = (contracts, expenses) => {
  if (!riskChart || !expenseChart) return

  // 合同风险分布(饼图):低饱和红/琥珀/绿 + 灰阶,与状态标签语义一致
  const riskMap = { '高风险': 0, '中风险': 0, '低风险': 0, '未审查': 0 }
  contracts.forEach((c) => {
    if (c.status === 'completed' && c.riskLevel && riskMap[c.riskLevel] !== undefined) {
      riskMap[c.riskLevel]++
    } else {
      riskMap['未审查']++
    }
  })
  riskChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 份 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#79716B', fontSize: 12 } },
    color: ['#B42318', '#B54708', '#067647', '#D6D3D1'],
    series: [{
      type: 'pie',
      radius: ['42%', '65%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { color: '#79716B' },
      data: Object.entries(riskMap).map(([name, value]) => ({ name, value }))
    }]
  })

  // 近 6 个月费用趋势(柱状图):强调色青绿
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
    tooltip: { trigger: 'axis' },
    grid: { left: 56, right: 16, top: 16, bottom: 28 },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11, color: '#79716B' } },
    yAxis: { type: 'value', axisLabel: { color: '#79716B' }, splitLine: { lineStyle: { color: '#F0EFEE' } } },
    series: [{
      type: 'bar',
      data: values,
      barMaxWidth: 28,
      itemStyle: { color: '#0D9488', borderRadius: [4, 4, 0, 0] }
    }]
  })
}

onMounted(async () => {
  await initCharts()
  await fetchData()
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

/* —— 统计数据行:白底细边框,格子间细分隔线 —— */
.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-card);
  background: var(--rp-surface);
}

.stat-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
}

.stat-cell + .stat-cell { border-left: 1px solid var(--rp-divider); }

.stat-cell__icon {
  flex: 0 0 auto;
  color: var(--rp-text-muted);
  font-size: 16px;
}
.stat-cell__icon--risk { color: var(--rp-danger); }

.stat-cell__label {
  color: var(--rp-text-muted);
  font-size: 12px;
  margin-bottom: 3px;
}

/* 统计数字:22-28px 区间,等宽数字 */
.stat-cell__value {
  color: var(--rp-text);
  font-size: 24px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chart-row { margin-bottom: 16px; }

.chart-row > :deep(.el-col),
.content-row > :deep(.el-col) { margin-bottom: 16px; }

.chart-box {
  height: 260px;
}

.content-row {
  align-items: flex-start;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 区块标题:14px 层级 */
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--rp-text);
}

.recent-card :deep(.el-table__row) {
  cursor: pointer;
}

/* —— 待处理事项:数据行,细线分隔,点击跳转 —— */
.todo-list { display: flex; flex-direction: column; }

.todo-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  padding: 13px 4px;
  border: 0;
  border-bottom: 1px solid var(--rp-divider);
  background: transparent;
  color: var(--rp-text-secondary);
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  text-align: left;
  transition: background var(--rp-duration) var(--rp-ease);
}
.todo-item:last-child { border-bottom: 0; }
.todo-item:hover { background: var(--rp-surface-muted); }
.todo-item__icon { flex: 0 0 auto; color: var(--rp-text-muted); font-size: 15px; }
.todo-item__label { flex: 1; min-width: 0; }
.todo-item__count {
  color: var(--rp-text);
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.todo-item__arrow { flex: 0 0 auto; color: var(--rp-text-muted); font-size: 14px; }

.todo-empty { padding: 24px 0; color: var(--rp-text-muted); font-size: 13px; text-align: center; }

@media (max-width: 1023px) {
  .stat-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  /* 两行布局下,第二行首格不需要左边线 */
  .stat-cell:nth-child(3) { border-left: 0; }
  .stat-cell:nth-child(n + 3) { border-top: 1px solid var(--rp-divider); }
}

@media (max-width: 767px) {
  .chart-box { height: 220px; }
  .stat-cell { padding: 14px 16px; }
  .stat-cell__value { font-size: 22px; }
}

@media (max-width: 480px) {
  .stat-strip { grid-template-columns: 1fr; }
  .stat-cell + .stat-cell { border-left: 0; border-top: 1px solid var(--rp-divider); }
}
</style>
