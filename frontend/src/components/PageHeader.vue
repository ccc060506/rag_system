<template>
  <header class="page-header">
    <div class="page-heading">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="page-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--rule);
}

.page-title {
  margin: 0;
  color: var(--ink);
  font-size: 30px;
  font-weight: 300;
  line-height: 1.2;
  letter-spacing: -0.025em;
  text-wrap: balance;
}

.page-subtitle {
  margin: 8px 0 0;
  color: var(--ink-3);
  font-size: 14px;
  line-height: 1.6;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 24px;
    padding-bottom: 16px;
  }

  .page-title {
    font-size: 24px;
  }

  .page-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
