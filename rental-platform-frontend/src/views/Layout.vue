<template>
  <div class="app-shell" :class="{ 'is-chat-route': route.name === 'AiChat' }">
    <aside v-if="!isMobile" class="desktop-sidebar" :class="{ 'is-compact': isCompact }">
      <AppSidebar
        :active-menu="activeMenu"
        :collapsed="isCompact"
        :role="userStore.user?.role"
      />
    </aside>

    <section class="app-workspace">
      <header class="layout-header">
        <div class="header-left">
          <button v-if="isMobile" class="icon-button menu-trigger" type="button" aria-label="打开主导航" @click="mobileMenuOpen = true">
            <el-icon><Menu /></el-icon>
          </button>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click">
            <button class="user-area" type="button" aria-label="打开用户菜单">
              <el-avatar :size="26" class="user-avatar">
                {{ userInitial }}
              </el-avatar>
              <span class="user-copy">
                <strong class="username">{{ displayName }}</strong>
                <small>{{ userStore.user?.role === 'admin' ? '管理员' : '租住用户' }}</small>
              </span>
              <el-icon class="user-arrow"><ChevronDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><LogOut /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="layout-main">
        <router-view />
      </main>
    </section>

    <el-drawer
      v-model="mobileMenuOpen"
      direction="ltr"
      size="264px"
      :with-header="false"
      class="mobile-nav-drawer"
      aria-label="主导航"
    >
      <AppSidebar
        :active-menu="activeMenu"
        :role="userStore.user?.role"
        @navigate="mobileMenuOpen = false"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronDown, LogOut, Menu, User } from '@lucide/vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileMenuOpen = ref(false)
const isMobile = ref(false)
const isCompact = ref(false)
let mobileMedia
let compactMedia

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta?.title || '工作台')
const displayName = computed(() => userStore.user?.nickname || userStore.user?.username || '用户')
const userInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())

const syncViewport = () => {
  isMobile.value = mobileMedia.matches
  isCompact.value = compactMedia.matches
  if (!isMobile.value) mobileMenuOpen.value = false
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

watch(() => route.fullPath, () => {
  mobileMenuOpen.value = false
})

onMounted(() => {
  mobileMedia = window.matchMedia('(max-width: 767px)')
  compactMedia = window.matchMedia('(min-width: 768px) and (max-width: 1199px)')
  syncViewport()
  mobileMedia.addEventListener('change', syncViewport)
  compactMedia.addEventListener('change', syncViewport)
  userStore.fetchUser()
})

onBeforeUnmount(() => {
  mobileMedia?.removeEventListener('change', syncViewport)
  compactMedia?.removeEventListener('change', syncViewport)
})
</script>

<style scoped>
.app-shell {
  display: grid;
  min-height: 100dvh;
  grid-template-columns: 216px minmax(0, 1fr);
  overflow: hidden;
  background: var(--rp-bg);
}

/* 浅色工具侧栏:与内容区同底色,仅以细边线分隔 */
.desktop-sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  overflow: hidden;
  border-right: 1px solid var(--rp-border);
  background: var(--rp-sidebar-bg);
}

.desktop-sidebar.is-compact { width: 64px; }
.app-workspace { min-width: 0; height: 100dvh; overflow: hidden; }

/* 极简顶栏:纯色底 + 细下边线,仅页面名称与用户菜单 */
.layout-header {
  display: flex;
  height: 56px;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid var(--rp-border);
  background: var(--rp-bg);
}

.header-left, .header-right, .user-area { display: flex; align-items: center; }
.header-left { min-width: 0; gap: 10px; }
.page-title { margin: 0; color: var(--rp-text); font-size: 14px; font-weight: 600; white-space: nowrap; }

.icon-button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-btn);
  background: var(--rp-surface);
  color: var(--rp-text-secondary);
  cursor: pointer;
}

.user-area {
  gap: 8px;
  padding: 4px 6px 4px 4px;
  border: 1px solid transparent;
  border-radius: var(--rp-radius-btn);
  background: transparent;
  color: var(--rp-text-secondary);
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  transition: background var(--rp-duration) var(--rp-ease), border-color var(--rp-duration) var(--rp-ease);
}
.user-area:hover { border-color: var(--rp-border); background: var(--rp-surface); }
.user-avatar { background: var(--rp-primary-light); color: var(--rp-primary); font-size: 11px; font-weight: 600; }
.user-copy { display: flex; flex-direction: column; gap: 1px; }
.username { max-width: 120px; overflow: hidden; color: var(--rp-text); font-size: 12px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.user-copy small { color: var(--rp-text-muted); font-size: 10px; }
.user-arrow { color: var(--rp-text-muted); font-size: 12px; }

.layout-main {
  height: calc(100dvh - 56px);
  min-width: 0;
  overflow: auto;
  padding: 24px 28px 28px;
}
.is-chat-route .layout-main { overflow: hidden; padding: 20px 24px; }

:global(.mobile-nav-drawer .el-drawer__body) { padding: 0; background: var(--rp-sidebar-bg); }

@media (max-width: 1199px) and (min-width: 768px) {
  .app-shell { grid-template-columns: 64px minmax(0, 1fr); }
  .layout-header { padding-inline: 20px; }
  .layout-main { padding: 20px; }
  .is-chat-route .layout-main { padding: 16px; }
}

@media (max-width: 767px) {
  .app-shell { display: block; }
  .layout-header { height: 52px; padding: 0 14px; }
  .user-copy, .user-arrow { display: none; }
  .layout-main { height: calc(100dvh - 52px); padding: 16px 14px calc(20px + env(safe-area-inset-bottom)); }
  .is-chat-route .layout-main { padding: 12px 12px calc(12px + env(safe-area-inset-bottom)); }
}
</style>
