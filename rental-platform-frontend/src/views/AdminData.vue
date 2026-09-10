<template>
  <div class="admin-data-container">
    <el-card shadow="hover" class="list-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name" />
      </el-tabs>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          :placeholder="searchPlaceholder"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="rows" stripe v-loading="loading" style="width: 100%">
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :min-width="col.width || 120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span>{{ col.formatter ? col.formatter(row[col.prop], row) : (row[col.prop] ?? '-') }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import {
  getAdminContracts, getAdminHouses, getAdminExpenses,
  getAdminRepairs, getAdminMoveOuts
} from '@/api/admin'

const activeTab = ref('contracts')
const loading = ref(false)
const rows = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const tabs = [
  { name: 'contracts', label: '合同' },
  { name: 'houses', label: '房源' },
  { name: 'expenses', label: '费用' },
  { name: 'repairs', label: '报修' },
  { name: 'moveOuts', label: '退租' }
]

const riskLevelLabel = (level) => {
  const map = {
    '高风险': '高风险', '中风险': '中风险', '低风险': '低风险',
    high: '高风险', medium: '中风险', low: '低风险'
  }
  return map[level] || '未知'
}

const contractStatusLabel = (status) => {
  const map = { pending: '待审查', processing: '审查中', completed: '已审查', failed: '审查失败' }
  return map[status] || '未知'
}

const houseStatusLabel = (status) => {
  const map = { viewing: '待出租', signed: '已签约', cancelled: '已取消' }
  return map[status] || '未知'
}

const flowStatusLabel = (status) => {
  const map = { pending: '待处理', processing: '处理中', completed: '已完成', cancelled: '已取消' }
  return map[status] || '未知'
}

const paidLabel = (isPaid) => (isPaid === 1 ? '已缴' : '未缴')

const columnMap = {
  contracts: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'title', label: '合同标题', width: 180 },
    { prop: 'fileName', label: '文件名', width: 160 },
    { prop: 'fileType', label: '类型', width: 80 },
    { prop: 'status', label: '状态', width: 90, formatter: contractStatusLabel },
    { prop: 'riskLevel', label: '风险等级', width: 90, formatter: riskLevelLabel },
    { prop: 'riskCount', label: '风险数', width: 80 },
    { prop: 'username', label: '所属用户', width: 120 },
    { prop: 'createdAt', label: '上传时间', width: 170 }
  ],
  houses: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'title', label: '房源标题', width: 180 },
    { prop: 'address', label: '地址', width: 220 },
    { prop: 'rentPrice', label: '月租金', width: 100 },
    { prop: 'deposit', label: '押金', width: 100 },
    { prop: 'area', label: '面积(m²)', width: 100 },
    { prop: 'status', label: '状态', width: 90, formatter: houseStatusLabel },
    { prop: 'username', label: '所属用户', width: 120 },
    { prop: 'createdAt', label: '创建时间', width: 170 }
  ],
  expenses: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'expenseType', label: '费用类型', width: 100 },
    { prop: 'amount', label: '金额(元)', width: 110 },
    { prop: 'billMonth', label: '账单月份', width: 110 },
    { prop: 'dueDate', label: '截止日期', width: 120 },
    { prop: 'isPaid', label: '缴费状态', width: 90, formatter: paidLabel },
    { prop: 'username', label: '所属用户', width: 120 },
    { prop: 'createdAt', label: '创建时间', width: 170 }
  ],
  repairs: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'title', label: '维修标题', width: 200 },
    { prop: 'urgency', label: '紧急程度', width: 100 },
    { prop: 'status', label: '状态', width: 90, formatter: flowStatusLabel },
    { prop: 'username', label: '所属用户', width: 120 },
    { prop: 'createdAt', label: '提交时间', width: 170 }
  ],
  moveOuts: [
    { prop: 'id', label: 'ID', width: 70 },
    { prop: 'contractId', label: '合同ID', width: 90 },
    { prop: 'moveOutDate', label: '退租日期', width: 120 },
    { prop: 'totalDeposit', label: '押金总额', width: 110 },
    { prop: 'deductionAmount', label: '扣除金额', width: 110 },
    { prop: 'refundAmount', label: '应退金额', width: 110 },
    { prop: 'status', label: '状态', width: 90, formatter: flowStatusLabel },
    { prop: 'username', label: '所属用户', width: 120 },
    { prop: 'createdAt', label: '创建时间', width: 170 }
  ]
}

const columns = computed(() => columnMap[activeTab.value] || [])

const searchPlaceholder = computed(() => {
  const map = {
    contracts: '搜索合同标题或文件名',
    houses: '搜索房源标题或地址',
    expenses: '搜索费用类型',
    repairs: '搜索维修标题或描述',
    moveOuts: '搜索状态（待处理/已完成/已取消）'
  }
  return map[activeTab.value] || '搜索'
})

const apiMap = {
  contracts: getAdminContracts,
  houses: getAdminHouses,
  expenses: getAdminExpenses,
  repairs: getAdminRepairs,
  moveOuts: getAdminMoveOuts
}

const fetchData = async () => {
  loading.value = true
  try {
    const api = apiMap[activeTab.value]
    const res = await api({
      keyword: searchKeyword.value.trim() || undefined,
      current: currentPage.value,
      size: pageSize.value
    })
    rows.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchData()
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.admin-data-container {
  max-width: 1280px;
  margin: 0 auto;
}

.list-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  width: 320px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
