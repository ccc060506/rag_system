<template>
  <div class="chat-page">
    <Starfield />

    <div v-if="isEmpty" class="chat-hero">
      <div class="hero-inner">
        <div class="welcome-mark">
          <Sparkles :size="24" :stroke-width="2" />
        </div>
        <h1>有什么可以帮你？</h1>
        <p class="hero-sub">基于你的知识库回答问题，并给出可追溯的引用来源。</p>

        <div class="hero-composer">
          <ChatComposer
            v-model="inputText"
            variant="hero"
            :streaming="streaming"
            @send="sendMessage"
          />
          <p class="composer-hint">Enter 发送，Shift + Enter 换行</p>
        </div>

        <div class="suggestions">
          <button
            v-for="tip in quickTips"
            :key="tip.title"
            type="button"
            class="suggestion"
            @click="sendTip(tip.prompt)"
          >
            <component :is="tip.icon" :size="18" class="suggestion-icon" />
            <span class="suggestion-title">{{ tip.title }}</span>
            <span class="suggestion-desc">{{ tip.desc }}</span>
          </button>
        </div>
      </div>
    </div>

    <template v-else>
      <div ref="msgEl" class="chat-scroll">
        <div class="chat-column">
          <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
            <div v-if="msg.role === 'assistant'" class="avatar assistant-avatar">
              <Bot :size="16" />
            </div>

            <div class="msg-body">
              <div class="bubble">
                <div class="markdown" v-html="renderMd(msg.content)"></div>

                <div v-if="msg.citations?.length" class="citations">
                  <div class="citations-label">
                    <Paperclip :size="13" />
                    <span>来源</span>
                  </div>
                  <div
                    v-for="(citation, idx) in msg.citations"
                    :key="citation.fileId || idx"
                    class="citation"
                  >
                    <FileText :size="15" class="citation-icon" />
                    <div class="citation-copy">
                      <div class="citation-file">{{ citation.fileName || '未命名文档' }}</div>
                      <div v-if="citation.snippet" class="citation-snippet">{{ citation.snippet }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="msg.role === 'user'" class="avatar user-avatar">{{ userInitial }}</div>
          </div>

          <div v-if="streaming" class="msg-row assistant">
            <div class="avatar assistant-avatar">
              <Bot :size="16" />
            </div>
            <div class="msg-body">
              <div class="bubble">
                <div v-if="streamBuffer" class="markdown" v-html="renderMd(streamBuffer)"></div>
                <div v-else class="thinking">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <span v-if="streamBuffer" class="cursor"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="composer-area">
        <div class="composer-column">
          <ChatComposer
            v-model="inputText"
            :streaming="streaming"
            show-clear
            @send="sendMessage"
            @clear="clearChat"
          />
          <p class="composer-hint">Enter 发送，Shift + Enter 换行</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BookOpen,
  Bot,
  FileText,
  Key,
  Paperclip,
  Sparkles,
} from 'lucide-vue-next'
import Starfield from '@/components/Starfield.vue'
import ChatComposer from '@/components/ChatComposer.vue'
import { useAuthStore } from '@/stores/auth'
import { logApi } from '@/api'
import { marked } from 'marked'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const toast = inject('toast')

const messages = ref([])
const inputText = ref('')
const streaming = ref(false)
const streamBuffer = ref('')
const msgEl = ref(null)
const sessionId = ref('session-' + Date.now())
const restoredLogId = ref('')

const userInitial = computed(() => (auth.username || 'U').charAt(0).toUpperCase())
const isEmpty = computed(() => messages.value.length === 0 && !streaming.value)

const quickTips = [
  {
    icon: BookOpen,
    title: '使用知识库',
    desc: '如何上传文档并让 AI 基于它回答？',
    prompt: '如何使用知识库功能？',
  },
  {
    icon: FileText,
    title: '支持的文件',
    desc: '系统可以解析哪些文档格式？',
    prompt: '系统支持哪些文件格式？',
  },
  {
    icon: Key,
    title: '配置 API Key',
    desc: '如何绑定自己的 DeepSeek Key？',
    prompt: '如何配置 DeepSeek API Key？',
  },
]

function renderMd(text) {
  if (!text) return ''
  return marked.parse(text, { breaks: true })
}

function sendTip(prompt) {
  inputText.value = prompt
  sendMessage()
}

function parseSources(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

async function loadHistoryLog() {
  const logId = route.query.logId
  if (!logId) return
  try {
    const res = await logApi.getDetail(String(logId))
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '历史对话加载失败')
    }
    const detail = res.data
    messages.value = [
      { role: 'user', content: detail.question || '' },
      {
        role: 'assistant',
        content: detail.answer || '暂无回答内容',
        citations: parseSources(detail.sourceContext).map((fileName) => ({ fileName })),
      },
    ]
    sessionId.value = 'log-' + detail.id
    restoredLogId.value = String(detail.id)
    await nextTick()
    scrollToBottom()
  } catch (e) {
    toast?.(e.userMessage || e.message || '历史对话加载失败', 'error')
    router.replace({ path: '/chat' })
  }
}

onMounted(loadHistoryLog)

watch(
  () => route.query.logId,
  (id) => {
    if (id && String(id) !== restoredLogId.value) loadHistoryLog()
  }
)

async function sendMessage() {
  const question = inputText.value.trim()
  if (!question || streaming.value) return

  messages.value.push({ role: 'user', content: question })
  inputText.value = ''
  await nextTick()
  scrollToBottom()

  streaming.value = true
  streamBuffer.value = ''
  let fullContent = ''
  let citations = []

  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        sessionId: sessionId.value,
        question,
        knowledgeBaseIds: [],
      }),
    })

    if (!response.ok || !response.body) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        if (buffer) {
          processLines(buffer.split('\n').filter((line) => line.startsWith('data:')))
        }
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      processLines(lines.filter((line) => line.startsWith('data:')))
    }

    function processLines(lines) {
      for (const line of lines) {
        try {
          const data = JSON.parse(line.slice(5))
          if (data.isDone) {
            citations = data.citations || []
            if (data.content) {
              fullContent += data.content
              streamBuffer.value = fullContent
            }
          } else if (data.content) {
            fullContent += data.content
            streamBuffer.value = fullContent
            scrollToBottom()
          }
        } catch (e) {
          console.warn('解析流式响应失败:', e, line)
        }
      }
    }

    messages.value.push({
      role: 'assistant',
      content: fullContent || streamBuffer.value || '没有收到回答，请稍后重试。',
      citations,
    })
    await nextTick()
    messages.value = [...messages.value]
  } catch (error) {
    console.error('流式请求失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '请求失败，请检查网络或后端服务。',
      citations: [],
    })
  } finally {
    streaming.value = false
    streamBuffer.value = ''
    await nextTick()
    scrollToBottom()
  }
}

function clearChat() {
  messages.value = []
  streamBuffer.value = ''
  sessionId.value = 'session-' + Date.now()
  restoredLogId.value = ''
  if (route.query.logId) router.replace({ path: '/chat' })
}

function scrollToBottom() {
  if (!msgEl.value) return
  setTimeout(() => {
    msgEl.value.scrollTop = msgEl.value.scrollHeight
  }, 40)
}
</script>

<style scoped>
.chat-page {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--bg);
}

/* ─── hero / centered composer ─── */
.chat-hero {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  padding: 32px 20px;
}

.hero-inner {
  width: 100%;
  max-width: 760px;
  text-align: center;
  animation: fade-up 0.4s ease;
}

.welcome-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin: 0 auto;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--primary-light), var(--primary-active));
  color: var(--on-dark);
  box-shadow: 0 10px 32px var(--welcome-shadow);
}

.hero-inner h1 {
  margin: 24px 0 8px;
  color: var(--ink);
  font-size: 36px;
  font-weight: 300;
  letter-spacing: -0.025em;
  text-wrap: balance;
}

.hero-sub {
  max-width: 520px;
  margin: 0 auto;
  color: var(--ink-2);
  font-size: 15px;
  line-height: 1.7;
}

.hero-composer {
  max-width: 720px;
  margin: 32px auto 0;
  text-align: left;
}

.composer-hint {
  margin: 12px 0 0;
  color: var(--ink-3);
  font-size: 11px;
  text-align: center;
}

.suggestions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 720px;
  margin: 32px auto 0;
  text-align: left;
}

.suggestion {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  background: var(--suggestion-bg);
  border: 1px solid var(--glass-border-soft);
  border-radius: 14px;
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;
}

.suggestion:hover {
  background: var(--suggestion-hover-bg);
  border-color: var(--suggestion-hover-border);
  transform: translateY(-1px);
}

.suggestion-icon {
  margin-bottom: 8px;
  color: var(--primary-light);
}

.suggestion-title {
  font-size: 13px;
  font-weight: 600;
}

.suggestion-desc {
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.5;
}

/* ─── messages ─── */
.chat-scroll {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 24px 8px;
}

.chat-column {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
}

.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  animation: fade-up 0.2s ease;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.assistant-avatar {
  background: var(--primary-soft);
  border: 1px solid var(--primary-100);
  color: var(--primary-light);
}

.user-avatar {
  background: var(--secondary-soft);
  border: 1px solid var(--user-avatar-border);
  color: var(--secondary);
}

.msg-body {
  max-width: 76%;
}

.msg-row.user .msg-body {
  max-width: 72%;
}

.bubble {
  padding: 16px 16px;
  background: var(--bubble-bg);
  border: 1px solid var(--glass-border-soft);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
  box-shadow: none;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.msg-row.user .bubble {
  background: var(--primary-soft);
  border-color: var(--primary-100);
  border-bottom-left-radius: 14px;
  border-bottom-right-radius: 4px;
}

.markdown :deep(p) {
  margin: 0 0 8px;
}

.markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown :deep(li) {
  margin: 4px 0;
}

.markdown :deep(code) {
  padding: 4px 4px;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 12px;
}

.markdown :deep(pre) {
  margin: 12px 0;
  padding: 12px;
  overflow-x: auto;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.markdown :deep(pre code) {
  padding: 0;
  background: none;
  border: none;
}

.markdown :deep(blockquote) {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 2px solid var(--primary-100);
  color: var(--ink-2);
}

.markdown :deep(a) {
  color: var(--primary-light);
}

/* ─── citations ─── */
.citations {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.citations-label {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 600;
}

.citation {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 12px;
  margin-top: 8px;
  background: var(--surface-subtle);
  border: 1px solid var(--border);
  border-radius: 8px;
}

.citation-icon {
  margin-top: 2px;
  flex-shrink: 0;
  color: var(--primary-light);
}

.citation-copy {
  min-width: 0;
}

.citation-file {
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 600;
}

.citation-snippet {
  display: -webkit-box;
  margin-top: 1px;
  overflow: hidden;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

/* ─── streaming ─── */
.thinking {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.thinking span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-light);
  opacity: 0.35;
  animation: pulse 1.1s infinite ease-in-out;
}

.thinking span:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking span:nth-child(3) {
  animation-delay: 0.3s;
}

.cursor {
  display: inline-block;
  width: 2px;
  height: 15px;
  margin-left: 2px;
  vertical-align: -2px;
  background: var(--primary-light);
  animation: blink 0.9s step-end infinite;
}

/* ─── docked composer ─── */
.composer-area {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
  padding: 12px 24px 20px;
  background: linear-gradient(to top, var(--dock-fade) 45%, transparent);
}

.composer-column {
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  50% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@media (max-width: 1023px) {
  .chat-hero {
    padding: 24px 16px;
  }

  .chat-scroll {
    padding: 24px 16px 8px;
  }

  .composer-area {
    padding: 12px 16px 16px;
  }
}

@media (max-width: 767px) {
  .hero-inner h1 {
    font-size: 24px;
  }

  .suggestions {
    grid-template-columns: 1fr;
    margin-top: 24px;
  }

  .chat-hero {
    align-items: flex-start;
    padding-top: 48px;
  }

  .msg-body,
  .msg-row.user .msg-body {
    max-width: 88%;
  }

  .avatar {
    width: 28px;
    height: 28px;
  }
}

@media (max-width: 480px) {
  .chat-scroll {
    padding: 16px 12px 8px;
  }

  .composer-area {
    padding: 8px 12px 12px;
  }

  .bubble {
    padding: 12px 12px;
    font-size: 13.5px;
  }

  .welcome-mark {
    width: 48px;
    height: 48px;
    border-radius: 14px;
  }
}
</style>
