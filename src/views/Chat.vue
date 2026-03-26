<template>
  <div class="chat-page">
    <!-- 消息区 -->
    <div class="chat-messages" ref="msgEl">
      <!-- 欢迎 -->
      <div v-if="messages.length === 0" class="chat-welcome">
        <div class="welcome-icon">🌸</div>
        <div class="welcome-title">你好！我是 AI 助手</div>
        <div class="welcome-sub">请问有什么可以帮助你的吗？</div>
        <div class="welcome-tips">
          <div v-for="tip in quickTips" :key="tip" class="tip-chip" @click="sendTip(tip)">
            {{ tip }}
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="(msg, i) in messages" :key="i" class="msg-row" :class="msg.role">
        <div v-if="msg.role === 'assistant'" class="msg-avatar">🌸</div>
        <div class="msg-bubble">
          <div class="msg-content" v-html="renderMd(msg.content)"></div>
          <!-- 引用来源 -->
          <div v-if="msg.citations?.length" class="citations">
            <div class="citations-title">📎 来源引用</div>
            <div v-for="c in msg.citations" :key="c.fileId" class="citation-item">
              <span class="citation-file">{{ c.fileName }}</span>
              <span class="citation-snippet">{{ c.snippet }}</span>
            </div>
          </div>
        </div>
        <div v-if="msg.role === 'user'" class="msg-avatar user-av">
          {{ auth.username?.charAt(0).toUpperCase() }}
        </div>
      </div>

      <!-- 流式中 -->
      <div v-if="streaming" class="msg-row assistant">
        <div class="msg-avatar">🌸</div>
        <div class="msg-bubble">
          <div class="msg-content" v-html="renderMd(streamBuffer)"></div>
          <span class="cursor-blink">▋</span>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input-area">
      <div class="input-wrap">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 6 }"
          placeholder="请输入您的问题..."
          resize="none"
          class="chat-textarea"
          @keydown.enter.prevent="handleEnter"
        />
        <div class="input-actions">
          <el-button
            circle
            @click="clearChat"
            title="清空对话"
            :icon="Delete"
            style="font-size:16px"
          />
          <el-button
            type="primary"
            circle
            :disabled="!inputText.trim() || streaming"
            @click="sendMessage"
            style="font-size:18px"
          >
            <span v-if="streaming">●</span>
            <span v-else>↑</span>
          </el-button>
        </div>
      </div>
      <div class="input-hint">Enter 发送 · Shift+Enter 换行</div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, inject } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { marked } from 'marked'

const auth    = useAuthStore()
const toast   = inject('toast')

const messages    = ref([])
const inputText   = ref('')
const streaming   = ref(false)
const streamBuffer = ref('')
const msgEl       = ref(null)

const quickTips = [
  '如何使用知识库功能？',
  '系统支持哪些文件格式？',
  '如何配置 DeepSeek API Key？',
]

function renderMd(text) {
  if (!text) return ''
  return marked.parse(text, { breaks: true })
}

function handleEnter(e) {
  if (e.shiftKey) return
  sendMessage()
}

function sendTip(tip) {
  inputText.value = tip
  sendMessage()
}

async function sendMessage() {
  const q = inputText.value.trim()
  if (!q || streaming.value) return

  messages.value.push({ role: 'user', content: q })
  inputText.value = ''
  await nextTick()
  scrollToBottom()

  streaming.value    = true
  streamBuffer.value = ''

  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId: 'session-' + Date.now(),
        question: q,
        knowledgeBaseIds: []
      })
    })

    const reader  = response.body.getReader()
    const decoder = new TextDecoder()
    let citations = []

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        const finalChunk = decoder.decode()
        if (finalChunk) streamBuffer.value += finalChunk
        streamBuffer.value = String(streamBuffer.value)
        await nextTick(); scrollToBottom()
        break
      }

      const chunk = decoder.decode(value, { stream: true })
      const lines = chunk.split('\n').filter(l => l.startsWith('data:'))

      for (const line of lines) {
        try {
          const data = JSON.parse(line.slice(5))
          if (data.isDone) {
            citations = data.citations || []
          } else if (data.content) {
            streamBuffer.value += data.content
            await nextTick(); scrollToBottom()
          }
        } catch { /* 忽略解析错误 */ }
      }
    }
    messages.value.push({ role: 'assistant', content: streamBuffer.value, citations })
  } catch (error) {
    console.error('流传输错误:', error)
    messages.value.push({ role: 'assistant', content: '⚠️ 请求失败，请检查网络或 API 配置。', citations: [] })
  } finally {
    streaming.value    = false
    await nextTick()
    streamBuffer.value = ''
    scrollToBottom()
  }
}

function clearChat() { messages.value = [] }

function scrollToBottom() {
  if (msgEl.value) msgEl.value.scrollTop = msgEl.value.scrollHeight
}
</script>

<style scoped>
.chat-page { display:flex; flex-direction:column; height:100vh; background:var(--bg-base); }

.chat-messages {
  flex: 1; overflow-y:auto;
  padding: 28px 20%;
  display: flex; flex-direction:column; gap:20px;
}

.chat-welcome {
  display:flex; flex-direction:column;
  align-items:center; justify-content:center;
  flex:1; gap:10px; padding-top:80px;
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:none; } }

.welcome-icon  { font-size:48px; color:var(--accent); }
.welcome-title { font-size:20px; font-weight:600; }
.welcome-sub   { font-size:14px; color:var(--text-muted); }
.welcome-tips  { display:flex; flex-wrap:wrap; gap:8px; margin-top:10px; justify-content:center; }
.tip-chip {
  padding:6px 14px; border:1px solid var(--border-strong);
  border-radius:20px; font-size:13px; color:var(--text-secondary);
  cursor:pointer; transition:all 0.2s;
}
.tip-chip:hover { border-color:var(--accent); color:var(--accent); background:var(--accent-dim); }

.msg-row { display:flex; gap:12px; align-items:flex-start; animation:fadeIn 0.2s ease; }
.msg-row.user { flex-direction:row-reverse; }

.msg-avatar {
  width:32px; height:32px;
  background:var(--accent-dim); color:var(--accent);
  border-radius:50%; display:flex; align-items:center; justify-content:center;
  font-size:14px; flex-shrink:0;
}
.user-av { background:var(--blue-dim); color:var(--blue); font-weight:700; font-size:13px; }

.msg-bubble {
  max-width:72%; background:var(--bg-card);
  border:1px solid var(--border); border-radius:12px;
  padding:12px 16px; font-size:14px; line-height:1.7;
}
.msg-row.user .msg-bubble {
  background:var(--blue-dim); border-color:rgba(59,130,246,0.3); color:var(--text-primary);
}

.msg-content :deep(p) { margin:0 0 8px; }
.msg-content :deep(p:last-child) { margin-bottom:0; }
.msg-content :deep(code) { background:rgba(0,0,0,0.3); padding:1px 5px; border-radius:4px; font-family:var(--font-mono); font-size:12px; }
.msg-content :deep(pre) { background:rgba(0,0,0,0.3); padding:12px; border-radius:8px; overflow-x:auto; margin:8px 0; }
.msg-content :deep(pre code) { background:none; padding:0; }

.cursor-blink { display:inline-block; color:var(--accent); animation:blink 0.8s step-end infinite; }
@keyframes blink { 50% { opacity:0; } }

.citations { margin-top:12px; padding-top:10px; border-top:1px solid var(--border); }
.citations-title { font-size:11px; color:var(--text-muted); margin-bottom:6px; letter-spacing:0.04em; }
.citation-item { display:flex; gap:8px; align-items:flex-start; font-size:12px; margin-bottom:4px; }
.citation-file { color:var(--accent); flex-shrink:0; }
.citation-snippet { color:var(--text-muted); line-height:1.4; }

.chat-input-area {
  padding:16px 20%; background:var(--bg-base); border-top:1px solid var(--border);
}
.input-wrap {
  display:flex; gap:10px; align-items:flex-end;
  background:var(--bg-card); border:1px solid var(--border-strong);
  border-radius:14px; padding:10px 12px; transition:border-color 0.2s;
}
.input-wrap:focus-within { border-color:var(--accent); box-shadow:0 0 0 3px var(--accent-dim); }
.input-actions { display:flex; align-items:center; gap:6px; }
.input-hint { text-align:center; font-size:11px; color:var(--text-muted); margin-top:8px; }

/* 覆盖 el-input textarea 的边框和背景 */
.chat-textarea :deep(.el-textarea__inner) {
  background:transparent !important;
  border:none !important;
  box-shadow:none !important;
  color:var(--text-primary);
  font-size:14px;
  padding:0;
  resize:none;
}
</style>