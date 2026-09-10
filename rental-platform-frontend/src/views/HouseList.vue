<template>
  <div class="list-container">
    <el-card class="list-card" shadow="hover">
      <!-- 顶部操作 -->
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 添加房源
        </el-button>
      </div>

      <!-- 房源列表 -->
      <el-table :data="houseList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="rentPrice" label="月租金(元)" width="110" />
        <el-table-column prop="deposit" label="押金(元)" width="100" />
        <el-table-column prop="area" label="面积(m²)" width="100" />
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
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-popconfirm
              title="确定要删除这套房源吗？"
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

    <!-- 添加/编辑房源弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑房源' : '添加房源'"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="房源标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入房源标题" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="月租金(元)" prop="rentPrice">
          <el-input-number v-model="form.rentPrice" :min="0" :precision="2" style="width: 100%" placeholder="请输入月租金" />
        </el-form-item>
        <el-form-item label="押金(元)" prop="deposit">
          <el-input-number v-model="form.deposit" :min="0" :precision="2" style="width: 100%" placeholder="请输入押金" />
        </el-form-item>
        <el-form-item label="面积(m²)" prop="area">
          <el-input-number v-model="form.area" :min="0" :precision="2" style="width: 100%" placeholder="请输入面积" />
        </el-form-item>
        <el-form-item label="户型" prop="rooms">
          <el-input-number v-model="form.rooms" :min="1" :max="10" style="width: 100%" placeholder="请输入室数" />
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input-number v-model="form.floor" :min="1" style="width: 100%" placeholder="请输入楼层" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" :rows="3" placeholder="请输入备注" />
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { addHouse, updateHouse, getHouseList, deleteHouse } from '@/api/house'

const loading = ref(false)
const submitting = ref(false)
const houseList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = ref({
  title: '',
  address: '',
  rentPrice: null,
  deposit: null,
  area: null,
  rooms: 1,
  floor: 1,
  contactName: '',
  contactPhone: '',
  notes: ''
})

const rules = {
  title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  rentPrice: [{ required: true, message: '请输入月租金', trigger: 'blur' }],
  deposit: [{ required: true, message: '请输入押金', trigger: 'blur' }],
  area: [{ required: true, message: '请输入面积', trigger: 'blur' }],
  rooms: [{ required: true, message: '请输入户型室数', trigger: 'blur' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const statusType = (status) => {
  const map = { viewing: 'warning', signed: 'success', cancelled: 'info' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { viewing: '待出租', signed: '已签约', cancelled: '已取消' }
  return map[status] || '未知'
}

const resetForm = () => {
  form.value = {
    title: '',
    address: '',
    rentPrice: null,
    deposit: null,
    area: null,
    rooms: 1,
    floor: 1,
    contactName: '',
    contactPhone: '',
    notes: ''
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
    title: row.title || '',
    address: row.address || '',
    rentPrice: row.rentPrice ?? null,
    deposit: row.deposit ?? null,
    area: row.area ?? null,
    rooms: row.rooms ?? 1,
    floor: row.floor ?? 1,
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    notes: row.notes || ''
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
      await updateHouse({ id: editingId.value, ...form.value })
      ElMessage.success('房源信息更新成功')
    } else {
      await addHouse(form.value)
      ElMessage.success('房源添加成功')
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
    await deleteHouse(row.id)
    ElMessage.success('删除成功')
    houseList.value = houseList.value.filter(h => h.id !== row.id)
  } catch {
    // 错误已在拦截器中处理
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getHouseList({ current: currentPage.value, size: pageSize.value })
    houseList.value = res.data.records || []
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
