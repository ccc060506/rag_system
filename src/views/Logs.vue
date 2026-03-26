<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">历史对话记录</div>
        <div class="page-subtitle">查看 AI 对话记录与 Token 消耗</div>
      </div>
    </div>

    <el-card class="table-card">
      <el-table 
        :data="list" 
        v-loading="loading" 
        style="width: 100%" 
        @row-click="showDetail"
        row-class-name="clickable-row"
      >
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="light">{{ row.username || '匿名' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="提问内容" min-width="250">
          <template #default="{ row }">
            <el-tooltip :content="row.question" placement="top" :show-after="500">
              <div class="log-question">{{ truncate(row.question, 40) }}</div>
            </el-tooltip>
          </template>
        </el-table-column>

        <el-table-column label="模型" width="150">
          <template #default="{ row }">
            <el-tag type="success" size="small" effect="plain">{{ row.modelName }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="Token 消耗" width="120">
          <template #default="{ row }">
            <span class="token-val">{{ row.tokensUsed }}</span>
          </template>
        </el-table-column>

        <el-table-column label="来源文件">
          <template #default="{ row }">
            <div v-if="row.sourceContext?.length" class="source-list">
              <el-tag 
                v-for="s in row.sourceContext.slice(0, 2)" 
                :key="s" 
                size="small" 
                type="info" 
                class="source-tag"
              >
                {{ s }}
              </el-tag>
              <span v-if="row.sourceContext.length > 2" class="source-more">
                +{{ row.sourceContext.length - 2 }}
              </span>
            </div>
            <span v-else class="text-muted-sm">无</span>
          </template>
        </el-table-column>

        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <span style="font-size: 12px; color: var(--el-text-color-secondary)">
              {{ formatTime(row.createTimeStr) }}
            </span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无对话记录" :image-size="60" />
        </template>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
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
      v-model="detailVisible" 
      title="对话详情" 
      width="600px"
      append-to-body
      destroy-on-close
    >
      <div class="detail-container">
        <div class="detail-item">
          <div class="detail-label">
            <el-icon style="vertical-align: middle; margin-right: 4px;"><User /></el-icon>
            用户提问：
          </div>
          <div class="detail-content user-q">
            {{ currentItem.question || '暂无提问内容' }}
          </div>
        </div>
        
        <div class="detail-item">
          <div class="detail-label">
            <el-icon style="vertical-align: middle; margin-right: 4px;"><ChatLineRound /></el-icon>
            AI 回复：
          </div>
          <div class="detail-content ai-a">
            {{ currentItem.answer || '暂无回复内容' }}
          </div>
        </div>

        <div class="detail-footer-info" style="margin-top: 10px; font-size: 12px; color: #999;">
          <span>使用模型: {{ currentItem.modelName }}</span>
          <span style="margin-left: 20px;">Token 消耗: {{ currentItem.tokensUsed }}</span>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { logApi } from '@/api/index'

const toast = inject('toast')
const list        = ref([])
const loading     = ref(false)
const page        = ref(1)
const pageSize    = ref(13)
const total       = ref(0)
const totalPages  = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function truncate(str, len) {
  if (!str) return '-'
  return str.length > len ? str.slice(0, len) + '...' : str
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

const detailVisible = ref(false)
const currentItem = ref({})
const showDetail = (item) => {
  currentItem.value = item
  detailVisible.value = true
}

async function fetchList() {
  loading.value = true
  try {
    const res = await logApi.getPage(page.value, pageSize.value)
    if (res.code === 200) {
      list.value  = res.data.list || []
      total.value = res.data.total   || 0
    }
  } catch {
    toast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
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
.log-question {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 13px;
}
.token-val {
  font-family: var(--font-mono);
  color: var(--warning);
  font-size: 12px;
}
.source-list { display: flex; gap: 4px; flex-wrap: wrap; }
.source-tag {
  background: var(--bg-hover);
  padding: 2px 7px;
  border-radius: 4px;
  font-size: 11px;
  color: var(--text-muted);
  border: 1px solid var(--border);
}
.source-more { font-size: 11px; color: var(--text-muted); }
.text-muted-sm { font-size: 12px; color: var(--text-muted); }
.clickable-row {
  cursor: pointer;
  transition: background-color 0.2s;
}
.clickable-row:hover {
  background-color: var(--bg-hover);
}
.detail-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.detail-label {
  font-weight: bold;
  margin-bottom: 8px;
  color: var(--text-muted);
}
.detail-content {
  padding: 12px;
  border-radius: 8px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.user-q {
  background-color: var(--bg-hover);
}
.ai-a {
  background-color: var(--accent-dim);
  border: 1px solid var(--accent);
}
</style>
