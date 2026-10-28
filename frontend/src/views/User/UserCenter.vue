<template>
  <div class="page-content">
    <PageHeader title="个人中心" subtitle="管理账号信息与使用配额" />

    <div class="user-grid">
      <div class="left-col">
        <el-card class="profile-card" v-loading="userLoading">
          <ErrorState
            v-if="userError"
            title="用户信息加载失败"
            :message="userError"
            @retry="loadUser"
          />

          <template v-else>
            <div class="profile-head">
              <el-avatar :size="72" :src="userInfo.avatar || undefined" class="profile-avatar">
                {{ initial }}
              </el-avatar>
              <div class="profile-name">{{ userInfo.username || '—' }}</div>
              <div class="profile-email">{{ userInfo.email || '未设置邮箱' }}</div>
            </div>

            <div class="info-list">
              <div class="info-row">
                <span>年龄</span>
                <strong>{{ userInfo.age || '—' }}</strong>
              </div>
              <div class="info-row">
                <span>账号状态</span>
                <el-tag type="success" effect="plain">正常</el-tag>
              </div>
            </div>

            <el-button class="full-btn with-icon" @click="openEdit">
              <Pencil :size="15" />
              <span>编辑资料</span>
            </el-button>

            <RouterLink to="/user/config" class="config-link">
              <el-button type="primary" class="full-btn with-icon">
                <Settings :size="15" />
                <span>API 配置中心</span>
              </el-button>
            </RouterLink>
          </template>
        </el-card>

        <el-card class="quick-card">
          <div class="card-title">快捷入口</div>
          <RouterLink to="/chat" class="quick-link">
            <MessageSquare :size="16" class="quick-icon" />
            <span>开始 AI 问答</span>
            <ChevronRight :size="15" class="quick-chevron" />
          </RouterLink>
          <RouterLink to="/knowledge" class="quick-link">
            <BookOpen :size="16" class="quick-icon" />
            <span>管理知识库</span>
            <ChevronRight :size="15" class="quick-chevron" />
          </RouterLink>
          <RouterLink to="/logs" class="quick-link">
            <History :size="16" class="quick-icon" />
            <span>查看历史对话</span>
            <ChevronRight :size="15" class="quick-chevron" />
          </RouterLink>
        </el-card>
      </div>

      <div class="right-col">
        <el-card class="quota-card" v-loading="configLoading">
          <div class="card-head">
            <div>
              <div class="card-title">Token 配额</div>
              <div class="card-desc">当前账号的模型调用额度</div>
            </div>
            <el-tag type="info" effect="plain">实时</el-tag>
          </div>

          <ErrorState
            v-if="configError"
            title="配额加载失败"
            :message="configError"
            @retry="loadConfig"
          />

          <template v-else>
            <div class="quota-numbers">
              <span class="quota-used">{{ formatNum(configInfo.usedTokenQuota) }}</span>
              <span class="quota-sep">/</span>
              <span class="quota-total">{{ formatNum(configInfo.totalTokenQuota) }}</span>
              <span class="quota-unit">Tokens</span>
            </div>

            <el-progress :percentage="usageRate" :stroke-width="8" :show-text="false" />

            <div class="quota-meta">
              <span>已使用 {{ usageRate.toFixed(1) }}%</span>
              <span>剩余 {{ formatNum(remainingTokens) }} Tokens</span>
            </div>

            <el-divider />

            <div class="config-grid">
              <div class="config-item">
                <div class="config-label">当前模型</div>
                <div class="config-value model-value">{{ configInfo.preferredModel || '—' }}</div>
              </div>
              <div class="config-item">
                <div class="config-label">最大知识库数</div>
                <div class="config-value">{{ configInfo.maxKnowledgeBases ?? '—' }}</div>
              </div>
              <div class="config-item">
                <div class="config-label">最大存储空间</div>
                <div class="config-value">
                  {{ configInfo.maxStorageSize ? configInfo.maxStorageSize + ' MB' : '—' }}
                </div>
              </div>
              <div class="config-item">
                <div class="config-label">API Key</div>
                <div class="config-value key-value">{{ configInfo.apiKey || '未配置' }}</div>
              </div>
            </div>
          </template>
        </el-card>

        <el-card class="status-card">
          <div class="card-title">账号信息</div>
          <div class="status-row">
            <CheckCircle :size="16" class="status-icon success" />
            <span>账号状态正常，所有服务运行中</span>
          </div>
          <div class="status-row">
            <Key :size="16" class="status-icon" />
            <span>{{ configInfo.apiKey ? '已绑定 DeepSeek API Key' : '尚未绑定 DeepSeek API Key' }}</span>
          </div>
          <div class="status-row">
            <Database :size="16" class="status-icon" />
            <span>知识库问答服务已启用</span>
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="showEdit" title="编辑个人信息" width="440px" destroy-on-close>
      <el-form :model="editForm" label-position="top" @submit.prevent>
        <el-form-item label="昵称">
          <el-input v-model="editForm.username" placeholder="昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="editForm.age" :min="1" :max="120" style="width: 100%" />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleAvatarChange"
          >
            <img v-if="imageUrl" :src="imageUrl" class="avatar-preview" alt="头像预览" />
            <div v-else class="avatar-placeholder">
              <Plus :size="20" />
            </div>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import {
  BookOpen,
  CheckCircle,
  ChevronRight,
  Database,
  History,
  Key,
  MessageSquare,
  Pencil,
  Plus,
  Settings,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { userApi } from '@/api'

const OSS_REGION = import.meta.env.VITE_OSS_REGION || 'oss-cn-beijing'
const OSS_BUCKET = import.meta.env.VITE_OSS_BUCKET || 'rag-system-ccc'

const toast = inject('toast')

const userInfo = ref({})
const userLoading = ref(false)
const userError = ref('')

const configInfo = ref({})
const configLoading = ref(false)
const configError = ref('')

const showEdit = ref(false)
const saving = ref(false)
const editForm = ref({})
const imageUrl = ref('')
const avatarFile = ref(null)

const initial = computed(() => (userInfo.value.username || 'U').charAt(0).toUpperCase())
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

function handleAvatarChange(uploadFile) {
  avatarFile.value = uploadFile.raw
  imageUrl.value = URL.createObjectURL(uploadFile.raw)
}

async function uploadAvatar(file) {
  const { default: OSS } = await import('ali-oss')
  const res = await userApi.getOssSts()
  const credentials = res.data
  const client = new OSS({
    region: OSS_REGION,
    accessKeyId: credentials.accessKeyId,
    accessKeySecret: credentials.accessKeySecret,
    stsToken: credentials.securityToken,
    bucket: OSS_BUCKET,
  })
  const fileName = `avatar/${Date.now()}-${file.name}`
  const result = await client.put(fileName, file)
  return result.url
}

function openEdit() {
  editForm.value = {
    username: userInfo.value.username,
    email: userInfo.value.email,
    age: userInfo.value.age,
  }
  imageUrl.value = userInfo.value.avatar || ''
  avatarFile.value = null
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    if (avatarFile.value) {
      const avatarUrl = await uploadAvatar(avatarFile.value)
      if (avatarUrl) editForm.value.avatar = avatarUrl
    }
    const res = await userApi.update(editForm.value)
    if (res.code !== 200) {
      toast(res.msg || '保存失败', 'error')
      return
    }
    toast('保存成功')
    showEdit.value = false
    avatarFile.value = null
    loadUser()
  } catch (e) {
    toast(e.userMessage || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function loadUser() {
  userLoading.value = true
  userError.value = ''
  try {
    const res = await userApi.getInfo()
    if (res.code === 200) {
      userInfo.value = res.data || {}
    } else {
      userError.value = res.msg || '加载失败'
    }
  } catch (e) {
    userError.value = e.userMessage || '加载失败'
  } finally {
    userLoading.value = false
  }
}

async function loadConfig() {
  configLoading.value = true
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
    configLoading.value = false
  }
}

onMounted(() => {
  loadUser()
  loadConfig()
})
</script>

<style scoped>
.user-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.left-col,
.right-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.with-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* ─── profile ─── */
.profile-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 20px;
}

.profile-avatar {
  background: var(--accent-soft) !important;
  color: var(--accent) !important;
  font-size: 26px !important;
  font-weight: 600;
}

.profile-name {
  margin-top: 4px;
  color: var(--ink);
  font-size: 16px;
  font-weight: 600;
}

.profile-email {
  color: var(--ink-3);
  font-size: 12px;
}

.info-list {
  padding: 4px 0;
  border-top: 1px solid var(--border);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  color: var(--ink-3);
  font-size: 12.5px;
}

.info-row strong {
  color: var(--ink);
  font-weight: 500;
}

.full-btn {
  width: 100%;
  margin-top: 16px;
}

.config-link {
  display: block;
  margin-top: 8px;
  text-decoration: none;
}

/* ─── quick links ─── */
.card-title {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.card-desc {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.quick-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-card .card-title {
  margin-bottom: 8px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  color: var(--ink-2);
  font-size: 13px;
  text-decoration: none;
  transition: background 0.15s ease, color 0.15s ease;
}

.quick-link:hover {
  background: var(--surface-subtle);
  color: var(--ink);
}

.quick-icon {
  color: var(--accent);
  flex-shrink: 0;
}

.quick-chevron {
  margin-left: auto;
  color: var(--ink-3);
}

/* ─── quota ─── */
.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.quota-numbers {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}

.quota-used {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 34px;
  font-weight: 500;
  line-height: 1.1;
}

.quota-sep {
  color: var(--ink-3);
  font-size: 18px;
}

.quota-total {
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 16px;
}

.quota-unit {
  align-self: flex-end;
  margin-bottom: 4px;
  color: var(--ink-3);
  font-size: 11px;
}

.quota-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 12px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.config-label {
  margin-bottom: 4px;
  color: var(--ink-3);
  font-size: 11px;
}

.config-value {
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 500;
}

.model-value {
  color: var(--accent);
  font-family: var(--font-mono);
  font-size: 13px;
}

.key-value {
  overflow-wrap: anywhere;
  color: var(--ink-2);
  font-family: var(--font-mono);
  font-size: 12px;
}

/* ─── status ─── */
.status-card .card-title {
  margin-bottom: 8px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
  color: var(--ink-2);
  font-size: 13px;
}

.status-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.status-icon {
  flex-shrink: 0;
  color: var(--accent);
}

.status-icon.success {
  color: var(--success);
}

/* ─── avatar uploader ─── */
.avatar-uploader :deep(.el-upload) {
  overflow: hidden;
  border: 1.5px dashed var(--border-strong);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--accent);
}

.avatar-preview {
  display: block;
  width: 88px;
  height: 88px;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  color: var(--ink-3);
}

@media (max-width: 1023px) {
  .user-grid {
    grid-template-columns: 1fr;
  }

  .left-col,
  .right-col {
    display: contents;
  }

  .profile-card {
    order: 1;
    max-width: none;
  }

  .quota-card {
    order: 2;
  }

  .status-card {
    order: 3;
  }

  .quick-card {
    order: 4;
  }
}

@media (max-width: 480px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
