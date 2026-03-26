<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">敏感词管理</div>
        <div class="page-subtitle">维护敏感词库，实时同步至 Redis DFA 过滤器</div>
      </div>
      </div>

    <div class="search-bar" style="margin: 20px 0; display: flex; align-items: center; gap: 10px;">
      <el-input 
        v-model="searchWord" 
        placeholder="搜索敏感词..." 
        style="max-width:280px" 
        @keyup.enter="fetchList"
        clearable
      />
      <el-button @click="fetchList">搜索</el-button>
      <el-button @click="searchWord=''; fetchList()">重制</el-button>

      <div style="flex: 1"></div>

      <el-button 
        :loading="refreshing" 
        icon="Refresh" 
        @click="refreshDfa"
      >
        刷新 DFA
      </el-button>
      <el-button type="primary" icon="Plus" @click="openAdd">
        新增敏感词
      </el-button>
    </div>

    <el-card class="table-card">
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column prop="word" label="敏感词">
          <template #default="{ row }">
            <span class="word-text">{{ row.word }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="row.category === '政治' || row.category === '色情' ? 'danger' : 'warning'">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="level" label="等级" width="120">
          <template #default="{ row }">
            <el-tag :type="row.level === 2 ? 'danger' : 'info'" effect="dark">
              {{ row.level === 1 ? '替换' : '拦截' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteWord(row.id)">删除</el-button>
          </template>
        </el-table-column>
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
      v-model="showModal" 
      :title="editId ? '编辑敏感词' : '新增敏感词'" 
      width="450px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" label-position="top">
        <el-form-item label="敏感词内容">
          <el-input v-model="form.word" placeholder="请输入要过滤的关键词" />
        </el-form-item>
        
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="政治" value="政治" />
            <el-option label="色情" value="色情" />
            <el-option label="暴力" value="暴力" />
            <el-option label="广告" value="广告" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="敏感等级">
          <el-radio-group v-model="form.level">
            <el-radio :label="1">1 - 替换处理</el-radio>
            <el-radio :label="2">2 - 直接拦截</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showModal = false">取消</el-button>
          <el-button type="primary" @click="saveWord">保存提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { sensitiveApi } from '@/api/index'

const toast = inject('toast')
const list       = ref([])
const loading    = ref(false)
const page       = ref(1)
const pageSize   = ref(15)
const total      = ref(0)
const searchWord = ref('')
const showModal  = ref(false)
const editId     = ref(null)
const refreshing = ref(false)
const form = ref({ word: '', category: '政治', level: 1 })

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function categoryBadge(c) {
  const m = { 政治:'badge-red', 色情:'badge-red', 暴力:'badge-yellow', 广告:'badge-blue' }
  return m[c] || 'badge-blue'
}
function levelBadge(l) { return l === 2 ? 'badge-red' : 'badge-yellow' }
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN',{hour12:false}) : '-' }

async function fetchList() {
  if (loading.value) return
  
  loading.value = true
  try {
    const res = await sensitiveApi.getPage(page.value, pageSize.value, searchWord.value)
    // 调试打印：看看后端到底回了什么
    console.log('后端原始返回：', res) 
    
    if (res.code === 200 && res.data) {
      // 兼容性写法：防止 records 字段不存在导致 list 被清空
      list.value = res.data.list || (Array.isArray(res.data) ? res.data : [])
      total.value = res.data.total || list.value.length
    } else {
      toast(res.msg || '数据格式异常', 'error')
    }
  } catch (err) {
    console.error('请求发生错误：', err)
    toast('加载失败', 'error')
  } finally {
    setTimeout(() => {
      loading.value = false
    }, 200) 
  }
}

function openAdd() { editId.value=null; form.value={word:'',category:'政治',level:1}; showModal.value=true }
function openEdit(item) {
  editId.value = item.id
  form.value = { word:item.word, category:item.category, level:item.level }
  showModal.value = true
}

async function saveWord() {
  try {
    if (editId.value) {
      await sensitiveApi.update(editId.value, form.value)
    } else {
      await sensitiveApi.add([form.value])
    }
    toast('保存成功')
    showModal.value = false
    fetchList()
  } catch { toast('保存失败', 'error') }
}

async function deleteWord(id) {
  if (!confirm('确认删除？')) return
  try { await sensitiveApi.deleteById(id); toast('删除成功'); fetchList() }
  catch { toast('删除失败','error') }
}

async function refreshDfa() {
  refreshing.value = true
  try {
    const res = await sensitiveApi.refreshDfa()
    toast(res.data || '已刷新')
  } catch { toast('刷新失败','error') } finally { refreshing.value=false }
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
.word-text { font-family:var(--font-mono); color:var(--text-primary); font-size:13px; }
</style>
