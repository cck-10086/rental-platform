import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { title: '注册' } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'contract/upload', name: 'ContractUpload', component: () => import('@/views/ContractUpload.vue'), meta: { title: '合同上传' } },
      { path: 'contract/review/:id', name: 'ContractReview', component: () => import('@/views/ContractReview.vue'), meta: { title: '审查报告' } },
      { path: 'contract/list', name: 'ContractList', component: () => import('@/views/ContractList.vue'), meta: { title: '合同管理' } },
      { path: 'house/list', name: 'HouseList', component: () => import('@/views/HouseList.vue'), meta: { title: '房源管理' } },
      { path: 'expense/list', name: 'ExpenseList', component: () => import('@/views/ExpenseList.vue'), meta: { title: '费用管理' } },
      { path: 'repair/list', name: 'RepairList', component: () => import('@/views/RepairList.vue'), meta: { title: '维修报备' } },
      { path: 'move-out', name: 'MoveOut', component: () => import('@/views/MoveOut.vue'), meta: { title: '退租管理' } },
      { path: 'ai-chat', name: 'AiChat', component: () => import('@/views/AiChat.vue'), meta: { title: 'AI顾问' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '个人信息' } },
      // 管理后台（仅 admin）
      {
        path: 'admin/stats',
        name: 'AdminStats',
        component: () => import('@/views/AdminStats.vue'),
        meta: { title: '数据统计', requiresAdmin: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/AdminUsers.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      },
      {
        path: 'admin/data',
        name: 'AdminData',
        component: () => import('@/views/AdminData.vue'),
        meta: { title: '全局数据', requiresAdmin: true }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页；管理员页面校验角色
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录')
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    next('/dashboard')
  } else if (to.meta.requiresAdmin && token) {
    const { useUserStore } = await import('@/store/user')
    const userStore = useUserStore()
    if (!userStore.user) {
      try {
        await userStore.fetchUser()
      } catch {
        next('/login')
        return
      }
    }
    if (userStore.user?.role !== 'admin') {
      ElMessage.warning('无权限访问')
      next('/dashboard')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
