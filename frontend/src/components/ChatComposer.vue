<template>
  <div class="composer" :class="[`composer-${variant}`, { 'is-focused': focused }]">
    <el-input
      :model-value="modelValue"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 6 }"
      :placeholder="placeholder"
      resize="none"
      class="composer-input"
      @update:model-value="$emit('update:modelValue', $event)"
      @focus="focused = true"
      @blur="focused = false"
      @keydown.enter.exact.prevent="$emit('send')"
    />

    <div class="composer-actions">
      <button
        v-if="showClear"
        class="ghost-btn"
        type="button"
        title="清空对话"
        aria-label="清空对话"
        :disabled="!modelValue.trim() || streaming"
        @click="$emit('clear')"
      >
        <Trash2 :size="17" />
      </button>

      <button
        class="send-btn"
        type="button"
        aria-label="发送"
        :disabled="disabled || !modelValue.trim() || streaming"
        @click="$emit('send')"
      >
        <Loader2 v-if="streaming" :size="17" class="spin" />
        <Send v-else :size="17" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Loader2, Send, Trash2 } from 'lucide-vue-next'

defineProps({
  modelValue: { type: String, default: '' },
  streaming: { type: Boolean, default: false },
  showClear: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  variant: { type: String, default: 'dock' },
  placeholder: { type: String, default: '输入你的问题，Enter 发送' },
})

defineEmits(['update:modelValue', 'send', 'clear'])

const focused = ref(false)
</script>

<style scoped>
.composer {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 8px 12px 8px 16px;
  background: linear-gradient(180deg, var(--glass-bg-top), var(--glass-bg-bottom));
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: inset 0 1px 0 var(--glass-highlight);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.composer-hero {
  padding: 12px 12px 12px 20px;
  border-radius: 20px;
  background: linear-gradient(180deg, var(--glass-hero-top), var(--glass-hero-bottom));
}

.composer.is-focused {
  border-color: var(--focus-border);
  box-shadow:
    0 0 0 3px var(--focus-glow),
    inset 0 1px 0 var(--glass-highlight-strong);
}

.composer-input {
  flex: 1;
  min-width: 0;
}

.composer-input :deep(.el-textarea__inner) {
  min-height: 24px !important;
  padding: 8px 0 !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.6;
}

.composer-hero .composer-input :deep(.el-textarea__inner) {
  padding: 12px 0 !important;
  font-size: 15px;
}

.composer-input :deep(.el-textarea__inner::placeholder) {
  color: var(--placeholder-on-dark);
}

.composer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 2px;
}

.ghost-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-3);
  cursor: pointer;
}

.ghost-btn:hover:not(:disabled) {
  background: var(--glass-highlight-strong);
  color: var(--ink);
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--primary);
  color: var(--on-dark);
  cursor: pointer;
  box-shadow: 0 6px 18px var(--send-shadow);
  transition: background 0.15s ease, opacity 0.15s ease;
}

.composer-hero .send-btn {
  width: 40px;
  height: 40px;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-hover);
}

.ghost-btn:disabled,
.send-btn:disabled {
  opacity: 0.32;
  cursor: not-allowed;
}

.spin {
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 480px) {
  .composer {
    padding: 8px 8px 8px 16px;
  }

  .composer-hero {
    padding: 12px 12px 12px 16px;
    border-radius: 16px;
  }

  .composer-hero .send-btn {
    width: 36px;
    height: 36px;
  }
}
</style>
