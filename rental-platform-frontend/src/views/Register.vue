<template>
  <div class="register-container">
    <el-card class="register-card" shadow="never">
      <div class="register-logo">
        <el-icon :size="20" color="#ffffff"><House /></el-icon>
      </div>
      <h1 class="register-title">智能租房合同审查平台</h1>
      <p class="register-subtitle">创建您的账户</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" status-icon>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="UserFilled" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        已有账号？<router-link to="/login" class="link">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, UserFilled, Lock, Phone, House } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({
      username: form.username,
      nickname: form.nickname,
      password: form.password,
      phone: form.phone
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F6F8FC;
  /* 三团主色系柔光,与登录页同一套视觉 */
  background-image:
    radial-gradient(900px circle at 12% 18%, rgba(37, 99, 235, 0.16), transparent 55%),
    radial-gradient(700px circle at 88% 82%, rgba(37, 99, 235, 0.12), transparent 55%),
    radial-gradient(560px circle at 82% 10%, rgba(14, 165, 233, 0.12), transparent 60%);
  background-attachment: fixed;
  padding: 24px;
}
.register-card {
  width: 420px;
  border-radius: 18px;
  padding: 20px 16px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow:
    0 24px 48px -20px rgba(37, 99, 235, 0.28),
    0 2px 6px rgba(15, 23, 42, 0.04);
  animation: register-card-in 0.55s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes register-card-in {
  from { opacity: 0; transform: translateY(14px) scale(0.985); }
  to { opacity: 1; transform: none; }
}
.register-logo {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 6px auto 16px;
  box-shadow: 0 10px 22px -8px rgba(37, 99, 235, 0.55);
}
.register-title {
  text-align: center;
  font-size: 21px;
  color: #0F172A;
  font-weight: 700;
  letter-spacing: 0.01em;
  margin-bottom: 8px;
}
.register-subtitle {
  text-align: center;
  color: #64748B;
  margin-bottom: 30px;
  font-size: 13px;
}
.register-btn {
  width: 100%;
  border-radius: 10px;
}
.register-footer {
  text-align: center;
  font-size: 13px;
  color: #64748B;
}
.link {
  color: #2563EB;
  text-decoration: none;
  font-weight: 500;
}
.link:hover {
  text-decoration: underline;
}
</style>
