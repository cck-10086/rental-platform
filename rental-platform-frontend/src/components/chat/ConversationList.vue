<template>
  <section class="conversation-panel" aria-label="历史会话">
    <div class="conversation-panel__header">
      <div>
        <span>历史会话</span>
        <small>{{ conversations.length }} 个会话</small>
      </div>
      <el-button size="small" class="new-chat" @click="$emit('new')">
        <el-icon><Plus /></el-icon>新建对话
      </el-button>
    </div>

    <div class="conversation-panel__body">
      <el-skeleton v-if="loading" :rows="6" animated />
      <StatePanel
        v-else-if="error"
        type="error"
        title="会话加载失败"
        :description="error"
        action-label="重新加载"
        @action="$emit('retry')"
      />
      <StatePanel
        v-else-if="conversations.length === 0"
        title="暂无历史对话"
        description="输入问题后，会话会显示在这里。"
      />
      <template v-else>
        <div v-for="group in groupedConversations" :key="group.label" class="conversation-group">
          <div class="conversation-group__label">{{ group.label }}</div>
          <div
            v-for="conv in group.items"
            :key="conv.conversationId"
            type="button"
            class="conversation-item"
            :class="{ 'is-active': conv.conversationId === activeId }"
            role="button"
            tabindex="0"
            @click="$emit('select', conv)"
            @keydown.enter="$emit('select', conv)"
          >
            <span class="conversation-item__title">{{ conv.title || '未命名对话' }}</span>
            <span class="conversation-item__meta">
              <span>{{ conv.messageCount || 0 }} 条消息</span>
              <time>{{ formatTime(conv.lastTime) }}</time>
            </span>
            <button
              type="button"
              class="conversation-item__delete"
              aria-label="删除该会话"
              @click.stop="$emit('delete', conv)"
            ><el-icon><Trash2 /></el-icon></button>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Plus, Trash2 } from '@lucide/vue'
import StatePanel from '@/components/StatePanel.vue'

const props = defineProps({
  conversations: { type: Array, default: () => [] },
  activeId: { type: [String, Number], default: '' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

defineEmits(['new', 'select', 'delete', 'retry'])

/* 按 今天 / 近 7 天 / 更早 分组 */
const groupedConversations = computed(() => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const sevenDaysAgo = today - 6 * 24 * 3600 * 1000
  const groups = [
    { label: '今天', min: today, items: [] },
    { label: '近 7 天', min: sevenDaysAgo, items: [] },
    { label: '更早', min: -Infinity, items: [] }
  ]
  for (const conversation of props.conversations) {
    const time = new Date(conversation.lastTime).getTime() || 0
    groups.find(group => time >= group.min).items.push(conversation)
  }
  return groups.filter(group => group.items.length)
})

const formatTime = value => {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
  }
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
</script>

<style scoped>
/* 会话列:米白底 + 细分隔线,无卡片无阴影 */
.conversation-panel {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--rp-border);
  background: var(--rp-bg);
}

.conversation-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--rp-divider);
}
.conversation-panel__header > div { display: flex; flex-direction: column; gap: 1px; }
.conversation-panel__header span { color: var(--rp-text); font-size: 13px; font-weight: 600; }
.conversation-panel__header small { color: var(--rp-text-muted); font-size: 10px; }
.new-chat { min-height: 28px; padding: 0 10px; }
.conversation-panel__body { flex: 1; min-height: 0; overflow-y: auto; padding: 8px; }
.conversation-panel__body > .el-skeleton { padding: 8px; }
.conversation-group + .conversation-group { margin-top: 8px; }
.conversation-group__label { padding: 8px 8px 5px; color: var(--rp-text-muted); font-size: 10px; font-weight: 550; letter-spacing: .06em; }

.conversation-item {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 54px;
  align-items: stretch;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
  margin: 1px 0;
  padding: 9px 30px 9px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
  transition: background var(--rp-duration) var(--rp-ease);
}
.conversation-item:hover { background: var(--rp-surface-muted); }
/* 当前会话:浅色背景 + 细左线 */
.conversation-item.is-active { background: var(--rp-surface); }
.conversation-item.is-active::before { position: absolute; top: 10px; bottom: 10px; left: 0; width: 2px; border-radius: 0 2px 2px 0; background: var(--rp-primary); content: ''; }
.conversation-item__title { overflow: hidden; color: var(--rp-text); font-size: 12.5px; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }
.is-active .conversation-item__title { font-weight: 600; }
.conversation-item__meta { display: flex; justify-content: space-between; gap: 8px; color: var(--rp-text-muted); font-size: 10px; }
/* 删除按钮:仅在 hover 或选中时出现 */
.conversation-item__delete { position: absolute; top: 13px; right: 6px; display: grid; width: 26px; height: 26px; place-items: center; border: 0; border-radius: 4px; background: transparent; color: var(--rp-text-muted); cursor: pointer; opacity: 0; transition: opacity var(--rp-duration), background var(--rp-duration), color var(--rp-duration); }
.conversation-item:hover .conversation-item__delete,
.conversation-item.is-active .conversation-item__delete { opacity: 1; }
.conversation-item__delete:hover { background: var(--rp-danger-soft); color: var(--rp-danger); }

/* 抽屉中:列贴边显示,保留右边线 */
@media (max-width: 959px) {
  .conversation-panel { border-right: 0; }
}
</style>
