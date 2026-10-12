<template>
  <header class="page-header">
    <div class="page-header__copy">
      <h1>{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header__copy { min-width: 0; }

h1 {
  margin: 0;
  color: var(--rp-text);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.page-header__description {
  max-width: 65ch;
  margin: 4px 0 0;
  color: var(--rp-text-muted);
  font-size: 13px;
  line-height: 1.5;
}

.page-header__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

@media (max-width: 767px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
    margin-bottom: 16px;
  }

  .page-header__actions :deep(.el-button) { min-height: 40px; }
}
</style>
