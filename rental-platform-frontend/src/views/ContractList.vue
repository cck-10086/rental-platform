<template>
  <div class="list-container">
    <el-card class="list-card" shadow="hover">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索合同标题或文件名"
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

      <!-- 合同列表 -->
      <el-table
        :data="contracts"
        stripe
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="title" label="合同标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :type="riskLevelType(row.riskLevel)" size="small">
              {{ riskLevelLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="info" link size="small" @click="handleViewFile(row)">
              <el-icon><View /></el-icon> 查看文件
            </el-button>
            <el-button type="primary" link size="small" @click="handleView(row)">
              <el-icon><View /></el-icon> 查看详情
            </el-button>
            <el-popconfirm
              title="确定要删除这份合同吗？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handlePageChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, Delete } from '@element-plus/icons-vue'
import { getContractList, deleteContract } from '@/api/contract'
import request from '@/api/request'

const router = useRouter()

const loading = ref(false)
const searchKeyword = ref('')
const contracts = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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

const handleSearch = () => {
  currentPage.value = 1
  fetchList()
}

const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  fetchList()
}

const handlePageChange = () => {
  fetchList()
}

const handleView = (row) => {
  router.push(`/contract/review/${row.id}`)
}

const handleViewFile = async (row) => {
  try {
    const type = (row.fileType || '').toLowerCase()
    // PDF/TXT 浏览器可直接预览，Word 走下载
    const download = !['pdf', 'txt'].includes(type)
    const res = await request.get(`/contract/file/${row.id}`, {
      params: { download },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    if (download) {
      const link = document.createElement('a')
      link.href = url
      link.download = row.fileName || `合同${row.id}`
      link.click()
      URL.revokeObjectURL(url)
    } else {
      window.open(url, '_blank')
    }
  } catch {
    ElMessage.error('文件获取失败，请稍后重试')
  }
}

const handleDelete = async (row) => {
  try {
    await deleteContract(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // 错误已在拦截器中处理
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getContractList({
      keyword: searchKeyword.value.trim() || undefined,
      current: currentPage.value,
      size: pageSize.value
    })
    contracts.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.list-container {
  max-width: 1200px;
  margin: 0 auto;
}

.list-card {
  border-radius: 8px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
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
