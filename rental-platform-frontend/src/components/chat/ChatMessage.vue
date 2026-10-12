<template>
  <article class="message" :class="[`is-${message.role}`, { 'is-grouped': !showAvatar }]">
    <div class="message__avatar" :class="`is-${message.role}`" :aria-hidden="!showAvatar">
      <template v-if="showAvatar">
        <el-icon v-if="message.role === 'user'"><User /></el-icon>
        <el-icon v-else><Bot /></el-icon>
      </template>
    </div>
    <div class="message__content">
      <div class="message__bubble">
        <template v-if="message.role === 'user'">{{ message.content }}</template>
        <div v-else class="markdown-content" v-html="renderMarkdown(message.content)"></div>
        <span v-if="streaming" class="stream-cursor" aria-label="正在生成"></span>
      </div>
      <time v-if="showAvatar" class="message__time">{{ formatTime(message.time) }}</time>
    </div>
  </article>
</template>

<script setup>
import { Bot, User } from '@lucide/vue'

defineProps({
  message: { type: Object, required: true },
  showAvatar: { type: Boolean, default: true },
  streaming: { type: Boolean, default: false }
})

const renderMarkdown = text => String(text || '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  .replace(/`([^`]+)`/g, '<code>$1</code>')
  .replace(/\n/g, '<br/>')

const formatTime = value => {
  if (!value) return ''
  return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}
</script>

<style scoped>
.message { display: flex; gap: 9px; margin-top: 14px; }
.message.is-grouped { margin-top: 4px; }
.message.is-user { flex-direction: row-reverse; }
.message__avatar { display: grid; width: 26px; height: 26px; flex: 0 0 26px; place-items: center; border-radius: 6px; font-size: 12px; }
.message__avatar.is-user { background: var(--rp-primary); color: #fff; }
.message__avatar.is-ai { border: 1px solid var(--rp-border); background: var(--rp-surface-muted); color: var(--rp-text-muted); }
.message__content { display: flex; max-width: min(74%, 720px); align-items: flex-start; flex-direction: column; }
.is-user .message__content { align-items: flex-end; }
/* AI 消息:纯背景 + 细边框;用户消息:低饱和强调色 */
.message__bubble { padding: 9px 13px; border: 1px solid var(--rp-border); border-radius: var(--rp-radius-bubble); background: var(--rp-surface); color: var(--rp-text-secondary); font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; }
.is-user .message__bubble { border-color: #B9DED7; border-radius: var(--rp-radius-bubble); background: #D9EDE8; color: #0B4B44; }
.message__time { margin-top: 3px; color: var(--rp-text-muted); font-size: 10px; }
.markdown-content :deep(code) { padding: 2px 5px; border-radius: 4px; background: var(--rp-surface-muted); color: var(--rp-primary-hover); font-size: 12px; }
.markdown-content :deep(strong) { color: var(--rp-text); }
.stream-cursor { display: inline-block; width: 2px; height: 12px; margin-left: 3px; vertical-align: -2px; background: var(--rp-primary); animation: cursor-blink 1s steps(2) infinite; }
@keyframes cursor-blink { 50% { opacity: 0; } }

@media (max-width: 767px) {
  .message__content { max-width: 86%; }
}
</style>
