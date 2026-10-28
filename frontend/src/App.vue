<template>
  <RouterView />
</template>

<script setup>
import { provide } from 'vue'
import { ElMessage } from 'element-plus'

function showToast(msg, type = 'success') {
  ElMessage({
    message: msg,
    type: type === 'error' ? 'error' : 'success',
    duration: 2600,
    grouping: true,
  })
}

provide('toast', showToast)
</script>
