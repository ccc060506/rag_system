<template>
  <div class="side-nav">
    <div class="brand">
      <div class="brand-mark">
        <Sparkles :size="17" :stroke-width="2" />
      </div>
      <div class="brand-copy">
        <div class="brand-name">RAG-AI</div>
        <div class="brand-sub">企业知识库</div>
      </div>
    </div>

    <nav class="nav" aria-label="主导航">
      <div v-for="group in navGroups" :key="group.label" class="nav-group">
        <div class="nav-label">{{ group.label }}</div>
        <RouterLink
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
          @click="$emit('navigate')"
        >
          <component :is="item.icon" :size="17" :stroke-width="1.75" class="nav-icon" />
          <span>{{ item.name }}</span>
        </RouterLink>
      </div>
    </nav>

    <div class="user-block">
      <div class="avatar">{{ initial }}</div>
      <div class="user-copy">
        <div class="user-name">{{ username }}</div>
        <div class="user-role">已登录</div>
      </div>
      <button class="logout" type="button" aria-label="退出登录" title="退出登录" @click="logout">
        <LogOut :size="17" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3,
  BookOpen,
  History,
  LogOut,
  MessageSquare,
  Shield,
  Sparkles,
  User,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api'

defineEmits(['navigate'])

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const username = computed(() => auth.username || localStorage.getItem('username') || 'User')
const initial = computed(() => (username.value || 'U').charAt(0).toUpperCase())

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')

const navGroups = [
  {
    label: '核心功能',
    items: [
      { name: 'AI 问答', path: '/chat', icon: MessageSquare },
      { name: '历史对话', path: '/logs', icon: History },
    ],
  },
  {
    label: '管理中心',
    items: [
      { name: '个人中心', path: '/user', icon: User },
      { name: '知识库', path: '/knowledge', icon: BookOpen },
    ],
  },
  {
    label: '内容安全',
    items: [
      { name: '敏感词管理', path: '/sensitive', icon: Shield },
      { name: '敏感词分类', path: '/sensitive-category', icon: BarChart3 },
    ],
  },
]

async function logout() {
  try {
    await userApi.logout()
  } catch {
    // 后端不可用时仍然清理本地会话
  }
  auth.logout?.()
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}
</script>

<style scoped>
.side-nav {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: var(--sidebar);
  color: var(--sidebar-text);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
  flex-shrink: 0;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-active));
  color: var(--on-dark);
  box-shadow: 0 4px 14px var(--brand-shadow);
}

.brand-name {
  color: var(--on-dark);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.2;
}

.brand-sub {
  margin-top: 2px;
  color: var(--sidebar-muted);
  font-size: 10.5px;
  letter-spacing: 0.02em;
}

.nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 12px 16px;
}

.nav-group {
  margin-bottom: 20px;
}

.nav-label {
  padding: 0 12px;
  margin-bottom: 8px;
  color: var(--sidebar-muted);
  font-size: 11px;
  font-weight: 500;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  color: var(--sidebar-text);
  font-size: 13px;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
}

.nav-item:hover {
  background: var(--nav-hover);
  color: var(--on-dark);
}

.nav-item.active {
  background: var(--sidebar-active);
  color: var(--on-dark);
  font-weight: 500;
  box-shadow: inset 2px 0 0 var(--accent);
}

.nav-icon {
  flex-shrink: 0;
  opacity: 0.78;
}

.nav-item.active .nav-icon,
.nav-item:hover .nav-icon {
  opacity: 1;
}

.user-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  flex-shrink: 0;
  border-top: 1px solid var(--nav-border);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--primary-alpha-18);
  border: 1px solid var(--avatar-border);
  color: var(--avatar-text);
  font-size: 12px;
  font-weight: 600;
}

.user-copy {
  flex: 1;
  min-width: 0;
}

.user-name {
  overflow: hidden;
  color: var(--sidebar-user-name);
  font-size: 12.5px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-role {
  margin-top: 1px;
  color: var(--sidebar-muted);
  font-size: 10.5px;
}

.logout {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: var(--sidebar-muted);
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease;
}

.logout:hover {
  background: var(--logout-hover-bg);
  color: var(--logout-hover-color);
}

.nav::-webkit-scrollbar {
  width: 6px;
}

.nav::-webkit-scrollbar-thumb {
  background: var(--sidebar-scrollbar);
}
</style>
