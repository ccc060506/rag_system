<template>
  <el-container class="layout-container" style="height: 100vh;">

    <!-- ─── 侧边栏 ─── -->
    <el-aside width="260px" class="sidebar">
      <el-container style="height: 100%; flex-direction: column;">

        <!-- Logo 区 -->
        <el-header height="80px" class="sidebar-header">
          <div class="sidebar-brand">
            <span class="brand-icon">🌸</span>
            <div>
              <div class="brand-name">RAG-AI</div>
              <div class="brand-sub">企业知识库系统</div>
            </div>
          </div>
        </el-header>

        <!-- 导航区 -->
        <el-main class="sidebar-nav-wrap">
          <div class="sidebar-nav">
            <div v-for="group in navGroups" :key="group.label" class="nav-group-container">
              <div class="nav-group-label">{{ group.label }}</div>
              <RouterLink
                v-for="item in group.items"
                :key="item.path"
                :to="item.path"
                class="nav-item"
                :class="{ active: isActive(item.path) }"
              >
                <span class="nav-icon">{{ item.icon }}</span>
                <span>{{ item.name }}</span>
              </RouterLink>
            </div>
          </div>
        </el-main>

        <!-- 用户区：头像 + 用户名 + 退出按钮横排 -->
        <el-footer height="72px" class="sidebar-footer-wrap">
          <div class="sidebar-footer">
            <div class="user-avatar">
              {{ username.charAt(0).toUpperCase() }}
            </div>
            <div class="user-name">{{ username }}</div>
            <button class="logout-btn" @click="logout" title="退出登录">⏻</button>
          </div>
        </el-footer>  

      </el-container>
    </el-aside>

    <!-- ─── 主内容区 ─── -->
    <el-container class="main-wrap">
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>

  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'   // ← 修复：引入 auth store

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

// ── 从 store 读取真实用户名 ──
const username = computed(() => auth.username || localStorage.getItem('username') || 'User')

// ── 路由匹配（支持子路由高亮）──
const isActive = (path) => route.path === path || route.path.startsWith(path + '/')

// ── 路由路径与 router/index.js 对齐 ──
const navGroups = [
  {
    label: '核心功能',
    items: [
      { name: 'AI 问答',      path: '/chat',               icon: '🤖' },
      { name: '历史对话记录',  path: '/logs',               icon: '📜' },
    ]
  },
  {
    label: '管理中心',
    items: [
      { name: '个人中心', path: '/user',      icon: '👤' },
      { name: '知识库',   path: '/knowledge', icon: '📚' },
    ]
  },
  {
    label: '内容安全',
    items: [
      { name: '敏感词管理', path: '/sensitive',          icon: '🚫' },
      { name: '敏感词分类', path: '/sensitive-category', icon: '📊' },
    ]
  }
]

// ── 退出登录：清 token + 跳登录页 ──
function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  auth.logout?.()          // 如果 store 有 logout 方法就调用
  router.push('/login')    // ← 修复：恢复跳转
}
</script>

<style scoped>
/* ─── 整体 ─── */
.layout-container {
  height: 100vh;
  overflow: hidden;
}

/* ─── 侧边栏液态玻璃 ─── */
.sidebar {
  background: rgba(255, 240, 248, 0.72) !important;
  backdrop-filter: blur(20px) saturate(1.8) !important;
  -webkit-backdrop-filter: blur(20px) saturate(1.8) !important;
  border-right: 1px solid rgba(210, 160, 190, 0.30) !important;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

/* Logo 头部 */
.sidebar-header {
  padding: 0 !important;
  border-bottom: 1px solid rgba(210, 160, 190, 0.25);
  flex-shrink: 0;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  height: 100%;
}
.brand-icon {
  font-size: 24px;
  color: var(--accent);
  line-height: 1;
}
.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.03em;
  font-family: var(--font-mono);
}
.brand-sub {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 1px;
}

/* 导航滚动区 */
.sidebar-nav-wrap {
  padding: 0 !important;
  overflow-y: auto !important;
  flex: 1;
}
.sidebar-nav {
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.nav-group-label {
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 0 8px;
  margin-bottom: 4px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13.5px;
  transition: all 0.18s;
}
.nav-item:hover {
  background: rgba(230, 190, 215, 0.40);   /* ← 修复：不再变纯黑 */
  color: var(--text-primary);
}
.nav-item.active {
  background: var(--accent-dim);
  color: var(--accent);
  font-weight: 500;
}
.nav-icon {
  font-size: 14px;
  width: 18px;
  text-align: center;
}

/* ─── 底部用户区：横排布局 ─── */
.sidebar-footer-wrap {
  padding: 0 !important;
  border-top: 1px solid rgba(210, 160, 190, 0.25);
  flex-shrink: 0;
}
.sidebar-footer {
  display: flex;              /* ← 横排 */
  align-items: center;
  flex-direction: row;        /* ← 明确横排，修复退出按钮跑到下面的问题 */
  gap: 10px;
  padding: 0 14px;
  height: 100%;
}
.user-avatar {
  width: 36px;
  height: 36px;
  background: var(--accent-dim);
  color: var(--accent);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-mono);
  border: 1px solid rgba(224, 82, 122, 0.25);
  flex-shrink: 0;
}
.user-name {
  flex: 1;                    /* ← 占满中间空间，把按钮推到最右 */
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.logout-btn {
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  font-size: 18px;
  padding: 4px 6px;
  border-radius: 6px;
  transition: color 0.2s, background 0.2s;
  flex-shrink: 0;
  margin-left: auto;          /* ← 推到最右边 */
}
.logout-btn:hover {
  color: var(--danger);
  background: rgba(224, 82, 122, 0.10);
}

/* ─── 主内容区 ─── */
.main-wrap {
  flex: 1;
  overflow: hidden;
}
.main-content {
  padding: 0 !important;
  height: 100%;
  overflow: hidden;
  background: transparent !important;
}
</style>