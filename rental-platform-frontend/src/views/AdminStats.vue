<template>
  <div class="admin-stats-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col v-for="card in cards" :key="card.label" :span="8">
        <el-card class="stat-card" shadow="never">
          <div class="stat-card-inner">
            <div class="stat-icon" :style="{ backgroundColor: card.bg, color: card.color }">
              <el-icon :size="20"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-value">{{ card.value.toFixed(card.precision || 0) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="tip-card">
      <template #header>
        <span class="card-title">说明</span>
      </template>
      <p>此处为平台整体数据统计,仅管理员可见。高风险合同指 AI 审查后风险等级为"高风险"的合同;本月费用为当前自然月内新增费用记录的金额合计。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User, Document, Warning, CircleCloseFilled, OfficeBuilding, Tools, Money } from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/admin'

const stats = ref({})

// 图标块配色:浅底深字,语义色(蓝=常规、琥珀=待办、红=风险、绿=资金)
const cards = computed(() => [
  { label: '用户总数', value: Number(stats.value.userCount) || 0, icon: User, color: '#2563EB', bg: '#EFF6FF' },
  { label: '合同总数', value: Number(stats.value.contractCount) || 0, icon: Document, color: '#0F766E', bg: '#E1F5EE' },
  { label: '待审查合同', value: Number(stats.value.pendingContractCount) || 0, icon: Warning, color: '#D97706', bg: '#FFFBEB' },
  { label: '高风险合同', value: Number(stats.value.highRiskContractCount) || 0, icon: CircleCloseFilled, color: '#DC2626', bg: '#FEF2F2' },
  { label: '房源总数', value: Number(stats.value.houseCount) || 0, icon: OfficeBuilding, color: '#475569', bg: '#F1F5F9' },
  { label: '待处理报修', value: Number(stats.value.pendingRepairCount) || 0, icon: Tools, color: '#C2410C', bg: '#FFF7ED' },
  { label: '本月费用(元)', value: Number(stats.value.monthExpense) || 0, icon: Money, color: '#059669', bg: '#ECFDF5', precision: 2 }
])

const fetchStats = async () => {
  try {
    const res = await getAdminStats()
    stats.value = res.data || {}
  } catch {
    // 错误已在拦截器中处理
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-stats-container {
  max-width: 1200px;
  margin: 0 auto;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
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

.tip-card {
  border-radius: 14px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1E293B;
}

.tip-card p {
  color: #475569;
  line-height: 1.8;
  margin: 0;
  font-size: 13px;
}
</style>
