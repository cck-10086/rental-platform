<template>
  <div class="register-container">
    <main class="auth-card">
      <div class="auth-brand">
        <span class="auth-brand__mark"><el-icon><House /></el-icon></span>
        <span class="auth-brand__name">租住智审</span>
      </div>

      <header class="auth-heading">
        <h1>智能租房合同审查平台</h1>
        <p>创建账户，开始管理合同、房源与租住记录</p>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top" status-icon>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="CircleUser" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少 6 位密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="用于账号联系" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="register-btn" :loading="loading" @click="handleRegister">创建账户</el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">已有账号？<router-link to="/login" class="link">返回登录</router-link></div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleUser, House, Lock, Phone, User } from '@lucide/vue'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '', phone: '' })

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码输入不一致'))
  else callback()
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
    await register({ username: form.username, nickname: form.nickname, password: form.password, phone: form.phone })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已在请求拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 单列窄表单:与登录页同构,轻边框、小圆角、无装饰 */
.register-container {
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
  margin-bottom: 24px;
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

.auth-heading { margin-bottom: 20px; }
.auth-heading h1 {
  margin: 0;
  color: var(--rp-text);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.35;
}
.auth-heading p { margin: 6px 0 0; color: var(--rp-text-muted); font-size: 13px; line-height: 1.5; }

.auth-card :deep(.el-form-item) { margin-bottom: 16px; }
.auth-card :deep(.el-form-item__label) { margin-bottom: 6px; line-height: 1.3; }
.auth-card :deep(.el-input__wrapper) { min-height: 38px; }
.auth-card :deep(.el-form-item:last-child) { margin-top: 6px; margin-bottom: 0; }

.register-btn { width: 100%; min-height: 38px; justify-content: center; }

.register-footer { margin-top: 18px; color: var(--rp-text-muted); font-size: 13px; text-align: center; }
.link { color: var(--rp-primary); font-weight: 500; text-decoration: none; }
.link:hover { text-decoration: underline; }

@keyframes auth-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }

@media (max-width: 480px) {
  .auth-card { padding: 24px 20px; border: 0; background: transparent; }
}
</style>
