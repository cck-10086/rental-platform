<template>
  <section class="state-panel" :class="`is-${type}`" role="status" aria-live="polite">
    <el-skeleton v-if="type === 'loading'" :rows="rows" animated />
    <template v-else>
      <div class="state-panel__icon" aria-hidden="true">
        <el-icon :size="22"><component :is="icon" /></el-icon>
      </div>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      <el-button v-if="actionLabel" :type="type === 'error' ? 'primary' : 'default'" @click="$emit('action')">
        {{ actionLabel }}
      </el-button>
    </template>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { Inbox, RefreshCw, TriangleAlert } from '@lucide/vue'

const props = defineProps({
  type: { type: String, default: 'empty', validator: value => ['loading', 'empty', 'error'].includes(value) },
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '当前还没有可展示的内容。' },
  actionLabel: { type: String, default: '' },
  rows: { type: Number, default: 5 }
})

defineEmits(['action'])

const icon = computed(() => {
  if (props.type === 'error') return TriangleAlert
  if (props.actionLabel) return RefreshCw
  return Inbox
})
</script>

<style scoped>
.state-panel {
  display: flex;
  min-height: 220px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 9px;
  padding: 32px;
  text-align: center;
}

.state-panel__icon {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 3px;
  place-items: center;
  border: 1px solid var(--rp-border);
  border-radius: 13px;
  background: var(--rp-surface-muted);
  color: var(--rp-text-muted);
}

.is-error .state-panel__icon {
  border-color: var(--rp-danger-border);
  background: var(--rp-danger-soft);
  color: var(--rp-danger);
}

h3 { margin: 0; color: var(--rp-text); font-size: 15px; font-weight: 600; }
p { max-width: 42ch; margin: 0 0 7px; color: var(--rp-text-muted); font-size: 13px; line-height: 1.6; }
.state-panel :deep(.el-skeleton) { width: 100%; }
</style>
