<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">知识库</div>
        <div class="page-subtitle">管理上传的文档，支持 PDF / Word / Excel</div>
      </div>
      <el-button type="primary" @click="showUpload = true">＋ 上传文档</el-button>
    </div>

    <!-- 搜索 -->
    <div class="search-bar" style="margin:20px 0;display:flex;gap:10px;">
      <el-input
        v-model="searchText"
        style="max-width:280px"
        placeholder="搜索文件名..."
        clearable
        @keyup.enter="fetchList"
      />
      <el-button @click="fetchList">搜索</el-button>
      <el-button @click="searchText=''; fetchList()">重置</el-button>
    </div>

    <!-- 统计卡 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon">{{ s.icon }}</div>
        <div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <el-card class="table-card" style="margin-top:20px">
      <el-table :data="list" v-loading="loading" style="width:100%">
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <span class="file-type-icon">{{ fileIcon(row.fileType) }}</span>
              <a :href="row.fileUrl" target="_blank" class="file-link">{{ row.fileName }}</a>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag type="primary" size="small" effect="plain">
              {{ row.fileType?.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>

        <el-table-column label="上传时间" width="180">
          <template #default="{ row }">{{ formatTime(row.uploadTime) }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default>
            <el-tag type="success" size="small" effect="plain">已处理</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="deleteFile(row.id)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无文件，点击上传开始构建知识库" :image-size="60" />
        </template>
      </el-table>

      <div style="margin-top:20px;display:flex;justify-content:flex-end;">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          background
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog
      v-model="showUpload"
      title="上传文档"
      width="500px"
      destroy-on-close
    >
      <el-upload
        drag
        multiple
        :auto-upload="false"
        accept=".pdf,.docx,.doc,.xlsx,.xls,.txt"
        :on-change="onFileChange"
        :file-list="pendingFileList"
      >
        <el-icon style="font-size:40px;color:var(--text-muted)"><UploadFilled /></el-icon>
        <div style="margin-top:8px;font-size:14px;color:var(--text-secondary)">
          拖拽文件到此处，或点击选择
        </div>
        <div style="font-size:12px;color:var(--text-muted);margin-top:4px">
          支持 PDF · Word · Excel · TXT
        </div>
      </el-upload>

      <div style="margin:15px 0;display:flex;align-items:center;gap:8px;">
        <el-checkbox v-model="isPublic">公开文档（勾选后所有人可见）</el-checkbox>
      </div>

      <template #footer>
        <el-button @click="showUpload = false; pendingFiles = []; pendingFileList = []">取消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="!pendingFiles.length"
          @click="doUpload"
        >
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { knowledgeApi } from '@/api/index'

const toast = inject('toast')

const list          = ref([])
const loading       = ref(false)
const page          = ref(1)
const pageSize      = ref(10)
const total         = ref(0)
const searchText    = ref('')
const showUpload    = ref(false)
const pendingFiles  = ref([])
const pendingFileList = ref([])
const uploading     = ref(false)
const isPublic      = ref(false)
const totalPages    = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const stats = computed(() => [
  { icon: '⊟', label: '总文件数', value: total.value },
  { icon: '📄', label: 'PDF 文件', value: list.value.filter(f => f.fileType === 'pdf').length },
  { icon: '📝', label: 'Word 文件', value: list.value.filter(f => ['doc','docx'].includes(f.fileType)).length },
  { icon: '📊', label: 'Excel 文件', value: list.value.filter(f => ['xls','xlsx'].includes(f.fileType)).length },
])

function fileIcon(type) {
  const m = { pdf:'📄', doc:'📝', docx:'📝', xls:'📊', xlsx:'📊', txt:'📃' }
  return m[type?.toLowerCase()] || '📁'
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

function onFileChange(file, fileList) {
  pendingFileList.value = fileList
  pendingFiles.value = fileList.map(f => f.raw)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await knowledgeApi.getPage(page.value, pageSize.value, searchText.value)
    if (res.code === 200) {
      list.value  = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch {
    toast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

async function doUpload() {
  if (!pendingFiles.value.length) return
  uploading.value = true
  try {
    for (const file of pendingFiles.value) {
      const fd = new FormData()
      fd.append('file', file)
      fd.append('isPublic',  isPublic.value ? 'true' : 'false')
      await knowledgeApi.upload(fd)
    }
    toast('上传成功')
    showUpload.value = false
    pendingFiles.value = []
    pendingFileList.value = []
    fetchList()
  } catch {
    toast('上传失败', 'error')
  } finally {
    uploading.value = false
  }
}

async function deleteFile(id) {
  try {
    await ElMessageBox.confirm('确认删除该文件？', '提示', { type: 'warning' })
    await knowledgeApi.deleteById(id)
    toast('删除成功')
    fetchList()
  } catch {}
}

onMounted(fetchList)
</script>

<script>
import { ElMessageBox } from 'element-plus'
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

.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:6px; }
.page-title  { font-size:20px; font-weight:700; color:var(--text-primary); }
.page-subtitle { font-size:13px; color:var(--text-muted); margin-top:3px; }

.stat-cards { display:flex; gap:14px; margin-bottom:4px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.60);
  backdrop-filter: blur(14px) saturate(1.5);
  -webkit-backdrop-filter: blur(14px) saturate(1.5);
  border: 1px solid rgba(210, 170, 195, 0.35);
  box-shadow: 0 4px 16px rgba(180, 120, 160, 0.12);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  flex: 1;
}
.stat-icon  { font-size: 24px; }
.stat-value { font-size: 22px; font-weight: 700; font-family: var(--font-mono); color: var(--accent); }
.stat-label { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.file-name  { display:flex; align-items:center; gap:8px; color:var(--text-primary); font-size:13px; }
.file-type-icon { font-size:16px; }
.file-link  { color:inherit; text-decoration:none; }
.file-link:hover { color:#409eff; text-decoration:underline; }
</style>