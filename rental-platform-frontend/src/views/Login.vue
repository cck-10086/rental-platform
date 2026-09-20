<template>
  <div class="login-container">
    <el-card class="login-card" shadow="never">
      <div class="login-logo">
        <el-icon :size="20" color="#ffffff"><House /></el-icon>
      </div>
      <h1 class="login-title">智能租房合同审查平台</h1>
      <p class="login-subtitle">登录您的账户</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" status-icon>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        还没有账号？<router-link to="/register" class="link">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, House } from '@element-plus/icons-vue'
import { login } from '@/api/user'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    userStore.setToken(res.data.token)
    userStore.setUser(res.data)
    router.push('/dashboard')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F6F8FC;
  /* 三团主色系柔光(蓝 + 浅天蓝),浓度提到肉眼可辨,玻璃卡透出层次 */
  background-image:
    radial-gradient(900px circle at 12% 18%, rgba(37, 99, 235, 0.16), transparent 55%),
    radial-gradient(700px circle at 88% 82%, rgba(37, 99, 235, 0.12), transparent 55%),
    radial-gradient(560px circle at 82% 10%, rgba(14, 165, 233, 0.12), transparent 60%);
  background-attachment: fixed;
  padding: 24px;
}
.login-card {
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
  animation: login-card-in 0.55s cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes login-card-in {
  from { opacity: 0; transform: translateY(14px) scale(0.985); }
  to { opacity: 1; transform: none; }
}
.login-logo {
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
.login-title {
  text-align: center;
  font-size: 21px;
  color: #0F172A;
  font-weight: 700;
  letter-spacing: 0.01em;
  margin-bottom: 8px;
}
.login-subtitle {
  text-align: center;
  color: #64748B;
  margin-bottom: 30px;
  font-size: 13px;
}
.login-btn {
  width: 100%;
  border-radius: 10px;
}
.login-footer {
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
