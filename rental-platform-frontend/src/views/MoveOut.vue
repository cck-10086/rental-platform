<template>
  <div class="list-container">
    <!-- 页头 -->
    <div class="rp-page-header">
      <div>
        <h2>退租管理</h2>
        <p class="rp-page-desc">退租清单与押金结算记录,应退金额自动计算</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 创建退租清单
      </el-button>
    </div>

    <el-card class="list-card" shadow="never">
      <!-- 首屏骨架屏 -->
      <el-skeleton v-if="loading && moveOutList.length === 0" :rows="6" animated />

      <template v-else>
      <!-- 退租列表 -->
      <el-table class="rp-desktop-table" :data="moveOutList" stripe v-loading="loading" style="width: 100%">
        <el-table-column label="合同信息" min-width="220">
          <template #default="{ row }">
            <div class="contract-cell">
              <span class="contract-cell__title">{{ contractTitle(row.contractId) }}</span>
              <span class="contract-cell__id">合同ID {{ row.contractId || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="moveOutDate" label="退租日期" width="120" />
        <el-table-column prop="totalDeposit" label="押金总额(元)" width="120" />
        <el-table-column prop="deductionAmount" label="扣除金额(元)" width="120" />
        <el-table-column label="应退金额(元)" width="120">
          <template #default="{ row }">
            {{ calcRefund(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Pencil /></el-icon> 编辑
            </el-button>
            <el-popconfirm
              title="确定要删除这条退租记录吗？"
              confirm-button-text="确认"
              cancel-button-text="取消"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button type="danger" link size="small">
                  <el-icon><Trash2 /></el-icon> 删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="rp-mobile-list">
        <article v-for="item in moveOutList" :key="item.id" class="rp-mobile-card">
          <div class="rp-mobile-card__header">
            <h3 class="rp-mobile-card__title">{{ contractTitle(item.contractId) }}</h3>
            <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
          </div>
          <div class="rp-mobile-card__meta">
            <span>退租 {{ item.moveOutDate || '-' }}</span>
            <span>押金 ¥{{ item.totalDeposit || 0 }}</span>
            <span>扣除 ¥{{ item.deductionAmount || 0 }}</span>
            <strong>应退 ¥{{ calcRefund(item) }}</strong>
          </div>
          <div class="rp-mobile-card__footer">
            <el-button link type="primary" @click="handleEdit(item)"><el-icon><Pencil /></el-icon>编辑</el-button>
            <el-popconfirm title="确定要删除这条退租记录吗？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(item)">
              <template #reference><el-button link type="danger"><el-icon><Trash2 /></el-icon>删除</el-button></template>
            </el-popconfirm>
          </div>
        </article>
        <el-empty v-if="moveOutList.length === 0" description="暂无退租记录" :image-size="72" />
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
      </template>
    </el-card>

    <!-- 创建/编辑退租清单弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑退租清单' : '创建退租清单'"
      width="550px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item label="关联合同" prop="contractId">
          <el-select v-model="form.contractId" placeholder="请选择关联合同" style="width: 100%" filterable>
            <el-option
              v-for="c in contractOptions"
              :key="c.id"
              :label="c.title || c.contractNo || `合同${c.id}`"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="退租日期" prop="moveOutDate">
          <el-date-picker
            v-model="form.moveOutDate"
            type="date"
            placeholder="请选择退租日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="押金总额(元)" prop="totalDeposit">
          <el-input-number v-model="form.totalDeposit" :min="0" :precision="2" style="width: 100%" placeholder="请输入押金总额" />
        </el-form-item>
        <el-form-item label="扣除金额(元)" prop="deductionAmount">
          <el-input-number v-model="form.deductionAmount" :min="0" :precision="2" style="width: 100%" placeholder="请输入扣除金额" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Pencil, Trash2 } from '@lucide/vue'
import { createMoveOut, updateMoveOut, getMoveOutList, deleteMoveOut } from '@/api/moveOut'
import { getContractList } from '@/api/contract'

const loading = ref(false)
const submitting = ref(false)
const moveOutList = ref([])
const contractOptions = ref([])
// 合同ID到标题的映射，用于列表"合同信息"列展示合同标题
const contractTitleMap = ref({})
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = ref({
  contractId: null,
  moveOutDate: '',
  totalDeposit: null,
  deductionAmount: null,
  remark: ''
})

const rules = {
  contractId: [{ required: true, message: '请选择关联合同', trigger: 'change' }],
  moveOutDate: [{ required: true, message: '请选择退租日期', trigger: 'change' }],
  totalDeposit: [{ required: true, message: '请输入押金总额', trigger: 'blur' }],
  deductionAmount: [{ required: true, message: '请输入扣除金额', trigger: 'blur' }]
}

const calcRefund = (row) => {
  const total = Number(row.totalDeposit) || 0
  const deduction = Number(row.deductionAmount) || 0
  return (total - deduction).toFixed(2)
}

// 根据合同ID取标题，未匹配到时回退为"合同+ID"
const contractTitle = (id) => contractTitleMap.value[id] || `合同${id}`

const statusType = (status) => {
  const map = { pending: 'warning', completed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { pending: '待处理', completed: '已完成', cancelled: '已取消' }
  return map[status] || '未知'
}

const resetForm = () => {
  form.value = {
    contractId: null,
    moveOutDate: '',
    totalDeposit: null,
    deductionAmount: null,
    remark: ''
  }
}

const fetchContracts = async () => {
  try {
    const res = await getContractList({ size: 1000 })
    contractOptions.value = res.data.records || []
    // 建立 id -> 标题 映射，供"合同信息"列展示
    contractTitleMap.value = Object.fromEntries(
      contractOptions.value.map(c => [c.id, c.title || `合同${c.id}`])
    )
  } catch {
    // 错误已在拦截器中处理
  }
}

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  fetchContracts()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    contractId: row.contractId ?? null,
    moveOutDate: row.moveOutDate || '',
    totalDeposit: row.totalDeposit ?? null,
    deductionAmount: row.deductionAmount ?? null,
    remark: row.remark || ''
  }
  fetchContracts()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleDialogClosed = () => {
  resetForm()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateMoveOut({ id: editingId.value, ...form.value })
      ElMessage.success('退租清单更新成功')
    } else {
      await createMoveOut(form.value)
      ElMessage.success('退租清单创建成功')
    }
    dialogVisible.value = false
    resetForm()
    fetchList()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await deleteMoveOut(row.id)
    ElMessage.success('删除成功')
    moveOutList.value = moveOutList.value.filter(m => m.id !== row.id)
  } catch {
    // 错误已在拦截器中处理
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getMoveOutList({ current: currentPage.value, size: pageSize.value })
    moveOutList.value = res.data.records || []
    total.value = Number(res.data.total) || 0
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchContracts()
})
</script>

<style scoped>
.list-container {
  max-width: 1200px;
  margin: 0 auto;
}

.contract-cell__title {
  display: block;
  line-height: 1.4;
}

.contract-cell__id {
  display: block;
  margin-top: 2px;
  font-size: 12px;
  color: var(--rp-text-muted);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
