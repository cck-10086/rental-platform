<template>
  <div class="list-container">
    <el-card class="list-card" shadow="hover">
      <!-- 顶部操作 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 添加费用
        </el-button>
      </div>

      <!-- 费用列表 -->
      <el-table :data="expenseList" stripe v-loading="loading" style="width: 100%">
        <el-table-column label="费用类型" width="120">
          <template #default="{ row }">
            <el-tag :type="expenseTypeColor(row.expenseType)" size="small">
              {{ row.expenseType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额(元)" width="110" />
        <el-table-column prop="billMonth" label="账单月份" width="120" />
        <el-table-column prop="dueDate" label="缴费截止日期" width="130" />
        <el-table-column label="缴费状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPaid ? 'success' : 'danger'" size="small">
              {{ row.isPaid ? '已缴' : '未缴' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              v-if="!row.isPaid"
              type="success"
              link
              size="small"
              @click="handleMarkPaid(row)"
            >
              <el-icon><Check /></el-icon> 标记已缴
            </el-button>
            <el-popconfirm
              title="确定要删除这笔费用吗？"
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
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 添加/编辑费用弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑费用' : '添加费用'"
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
        <el-form-item label="费用类型" prop="expenseType">
          <el-select v-model="form.expenseType" placeholder="请选择费用类型" style="width: 100%">
            <el-option label="水费" value="水" />
            <el-option label="电费" value="电" />
            <el-option label="燃气费" value="燃气" />
            <el-option label="物业费" value="物业" />
            <el-option label="网络费" value="网络" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" placeholder="请输入金额" />
        </el-form-item>
        <el-form-item label="账单月份" prop="billMonth">
          <el-date-picker
            v-model="form.billMonth"
            type="month"
            placeholder="请选择账单月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="缴费截止日期" prop="dueDate">
          <el-date-picker
            v-model="form.dueDate"
            type="date"
            placeholder="请选择缴费截止日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Edit, Delete, Check } from '@element-plus/icons-vue'
import { addExpense, updateExpense, getExpenseList, markPaid, deleteExpense } from '@/api/expense'

const loading = ref(false)
const submitting = ref(false)
const expenseList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = ref({
  expenseType: '',
  amount: null,
  billMonth: '',
  dueDate: '',
  remark: ''
})

const rules = {
  expenseType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  billMonth: [{ required: true, message: '请选择账单月份', trigger: 'change' }],
  dueDate: [{ required: true, message: '请选择缴费截止日期', trigger: 'change' }]
}

const expenseTypeColor = (type) => {
  const map = {
    '水': '',
    '电': 'warning',
    '燃气': 'danger',
    '物业': 'success',
    '网络': 'info',
    '其他': ''
  }
  return map[type] || ''
}

const resetForm = () => {
  form.value = {
    expenseType: '',
    amount: null,
    billMonth: '',
    dueDate: '',
    remark: ''
  }
}

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleEdit = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    expenseType: row.expenseType || '',
    amount: row.amount ?? null,
    billMonth: row.billMonth || '',
    dueDate: row.dueDate || '',
    remark: row.remark || ''
  }
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
      await updateExpense({ id: editingId.value, ...form.value })
      ElMessage.success('费用信息更新成功')
    } else {
      await addExpense(form.value)
      ElMessage.success('费用添加成功')
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

const handleMarkPaid = async (row) => {
  try {
    await markPaid(row.id)
    ElMessage.success('已标记为已缴')
    row.isPaid = 1
  } catch {
    // 错误已在拦截器中处理
  }
}

const handleDelete = async (row) => {
  try {
    await deleteExpense(row.id)
    ElMessage.success('删除成功')
    expenseList.value = expenseList.value.filter(e => e.id !== row.id)
  } catch {
    // 错误已在拦截器中处理
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getExpenseList({ current: currentPage.value, size: pageSize.value })
    expenseList.value = res.data.records || []
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

.toolbar {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
