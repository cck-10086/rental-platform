<template>
  <div class="upload-container">
    <el-card class="upload-card" shadow="hover">
      <template #header>
        <span class="card-title">上传合同文件</span>
      </template>

      <!-- 上传区域 -->
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :before-upload="beforeUpload"
        accept=".pdf,.doc,.docx,.txt"
      >
        <el-icon class="upload-icon" :size="64"><UploadFilled /></el-icon>
        <div class="upload-text">
          <em>将合同文件拖到此处，或点击上传</em>
        </div>
        <div class="upload-hint">
          支持 PDF、Word、TXT 格式，文件大小不超过20MB
        </div>
      </el-upload>

      <!-- 合同标题 -->
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="upload-form">
        <el-form-item label="合同标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入合同标题，如：XX小区2024年租房合同"
            clearable
          />
        </el-form-item>
      </el-form>

      <!-- 上传按钮 -->
      <div class="upload-actions">
        <el-button
          type="primary"
          size="large"
          :loading="uploading"
          :disabled="!fileReady"
          @click="handleUpload"
        >
          <el-icon><Upload /></el-icon> 开始上传
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Upload } from '@element-plus/icons-vue'
import { uploadContract } from '@/api/contract'

const router = useRouter()

const uploadRef = ref(null)
const formRef = ref(null)
const uploading = ref(false)
const fileList = ref([])

const form = reactive({
  title: ''
})

const rules = {
  title: [{ required: true, message: '请输入合同标题', trigger: 'blur' }]
}

const fileReady = computed(() => {
  return fileList.value.length > 0
})

const handleFileChange = (file) => {
  fileList.value = [file]
}

const handleFileRemove = () => {
  fileList.value = []
}

const beforeUpload = (file) => {
  const maxSize = 20 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过20MB')
    return false
  }
  return true
}

const handleUpload = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  const file = fileList.value[0].raw
  if (!file) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', form.title)

    const res = await uploadContract(formData)
    ElMessage.success('合同上传成功，即将跳转到审查页面')
    const contractId = res.data?.id || res.data
    router.push(`/contract/review/${contractId}`)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-container {
  max-width: 680px;
  margin: 0 auto;
}

.upload-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.upload-area {
  margin-bottom: 24px;
}

.upload-area :deep(.el-upload-dragger) {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 48px 24px;
  transition: border-color 0.3s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

.upload-icon {
  color: #c0c4cc;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.upload-text em {
  font-style: normal;
  color: #409eff;
}

.upload-hint {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.upload-form {
  margin-top: 8px;
}

.upload-actions {
  text-align: center;
  margin-top: 8px;
}

.upload-actions .el-button {
  min-width: 180px;
}
</style>
