<template>
  <div class="page-content">
    <PageHeader title="敏感词管理" subtitle="维护敏感词库，实时同步至 Redis DFA 过滤器">
      <template #actions>
        <el-button class="with-icon" :loading="refreshing" @click="refreshDfa">
          <RefreshCw :size="15" />
          <span>刷新 DFA</span>
        </el-button>
        <el-button type="primary" class="with-icon" @click="openAdd">
          <Plus :size="15" />
          <span>新增敏感词</span>
        </el-button>
      </template>
    </PageHeader>

    <div class="toolbar">
      <el-input
        v-model="searchWord"
        class="search-input"
        placeholder="搜索敏感词..."
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
      <span class="count-text">共 {{ total }} 条</span>
    </div>

    <ErrorState v-if="error" title="敏感词加载失败" :message="error" @retry="fetchList" />

    <el-card v-else class="table-card">
      <div class="table-wrap">
        <el-table v-if="!isMobile" :data="list" v-loading="loading" style="width: 100%">
          <el-table-column label="敏感词" min-width="220">
            <template #default="{ row }">
              <span class="word-text">{{ row.word }}</span>
            </template>
          </el-table-column>

          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">
                {{ row.category || '未分类' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="等级" width="110">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">
                {{ row.level === 2 ? '拦截' : '替换' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              <span class="muted-cell">{{ formatTime(row.createTime) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button text size="small" @click="openEdit(row)">
                  <Pencil :size="14" />
                  <span class="btn-text">编辑</span>
                </el-button>
                <el-button type="danger" text size="small" @click="deleteWord(row)">
                  <Trash2 :size="14" />
                  <span class="btn-text">删除</span>
                </el-button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="还没有敏感词" :image-size="64">
              <template #image>
                <div class="empty-illustration">
                  <Shield :size="26" />
                </div>
              </template>
              <el-button type="primary" class="with-icon" @click="openAdd">
                <Plus :size="15" />
                <span>新增敏感词</span>
              </el-button>
            </el-empty>
          </template>
        </el-table>

        <div v-else class="word-list" v-loading="loading">
          <div v-for="row in list" :key="row.id" class="word-row">
            <div class="word-row-main">
              <span class="word-text">{{ row.word }}</span>
              <div class="word-meta">
                <el-tag type="info" effect="plain" size="small">
                  {{ row.category || '未分类' }}
                </el-tag>
                <el-tag type="info" effect="plain" size="small">
                  {{ row.level === 2 ? '拦截' : '替换' }}
                </el-tag>
                <span class="muted-cell">{{ formatTime(row.createTime) }}</span>
              </div>
            </div>
            <div class="word-actions">
              <el-button text size="small" @click="openEdit(row)">
                <Pencil :size="14" />
                <span class="btn-text">编辑</span>
              </el-button>
              <el-button type="danger" text size="small" @click="deleteWord(row)">
                <Trash2 :size="14" />
                <span class="btn-text">删除</span>
              </el-button>
            </div>
          </div>

          <el-empty v-if="!loading && !list.length" description="还没有敏感词" :image-size="64">
            <template #image>
              <div class="empty-illustration">
                <Shield :size="26" />
              </div>
            </template>
            <el-button type="primary" class="with-icon" @click="openAdd">
              <Plus :size="15" />
              <span>新增敏感词</span>
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
      v-model="showModal"
      :title="editId ? '编辑敏感词' : '新增敏感词'"
      width="460px"
      destroy-on-close
    >
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="敏感词内容">
          <el-input v-model="form.word" placeholder="请输入要过滤的关键词" @keyup.enter="saveWord" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="敏感等级">
          <el-radio-group v-model="form.level">
            <el-radio :label="1">1 替换处理</el-radio>
            <el-radio :label="2">2 直接拦截</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showModal = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveWord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, onUnmounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Pencil, Plus, RefreshCw, Search, Shield, Trash2 } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { sensitiveApi } from '@/api'

const toast = inject('toast')

const list = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchWord = ref('')

const showModal = ref(false)
const editId = ref(null)
const saving = ref(false)
const refreshing = ref(false)
const categories = ['政治', '色情', '暴力', '广告', '违法', '其他']
const form = ref({ word: '', category: '政治', level: 1 })

const isMobile = ref(false)
let mobileQuery

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', { hour12: false })
}

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const res = await sensitiveApi.getPage(page.value, pageSize.value, searchWord.value)
    if (res.code === 200) {
      list.value = res.data?.list || (Array.isArray(res.data) ? res.data : [])
      total.value = res.data?.total ?? list.value.length
    } else {
      error.value = res.msg || '加载失败'
    }
  } catch (e) {
    error.value = e.userMessage || '加载失败'
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchWord.value = ''
  page.value = 1
  fetchList()
}

function openAdd() {
  editId.value = null
  form.value = { word: '', category: '政治', level: 1 }
  showModal.value = true
}

function openEdit(row) {
  editId.value = row.id
  form.value = {
    word: row.word,
    category: row.category || '其他',
    level: row.level ?? 1,
  }
  showModal.value = true
}

async function saveWord() {
  if (!form.value.word.trim()) {
    toast('请输入敏感词内容', 'error')
    return
  }

  saving.value = true
  try {
    const res = editId.value
      ? await sensitiveApi.update(editId.value, form.value)
      : await sensitiveApi.add([form.value])

    if (res.code !== 200) {
      toast(res.msg || '保存失败', 'error')
      return
    }
    toast('保存成功')
    showModal.value = false
    fetchList()
  } catch (e) {
    toast(e.userMessage || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function deleteWord(row) {
  try {
    await ElMessageBox.confirm(`确认删除敏感词「${row.word}」？`, '删除敏感词', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const res = await sensitiveApi.deleteById(row.id)
    if (res.code !== 200) {
      toast(res.msg || '删除失败', 'error')
      return
    }
    toast('删除成功')
    fetchList()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    toast(e.userMessage || '删除失败', 'error')
  }
}

async function refreshDfa() {
  refreshing.value = true
  try {
    const res = await sensitiveApi.refreshDfa()
    if (res.code === 200) {
      toast(res.data || '已刷新')
    } else {
      toast(res.msg || '刷新失败', 'error')
    }
  } catch (e) {
    toast(e.userMessage || '刷新失败', 'error')
  } finally {
    refreshing.value = false
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
.with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

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

.input-icon {
  color: var(--ink-3);
}

.toolbar-spacer {
  flex: 1;
}

.count-text {
  color: var(--ink-3);
  font-size: 12px;
}

.table-card {
  overflow: hidden;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.word-text {
  color: var(--ink);
  font-family: var(--font-mono);
  font-size: 13px;
}

.word-list {
  display: flex;
  flex-direction: column;
}

.word-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--rule);
}

.word-row:last-of-type {
  border-bottom: 0;
}

.word-row-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.word-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
}

.word-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.word-actions :deep(.el-button) {
  min-width: 44px;
  min-height: 44px;
  padding: 0 8px;
}

.word-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.muted-cell {
  color: var(--ink-3);
  font-size: 12.5px;
}

.btn-text {
  margin-left: 4px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 0;
}

@media (max-width: 767px) {
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
</style>
