<template>
  <div class="page-content">
    <PageHeader title="API 配置" subtitle="管理 DeepSeek API Key 与使用配额">
      <template #actions>
        <RouterLink to="/user" class="back-link">
          <el-button class="with-icon">
            <span>返回个人中心</span>
          </el-button>
        </RouterLink>
      </template>
    </PageHeader>

    <ErrorState v-if="configError" title="配置加载失败" :message="configError" @retry="loadConfig" />

    <template v-else>
      <el-card class="key-card" v-loading="loading">
        <div class="key-head">
          <div class="key-icon">
            <Key :size="18" />
          </div>
          <div class="key-heading">
            <div class="section-title">DeepSeek API Key</div>
            <div class="section-desc">绑定后用于调用对话模型</div>
          </div>
          <el-tag :type="configInfo.apiKey ? 'success' : 'info'" effect="plain">
            {{ configInfo.apiKey ? '已配置' : '未配置' }}
          </el-tag>
        </div>

        <div v-if="configInfo.apiKey" class="key-box">
          <div class="key-main">
            <CheckCircle :size="16" class="ok-icon" />
            <span class="key-masked">{{ configInfo.apiKey }}</span>
          </div>
          <div class="key-sub">
            <el-tag type="info" effect="plain" size="small">{{ configInfo.preferredModel || '—' }}</el-tag>
            <span>已通过验证</span>
          </div>
          <el-button @click="openKeyEdit">更换</el-button>
        </div>

        <div v-else class="key-empty">
          <div>
            <div class="empty-title">尚未配置 API Key</div>
            <div class="empty-desc">配置后才能使用 AI 问答</div>
          </div>
          <el-button type="primary" @click="openKeyEdit">立即配置</el-button>
        </div>
      </el-card>

      <el-card class="quota-card" v-loading="loading">
        <div class="section-title">Token 使用情况</div>
        <div class="section-desc">账号当前的模型调用额度</div>

        <div class="quota-stats">
          <div class="quota-stat">
            <div class="quota-num">{{ formatNum(configInfo.usedTokenQuota) }}</div>
            <div class="quota-label">已使用</div>
          </div>
          <div class="quota-stat">
            <div class="quota-num accent">{{ formatNum(remainingTokens) }}</div>
            <div class="quota-label">剩余</div>
          </div>
          <div class="quota-stat">
            <div class="quota-num">{{ formatNum(configInfo.totalTokenQuota) }}</div>
            <div class="quota-label">总配额</div>
          </div>
        </div>

        <div class="progress-head">
          <span>使用率</span>
          <span class="pct">{{ usageRate.toFixed(1) }}%</span>
        </div>
        <el-progress :percentage="usageRate" :stroke-width="8" :show-text="false" />

        <el-divider />

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">当前模型</span>
            <span class="detail-value model">{{ configInfo.preferredModel || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">最大知识库数</span>
            <span class="detail-value">{{ configInfo.maxKnowledgeBases ?? '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">最大存储空间</span>
            <span class="detail-value">
              {{ configInfo.maxStorageSize ? configInfo.maxStorageSize + ' MB' : '—' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">API Key</span>
            <span class="detail-value mono">{{ configInfo.apiKey || '未配置' }}</span>
          </div>
        </div>
      </el-card>
    </template>

    <el-dialog v-model="showKeyEdit" title="配置 API Key" width="460px" destroy-on-close>
      <el-form :model="keyForm" label-position="top" @submit.prevent>
        <el-form-item label="DeepSeek API Key">
          <el-input
            v-model="keyForm.apiKey"
            type="password"
            show-password
            placeholder="sk-xxxxxxxxxxxx"
            @keyup.enter="saveKey"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-hint">Key 仅用于调用 DeepSeek，服务端会加密存储并脱敏展示。</p>

      <template #footer>
        <el-button @click="showKeyEdit = false">取消</el-button>
        <el-button type="primary" :loading="savingKey" @click="saveKey">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { CheckCircle, Key } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { userApi } from '@/api'

const toast = inject('toast')

const configInfo = ref({})
const loading = ref(false)
const configError = ref('')
const showKeyEdit = ref(false)
const savingKey = ref(false)
const keyForm = ref({ apiKey: '' })

const usageRate = computed(() => {
  const rate = Number(configInfo.value.tokenUsageRate)
  return Number.isFinite(rate) ? Math.min(100, Math.max(0, rate)) : 0
})

const remainingTokens = computed(() => {
  const total = Number(configInfo.value.totalTokenQuota) || 0
  const used = Number(configInfo.value.usedTokenQuota) || 0
  return Math.max(0, total - used)
})

function formatNum(value) {
  return Number(value || 0).toLocaleString()
}

function openKeyEdit() {
  keyForm.value = { apiKey: '' }
  showKeyEdit.value = true
}

async function saveKey() {
  const apiKey = keyForm.value.apiKey.trim()
  if (!apiKey) {
    toast('请输入 API Key', 'error')
    return
  }

  savingKey.value = true
  try {
    const res = await userApi.verifyKey(apiKey)
    if (res.code === 200) {
      toast('API Key 验证通过，已保存')
      showKeyEdit.value = false
      loadConfig()
    } else {
      toast(res.msg || 'API Key 无效', 'error')
    }
  } catch (e) {
    toast(e.userMessage || '保存失败', 'error')
  } finally {
    savingKey.value = false
  }
}

async function loadConfig() {
  loading.value = true
  configError.value = ''
  try {
    const res = await userApi.getConfig()
    if (res.code === 200) {
      configInfo.value = res.data || {}
    } else {
      configError.value = res.msg || '加载失败'
    }
  } catch (e) {
    configError.value = e.userMessage || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.back-link {
  text-decoration: none;
}

.key-card {
  margin-bottom: 16px;
}

.key-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.key-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
}

.key-heading {
  flex: 1;
  min-width: 0;
}

.section-title {
  color: var(--ink);
  font-size: 15px;
  font-weight: 600;
}

.section-desc {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.key-box {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--surface-subtle);
  border: 1px solid var(--border);
  border-radius: 10px;
  flex-wrap: wrap;
}

.key-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 180px;
}

.ok-icon {
  flex-shrink: 0;
  color: var(--success);
}

.key-masked {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.key-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-3);
  font-size: 12px;
}

.key-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: var(--surface-subtle);
  border: 1px dashed var(--border-strong);
  border-radius: 10px;
}

.empty-title {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.empty-desc {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.quota-card .section-desc {
  margin-bottom: 20px;
}

.quota-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.quota-stat {
  text-align: center;
}

.quota-num {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 500;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.quota-num.accent {
  color: var(--ink);
}

.quota-label {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--ink-2);
  font-size: 12.5px;
}

.pct {
  color: var(--accent);
  font-weight: 500;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  color: var(--ink-3);
  font-size: 11px;
}

.detail-value {
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 500;
}

.detail-value.model {
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 13px;
}

.detail-value.mono {
  overflow-wrap: anywhere;
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 400;
}

.dialog-hint {
  margin: 4px 0 0;
  color: var(--ink-3);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 767px) {
  .key-box {
    align-items: flex-start;
    flex-direction: column;
  }

  .key-sub {
    flex-wrap: wrap;
  }

  .key-empty {
    align-items: flex-start;
    flex-direction: column;
  }

  .quota-num {
    font-size: 21px;
  }
}

@media (max-width: 480px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .quota-stats {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .quota-stat {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    text-align: left;
  }

  .quota-label {
    margin-top: 0;
  }
}
</style>
