<template>
  <div class="admin-stats-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col v-for="card in cards" :key="card.label" :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon" :style="{ backgroundColor: card.bg, color: card.color }">
              <el-icon :size="30"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <el-statistic :title="card.label" :value="card.value" :precision="card.precision || 0" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="tip-card">
      <template #header>
        <span class="card-title">说明</span>
      </template>
      <p>此处为平台整体数据统计，仅管理员可见。高风险合同指 AI 审查后风险等级为"高风险"的合同；本月费用为当前自然月内新增费用记录的金额合计。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User, Document, Warning, CircleCloseFilled, OfficeBuilding, Tools, Money } from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/admin'

const stats = ref({})

const cards = computed(() => [
  { label: '用户总数', value: Number(stats.value.userCount) || 0, icon: User, color: '#409eff', bg: 'rgba(64,158,255,0.1)' },
  { label: '合同总数', value: Number(stats.value.contractCount) || 0, icon: Document, color: '#67c23a', bg: 'rgba(103,194,58,0.1)' },
  { label: '待审查合同', value: Number(stats.value.pendingContractCount) || 0, icon: Warning, color: '#e6a23c', bg: 'rgba(230,162,60,0.1)' },
  { label: '高风险合同', value: Number(stats.value.highRiskContractCount) || 0, icon: CircleCloseFilled, color: '#f56c6c', bg: 'rgba(245,108,108,0.1)' },
  { label: '房源总数', value: Number(stats.value.houseCount) || 0, icon: OfficeBuilding, color: '#a855f7', bg: 'rgba(168,85,247,0.1)' },
  { label: '待处理报修', value: Number(stats.value.pendingRepairCount) || 0, icon: Tools, color: '#f97316', bg: 'rgba(249,115,22,0.1)' },
  { label: '本月费用（元）', value: Number(stats.value.monthExpense) || 0, icon: Money, color: '#10b981', bg: 'rgba(16,185,129,0.1)', precision: 2 }
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
  border-radius: 8px;
  margin-bottom: 20px;
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.tip-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.tip-card p {
  color: #606266;
  line-height: 1.8;
  margin: 0;
}
</style>
