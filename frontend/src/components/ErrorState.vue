<template>
  <div class="error-state" role="alert">
    <div class="error-icon">
      <AlertTriangle :size="18" />
    </div>
    <div class="error-copy">
      <div class="error-title">{{ title }}</div>
      <div class="error-message">{{ message }}</div>
    </div>
    <el-button v-if="retryable" size="small" @click="$emit('retry')">
      <RefreshCw :size="14" />
      <span>重试</span>
    </el-button>
  </div>
</template>

<script setup>
import { AlertTriangle, RefreshCw } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '加载失败' },
  message: { type: String, default: '请稍后重试' },
  retryable: { type: Boolean, default: true },
})

defineEmits(['retry'])
</script>

<style scoped>
.error-state {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px;
  background: var(--surface);
  border: 1px solid var(--error-border);
  border-radius: var(--radius);
  color: var(--ink);
}

.error-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 9px;
  background: var(--danger-soft);
  color: var(--danger);
}

.error-copy {
  flex: 1;
  min-width: 0;
}

.error-title {
  font-size: 13px;
  font-weight: 600;
}

.error-message {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 12px;
  word-break: break-word;
}

.error-state :deep(.el-button) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 480px) {
  .error-state {
    flex-wrap: wrap;
  }

  .error-state :deep(.el-button) {
    margin-left: 48px;
  }
}
</style>
