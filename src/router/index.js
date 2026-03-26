import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/components/AppLayout.vue'),
    children: [
      { path: '',        redirect: '/chat' },
      { path: 'chat',    name: 'Chat',      component: () => import('@/views/Chat.vue') },
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/Knowledge.vue') },
      { path: 'logs',    name: 'Logs',      component: () => import('@/views/Logs.vue') },
      { path: 'sensitive', name: 'Sensitive', component: () => import('@/views/SensitiveWord.vue') },
      { path: 'sensitive-category', name: 'SensitiveCategory', component: () => import('@/views/SensitiveCategory.vue') },
      { path: 'user',    name: 'User',      component: () => import('@/views/User/UserCenter.vue') },
      { path: 'user/config', name: 'UserConfig', component: () => import('@/views/User/UserConfig.vue') },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  // 直接检查本地存储是否有 token
  const hasToken = !!localStorage.getItem('token') 
  
  // 如果不是公开页面 且 没有 token，强制去登录页
  if (!to.meta.public && !hasToken) {
    return { name: 'Login' }
  }
  
  // 如果已经有 token 还想去登录页，直接拦截去首页
  if (to.name === 'Login' && hasToken) {
    return '/'
  }
})

export default router
