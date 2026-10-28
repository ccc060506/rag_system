import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      // 只代理接口路径，避免把 /user、/user/config、/login、/logs 这些前端路由也转发到后端
      '/user/center': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '^/user/config/(select|verify)': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/sensitiveWord': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '^/log/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/KnowledgeBase': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
      '/ai/chat': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      }
    }
  }
})