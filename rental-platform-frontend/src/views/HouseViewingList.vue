<template>
  <div class="list-container">
    <div class="rp-page-header">
      <div>
        <h2>看房记录</h2>
        <p class="rp-page-desc">记录看房时间、房源印象和评分，便于后续比较</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 添加记录
      </el-button>
    </div>

    <el-card class="list-card" shadow="never">
      <el-skeleton v-if="loading && viewingList.length === 0" :rows="6" animated />

      <template v-else>
        <el-table class="rp-desktop-table" :data="viewingList" stripe style="width: 100%">
          <el-table-column label="房源" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ houseLabel(row.houseId) }}</template>
          </el-table-column>
          <el-table-column label="看房时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.viewingTime) }}</template>
          </el-table-column>
          <el-table-column prop="impression" label="总体印象" min-width="220" show-overflow-tooltip />
          <el-table-column label="评分" width="150">
            <template #default="{ row }">
              <el-rate :model-value="row.score || 0" disabled />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                <el-icon><Pencil /></el-icon> 编辑
              </el-button>
              <el-popconfirm
                title="确定要删除这条看房记录吗？"
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
          <article v-for="viewing in viewingList" :key="viewing.id" class="rp-mobile-card">
            <div class="rp-mobile-card__header">
              <h3 class="rp-mobile-card__title">{{ houseLabel(viewing.houseId) }}</h3>
              <el-rate :model-value="viewing.score || 0" disabled size="small" />
            </div>
            <div class="rp-mobile-card__meta">
              <span>{{ formatDateTime(viewing.viewingTime) }}</span>
              <span>{{ viewing.impression || '暂无总体印象' }}</span>
              <span v-if="viewing.pros">优点：{{ viewing.pros }}</span>
              <span v-if="viewing.cons">不足：{{ viewing.cons }}</span>
            </div>
            <div class="rp-mobile-card__footer">
              <el-button link type="primary" @click="handleEdit(viewing)"><el-icon><Pencil /></el-icon>编辑</el-button>
              <el-popconfirm title="确定要删除这条看房记录吗？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(viewing)">
                <template #reference><el-button link type="danger"><el-icon><Trash2 /></el-icon>删除</el-button></template>
              </el-popconfirm>
            </div>
          </article>
          <el-empty v-if="viewingList.length === 0" description="暂无看房记录，点击上方按钮添加" :image-size="72" />
        </div>
      </template>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑看房记录' : '添加看房记录'"
      width="600px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="房源" prop="houseId">
          <el-select v-model="form.houseId" placeholder="请选择房源" filterable style="width: 100%">
            <el-option v-for="house in houseList" :key="house.id" :label="house.title" :value="house.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="看房时间" prop="viewingTime">
          <el-date-picker
            v-model="form.viewingTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择看房时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总体印象" prop="impression">
          <el-input v-model="form.impression" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="记录房屋整体感受" />
        </el-form-item>
        <el-form-item label="优点">
          <el-input v-model="form.pros" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="记录位置、采光、配套等优点" />
        </el-form-item>
        <el-form-item label="不足">
          <el-input v-model="form.cons" type="textarea" :rows="2" maxlength="1000" show-word-limit placeholder="记录噪声、设施、价格等不足" />
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-rate v-model="form.score" />
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
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Pencil, Plus, Trash2 } from '@lucide/vue'
import { getHouseList } from '@/api/house'
import {
  addHouseViewing,
  deleteHouseViewing,
  getHouseViewingList,
  updateHouseViewing
} from '@/api/houseViewing'

const loading = ref(false)
const submitting = ref(false)
const viewingList = ref([])
const houseList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const emptyForm = () => ({
  houseId: null,
  viewingTime: '',
  impression: '',
  pros: '',
  cons: '',
  score: 0
})

const form = ref(emptyForm())

const rules = {
  houseId: [{ required: true, message: '请选择房源', trigger: 'change' }],
  viewingTime: [{ required: true, message: '请选择看房时间', trigger: 'change' }],
  impression: [{ required: true, message: '请填写总体印象', trigger: 'blur' }],
  score: [{ required: true, type: 'number', min: 1, message: '请至少选择一星', trigger: 'change' }]
}

const houseLabel = (houseId) => houseList.value.find(house => house.id === houseId)?.title || `房源 #${houseId}`

const formatDateTime = (value) => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

const resetForm = () => {
  form.value = emptyForm()
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
    houseId: row.houseId,
    viewingTime: row.viewingTime || '',
    impression: row.impression || '',
    pros: row.pros || '',
    cons: row.cons || '',
    score: row.score || 0
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
      await updateHouseViewing({ id: editingId.value, ...form.value })
      ElMessage.success('看房记录更新成功')
    } else {
      await addHouseViewing(form.value)
      ElMessage.success('看房记录添加成功')
    }
    dialogVisible.value = false
    await fetchViewingList()
  } catch {
    // 错误已在请求拦截器中提示
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await deleteHouseViewing(row.id)
    ElMessage.success('看房记录删除成功')
    viewingList.value = viewingList.value.filter(item => item.id !== row.id)
  } catch {
    // 错误已在请求拦截器中提示
  }
}

const fetchHouses = async () => {
  const res = await getHouseList({ current: 1, size: 100 })
  houseList.value = res.data.records || []
}

const fetchViewingList = async () => {
  const res = await getHouseViewingList()
  viewingList.value = res.data || []
}

const fetchData = async () => {
  loading.value = true
  try {
    await Promise.all([fetchHouses(), fetchViewingList()])
  } catch {
    // 错误已在请求拦截器中提示
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.list-container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
