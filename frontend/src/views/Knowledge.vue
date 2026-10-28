<template>
  <div class="page-content">
    <PageHeader title="知识库" subtitle="管理上传的文档，支持 PDF / Word / Excel / TXT">
      <template #actions>
        <el-button type="primary" class="with-icon" @click="showUpload = true">
          <Upload :size="15" />
          <span>上传文档</span>
        </el-button>
      </template>
    </PageHeader>

    <ErrorState
      v-if="statsError"
      title="统计加载失败"
      :message="statsError"
      @retry="fetchStats"
    />

    <div v-else class="stat-grid" v-loading="statsLoading">
      <div v-for="stat in stats" :key="stat.label" class="stat-card">
        <div class="stat-icon" :class="stat.tone">
          <component :is="stat.icon" :size="18" />
        </div>
        <div class="stat-copy">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <el-input
        v-model="searchText"
        class="search-input"
        placeholder="搜索文件名..."
        clearable
        @keyup.enter="fetchList"
        @clear="fetchList"
      >
        <template #prefix>
          <Search :size="15" class="input-icon" />
        </template>
      </el-input>
      <el-button @click="fetchList">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <div class="toolbar-spacer"></div>
      <el-button class="with-icon" :loading="loading" @click="loadData">
        <RotateCcw :size="15" />
        <span>刷新</span>
      </el-button>
    </div>

    <ErrorState v-if="error" title="知识库加载失败" :message="error" @retry="loadData" />

    <el-card v-else class="table-card">
      <div class="table-wrap">
        <el-table v-if="!isMobile" :data="list" v-loading="loading" style="width: 100%">
          <el-table-column label="文件名" min-width="240">
            <template #default="{ row }">
              <div class="file-cell">
                <component :is="fileIcon(row.fileType)" :size="17" class="file-icon" />
                <a
                  :href="row.fileUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="file-link"
                  :title="row.fileName"
                >
                  {{ row.fileName }}
                </a>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ (row.fileType || 'FILE').toUpperCase() }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="大小" width="100">
            <template #default="{ row }">
              <span class="mono muted-cell">{{ formatSize(row.fileSize) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="上传时间" width="180">
            <template #default="{ row }">
              <span class="muted-cell">{{ formatTime(row.uploadTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="100">
            <template #default>
              <el-tag type="success" effect="plain">已处理</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button
                type="danger"
                text
                size="small"
                @click="deleteFile(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="还没有文档，上传第一篇开始构建知识库" :image-size="64">
              <template #image>
                <div class="empty-illustration">
                  <Files :size="26" />
                </div>
              </template>
              <el-button type="primary" class="with-icon" @click="showUpload = true">
                <Upload :size="15" />
                <span>上传文档</span>
              </el-button>
            </el-empty>
          </template>
        </el-table>

        <div v-else class="file-list" v-loading="loading">
          <div v-for="row in list" :key="row.id" class="file-row">
            <div class="file-row-main">
              <div class="file-name-row">
                <component :is="fileIcon(row.fileType)" :size="16" class="file-icon" />
                <a
                  :href="row.fileUrl"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="file-link"
                  :title="row.fileName"
                >
                  {{ row.fileName }}
                </a>
              </div>
              <div class="file-meta">
                <el-tag type="info" effect="plain" size="small">
                  {{ (row.fileType || 'FILE').toUpperCase() }}
                </el-tag>
                <span class="muted-cell">{{ formatSize(row.fileSize) }}</span>
                <span class="muted-cell">{{ formatTime(row.uploadTime) }}</span>
                <el-tag type="success" effect="plain" size="small">已处理</el-tag>
              </div>
            </div>
            <el-button type="danger" text size="small" class="row-action" @click="deleteFile(row)">
              删除
            </el-button>
          </div>

          <el-empty v-if="!loading && !list.length" description="还没有文档，上传第一篇开始构建知识库" :image-size="64">
            <template #image>
              <div class="empty-illustration">
                <Files :size="26" />
              </div>
            </template>
            <el-button type="primary" class="with-icon" @click="showUpload = true">
              <Upload :size="15" />
              <span>上传文档</span>
            </el-button>
          </el-empty>
        </div>
      </div>

      <div class="pagination-row">
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

    <el-dialog
      v-model="showUpload"
      title="上传文档"
      width="520px"
      destroy-on-close
      @close="resetUpload"
    >
      <el-upload
        drag
        multiple
        :auto-upload="false"
        accept=".pdf,.docx,.doc,.xlsx,.xls,.txt"
        :on-change="onFileChange"
        :file-list="pendingFileList"
      >
        <div class="drop-icon">
          <UploadCloud :size="26" />
        </div>
        <div class="drop-title">拖拽文件到此处，或点击选择</div>
        <div class="drop-desc">支持 PDF、Word、Excel、TXT</div>
      </el-upload>

      <div class="public-row">
        <el-checkbox v-model="isPublic">公开文档</el-checkbox>
        <span class="public-desc">勾选后其他用户也可以检索和引用</span>
      </div>

      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
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
import { computed, inject, onMounted, onUnmounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import {
  File,
  FileSpreadsheet,
  FileText,
  Files,
  RotateCcw,
  Search,
  Upload,
  UploadCloud,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { knowledgeApi } from '@/api'

const toast = inject('toast')

const list = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchText = ref('')

const allFiles = ref([])
const statsTotal = ref(0)
const statsLoading = ref(false)
const statsError = ref('')

const showUpload = ref(false)
const pendingFiles = ref([])
const pendingFileList = ref([])
const uploading = ref(false)
const isPublic = ref(false)

const isMobile = ref(false)
let mobileQuery

const stats = computed(() => [
  {
    label: '总文件数',
    value: statsTotal.value,
    icon: Files,
    tone: 'tone-accent',
  },
  {
    label: 'PDF 文件',
    value: allFiles.value.filter((file) => file.fileType?.toLowerCase() === 'pdf').length,
    icon: FileText,
    tone: 'tone-red',
  },
  {
    label: 'Word 文件',
    value: allFiles.value.filter((file) => ['doc', 'docx'].includes(file.fileType?.toLowerCase())).length,
    icon: File,
    tone: 'tone-blue',
  },
  {
    label: 'Excel 文件',
    value: allFiles.value.filter((file) => ['xls', 'xlsx'].includes(file.fileType?.toLowerCase())).length,
    icon: FileSpreadsheet,
    tone: 'tone-green',
  },
])

function fileIcon(type) {
  const map = {
    pdf: FileText,
    doc: File,
    docx: File,
    xls: FileSpreadsheet,
    xlsx: FileSpreadsheet,
    txt: FileText,
  }
  return map[type?.toLowerCase()] || File
}

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const res = await knowledgeApi.getPage(page.value, pageSize.value, searchText.value)
    if (res.code === 200) {
      list.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (e) {
    error.value = e.userMessage || '加载失败'
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  statsLoading.value = true
  statsError.value = ''
  try {
    const res = await knowledgeApi.getPage(1, 500)
    if (res.code === 200) {
      allFiles.value = res.data.list || []
      statsTotal.value = res.data.total ?? allFiles.value.length
    } else {
      statsError.value = res.msg || '统计加载失败'
    }
  } catch (e) {
    statsError.value = e.userMessage || '统计加载失败'
  } finally {
    statsLoading.value = false
  }
}

function loadData() {
  fetchList()
  fetchStats()
}

function resetSearch() {
  searchText.value = ''
  page.value = 1
  fetchList()
}

function onFileChange(file, fileList) {
  pendingFileList.value = fileList
  pendingFiles.value = fileList.map((item) => item.raw)
}

function resetUpload() {
  pendingFiles.value = []
  pendingFileList.value = []
  isPublic.value = false
}

async function doUpload() {
  if (!pendingFiles.value.length) return

  uploading.value = true
  let uploaded = 0
  try {
    for (const file of pendingFiles.value) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('isPublic', isPublic.value ? 'true' : 'false')
      const res = await knowledgeApi.upload(formData)
      if (res.code !== 200) {
        throw new Error(res.msg || `上传 ${file.name} 失败`)
      }
      uploaded++
    }
    toast(uploaded > 1 ? `上传成功，共 ${uploaded} 个文件` : '上传成功')
    showUpload.value = false
    resetUpload()
    loadData()
  } catch (e) {
    toast(e.userMessage || e.message || '上传失败', 'error')
    if (uploaded > 0) loadData()
  } finally {
    uploading.value = false
  }
}

async function deleteFile(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.fileName}」？`, '删除文档', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const res = await knowledgeApi.deleteById(row.id)
    if (res.code !== 200) {
      toast(res.msg || '删除失败', 'error')
      return
    }
    toast('删除成功')
    loadData()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    toast(e.userMessage || '删除失败', 'error')
  }
}

function syncMobile(event) {
  isMobile.value = event.matches
}

onMounted(() => {
  mobileQuery = window.matchMedia('(max-width: 767px)')
  isMobile.value = mobileQuery.matches
  mobileQuery.addEventListener('change', syncMobile)
  loadData()
})

onUnmounted(() => {
  mobileQuery?.removeEventListener('change', syncMobile)
})
</script>

<style scoped>
.with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.input-icon {
  color: var(--ink-3);
}

/* ─── stats ─── */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
  border-radius: var(--radius);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
}

.tone-accent {
  background: var(--accent-soft);
  color: var(--accent);
}

.tone-red {
  background: var(--danger-soft);
  color: var(--danger);
}

.tone-blue {
  background: var(--blue-dim);
  color: var(--blue);
}

.tone-green {
  background: var(--success-soft);
  color: var(--success);
}

.stat-copy {
  min-width: 0;
}

.stat-value {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 500;
  line-height: 1.2;
}

.stat-label {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

/* ─── toolbar ─── */
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.toolbar-spacer {
  flex: 1;
}

/* ─── table ─── */
.table-card {
  overflow: hidden;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-icon {
  flex-shrink: 0;
  color: var(--accent);
}

.file-list {
  display: flex;
  flex-direction: column;
}

.file-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--rule);
}

.file-row:last-of-type {
  border-bottom: 0;
}

.file-row-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.file-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.file-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
}

.row-action {
  min-width: 44px;
  min-height: 44px;
  flex-shrink: 0;
}

.file-link {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-link:hover {
  color: var(--accent);
  text-decoration: underline;
}

.muted-cell {
  color: var(--ink-3);
  font-size: 12.5px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 0;
}

/* ─── upload dialog ─── */
.drop-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  border-radius: 12px;
  background: var(--accent-soft);
  color: var(--accent);
}

.drop-title {
  color: var(--ink);
  font-size: 13.5px;
  font-weight: 500;
}

.drop-desc {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.public-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.public-desc {
  color: var(--ink-3);
  font-size: 12px;
}

/* ─── responsive ─── */
@media (max-width: 1023px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .search-input {
    width: 220px;
  }
}

@media (max-width: 767px) {
  .toolbar {
    align-items: stretch;
  }

  .search-input {
    width: 100%;
  }

  .toolbar-spacer {
    display: none;
  }

  .pagination-row {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .stat-grid {
    gap: 12px;
  }

  .stat-card {
    padding: 12px 16px;
  }

  .stat-value {
    font-size: 19px;
  }
}
</style>
