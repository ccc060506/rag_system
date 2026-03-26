<template>
  <RouterView />
  <!-- 全局 Toast -->
  <div class="toast-wrap">
    <div
      v-for="t in toasts"
      :key="t.id"
      class="toast"
      :class="t.type === 'success' ? 'toast-success' : 'toast-error'"
    >
      {{ t.msg }}
    </div>
  </div>
</template>

<script setup>
import { provide } from 'vue'
import { ElMessage } from 'element-plus'

function showToast(msg, type = 'success') {
  ElMessage({ message: msg, type: type === 'error' ? 'error' : 'success', duration: 3000 })
}

provide('toast', showToast)
</script>
