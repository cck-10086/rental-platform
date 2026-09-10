<template>
  <div class="review-container">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/dashboard' }">工作台</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/contract/list' }">合同管理</el-breadcrumb-item>
      <el-breadcrumb-item>审查报告</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 加载状态 -->
    <div v-loading="loading" class="review-content">
      <!-- 合同基本信息 -->
      <el-card class="info-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">合同基本信息</span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="合同标题">
            <span class="info-value">{{ contract.title || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="文件名">
            <span class="info-value">{{ contract.fileName || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            <span class="info-value">{{ contract.createdAt || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="riskLevelType(contract.riskLevel)" size="small">
              {{ riskLevelLabel(contract.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 审查摘要 -->
      <el-card class="summary-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">审查摘要</span>
          </div>
        </template>
        <div class="summary-stats">
          <div class="summary-item summary-high">
            <div class="summary-number">{{ summary.high }}</div>
            <div class="summary-label">高风险条款</div>
          </div>
          <el-divider direction="vertical" class="summary-divider" />
          <div class="summary-item summary-medium">
            <div class="summary-number">{{ summary.medium }}</div>
            <div class="summary-label">中风险条款</div>
          </div>
          <el-divider direction="vertical" class="summary-divider" />
          <div class="summary-item summary-low">
            <div class="summary-number">{{ summary.low }}</div>
            <div class="summary-label">低风险条款</div>
          </div>
          <el-divider direction="vertical" class="summary-divider" />
          <div class="summary-item summary-total">
            <div class="summary-number">{{ summary.total }}</div>
            <div class="summary-label">条款总数</div>
          </div>
        </div>
        <!-- 风险进度条 -->
        <div class="summary-progress">
          <el-progress
            :percentage="summary.highPercent"
            :color="['#f56c6c', '#e6a23c', '#67c23a']"
            :stroke-width="18"
          >
            <span class="progress-text">
              高风险占比 {{ summary.highPercent }}%
            </span>
          </el-progress>
        </div>
      </el-card>

      <!-- 风险条款详情 -->
      <el-card class="clauses-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">风险条款详情</span>
            <span class="card-count">共 {{ records.length }} 条</span>
          </div>
        </template>

        <el-empty
          v-if="records.length === 0"
          description="暂无审查记录"
          :image-size="120"
        />

        <div v-else class="clause-list">
          <div
            v-for="(record, index) in records"
            :key="record.id || index"
            class="clause-item"
            :class="`clause-${record.riskLevel}`"
          >
            <!-- 序号和风险等级 -->
            <div class="clause-header">
              <span class="clause-index">#{{ index + 1 }}</span>
              <div class="clause-tags">
                <el-tag
                  :type="riskLevelType(record.riskLevel)"
                  size="small"
                  effect="dark"
                >
                  {{ riskLevelLabel(record.riskLevel) }}
                </el-tag>
                <el-tag v-if="record.riskType" type="info" size="small">
                  {{ record.riskType }}
                </el-tag>
              </div>
            </div>

            <!-- 条款原文 -->
            <div class="clause-original">
            <div class="clause-section-label">
                <el-icon><ChatLineSquare /></el-icon> 条款原文
              </div>
              <div class="clause-original-text">
                {{ record.clauseContent || '-' }}
              </div>
            </div>

            <!-- 大白话解读 -->
            <div v-if="record.riskExplanation" class="clause-explanation">
              <el-alert
                :title="record.riskExplanation"
                type="warning"
                :closable="false"
                show-icon
              >
                <template #title>
                  <div class="alert-title-wrapper">
                    <el-icon class="alert-title-icon"><WarningFilled /></el-icon>
                    <span class="alert-title-text">大白话解读</span>
                  </div>
                  <div class="alert-content">{{ record.riskExplanation }}</div>
                </template>
              </el-alert>
            </div>

            <!-- 建议 -->
            <div v-if="record.suggestion" class="clause-suggestion">
              <el-alert
                type="info"
                :closable="false"
                show-icon
              >
                <template #title>
                  <div class="alert-title-wrapper">
                    <el-icon class="alert-title-icon"><InfoFilled /></el-icon>
                    <span class="alert-title-text">修改建议</span>
                  </div>
                  <div class="alert-content">{{ record.suggestion }}</div>
                </template>
              </el-alert>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 底部操作 -->
      <div class="review-actions">
        <el-button size="large" @click="handleDownloadFile">
          <el-icon><Download /></el-icon> 下载原文件
        </el-button>
        <el-button type="primary" size="large" :loading="exporting" @click="handleExport">
          <el-icon><Printer /></el-icon> 导出报告 PDF
        </el-button>
        <el-button size="large" @click="$router.push('/contract/list')">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ChatLineSquare, WarningFilled, InfoFilled, Printer, ArrowLeft, Download
} from '@element-plus/icons-vue'
import { getContractDetail, getReviewRecords } from '@/api/contract'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const exporting = ref(false)
const contractId = computed(() => route.params.id)

const contract = reactive({
  title: '',
  fileName: '',
  createdAt: '',
  riskLevel: ''
})

const records = ref([])

const summary = computed(() => {
  const list = records.value
  const high = list.filter(r => r.riskLevel === '高风险').length
  const medium = list.filter(r => r.riskLevel === '中风险').length
  const low = list.filter(r => r.riskLevel === '低风险').length
  const total = list.length
  const highPercent = total > 0 ? Math.round((high / total) * 100) : 0
  return { high, medium, low, total, highPercent }
})

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

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await request.get(`/contract/report/${contractId.value}`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = `合同审查报告_${contractId.value}.pdf`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('报告已导出')
  } catch {
    ElMessage.error('报告导出失败，请稍后重试')
  } finally {
    exporting.value = false
  }
}

const handleDownloadFile = async () => {
  try {
    const res = await request.get(`/contract/file/${contractId.value}`, {
      params: { download: true },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = contract.fileName || `合同${contractId.value}`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('文件下载失败，请稍后重试')
  }
}

const fetchData = async () => {
  if (!contractId.value || contractId.value === '0') {
    ElMessage.warning('无效的合同ID')
    return
  }

  loading.value = true
  try {
    // 并行获取合同详情和审查记录
    const [detailRes, recordsRes] = await Promise.all([
      getContractDetail(contractId.value),
      getReviewRecords(contractId.value)
    ])

    const detail = detailRes.data
    contract.title = detail.title || ''
    contract.fileName = detail.fileName || ''
    contract.createdAt = detail.createdAt || ''
    contract.riskLevel = detail.riskLevel || ''

    records.value = recordsRes.data || []
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.review-container {
  max-width: 1000px;
  margin: 0 auto;
}

.breadcrumb {
  margin-bottom: 16px;
}

.review-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
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

.card-count {
  font-size: 13px;
  color: #909399;
}

/* 合同基本信息 */
.info-card {
  border-radius: 8px;
}

.info-value {
  font-weight: 500;
}

/* 审查摘要 */
.summary-card {
  border-radius: 8px;
}

.summary-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 8px 0 16px;
}

.summary-item {
  text-align: center;
  flex: 1;
}

.summary-number {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}

.summary-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.summary-high .summary-number { color: #f56c6c; }
.summary-medium .summary-number { color: #e6a23c; }
.summary-low .summary-number { color: #67c23a; }
.summary-total .summary-number { color: #303133; }

.summary-divider {
  height: 48px;
}

.summary-progress {
  padding: 8px 0;
}

.progress-text {
  font-size: 13px;
}

/* 风险条款详情 */
.clauses-card {
  border-radius: 8px;
}

.clause-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.clause-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 20px;
  transition: border-color 0.3s;
}

.clause-item.clause-high {
  border-left: 4px solid #f56c6c;
  background: #fef0f0;
}

.clause-item.clause-medium {
  border-left: 4px solid #e6a23c;
  background: #fdf6ec;
}

.clause-item.clause-low {
  border-left: 4px solid #67c23a;
  background: #f0f9eb;
}

.clause-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.clause-index {
  font-size: 18px;
  font-weight: 700;
  color: #909399;
  min-width: 36px;
}

.clause-tags {
  display: flex;
  gap: 8px;
}

/* 条款原文 */
.clause-original {
  margin-bottom: 16px;
}

.clause-section-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.clause-original-text {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  white-space: pre-wrap;
  border-left: 3px solid #dcdfe6;
}

/* 解读和建议 */
.clause-explanation,
.clause-suggestion {
  margin-bottom: 12px;
}

.alert-title-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.alert-title-icon {
  font-size: 16px;
}

.alert-title-text {
  font-weight: 600;
  font-size: 14px;
}

.alert-content {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

/* 底部操作 */
.review-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 16px 0 32px;
}

@media print {
  .breadcrumb,
  .review-actions {
    display: none !important;
  }
  .review-container {
    max-width: 100%;
  }
}
</style>
