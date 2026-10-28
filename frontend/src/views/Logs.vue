<template>
  <div class="page-content">
    <PageHeader title="历史对话" subtitle="查看 AI 对话记录与 Token 消耗" />

    <ErrorState v-if="error" title="对话记录加载失败" :message="error" @retry="fetchList" />

    <el-card v-else class="table-card">
      <div class="table-wrap">
        <el-table
          v-if="!isMobile"
          :data="list"
          v-loading="loading"
          style="width: 100%"
          row-class-name="clickable-row"
          @row-click="openChat"
        >
          <el-table-column label="用户" width="150">
            <template #default="{ row }">
              <div class="user-cell">
                <span class="mini-avatar">{{ initial(row.username) }}</span>
                <span class="user-name">{{ row.username || '匿名' }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="提问内容" min-width="260">
            <template #default="{ row }">
              <div class="question-cell" :title="row.question">{{ row.question || '-' }}</div>
            </template>
          </el-table-column>

          <el-table-column label="模型" width="140">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.modelName || '-' }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column label="Token" width="130">
            <template #default="{ row }">
              <span class="mono token-cell">{{ row.tokensUsed || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="来源" min-width="170">
            <template #default="{ row }">
              <div v-if="sourceList(row).length" class="source-tags">
                <el-tag
                  v-for="source in sourceList(row).slice(0, 2)"
                  :key="source"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ source }}
                </el-tag>
                <span v-if="sourceList(row).length > 2" class="source-more">
                  +{{ sourceList(row).length - 2 }}
                </span>
              </div>
              <span v-else class="muted-cell">无</span>
            </template>
          </el-table-column>

          <el-table-column label="时间" width="170">
            <template #default="{ row }">
              <span class="muted-cell">{{ formatTime(row.createTimeStr) }}</span>
            </template>
          </el-table-column>

          <el-table-column width="40">
            <template #default>
              <ChevronRight :size="15" class="chevron" />
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="还没有对话记录" :image-size="64">
              <template #image>
                <div class="empty-illustration">
                  <History :size="26" />
                </div>
              </template>
            </el-empty>
          </template>
        </el-table>

        <div v-else class="log-list" v-loading="loading">
          <button
            v-for="(row, index) in list"
            :key="row.id || index"
            type="button"
            class="log-row"
            @click="openChat(row)"
          >
            <span class="log-question">{{ row.question || '-' }}</span>
            <span class="log-meta">
              <span class="log-user">{{ row.username || '匿名' }}</span>
              <span class="muted-cell">{{ formatTime(row.createTimeStr) }}</span>
              <span class="mono log-token">{{ row.tokensUsed }}</span>
              <ChevronRight :size="15" class="log-chevron" />
            </span>
          </button>

          <el-empty
            v-if="!loading && !list.length"
            description="还没有对话记录"
            :image-size="64"
          >
            <template #image>
              <div class="empty-illustration">
                <History :size="26" />
              </div>
            </template>
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
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronRight, History } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { logApi } from '@/api'

const router = useRouter()

const list = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const isMobile = ref(false)
let mobileQuery

function initial(name) {
  return (name || '匿').charAt(0).toUpperCase()
}

function sourceList(row) {
  const value = row?.sourceContext
  if (!value) return []
  if (Array.isArray(value)) return value.filter(Boolean)
  return String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

function openChat(row) {
  router.push({ path: '/chat', query: { logId: row.id } })
}

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const res = await logApi.getPage(page.value, pageSize.value)
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

function syncMobile(event) {
  isMobile.value = event.matches
}

onMounted(() => {
  mobileQuery = window.matchMedia('(max-width: 767px)')
  isMobile.value = mobileQuery.matches
  mobileQuery.addEventListener('change', syncMobile)
  fetchList()
})

onUnmounted(() => {
  mobileQuery?.removeEventListener('change', syncMobile)
})
</script>

<style scoped>
.table-card {
  overflow: hidden;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.log-list {
  display: flex;
  flex-direction: column;
}

.log-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 16px;
  border: 0;
  border-bottom: 1px solid var(--rule);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.log-row:last-of-type {
  border-bottom: 0;
}

.log-row:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: -2px;
}

.log-question {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--ink);
  font-size: 14px;
  line-height: 1.5;
}

.log-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 12px;
}

.log-user {
  color: var(--ink-2);
  font-size: 12.5px;
}

.log-token {
  color: var(--warning);
  font-size: 12px;
}

.log-chevron {
  margin-left: auto;
  color: var(--muted);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 11px;
  font-weight: 600;
}

.user-name {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-cell {
  overflow: hidden;
  color: var(--ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.token-cell {
  color: var(--warning);
  font-size: 12px;
}

.source-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.source-more {
  color: var(--ink-3);
  font-size: 11px;
}

.muted-cell {
  color: var(--ink-3);
  font-size: 12.5px;
}

.chevron {
  color: var(--ink-3);
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 0;
}

:deep(.clickable-row) {
  cursor: pointer;
}

@media (max-width: 767px) {
  .pagination-row {
    justify-content: center;
  }
}
</style>
