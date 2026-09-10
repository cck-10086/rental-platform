<template>
  <div class="profile-container">
    <el-card class="profile-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="card-title">个人信息</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="profile-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            <el-icon><Check /></el-icon> 保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 退出登录 -->
    <el-card class="logout-card" shadow="hover">
      <el-button type="danger" @click="handleLogout">
        <el-icon><SwitchButton /></el-icon> 退出登录
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, SwitchButton } from '@element-plus/icons-vue'
import { updateProfile, getUserInfo } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const saving = ref(false)

const form = ref({
  username: '',
  nickname: '',
  phone: ''
})

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const handleSave = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateProfile({
      nickname: form.value.nickname,
      phone: form.value.phone
    })
    ElMessage.success('个人信息修改成功')
    // 同步更新 store 中的用户信息
    if (userStore.user) {
      userStore.user.nickname = form.value.nickname
      userStore.user.phone = form.value.phone
    }
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    })
    .catch(() => {})
}

const fetchProfile = async () => {
  try {
    const res = await getUserInfo()
    const data = res.data || res
    form.value = {
      username: data.username || '',
      nickname: data.nickname || '',
      phone: data.phone || ''
    }
    if (userStore.user) {
      Object.assign(userStore.user, data)
    }
  } catch {
    // 错误已在拦截器中处理
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 8px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.profile-form {
  max-width: 450px;
}

.logout-card {
  border-radius: 8px;
  text-align: center;
}
</style>
