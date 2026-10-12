<template>
  <div class="login-container">
    <main class="auth-card">
      <div class="auth-brand">
        <span class="auth-brand__mark"><el-icon><House /></el-icon></span>
        <span class="auth-brand__name">租住智审</span>
      </div>

      <header class="auth-heading">
        <h1>智能租房合同审查平台</h1>
        <p>登录你的账户，管理合同审查与租住事务</p>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top" status-icon>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">还没有账号？<router-link to="/register" class="link">创建账号</router-link></div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { House, Lock, User } from '@lucide/vue'
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
/* 单列窄表单:居中偏上、轻边框、小圆角、无装饰 */
.login-container {
  display: flex;
  min-height: 100dvh;
  align-items: center;
  justify-content: center;
  padding: 32px 20px calc(32px + env(safe-area-inset-bottom));
  background: var(--rp-bg);
}

.auth-card {
  width: min(100%, 400px);
  padding: 32px;
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-card);
  background: var(--rp-surface);
  animation: auth-in .25s var(--rp-ease);
}

.auth-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 28px;
}
.auth-brand__mark {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-btn);
  background: var(--rp-surface-muted);
  color: var(--rp-primary);
  font-size: 15px;
}
.auth-brand__name { color: var(--rp-text-secondary); font-size: 13px; font-weight: 600; }

.auth-heading { margin-bottom: 24px; }
.auth-heading h1 {
  margin: 0;
  color: var(--rp-text);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.35;
}
.auth-heading p { margin: 6px 0 0; color: var(--rp-text-muted); font-size: 13px; line-height: 1.5; }

.auth-card :deep(.el-form-item) { margin-bottom: 18px; }
.auth-card :deep(.el-form-item__label) { margin-bottom: 6px; line-height: 1.3; }
.auth-card :deep(.el-input__wrapper) { min-height: 38px; }
.auth-card :deep(.el-form-item:last-child) { margin-top: 6px; margin-bottom: 0; }

.login-btn { width: 100%; min-height: 38px; justify-content: center; }

.login-footer { margin-top: 20px; color: var(--rp-text-muted); font-size: 13px; text-align: center; }
.link { color: var(--rp-primary); font-weight: 500; text-decoration: none; }
.link:hover { text-decoration: underline; }

@keyframes auth-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }

@media (max-width: 480px) {
  .auth-card { padding: 24px 20px; border: 0; background: transparent; }
}
</style>
