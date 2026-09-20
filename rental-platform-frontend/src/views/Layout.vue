<template>
  <el-container class="layout-container">
    <!-- 左侧垂直导航:浅色毛玻璃 -->
    <el-aside width="228px" class="layout-aside">
      <div class="logo-area">
        <div class="logo-mark">
          <el-icon :size="16" color="#ffffff"><House /></el-icon>
        </div>
        <span class="logo-title">租房平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="transparent"
        text-color="#475569"
        active-text-color="#2563EB"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>工作台</span>
        </el-menu-item>

        <el-sub-menu index="contract">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>合同管理</span>
          </template>
          <el-menu-item index="/contract/upload">上传合同</el-menu-item>
          <el-menu-item index="/contract/list">我的合同</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/house/list">
          <el-icon><OfficeBuilding /></el-icon>
          <span>房源管理</span>
        </el-menu-item>

        <el-menu-item index="/expense/list">
          <el-icon><Money /></el-icon>
          <span>费用管理</span>
        </el-menu-item>

        <el-menu-item index="/repair/list">
          <el-icon><Tools /></el-icon>
          <span>维修报备</span>
        </el-menu-item>

        <el-menu-item index="/move-out">
          <el-icon><Check /></el-icon>
          <span>退租管理</span>
        </el-menu-item>

        <el-menu-item index="/ai-chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 租房顾问</span>
        </el-menu-item>

        <!-- 管理后台(仅管理员可见) -->
        <el-sub-menu v-if="userStore.user?.role === 'admin'" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </template>
          <el-menu-item index="/admin/stats">数据统计</el-menu-item>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/data">全局数据</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-area">
              <el-avatar :size="30" :icon="UserFilled" class="user-avatar" />
              <span class="username">{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><User /></el-icon> 个人信息
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  House, HomeFilled, Document, OfficeBuilding, Money, Tools,
  Check, ChatDotRound, UserFilled, ArrowDown, User, SwitchButton, Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '工作台')

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  userStore.fetchUser()
})
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

/* —— 左侧毛玻璃侧栏 —— */
.layout-aside {
  background: var(--rp-glass, rgba(255, 255, 255, 0.78));
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-right: 1px solid #E2E8F0;
  overflow-y: auto;
  overflow-x: hidden;
}

.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid #F1F5F9;
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #2563EB;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  white-space: nowrap;
  letter-spacing: 0.01em;
}

/* —— 菜单:胶囊项 + 激活态浅蓝底 + 左侧主色指示条 —— */
.sidebar-menu {
  border-right: none;
  padding: 10px 10px 20px;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 42px;
  line-height: 42px;
  border-radius: 9px;
  margin-bottom: 2px;
  transition: background 180ms cubic-bezier(0.16, 1, 0.3, 1), color 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  background: #F8FAFC;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: #EFF6FF;
  font-weight: 500;
  position: relative;
}

.sidebar-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 2px;
  background: #2563EB;
}

.sidebar-menu :deep(.el-sub-menu .el-menu-item) {
  height: 36px;
  line-height: 36px;
  padding-left: 46px !important;
  font-size: 13px;
  min-width: 0;
}

.sidebar-menu :deep(.el-menu-item .el-icon),
.sidebar-menu :deep(.el-sub-menu__title .el-icon) {
  color: inherit;
}

/* —— 顶栏:毛玻璃 + 左侧页面标题 —— */
.layout-header {
  height: 60px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-bottom: 1px solid #E2E8F0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  position: relative;
  z-index: 2;
}

.page-title {
  font-size: 15px;
  font-weight: 600;
  color: #1E293B;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: background 180ms cubic-bezier(0.16, 1, 0.3, 1), border-color 180ms cubic-bezier(0.16, 1, 0.3, 1);
  color: #475569;
  outline: none;
}

.user-area:hover {
  background: #F8FAFC;
  border-color: #E2E8F0;
}

.user-avatar {
  background: #EFF6FF;
  color: #2563EB;
}

.username {
  font-size: 13px;
  color: #334155;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: #94A3B8;
}

/* —— 主内容区:浅底大留白 —— */
.layout-main {
  background: #F8FAFC;
  min-height: calc(100vh - 60px);
  padding: 24px 28px;
}

/* 窄屏基础响应式:侧栏收窄 */
@media (max-width: 900px) {
  .layout-aside { width: 64px !important; }
  .logo-title { display: none; }
  .sidebar-menu :deep(.el-menu-item span),
  .sidebar-menu :deep(.el-sub-menu__title span) { display: none; }
  .layout-main { padding: 16px; }
}
</style>
