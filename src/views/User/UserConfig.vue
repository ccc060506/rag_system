<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">API 配置中心</div>
        <div class="page-subtitle">管理你的 DeepSeek API Key 与使用配额</div>
      </div>
      <RouterLink to="/user">
        <el-button>← 返回个人中心</el-button>
      </RouterLink>
    </div>

    <div class="config-layout">
      <!-- API Key 配置 -->
      <el-card class="key-card">
        <div class="section-title">🔑 DeepSeek API Key</div>
        <p class="section-desc">绑定你的 DeepSeek API Key，系统将使用该 Key 调用 AI 能力。</p>

        <div v-if="configInfo.apiKey" class="key-display">
          <el-icon style="font-size:20px;color:var(--accent)"><CircleCheckFilled /></el-icon>
          <div class="key-info">
            <div class="key-masked">{{ maskKey(configInfo.apiKey) }}</div>
            <div class="key-status">
              <el-tag type="success" size="small">已验证</el-tag>
              <span class="key-model">模型：{{ configInfo.preferredModel }}</span>
            </div>
          </div>
          <el-button style="margin-left:auto" @click="showKeyEdit = true">更换</el-button>
        </div>
        <div v-else class="key-empty">
          <div class="key-empty-text">尚未配置 API Key</div>
          <el-button type="primary" @click="showKeyEdit = true">立即配置</el-button>
        </div>
      </el-card>

      <!-- Token 配额卡 -->
      <el-card>
        <div class="section-title">📊 Token 使用情况</div>
        <div v-if="loadingConfig" style="padding:20px;text-align:center">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else class="quota-detail">
          <el-row :gutter="20" style="margin-bottom:20px">
            <el-col :span="8" style="text-align:center">
              <div class="quota-num">{{ formatNum(configInfo.usedTokenQuota) }}</div>
              <div class="quota-label">已使用 Tokens</div>
            </el-col>
            <el-col :span="8" style="text-align:center">
              <div class="quota-num accent">{{ formatNum(configInfo.totalTokenQuota - configInfo.usedTokenQuota) }}</div>
              <div class="quota-label">剩余 Tokens</div>
            </el-col>
            <el-col :span="8" style="text-align:center">
              <div class="quota-num">{{ formatNum(configInfo.totalTokenQuota) }}</div>
              <div class="quota-label">总配额</div>
            </el-col>
          </el-row>

          <div class="progress-header" style="display:flex;justify-content:space-between;font-size:13px;margin-bottom:6px;">
            <span>使用率</span>
            <span class="quota-pct-text">{{ configInfo.tokenUsageRate?.toFixed(1) }}%</span>
          </div>
          <el-progress
            :percentage="configInfo.tokenUsageRate || 0"
            :stroke-width="12"
            :show-text="false"
            style="margin-bottom:20px"
          />

          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="当前模型">
              <span class="model-badge">{{ configInfo.preferredModel || '—' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="最大知识库数">
              {{ configInfo.maxKnowledgeBases || '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="最大存储空间">
              {{ configInfo.maxStorageSize ? configInfo.maxStorageSize + ' MB' : '—' }}
            </el-descriptions-item>
            <el-descriptions-item label="API Key">
              <span class="key-val">{{ maskKey(configInfo.apiKey) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>
    </div>

    <!-- API Key 编辑弹窗 -->
    <el-dialog v-model="showKeyEdit" title="配置 API Key" width="460px" destroy-on-close>
      <el-form :model="keyForm" label-position="top">
        <el-form-item label="DeepSeek API Key">
          <el-input
            v-model="keyForm.apiKey"
            type="password"
            show-password
            placeholder="sk-xxxxxxxxxxxx"
          />
        </el-form-item>
        <el-form-item label="首选模型">
          <el-select v-model="keyForm.preferredModel" style="width:100%">
            <el-option label="deepseek-chat" value="deepseek-chat" />
            <el-option label="deepseek-coder" value="deepseek-coder" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showKeyEdit = false">取消</el-button>
        <el-button type="primary" :loading="savingKey" @click="saveKey">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, inject } from 'vue'
import { CircleCheckFilled, Loading } from '@element-plus/icons-vue'
import { userApi } from '@/api/index'

const toast        = inject('toast')
const configInfo   = ref({})
const loadingConfig = ref(false)
const showKeyEdit  = ref(false)
const savingKey    = ref(false)
const keyForm      = ref({ apiKey: '', preferredModel: 'deepseek-chat' })

function formatNum(n) { return n ? n.toLocaleString() : '0' }
function maskKey(key) {
  if (!key) return '未配置'
  return key.slice(0, 6) + '****' + key.slice(-4)
}

async function saveKey() {
  savingKey.value = true
  try {
    await userApi.verifyKey(keyForm.value.apiKey)
    toast('保存成功')
    showKeyEdit.value = false
    loadConfig()
  } catch { toast('保存失败', 'error') } finally { savingKey.value = false }
}

async function loadConfig() {
  loadingConfig.value = true
  try {
    const res = await userApi.getConfig()
    if (res.code === 200) {
      configInfo.value = res.data || {}
      keyForm.value = {
        apiKey: configInfo.value.apiKey || '',
        preferredModel: configInfo.value.preferredModel || 'deepseek-chat'
      }
    }
  } catch {
    configInfo.value = {
      apiKey: 'sk-0dcd57c2e4634123', preferredModel: 'deepseek-chat',
      totalTokenQuota: 1000000, usedTokenQuota: 234567,
      tokenUsageRate: 23.5, maxKnowledgeBases: 10, maxStorageSize: 500
    }
  } finally { loadingConfig.value = false }
}

onMounted(loadConfig)
</script>

<style scoped>
.page-content {
  padding: 28px 32px;
  height: 100vh;
  overflow-y: auto;
  background: transparent;
}

/* 页头标题颜色 */
.page-title    { color: var(--text-primary) !important; }
.page-subtitle { color: var(--text-muted)   !important; }
.page-header  { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.page-title   { font-size:20px; font-weight:700; color:var(--text-primary); }
.page-subtitle { font-size:13px; color:var(--text-muted); margin-top:3px; }

.config-layout { display:flex; flex-direction:column; gap:20px; }

.section-title { font-size:15px; font-weight:600; color:var(--text-primary); margin-bottom:6px; }
.section-desc  { font-size:13px; color:var(--text-muted); margin-bottom:16px; }

.key-display { display:flex; align-items:center; gap:12px; padding:14px; background:var(--bg-hover); border-radius:var(--radius); }
.key-info    { flex:1; }
.key-masked  { font-family:var(--font-mono); font-size:14px; color:var(--text-primary); }
.key-status  { display:flex; align-items:center; gap:8px; margin-top:4px; }
.key-model   { font-size:12px; color:var(--text-muted); }

.key-empty      { display:flex; align-items:center; justify-content:space-between; padding:14px; background:var(--bg-hover); border-radius:var(--radius); }
.key-empty-text { font-size:13px; color:var(--text-muted); }

.quota-num   { font-size:28px; font-weight:700; font-family:var(--font-mono); color:var(--text-primary); }
.quota-num.accent { color:var(--accent); }
.quota-label { font-size:12px; color:var(--text-muted); margin-top:4px; }
.quota-pct-text { color:var(--accent); font-weight:500; }

.model-badge { color:var(--accent); font-family:var(--font-mono); font-size:13px; }
.key-val     { font-family:var(--font-mono); font-size:12px; color:var(--text-secondary); }
</style>