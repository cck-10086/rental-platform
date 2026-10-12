<template>
  <div class="upload-container">
    <el-card class="upload-card" shadow="never">
      <template #header>
        <div class="card-heading">
          <span class="card-title">上传合同文件</span>
          <small>系统将在上传后自动进入 AI 审查流程</small>
        </div>
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
        <el-icon class="upload-icon" :size="64"><CloudUpload /></el-icon>
        <div class="upload-text">
          <em>将合同文件拖到此处，或点击上传</em>
        </div>
        <div class="upload-hint">
          支持 PDF、Word、TXT 格式，文件大小不超过20MB
        </div>
      </el-upload>

      <div v-if="fileReady" class="selected-file">
        <div class="selected-file__icon"><el-icon><FileText /></el-icon></div>
        <div class="selected-file__copy">
          <strong>{{ selectedFileName }}</strong>
          <span>{{ selectedFileSize }}</span>
        </div>
        <el-tag type="success" size="small">已选择</el-tag>
      </div>

      <el-progress v-if="uploading" class="upload-progress" :percentage="uploadProgress" :stroke-width="6" />
      <el-alert v-if="uploadError" class="upload-error" type="error" :title="uploadError" show-icon :closable="false" />

      <!-- 合同标题 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="upload-form">
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
import { CloudUpload, FileText, Upload } from '@lucide/vue'
import { uploadContract } from '@/api/contract'

const router = useRouter()

const uploadRef = ref(null)
const formRef = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadError = ref('')
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
const selectedFileName = computed(() => fileList.value[0]?.name || '')
const selectedFileSize = computed(() => {
  const size = fileList.value[0]?.size || 0
  return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(2)} MB` : `${Math.max(1, Math.round(size / 1024))} KB`
})

const handleFileChange = (file) => {
  uploadError.value = ''
  if (!beforeUpload(file.raw || file)) {
    uploadRef.value?.clearFiles()
    fileList.value = []
    return
  }
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
  uploadProgress.value = 0
  uploadError.value = ''
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', form.title)

    const res = await uploadContract(formData, event => {
      if (event.total) uploadProgress.value = Math.min(99, Math.round((event.loaded / event.total) * 100))
    })
    uploadProgress.value = 100
    ElMessage.success('合同上传成功，即将跳转到审查页面')
    const contractId = res.data?.id || res.data
    router.push(`/contract/review/${contractId}`)
  } catch {
    uploadError.value = '合同上传失败，请确认文件格式和网络状态后重试。'
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

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--rp-text);
}
.card-heading { display: flex; flex-direction: column; gap: 4px; }
.card-heading small { color: var(--rp-text-muted); font-size: 11px; font-weight: 400; }

.upload-area {
  margin-bottom: 24px;
}

.upload-area :deep(.el-upload-dragger) {
  border: 1.5px dashed #D6D3D1;
  border-radius: var(--rp-radius-card);
  background: var(--rp-surface-muted);
  padding: 44px 24px;
  transition: border-color var(--rp-duration) var(--rp-ease), background var(--rp-duration) var(--rp-ease);
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: var(--rp-primary);
  background: var(--rp-primary-light);
}

.upload-icon {
  color: var(--rp-text-muted);
  margin-bottom: 14px;
}

.upload-text {
  font-size: 14px;
  color: var(--rp-text-secondary);
  margin-bottom: 8px;
}

.upload-text em {
  font-style: normal;
  color: var(--rp-primary);
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: var(--rp-text-muted);
  margin-top: 4px;
}

.upload-form {
  margin-top: 22px;
}

.selected-file { display: flex; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid var(--rp-border); border-radius: var(--rp-radius-btn); background: var(--rp-surface-muted); }
.selected-file__icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: var(--rp-radius-btn); background: var(--rp-surface); color: var(--rp-primary); }
.selected-file__copy { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }
.selected-file__copy strong { overflow: hidden; color: var(--rp-text); font-size: 12px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }
.selected-file__copy span { color: var(--rp-text-muted); font-size: 10px; }
.upload-progress, .upload-error { margin-top: 14px; }

.upload-actions {
  text-align: center;
  margin-top: 8px;
}

.upload-actions .el-button {
  min-width: 180px;
}

@media (max-width: 767px) {
  .upload-area :deep(.el-upload-dragger) { padding: 34px 16px; }
  .upload-icon { font-size: 48px !important; }
  .upload-actions .el-button { width: 100%; }
}
</style>
