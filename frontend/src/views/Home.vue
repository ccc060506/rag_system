<template>
  <div class="home">
    <header class="site-nav">
      <a href="#top" class="brand" aria-label="RAG-AI 首页">
        <span class="brand-mark"><Sparkles :size="16" :stroke-width="2" /></span>
        <span class="brand-name">RAG-AI</span>
        <span class="brand-sub">企业知识库</span>
      </a>

      <nav class="nav-links" aria-label="页面导航">
        <a href="#product">产品</a>
        <a href="#knowledge">知识库</a>
        <a href="#security">安全</a>
      </nav>

      <div class="nav-actions">
        <RouterLink v-if="!auth.isLoggedIn" to="/login" class="nav-login">登录</RouterLink>
        <RouterLink :to="workspacePath" class="btn-primary nav-cta">进入工作台</RouterLink>
      </div>
    </header>

    <main>
      <section id="top" class="hero">
        <Starfield />

        <div class="hero-inner">
          <h1>让知识可检索<br />让回答有出处。</h1>
          <p class="hero-sub">上传文档，构建你的企业知识库。每个回答都带可追溯的原文引用。</p>
          <div class="hero-actions">
            <RouterLink :to="workspacePath" class="btn-primary">进入工作台</RouterLink>
            <a href="#product" class="btn-ghost">看看能力</a>
          </div>
        </div>

        <div class="hero-shot">
          <img
            :src="shotChat"
            alt="AI 问答界面：基于知识库回答问题，并在回答下方列出引用来源"
            width="1440"
            height="680"
          />
        </div>
      </section>

      <section id="product" class="feature shell">
        <div class="feature-copy">
          <h2>回答，基于你的文档</h2>
          <p class="feature-lede">
            先检索知识库，再由模型组织答案。每条回答附上来源文件，点开就能回到原文。
          </p>
          <ul class="fact-list">
            <li>
              <Search :size="18" />
              <div>
                <h3>先检索，再回答</h3>
                <p>从知识库里找到相关原文，再交给模型组织成回答。</p>
              </div>
            </li>
            <li>
              <Paperclip :size="18" />
              <div>
                <h3>引用可追溯</h3>
                <p>每条回答列出引用的文件，答案与出处一一对应。</p>
              </div>
            </li>
            <li>
              <History :size="18" />
              <div>
                <h3>对话留痕</h3>
                <p>历史对话、使用的模型与 Token 消耗都可以查。</p>
              </div>
            </li>
          </ul>
        </div>
        <figure class="feature-shot">
          <img
            :src="shotAnswer"
            alt="对话详情：用户提问、AI 回答与来源文件"
            width="700"
            height="500"
          />
        </figure>
      </section>

      <section id="knowledge" class="feature feature-reverse shell">
        <figure class="feature-shot">
          <img
            :src="shotKnowledge"
            alt="知识库页面：文档列表、类型、大小、上传时间与处理状态"
            width="900"
            height="500"
          />
        </figure>
        <div class="feature-copy">
          <h2>文档进来，知识成形</h2>
          <p class="feature-lede">
            PDF、Word、Excel、TXT 直接上传，自动解析成可检索的片段，随问随用。
          </p>
          <ul class="fact-list">
            <li>
              <FileText :size="18" />
              <div>
                <h3>四种格式</h3>
                <p>PDF、Word、Excel、TXT 都能直接上传。</p>
              </div>
            </li>
            <li>
              <Scissors :size="18" />
              <div>
                <h3>自动解析切分</h3>
                <p>上传后生成可检索的知识片段，不需要手工整理。</p>
              </div>
            </li>
            <li>
              <Database :size="18" />
              <div>
                <h3>公开或私有</h3>
                <p>按文档选择是否进入全站检索范围。</p>
              </div>
            </li>
          </ul>
        </div>
      </section>

      <section id="security" class="feature shell">
        <div class="feature-copy">
          <h2>敏感内容，先过滤</h2>
          <p class="feature-lede">
            内置 DFA 敏感词过滤，按分类分级拦截或替换；每次问答都有记录。
          </p>
          <ul class="fact-list">
            <li>
              <ShieldCheck :size="18" />
              <div>
                <h3>实时过滤</h3>
                <p>敏感词在请求进入模型前完成匹配与处理。</p>
              </div>
            </li>
            <li>
              <Layers :size="18" />
              <div>
                <h3>分级处理</h3>
                <p>按分类选择拦截或替换，规则实时同步到过滤器。</p>
              </div>
            </li>
            <li>
              <ScrollText :size="18" />
              <div>
                <h3>全量审计</h3>
                <p>问答、来源文件与 Token 消耗都记录在案。</p>
              </div>
            </li>
          </ul>
        </div>
        <figure class="feature-shot">
          <img
            :src="shotSafety"
            alt="敏感词分类页面：按分类统计敏感词数量与处理等级"
            width="1312"
            height="520"
          />
        </figure>
      </section>

      <section class="steps shell">
        <h2>从第一份文档，到第一个答案</h2>
        <ol class="step-list">
          <li>
            <h3><Upload :size="17" /> 上传文档</h3>
            <p>拖入文件，选择是否公开。</p>
          </li>
          <li>
            <h3><Scissors :size="17" /> 自动切分</h3>
            <p>解析文本，生成可检索片段。</p>
          </li>
          <li>
            <h3><MessageSquare :size="17" /> 开始提问</h3>
            <p>回答基于原文，并附上引用。</p>
          </li>
        </ol>
      </section>

      <section class="assurance shell">
        <div class="assurance-copy">
          <h2>密钥留在环境里，数据留在你的库里</h2>
          <p>
            模型与存储凭据从环境变量读取。每个账号可以绑定自己的 DeepSeek API Key，配额与配置互不干扰。
          </p>
        </div>
        <ul class="assurance-list">
          <li>
            <KeyRound :size="18" />
            <div>
              <h3>环境变量注入</h3>
              <p>代码里不出现任何密钥或第三方凭据。</p>
            </div>
          </li>
          <li>
            <Lock :size="18" />
            <div>
              <h3>账号级配置</h3>
              <p>每个用户独立 API Key、模型偏好与额度。</p>
            </div>
          </li>
          <li>
            <ListChecks :size="18" />
            <div>
              <h3>可审计</h3>
              <p>对话、来源与 Token 都有记录可回查。</p>
            </div>
          </li>
        </ul>
      </section>

      <section class="cta shell">
        <h2>从第一份文档开始</h2>
        <p>登录后上传资料，直接开始提问。</p>
        <RouterLink :to="workspacePath" class="btn-primary">进入工作台</RouterLink>
      </section>
    </main>

    <footer class="site-footer shell">
      <div class="footer-brand">
        <span class="brand-mark"><Sparkles :size="14" :stroke-width="2" /></span>
        <span class="brand-name">RAG-AI</span>
      </div>
      <nav class="footer-links" aria-label="页脚导航">
        <a href="#product">产品</a>
        <a href="#knowledge">知识库</a>
        <a href="#security">安全</a>
        <RouterLink to="/login">登录</RouterLink>
      </nav>
    </footer>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Database,
  FileText,
  History,
  KeyRound,
  Layers,
  ListChecks,
  Lock,
  MessageSquare,
  Paperclip,
  Scissors,
  ScrollText,
  Search,
  ShieldCheck,
  Sparkles,
  Upload,
} from 'lucide-vue-next'
import Starfield from '@/components/Starfield.vue'
import { useAuthStore } from '@/stores/auth'
import shotAnswer from '@/assets/shots/answer.jpg'
import shotChat from '@/assets/shots/chat.jpg'
import shotKnowledge from '@/assets/shots/knowledge.jpg'
import shotSafety from '@/assets/shots/safety.jpg'

const auth = useAuthStore()
const workspacePath = computed(() => (auth.isLoggedIn ? '/chat' : '/login'))
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--paper);
  color: var(--ink);
  overflow-x: clip;
}

.shell {
  width: min(1240px, calc(100% - 96px));
  margin-inline: auto;
}

.home :focus-visible {
  outline: 2px solid var(--focus);
  outline-offset: 3px;
}

/* ─── nav ─── */
.site-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 32px;
  height: 64px;
  padding-inline: max(48px, calc((100% - 1240px) / 2));
  background: var(--paper);
  border-bottom: 1px solid var(--rule);
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink);
  text-decoration: none;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--accent);
  color: var(--accent-ink);
}

.brand-name {
  font-size: 15px;
  font-weight: 500;
  letter-spacing: -0.01em;
}

.brand-sub {
  color: var(--muted);
  font-size: 12px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-left: 24px;
}

.nav-links a {
  color: var(--ink-2);
  font-size: 14px;
  text-decoration: none;
}

.nav-links a:hover {
  color: var(--ink);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-left: auto;
}

.nav-login {
  color: var(--ink-2);
  font-size: 14px;
  text-decoration: none;
}

.nav-login:hover {
  color: var(--ink);
}

.btn-primary,
.btn-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 20px;
  font-size: 14px;
  font-family: var(--font-body);
  text-decoration: none;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.btn-primary {
  background: var(--accent);
  color: var(--accent-ink);
}

.btn-primary:hover {
  background: var(--accent-2);
  color: var(--accent-ink);
}

.btn-ghost {
  border: 1px solid var(--rule-2);
  color: var(--ink);
}

.btn-ghost:hover {
  border-color: var(--muted);
  background: var(--paper-3);
}

.nav-cta {
  min-height: 34px;
  padding: 0 16px;
  font-size: 13px;
}

/* ─── hero ─── */
.hero {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 80vh;
  padding-top: 120px;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  height: 33%;
  background: linear-gradient(to bottom, transparent, var(--paper));
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 2;
  width: min(1240px, calc(100% - 96px));
  margin-inline: auto;
  padding-bottom: 56px;
  animation: hero-rise 0.6s ease-out both;
}

.hero h1 {
  max-width: 14ch;
  margin: 0;
  color: var(--ink);
  font-size: clamp(44px, 5.6vw, 76px);
  font-weight: 300;
  line-height: 1.12;
  letter-spacing: -0.03em;
  overflow-wrap: break-word;
}

.hero-sub {
  max-width: 36em;
  margin: 24px 0 0;
  color: var(--ink-2);
  font-size: 17px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 32px;
}

.hero-shot {
  position: relative;
  z-index: 1;
  width: min(1240px, calc(100% - 96px));
  margin: 48px auto 0;
  overflow: hidden;
  border: 1px solid var(--rule-2);
  border-bottom: 0;
  background: var(--paper-2);
  animation: hero-rise 0.8s 0.1s ease-out both;
  mask-image: linear-gradient(to bottom, black 78%, transparent 100%);
  -webkit-mask-image: linear-gradient(to bottom, black 78%, transparent 100%);
}

.hero-shot img {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 7.5;
  object-fit: cover;
  object-position: 50% 42%;
}

/* ─── features ─── */
.feature {
  display: grid;
  grid-template-columns: minmax(0, 0.8fr) minmax(0, 1.2fr);
  gap: 72px;
  align-items: center;
  padding: 120px 0;
  border-top: 1px solid var(--rule);
}

.feature-reverse {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.8fr);
}

.feature h2,
.steps h2,
.assurance h2,
.cta h2 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(30px, 3.4vw, 44px);
  font-weight: 300;
  line-height: 1.2;
  letter-spacing: -0.025em;
  overflow-wrap: break-word;
}

.feature-lede {
  max-width: 32em;
  margin: 20px 0 0;
  color: var(--ink-2);
  font-size: 16px;
  line-height: 1.75;
}

.fact-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin: 36px 0 0;
  padding: 0;
  list-style: none;
}

.fact-list li {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.fact-list svg {
  flex-shrink: 0;
  margin-top: 4px;
  color: var(--muted);
}

.fact-list h3,
.assurance-list h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 500;
}

.fact-list p,
.assurance-list p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.65;
}

.feature-shot {
  margin: 0;
  border: 1px solid var(--rule-2);
  background: var(--paper-2);
}

.feature-shot img {
  display: block;
  width: 100%;
  height: auto;
}

/* ─── steps ─── */
.steps {
  padding: 120px 0;
  border-top: 1px solid var(--rule);
}

.step-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 48px;
  margin: 48px 0 0;
  padding: 0;
  list-style: none;
}

.step-list li {
  padding-top: 24px;
  border-top: 1px solid var(--rule-2);
}

.step-list h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 0;
  font-size: 18px;
  font-weight: 500;
}

.step-list h3 svg {
  color: var(--muted);
}

.step-list p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.65;
}

/* ─── assurance ─── */
.assurance {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 72px;
  padding: 120px 0;
  border-top: 1px solid var(--rule);
}

.assurance-copy p {
  max-width: 30em;
  margin: 20px 0 0;
  color: var(--ink-2);
  font-size: 16px;
  line-height: 1.75;
}

.assurance-list {
  display: flex;
  flex-direction: column;
  gap: 28px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.assurance-list li {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--rule);
}

.assurance-list li:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.assurance-list svg {
  flex-shrink: 0;
  margin-top: 4px;
  color: var(--muted);
}

/* ─── cta ─── */
.cta {
  padding: 120px 0;
  border-top: 1px solid var(--rule);
  text-align: center;
}

.cta p {
  margin: 20px 0 32px;
  color: var(--ink-2);
  font-size: 16px;
}

/* ─── footer ─── */
.site-footer {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 32px 0 48px;
  border-top: 1px solid var(--rule);
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-brand .brand-mark {
  width: 24px;
  height: 24px;
  background: var(--paper-3);
  color: var(--accent);
}

.footer-links {
  display: flex;
  gap: 24px;
}

.footer-links a {
  color: var(--muted);
  font-size: 13px;
  text-decoration: none;
}

.footer-links a:hover {
  color: var(--ink);
}

@keyframes hero-rise {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* ─── responsive ─── */
@media (max-width: 1023px) {
  .site-nav {
    padding-inline: 24px;
  }

  .nav-links {
    display: none;
  }

  .shell,
  .hero-inner,
  .hero-shot {
    width: calc(100% - 48px);
  }

  .feature,
  .feature-reverse,
  .assurance {
    grid-template-columns: minmax(0, 1fr);
    gap: 40px;
    padding: 80px 0;
  }

  .feature-reverse .feature-shot {
    order: 2;
  }

  .step-list {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .steps,
  .cta {
    padding: 80px 0;
  }

  .site-footer {
    flex-wrap: wrap;
    gap: 16px 24px;
  }
}

@media (max-width: 767px) {
  .site-nav {
    gap: 12px;
    padding-inline: 20px;
  }

  .brand-sub,
  .nav-login {
    display: none;
  }

  .hero {
    min-height: 0;
    padding-top: 88px;
  }

  .shell,
  .hero-inner,
  .hero-shot {
    width: calc(100% - 40px);
  }

  .hero h1 {
    font-size: 40px;
  }

  .hero-sub {
    font-size: 15px;
  }

  .hero-actions {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .btn-primary,
  .btn-ghost {
    width: 100%;
  }

  .hero-shot {
    margin-top: 40px;
  }

  .hero-shot img {
    aspect-ratio: 4 / 3;
    object-position: 50% 35%;
  }

  .feature,
  .feature-reverse,
  .assurance {
    padding: 64px 0;
  }

  .feature h2,
  .steps h2,
  .assurance h2,
  .cta h2 {
    font-size: 30px;
  }

  .steps,
  .cta {
    padding: 64px 0;
  }

  .site-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-inner,
  .hero-shot {
    animation: none;
  }
}
</style>
