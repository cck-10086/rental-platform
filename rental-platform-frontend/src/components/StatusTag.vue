<template>
  <el-tag :type="tagType" :effect="effect" :size="size" class="status-tag">
    <span class="status-tag__dot" aria-hidden="true"></span>
    <slot>{{ label }}</slot>
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, default: 'default' },
  label: { type: String, default: '' },
  size: { type: String, default: 'small' },
  effect: { type: String, default: 'light' }
})

const typeMap = {
  success: 'success', done: 'success', active: 'success', low: 'success',
  warning: 'warning', pending: 'warning', medium: 'warning',
  danger: 'danger', error: 'danger', high: 'danger', disabled: 'danger',
  info: 'info', default: 'info'
}

const tagType = computed(() => typeMap[String(props.status).toLowerCase()] || 'info')
</script>

<style scoped>
.status-tag { gap: 5px; }
.status-tag__dot {
  width: 5px;
  height: 5px;
  border-radius: 999px;
  background: currentColor;
  opacity: 0.78;
}
</style>
