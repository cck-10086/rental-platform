<template>
  <div class="message-scroller">
    <el-scrollbar ref="scrollbarRef" height="100%" @scroll="handleScroll">
      <div class="message-scroller__content">
        <div v-if="messages.length === 0 && !waiting" class="welcome-state">
          <div class="welcome-state__icon"><el-icon><Bot /></el-icon></div>
          <h3>从一个具体问题开始</h3>
          <p>我可以协助梳理合同风险、押金纠纷、退租通知和房东义务。回答仅供参考，重要事项请咨询专业律师。</p>
        </div>

        <template v-for="day in messagesByDay" :key="day.label">
          <div class="day-marker"><span>{{ day.label }}</span></div>
          <ChatMessage
            v-for="item in day.items"
            :key="item.key"
            :message="item.message"
            :show-avatar="item.showAvatar"
            :streaming="item.streaming"
          />
        </template>

        <ChatMessage
          v-if="waiting"
          :message="{ role: 'ai', content: '正在梳理问题…', time: Date.now() }"
          streaming
        />

        <el-alert
          v-if="error"
          class="stream-error"
          type="error"
          :title="error"
          description="你可以重新发送上一条问题。"
          show-icon
          :closable="false"
        >
          <template #default>
            <el-button size="small" @click="$emit('retry')">重新发送</el-button>
          </template>
        </el-alert>
      </div>
    </el-scrollbar>

    <transition name="jump">
      <button v-if="showJump" class="jump-button" type="button" aria-label="回到最新消息" @click="scrollToBottom(true)">
        <el-icon><ChevronDown /></el-icon>
        <span>最新消息</span>
      </button>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Bot, ChevronDown } from '@lucide/vue'
import ChatMessage from './ChatMessage.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  waiting: { type: Boolean, default: false },
  streaming: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

defineEmits(['retry'])

const scrollbarRef = ref(null)
const followLiveEdge = ref(true)
const showJump = ref(false)

const formatDayLabel = value => {
  const date = new Date(value || Date.now())
  const now = new Date()
  const yesterday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1)
  if (date.toDateString() === now.toDateString()) return '今天'
  if (date.toDateString() === yesterday.toDateString()) return '昨天'
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/* 连续消息减少重复头像:同角色同日只显示首个头像 */
const messagesByDay = computed(() => {
  const groups = []
  props.messages.forEach((message, index) => {
    const label = formatDayLabel(message.time)
    let group = groups.find(item => item.label === label)
    if (!group) {
      group = { label, items: [] }
      groups.push(group)
    }
    const previous = props.messages[index - 1]
    group.items.push({
      key: message.id || `${label}-${index}`,
      message,
      showAvatar: !previous || previous.role !== message.role || formatDayLabel(previous.time) !== label,
      streaming: props.streaming && index === props.messages.length - 1 && message.role !== 'user'
    })
  })
  return groups
})

const scrollToBottom = (force = false) => {
  if (!force && !followLiveEdge.value) return
  nextTick(() => {
    const wrap = scrollbarRef.value?.wrapRef
    if (!wrap) return
    scrollbarRef.value.setScrollTop(wrap.scrollHeight)
    followLiveEdge.value = true
    showJump.value = false
  })
}

const handleScroll = () => {
  const wrap = scrollbarRef.value?.wrapRef
  if (!wrap) return
  const distance = wrap.scrollHeight - wrap.scrollTop - wrap.clientHeight
  followLiveEdge.value = distance < 72
  showJump.value = distance >= 72
}

watch(() => [props.messages, props.waiting], () => scrollToBottom(), { deep: true })
onMounted(() => scrollToBottom(true))
defineExpose({ scrollToBottom })
</script>

<style scoped>
.message-scroller { position: relative; height: 100%; min-height: 0; overflow: hidden; background: var(--rp-surface); }
.message-scroller__content { width: min(100%, 860px); min-height: 100%; margin: 0 auto; padding: 22px 26px 30px; }
.welcome-state { display: flex; min-height: 100%; align-items: flex-start; justify-content: center; flex-direction: column; padding: 7vh 8%; }
.welcome-state__icon { display: grid; width: 36px; height: 36px; margin-bottom: 14px; place-items: center; border: 1px solid var(--rp-border); border-radius: var(--rp-radius-btn); background: var(--rp-surface-muted); color: var(--rp-text-muted); font-size: 17px; }
.welcome-state h3 { margin: 0; color: var(--rp-text); font-size: 18px; font-weight: 600; letter-spacing: -0.01em; }
.welcome-state p { max-width: 56ch; margin: 10px 0 0; color: var(--rp-text-muted); font-size: 13px; line-height: 1.7; }
/* 日期分隔线:简单细线 */
.day-marker { display: flex; align-items: center; gap: 10px; margin: 6px 0 14px; color: var(--rp-text-muted); font-size: 10px; }
.day-marker::before, .day-marker::after { height: 1px; flex: 1; background: var(--rp-border); content: ''; }
.stream-error { margin-top: 16px; }
.stream-error :deep(.el-alert__content) { width: 100%; }
.stream-error :deep(.el-alert__description) { margin-bottom: 10px; }
.jump-button { position: absolute; right: 20px; bottom: 16px; display: flex; min-height: 30px; align-items: center; gap: 6px; padding: 0 10px; border: 1px solid var(--rp-border); border-radius: 999px; background: var(--rp-surface); box-shadow: var(--rp-shadow-md); color: var(--rp-text-secondary); cursor: pointer; font-size: 11px; }
.jump-enter-active, .jump-leave-active { transition: opacity var(--rp-duration), transform var(--rp-duration); }
.jump-enter-from, .jump-leave-to { opacity: 0; transform: translateY(4px); }

@media (max-width: 767px) {
  .message-scroller__content { padding: 16px 12px 24px; }
  .welcome-state { padding: 5vh 6px; }
  .jump-button { right: 12px; bottom: 10px; }
}
</style>
