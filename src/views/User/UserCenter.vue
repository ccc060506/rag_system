<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">个人中心</div>
        <div class="page-subtitle">管理你的账号信息与 API 配置</div>
      </div>
    </div>

    <div class="user-layout">
      <!-- 左：个人信息卡 -->
      <div class="left-col">
        <el-card class="profile-card">
          <div class="avatar-section">
            <el-avatar :size="80" :src="userInfo.avatar || undefined" style="font-size:28px;background:var(--bg-hover);">
              {{ (userInfo.avatar || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="profile-name">{{ userInfo.username || '—' }}</div>
            <div class="profile-email">{{ userInfo.email || '未设置邮箱' }}</div>
          </div>

          <el-descriptions :column="1" border size="small" style="margin:12px 0">
            <el-descriptions-item label="年龄">{{ userInfo.age || '—' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">
              <el-tag type="success" size="small">正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <el-button class="full-btn" @click="openEdit">✎ 编辑资料</el-button>
          <RouterLink to="/user/config" style="text-decoration:none;display:block;margin-top:8px">
            <el-button type="primary" class="full-btn">⚙ API 配置中心</el-button>
          </RouterLink>
        </el-card>

        <!-- 快捷导航 -->
        <el-card class="nav-card">
          <div class="card-title">快捷入口</div>
          <RouterLink to="/chat"      class="quick-link">◎ 开始 AI 问答</RouterLink>
          <RouterLink to="/knowledge" class="quick-link">⊟ 管理知识库</RouterLink>
          <RouterLink to="/logs"      class="quick-link">≡ 查看日志</RouterLink>
        </el-card>
      </div>

      <!-- 右：配额信息 -->
      <div class="right-col">
        <el-card class="quota-card">
          <div class="card-title">Token 配额概览</div>
          <div v-if="loadingConfig" style="padding:20px;text-align:center">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <div v-else>
            <div class="quota-big">
              <div class="quota-used">{{ formatNum(configInfo.usedTokenQuota) }}</div>
              <div class="quota-sep">/</div>
              <div class="quota-total">{{ formatNum(configInfo.totalTokenQuota) }}</div>
              <div class="quota-unit">Tokens</div>
            </div>
            <el-progress
              :percentage="configInfo.tokenUsageRate || 0"
              :stroke-width="10"
              :show-text="false"
              style="margin-bottom:8px"
            />
            <div class="quota-pct-row">
              <span class="quota-pct-text">已使用 {{ configInfo.tokenUsageRate?.toFixed(1) }}%</span>
              <span class="quota-remain">剩余 {{ formatNum(configInfo.totalTokenQuota - configInfo.usedTokenQuota) }} Tokens</span>
            </div>

            <el-divider />

            <div class="config-grid">
              <div class="config-item">
                <div class="config-label">当前模型</div>
                <div class="config-value model-badge">{{ configInfo.preferredModel || '—' }}</div>
              </div>
              <div class="config-item">
                <div class="config-label">最大知识库数</div>
                <div class="config-value">{{ configInfo.maxKnowledgeBases || '—' }}</div>
              </div>
              <div class="config-item">
                <div class="config-label">最大存储空间</div>
                <div class="config-value">{{ configInfo.maxStorageSize ? configInfo.maxStorageSize + ' MB' : '—' }}</div>
              </div>
              <div class="config-item">
                <div class="config-label">API Key</div>
                <div class="config-value key-val">{{ maskKey(configInfo.apiKey) }}</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 账号信息 -->
        <el-card class="activity-card">
          <div class="card-title">账号信息</div>
          <div class="activity-list">
            <div class="activity-item">
              <span class="activity-dot green"></span>
              <span>账号状态正常，所有服务运行中</span>
            </div>
            <div class="activity-item">
              <span class="activity-dot blue"></span>
              <span>已绑定 DeepSeek API Key</span>
            </div>
            <div class="activity-item">
              <span class="activity-dot accent"></span>
              <span>知识库问答服务已启用</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEdit" title="编辑个人信息" width="420px" destroy-on-close>
      <el-form :model="editForm" label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="editForm.username" placeholder="昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="editForm.age" :min="1" :max="120" style="width:100%" />
        </el-form-item>
        <el-form-item label="新密码（留空不修改）">
          <el-input v-model="editForm.password" type="password" placeholder="新密码" show-password />
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            action="#" 
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleAvatarChange"
          >
            <img v-if="imageUrl" :src="imageUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
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
import { ref, onMounted, inject } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { userApi } from '@/api/index'
import { Plus } from '@element-plus/icons-vue'
import OSS from 'ali-oss';

const toast        = inject('toast')
const userInfo     = ref({})
const configInfo   = ref({})
const loadingConfig = ref(false)
const showEdit     = ref(false)
const saving       = ref(false)
const editForm     = ref({})
const imageUrl     = ref('')
const avatarFile   = ref(null) 
const handleAvatarChange = (uploadFile) => {
  avatarFile.value = uploadFile.raw
  imageUrl.value = URL.createObjectURL(uploadFile.raw)
}

const uploadAvatar = async (file) => {
  try {

    const res = await userApi.getOssSts() 
    const credentials = res.data

    const client = new OSS({
      region: 'oss-cn-beijing', 
      accessKeyId: credentials.accessKeyId,
      accessKeySecret: credentials.accessKeySecret,
      stsToken: credentials.securityToken,
      bucket: 'rag-system-ccc'
    });

    const fileName = `avatar/${Date.now()}-${file.name}`;
    const result = await client.put(fileName, file);
    return result.url;
  } catch (e) {
    console.error('OSS上传失败', e);
    throw e;
  }
}


function formatNum(n) { return n ? n.toLocaleString() : '0' }
function maskKey(key) {
  if (!key) return '未配置'
  return key.slice(0, 6) + '****' + key.slice(-4)
}

function openEdit() {
  editForm.value = {
    username: userInfo.value.username,
    email:    userInfo.value.email,
    age:      userInfo.value.age,
    password: ''
  }
  imageUrl.value = userInfo.value.avatar
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    if (avatarFile.value) {
      const ossUrl = await uploadAvatar(avatarFile.value)
      if (ossUrl) {
        editForm.value.avatar = ossUrl 
      }
    }
    await userApi.update(editForm.value)
    toast('保存成功')
    showEdit.value = false
    loadUser() 
    avatarFile.value = null
  } catch (err) {
    console.error(err)
    toast('保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function loadUser() {
  try {
    const res = await userApi.getInfo()
    if (res.code === 200) userInfo.value = res.data || {}
  } catch {
    userInfo.value = { username: localStorage.getItem('username') || 'User' }
  }
}

async function loadConfig() {
  loadingConfig.value = true
  try {
    const res = await userApi.getConfig()
    if (res.code === 200) configInfo.value = res.data || {}
  } catch {
    configInfo.value = {
      apiKey: 'sk-0dcd57c2e4634123', preferredModel: 'deepseek-chat',
      totalTokenQuota: 1000000, usedTokenQuota: 234567,
      tokenUsageRate: 23.5, maxKnowledgeBases: 10, maxStorageSize: 500
    }
  } finally { loadingConfig.value = false }
}

onMounted(() => { loadUser(); loadConfig() })
</script>

<style scoped>
.page-content {
  padding: 28px 32px;
  height: 100vh;
  overflow-y: auto;
  background: transparent;
}

.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 6px;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader :deep(.el-upload:hover) {
  border-color: var(--el-color-primary);
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
}
/* 页头标题颜色 */
.page-title    { color: var(--text-primary) !important; }
.page-subtitle { color: var(--text-muted)   !important; }
.page-header  { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }
.page-title   { font-size:20px; font-weight:700; color:var(--text-primary); }
.page-subtitle { font-size:13px; color:var(--text-muted); margin-top:3px; }

.user-layout { display:grid; grid-template-columns:280px 1fr; gap:20px; }
.left-col, .right-col { display:flex; flex-direction:column; gap:16px; }

.profile-card { display:flex; flex-direction:column; gap:12px; }
.avatar-section { display:flex; flex-direction:column; align-items:center; gap:8px; padding:10px 0; }
.profile-name  { font-size:16px; font-weight:600; color:var(--text-primary); }
.profile-email { font-size:12px; color:var(--text-muted); }
.full-btn { width:100%; }

.nav-card  { display:flex; flex-direction:column; gap:4px; }
.card-title { font-size:11px; font-weight:600; margin-bottom:10px; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.06em; }
.quick-link {
  display:flex; align-items:center; gap:8px;
  padding:9px 10px; border-radius:var(--radius);
  font-size:13px; color:var(--text-secondary); text-decoration:none; transition:all 0.15s;
}
.quick-link:hover { background:var(--bg-hover); color:var(--accent); }

.quota-big { display:flex; align-items:baseline; gap:6px; margin-bottom:14px; }
.quota-used  { font-size:36px; font-weight:700; color:var(--accent); font-family:var(--font-mono); }
.quota-sep   { font-size:20px; color:var(--text-muted); }
.quota-total { font-size:18px; color:var(--text-secondary); font-family:var(--font-mono); }
.quota-unit  { font-size:12px; color:var(--text-muted); align-self:flex-end; margin-bottom:4px; }
.quota-pct-row { display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px; }
.quota-pct-text { color:var(--accent); }
.quota-remain   { color:var(--text-muted); }

.config-grid  { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.config-label { font-size:11px; color:var(--text-muted); text-transform:uppercase; letter-spacing:0.05em; margin-bottom:4px; }
.config-value { font-size:14px; color:var(--text-primary); font-weight:500; }
.model-badge  { color:var(--accent); font-family:var(--font-mono); font-size:13px; }
.key-val      { font-family:var(--font-mono); font-size:12px; color:var(--text-secondary); }

.activity-list { display:flex; flex-direction:column; gap:12px; }
.activity-item { display:flex; align-items:center; gap:10px; font-size:13px; color:var(--text-secondary); }
.activity-dot  { width:8px; height:8px; border-radius:50%; flex-shrink:0; }
.activity-dot.green  { background:var(--accent); }
.activity-dot.blue   { background:var(--blue); }
.activity-dot.accent { background:var(--warning); }
</style>