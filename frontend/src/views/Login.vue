<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-grid"></div>
      <div class="brand-glow"></div>

      <div class="brand-top">
        <div class="brand-mark">
          <Sparkles :size="18" :stroke-width="2" />
        </div>
        <span class="brand-name">RAG-AI</span>
      </div>

      <div class="brand-copy">
        <h1>让知识可检索，<br />让回答有出处。</h1>
        <p class="desc">
          上传文档，构建属于你的企业知识库。每一次回答，都可以追溯到原文。
        </p>
        <div class="features">
          <div class="feature">
            <FileText :size="16" />
            <span>文档解析与切分</span>
          </div>
          <div class="feature">
            <Database :size="16" />
            <span>向量检索</span>
          </div>
          <div class="feature">
            <CheckCircle :size="16" />
            <span>可溯源回答</span>
          </div>
        </div>
      </div>

      <div class="brand-foot">RAG-AI 企业知识库</div>
    </section>

    <section class="form-panel">
      <div class="form-card">
        <div class="mobile-brand" aria-label="RAG-AI">
          <div class="brand-mark">
            <Sparkles :size="16" :stroke-width="2" />
          </div>
        </div>

        <h2>登录</h2>
        <p class="form-sub">使用你的账号进入工作台</p>

        <el-form :model="form" label-position="top" @submit.prevent>
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              size="large"
              placeholder="输入用户名"
              autocomplete="username"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <User :size="16" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              type="password"
              size="large"
              placeholder="输入密码"
              autocomplete="current-password"
              show-password
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <Lock :size="16" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>
        </el-form>

        <p v-if="error" class="error-text">{{ error }}</p>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
          <span>{{ loading ? '登录中...' : '登录' }}</span>
        </el-button>

        <div class="tip">
          <span>还没有账号？</span>
          <a href="#" @click.prevent="showRegister = true">立即注册</a>
        </div>
      </div>
    </section>

    <el-dialog v-model="showRegister" title="注册账号" width="420px" destroy-on-close>
      <el-form :model="regForm" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="regForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="regForm.password" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="regForm.email" placeholder="邮箱（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CheckCircle,
  Database,
  FileText,
  Lock,
  Sparkles,
  User,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api'

const router = useRouter()
const auth = useAuthStore()
const toast = inject('toast')

const form = ref({ username: '', password: '' })
const loading = ref(false)
const error = ref('')
const showRegister = ref(false)
const registering = ref(false)
const regForm = ref({ username: '', password: '', email: '' })

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    error.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  error.value = ''
  try {
    const res = await auth.login(form.value)
    if (res.code === 200) {
      router.push('/chat')
    } else {
      error.value = res.msg || '登录失败'
    }
  } catch (e) {
    error.value = e.userMessage || '服务器连接失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regForm.value.username || !regForm.value.password) {
    toast('请填写用户名和密码', 'error')
    return
  }

  registering.value = true
  try {
    const res = await userApi.register(regForm.value)
    if (res.code === 200) {
      toast('注册成功，请登录')
      showRegister.value = false
      form.value.username = regForm.value.username
    } else {
      toast(res.msg || '注册失败', 'error')
    }
  } catch (e) {
    toast(e.userMessage || '注册失败', 'error')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 44%) minmax(0, 56%);
  height: 100vh;
  background: var(--bg);
}

/* ─── left brand panel ─── */
.brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 40px 48px;
  background: var(--sidebar);
  color: var(--on-dark);
}

.brand-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--panel-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--panel-grid) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(circle at 30% 24%, var(--mask-black) 0%, transparent 72%);
  pointer-events: none;
}

.brand-glow {
  position: absolute;
  top: -80px;
  right: -120px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--brand-glow), transparent 66%);
  filter: blur(12px);
  pointer-events: none;
}

.brand-top,
.brand-copy,
.brand-foot {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-active));
  color: var(--on-dark);
  box-shadow: 0 4px 16px var(--brand-shadow-strong);
}

.brand-name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.brand-copy {
  max-width: 420px;
}

.brand-copy h1 {
  margin: 0 0 16px;
  color: var(--on-dark);
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 600;
  line-height: 1.28;
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.desc {
  max-width: 380px;
  margin: 0;
  color: var(--desc-on-dark);
  font-size: 14px;
  line-height: 1.75;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 32px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--sidebar-text);
  font-size: 13px;
}

.feature svg {
  color: var(--feature-icon);
  flex-shrink: 0;
}

.brand-foot {
  color: var(--brand-foot);
  font-size: 11px;
  letter-spacing: 0.02em;
}

/* ─── right form ─── */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

.form-card {
  width: 100%;
  max-width: 380px;
  padding: 32px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}

.form-card h2 {
  margin: 0 0 8px;
  color: var(--ink);
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.form-sub {
  margin: 0 0 24px;
  color: var(--ink-3);
  font-size: 13px;
}

.input-icon {
  color: var(--ink-3);
}

.error-text {
  margin: 2px 0 12px;
  color: var(--danger);
  font-size: 12.5px;
}

.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 42px;
  margin-top: 4px;
  font-size: 14px;
}

.tip {
  margin-top: 20px;
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

.tip a {
  margin-left: 4px;
  color: var(--accent);
  text-decoration: none;
}

.tip a:hover {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .form-panel {
    padding: 20px;
  }

  .mobile-brand {
    display: flex;
  }
}

@media (max-width: 480px) {
  .form-card {
    padding: 24px;
    border: none;
    box-shadow: none;
    background: transparent;
  }

  .form-panel {
    align-items: flex-start;
    padding-top: 64px;
  }
}
</style>
