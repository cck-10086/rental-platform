<template>
  <div class="app-sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="brand">
      <div class="brand__mark"><el-icon><House /></el-icon></div>
      <div v-if="!collapsed" class="brand__copy">
        <strong>租住智审</strong>
        <span>合同审查平台</span>
      </div>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :collapse-transition="false"
      router
      class="sidebar-menu"
      @select="$emit('navigate')"
    >
      <el-menu-item index="/dashboard"><el-icon><ClipboardList /></el-icon><template #title>工作台</template></el-menu-item>
      <el-sub-menu index="contract">
        <template #title><el-icon><FileText /></el-icon><span>合同管理</span></template>
        <el-menu-item index="/contract/upload">上传合同</el-menu-item>
        <el-menu-item index="/contract/list">我的合同</el-menu-item>
      </el-sub-menu>
      <el-menu-item index="/house/list"><el-icon><House /></el-icon><template #title>房源管理</template></el-menu-item>
      <el-menu-item index="/expense/list"><el-icon><Receipt /></el-icon><template #title>费用管理</template></el-menu-item>
      <el-menu-item index="/repair/list"><el-icon><Wrench /></el-icon><template #title>维修报备</template></el-menu-item>
      <el-menu-item index="/move-out"><el-icon><DoorOpen /></el-icon><template #title>退租管理</template></el-menu-item>
      <el-menu-item index="/ai-chat"><el-icon><Bot /></el-icon><template #title>AI 租房顾问</template></el-menu-item>
      <el-sub-menu v-if="role === 'admin'" index="admin">
        <template #title><el-icon><Settings2 /></el-icon><span>管理后台</span></template>
        <el-menu-item index="/admin/stats">数据统计</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/data">全局数据</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import {
  House, ClipboardList, FileText, Receipt, Wrench,
  DoorOpen, Bot, Settings2
} from '@lucide/vue'

defineProps({
  activeMenu: { type: String, required: true },
  collapsed: { type: Boolean, default: false },
  role: { type: String, default: '' }
})

defineEmits(['navigate'])
</script>

<style scoped>
.app-sidebar {
  display: flex;
  height: 100%;
  min-height: 100dvh;
  flex-direction: column;
  background: var(--rp-sidebar-bg);
  color: var(--rp-sidebar-text);
}

.brand {
  display: flex;
  height: 56px;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--rp-border);
}

.brand__mark {
  display: grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-btn);
  background: var(--rp-surface);
  color: var(--rp-primary);
  font-size: 15px;
}

.brand__copy { display: flex; min-width: 0; flex-direction: column; gap: 1px; }
.brand__copy strong { color: var(--rp-sidebar-text-strong); font-size: 13px; font-weight: 600; letter-spacing: .01em; }
.brand__copy span { color: var(--rp-text-muted); font-size: 10px; }

.sidebar-menu {
  flex: 1;
  padding: 10px 8px 16px;
  border-right: 0;
  background: transparent;
}

/* 工具型菜单项:紧凑高度、小圆角、细左边线激活态 */
.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  height: 36px;
  margin: 1px 0;
  border-radius: 4px;
  color: var(--rp-sidebar-text);
  font-size: 13px;
  line-height: 36px;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) { background: var(--rp-sidebar-hover); color: var(--rp-sidebar-text-strong); }

/* 激活态:细左边线 + 很浅的背景 + 加粗文字 */
.sidebar-menu :deep(.el-menu-item.is-active) {
  position: relative;
  background: var(--rp-sidebar-active);
  color: var(--rp-sidebar-text-strong);
  font-weight: 600;
}
.sidebar-menu :deep(.el-menu-item.is-active::before) {
  position: absolute;
  top: 8px;
  bottom: 8px;
  left: 0;
  width: 2px;
  border-radius: 0 2px 2px 0;
  background: var(--rp-primary);
  content: '';
}
.sidebar-menu :deep(.el-sub-menu .el-menu-item) { height: 32px; min-width: 0; padding-left: 44px !important; font-size: 12.5px; line-height: 32px; }
.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active) { font-weight: 550; }
.sidebar-menu :deep(.el-menu-item .el-icon), .sidebar-menu :deep(.el-sub-menu__title .el-icon) { color: inherit; font-size: 15px; }

.is-collapsed .brand { justify-content: center; padding: 0; }
.is-collapsed .sidebar-menu { padding-inline: 6px; }
.is-collapsed .sidebar-menu :deep(.el-sub-menu .el-menu-item) { padding-left: 20px !important; }
</style>
