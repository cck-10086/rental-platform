<template>
  <div class="admin-stats-container">
    <!-- 统计数据行:细边框 + 小图标,与工作台同构 -->
    <section class="stat-strip" aria-label="平台数据统计">
      <div v-for="card in cards" :key="card.label" class="stat-cell">
        <el-icon class="stat-cell__icon" :class="{ 'stat-cell__icon--risk': card.risk }"><component :is="card.icon" /></el-icon>
        <div class="stat-cell__info">
          <div class="stat-cell__label">{{ card.label }}</div>
          <div class="stat-cell__value">{{ card.value.toFixed(card.precision || 0) }}</div>
        </div>
      </div>
    </section>

    <el-card shadow="never" class="tip-card">
      <template #header>
        <span class="card-title">说明</span>
      </template>
      <p>此处为平台整体数据统计，仅管理员可见。高风险合同指 AI 审查后风险等级为“高风险”的合同；本月费用为当前自然月内新增费用记录的金额合计。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { User, FileText, FileSearch, ShieldCheck, House, Wrench, Receipt } from '@lucide/vue'
import { getAdminStats } from '@/api/admin'

const stats = ref({})

// 扁平数据行:仅高风险使用语义红,其余中性图标
const cards = computed(() => [
  { label: '用户总数', value: Number(stats.value.userCount) || 0, icon: User },
  { label: '合同总数', value: Number(stats.value.contractCount) || 0, icon: FileText },
  { label: '待审查合同', value: Number(stats.value.pendingContractCount) || 0, icon: FileSearch },
  { label: '高风险合同', value: Number(stats.value.highRiskContractCount) || 0, icon: ShieldCheck, risk: true },
  { label: '房源总数', value: Number(stats.value.houseCount) || 0, icon: House },
  { label: '待处理报修', value: Number(stats.value.pendingRepairCount) || 0, icon: Wrench },
  { label: '本月费用(元)', value: Number(stats.value.monthExpense) || 0, icon: Receipt, precision: 2 }
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

/* 统计数据行:白底细边框,格子间细分隔线 */
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
  padding: 16px 18px;
}

/* 网格换行时以边线自然分隔 */
.stat-cell:nth-child(n + 5) { border-top: 1px solid var(--rp-divider); }
.stat-cell:not(:nth-child(4n + 1)) { border-left: 1px solid var(--rp-divider); }

.stat-cell__icon { flex: 0 0 auto; color: var(--rp-text-muted); font-size: 16px; }
.stat-cell__icon--risk { color: var(--rp-danger); }

.stat-cell__label {
  color: var(--rp-text-muted);
  font-size: 12px;
  margin-bottom: 3px;
}

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

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--rp-text);
}

.tip-card p {
  color: var(--rp-text-secondary);
  line-height: 1.8;
  margin: 0;
  font-size: 13px;
}

@media (max-width: 1023px) {
  .stat-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stat-cell:nth-child(n + 3) { border-top: 1px solid var(--rp-divider); }
  .stat-cell:not(:nth-child(2n + 1)) { border-left: 1px solid var(--rp-divider); }
}

@media (max-width: 480px) {
  .stat-strip { grid-template-columns: 1fr; }
  .stat-cell:not(:first-child) { border-top: 1px solid var(--rp-divider); border-left: 0; }
}
</style>
