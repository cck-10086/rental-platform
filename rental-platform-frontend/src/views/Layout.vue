<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo-area">
        <el-icon :size="28" color="#409eff"><House /></el-icon>
        <span class="logo-title">租房合同审查平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
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
          <span>AI租房顾问</span>
        </el-menu-item>

        <!-- 管理后台（仅管理员可见） -->
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
        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-area">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
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
.layout-aside {
  background-color: #304156;
  overflow-y: auto;
  overflow-x: hidden;
}
.logo-area {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.logo-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
}
.sidebar-menu {
  border-right: none;
}
.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  z-index: 1;
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
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}
.user-area:hover {
  background: #f5f7fa;
}
.username {
  font-size: 14px;
  color: #303133;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.layout-main {
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>
