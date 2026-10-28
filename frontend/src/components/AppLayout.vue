<template>
  <RouterView v-if="isLanding" />

  <div v-else class="layout">
    <aside class="desktop-sidebar">
      <SideNav />
    </aside>

    <div class="workspace">
      <header class="mobile-topbar">
        <button class="icon-btn" type="button" aria-label="打开导航" @click="drawerOpen = true">
          <Menu :size="20" />
        </button>
        <div class="topbar-brand">
          <div class="topbar-mark">
            <Sparkles :size="14" :stroke-width="2" />
          </div>
          <span>RAG-AI</span>
        </div>
        <RouterLink to="/user" class="topbar-avatar" aria-label="个人中心">
          {{ initial }}
        </RouterLink>
      </header>

      <main class="main-content">
        <RouterView />
      </main>
    </div>

    <el-drawer
      v-model="drawerOpen"
      direction="ltr"
      size="264px"
      :with-header="false"
      class="nav-drawer"
      append-to-body
    >
      <SideNav @navigate="drawerOpen = false" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Menu, Sparkles } from 'lucide-vue-next'
import SideNav from '@/components/SideNav.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const drawerOpen = ref(false)

const isLanding = computed(() => route.name === 'Home')
const initial = computed(() => (auth.username || 'U').charAt(0).toUpperCase())

let media
function syncMedia(event) {
  if (!event.matches) {
    drawerOpen.value = false
  }
}

onMounted(() => {
  media = window.matchMedia('(max-width: 1023px)')
  media.addEventListener('change', syncMedia)
})

onUnmounted(() => {
  media?.removeEventListener('change', syncMedia)
})

watch(() => route.path, () => {
  drawerOpen.value = false
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--bg);
}

.desktop-sidebar {
  width: var(--sidebar-width);
  flex: 0 0 var(--sidebar-width);
  height: 100%;
}

.workspace {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.mobile-topbar {
  display: none;
  align-items: center;
  gap: 12px;
  height: var(--topbar-height);
  flex-shrink: 0;
  padding: 0 16px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ink-2);
  cursor: pointer;
}

.icon-btn:hover {
  background: var(--surface-subtle);
  color: var(--ink);
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.topbar-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 7px;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-active));
  color: var(--on-dark);
}

.topbar-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
}

:global(.nav-drawer) {
  background: var(--sidebar) !important;
}

:global(.nav-drawer .el-drawer__body) {
  padding: 0 !important;
  background: var(--sidebar) !important;
}

@media (max-width: 1023px) {
  .desktop-sidebar {
    display: none;
  }

  .mobile-topbar {
    display: flex;
  }
}
</style>
