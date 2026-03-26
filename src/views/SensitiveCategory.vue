<template>
  <div class="page-content">
    <div class="page-header">
      <div>
        <div class="page-title">敏感词分类</div>
        <div class="page-subtitle">按分类维度统计和管理敏感词库</div>
      </div>
    </div>

    <!-- 分类卡片：直接用 div grid，避免 el-row/el-col 冲突 -->
    <div class="category-grid">
      <el-card
        v-for="cat in categories"
        :key="cat.name"
        class="category-card"
        shadow="hover"
        :body-style="{ padding: '18px' }"
      >
        <div class="cat-header">
          <span class="cat-icon">{{ cat.icon }}</span>
          <span class="cat-name">{{ cat.name }}</span>
        </div>
        <div class="cat-count" :style="{ color: cat.color }">{{ cat.count }}</div>
        <div class="cat-label">条敏感词</div>
        <el-progress
          :percentage="cat.pct"
          :color="cat.color"
          :show-text="false"
          :stroke-width="4"
        />
      </el-card>
    </div>

    <el-card style="margin-top: 20px">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="font-size: 16px; font-weight: 600">数据详情</span>
          <el-radio-group v-model="activeCategory" size="small">
            <el-radio-button label="全部" />
            <el-radio-button label="政治" />
            <el-radio-button label="色情" />
            <el-radio-button label="暴力" />
            <el-radio-button label="违法" />
            <el-radio-button label="其他" />
          </el-radio-group>
        </div>
      </template>

      <el-table
        :data="filtered"
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="word" label="敏感词">
          <template #default="{ row }">
            <code style="color: var(--el-text-color-primary)">{{ row.word }}</code>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="分类" width="150">
          <template #default="{ row }">
            <el-tag 
              size="small" 
              effect="plain"
              style="
                color: #8b5cf6; 
                border-color: #ddd6fe; 
                background-color: #f5f3ff;
                font-weight: 500; ">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="level" label="等级" width="150">
          <template #default="{ row }">
            <el-tag
              :type="row.level === 2 ? 'danger' : 'warning'"
              size="small"
            >
              {{ row.level === 1 ? '替换' : '拦截' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="200">
          <template #default="{ row }">
            <span style="color: var(--el-text-color-secondary)">
              {{ row.createTime ? new Date(row.createTime).toLocaleDateString() : '-' }}
            </span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="该分类下暂无敏感词" :image-size="60" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { sensitiveApi } from '@/api/index'

const toast          = inject('toast')
const allWords       = ref([])
const loading        = ref(false)
const activeCategory = ref('全部')

const filtered = computed(() => {
  if (activeCategory.value === '全部') return allWords.value
  return allWords.value.filter(w => w.category === activeCategory.value)
})

const categories = computed(() => {
  const cats = [
    { name: '政治', icon: '🏛', color: '#f87171' },
    { name: '色情', icon: '🔞', color: '#f87171' },
    { name: '暴力', icon: '⚔',  color: '#fbbf24' },
    { name: '违法', icon: '⚖️', color: '#3b82f6' },
    { name: '其他', icon: '⊕',  color: '#8b5cf6' },
  ]
  const total = allWords.value.length || 1
  return cats.map(c => {
    const count = allWords.value.filter(w => w.category === c.name).length
    return { 
      ...c, 
      count, 
      pct: Math.round((count / total) * 100) 
    }
  })
})

async function loadAll() {
  loading.value = true
  try {
    const res = await sensitiveApi.getPage(1, 15, '')
    if (res.code === 200) {
      allWords.value = res.data.list || []
    }
  } catch (err) {
    console.error(err)
    toast('加载失败', 'error')
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
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

.page-header { margin-bottom: 20px; }
.page-title  { font-size: 20px; font-weight: 700; color: var(--text-primary); }
.page-subtitle { font-size: 13px; color: var(--text-muted); margin-top: 3px; }

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
  margin-bottom: 0;
}

.category-card {
  border-radius: var(--radius-lg);
  transition: transform 0.2s;
}
.category-card:hover { transform: translateY(-2px); }

.cat-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.cat-icon  { font-size: 20px; }
.cat-name  { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.cat-count {
  font-size: 32px;
  font-weight: 700;
  font-family: var(--font-mono);
  line-height: 1.1;
}
.cat-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 10px;
  margin-top: 2px;
}
</style>