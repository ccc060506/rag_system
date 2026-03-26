<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-grid"></div>
    <div class="bg-glow"></div>

    <div class="login-box">
      <div class="login-logo">
        <span class="logo-icon">🌸</span>
        <span class="logo-text">RAG-AI</span>
      </div>
      <p class="login-desc">企业级知识库智能问答系统</p>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="输入用户名"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
      </el-form>

      <el-button
        type="primary"
        class="login-btn"
        :loading="loading"
        @click="handleLogin"
        style="width:100%;height:42px;font-size:15px;margin-top:6px"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </el-button>

      <p v-if="error" class="login-error">{{ error }}</p>

      <div class="login-tip">
        <span>还没有账号？</span>
        <a href="#" @click.prevent="showRegister = true">立即注册</a>
      </div>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog
      v-model="showRegister"
      title="注册账号"
      width="420px"
      destroy-on-close
    >
      <el-form :model="regForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="regForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regForm.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="regForm.email" placeholder="邮箱（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api/index'

const router = useRouter()
const auth   = useAuthStore()
const toast  = inject('toast')

const form    = ref({ username: '', password: '' })
const loading = ref(false)
const error   = ref('')
const showRegister = ref(false)
const regForm = ref({ username: '', password: '', email: '' })

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value   = ''
  try {
    const res = await auth.login(form.value)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      router.push('/chat')
    } else {
      error.value = res.message || '登录失败'
    }
  } catch {
    error.value = '服务器连接失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  try {
    await userApi.register(regForm.value)
    toast('注册成功，请登录')
    showRegister.value = false
    form.value.username = regForm.value.username
  } catch {
    toast('注册失败', 'error')
  }
}
</script>

<style scoped>
.login-page {
  width: 100vw; height: 100vh;
  background: var(--bg-base);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0,229,160,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,229,160,0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

.bg-glow {
  position: absolute;
  width: 500px; height: 500px;
  background: radial-gradient(circle, rgba(0,229,160,0.08) 0%, transparent 60%);
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}

.login-box {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-strong);
  border-radius: 18px;
  padding: 40px 36px;
  width: 380px;
  box-shadow: 0 0 60px rgba(0,0,0,0.5);
}

.login-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.logo-icon { font-size: 28px; color: var(--accent); }
.logo-text  { font-size: 22px; font-weight: 700; font-family: var(--font-mono); letter-spacing: 0.05em; }

.login-desc {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.login-error {
  margin-top: 10px;
  font-size: 12px;
  color: var(--danger);
  text-align: center;
}

.login-tip {
  margin-top: 18px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
.login-tip a {
  color: var(--accent);
  text-decoration: none;
  margin-left: 4px;
}
.login-tip a:hover { text-decoration: underline; }
</style>