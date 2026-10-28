<template>
  <div class="page-content">
    <PageHeader title="敏感词分类" subtitle="按分类维度统计和管理敏感词库" />

    <ErrorState v-if="error" title="分类数据加载失败" :message="error" @retry="loadAll" />

    <template v-else>
      <div class="category-grid" v-loading="loading">
        <div v-for="cat in categories" :key="cat.name" class="category-card">
          <div class="cat-top">
            <div class="cat-icon" :style="{ background: cat.soft, color: cat.color }">
              <component :is="cat.icon" :size="17" />
            </div>
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
        </div>
      </div>

      <el-card class="table-card">
        <template #header>
          <div class="table-head">
            <span class="table-title">数据详情</span>
            <el-radio-group v-model="activeCategory" size="small" class="filter-group">
              <el-radio-button label="全部" />
              <el-radio-button v-for="cat in categories" :key="cat.name" :label="cat.name" />
            </el-radio-group>
          </div>
        </template>

        <div class="table-wrap">
          <el-table :data="filtered" v-loading="loading" style="width: 100%">
            <el-table-column label="敏感词" min-width="220">
              <template #default="{ row }">
                <span class="word-text">{{ row.word }}</span>
              </template>
            </el-table-column>

            <el-table-column label="分类" width="140">
              <template #default="{ row }">
                <el-tag type="info" effect="plain">{{ row.category || '未分类' }}</el-tag>
              </template>
            </el-table-column>

            <el-table-column label="等级" width="120">
              <template #default="{ row }">
                <el-tag :type="row.level === 2 ? 'danger' : 'warning'" effect="plain">
                  {{ row.level === 2 ? '拦截' : '替换' }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                <span class="muted-cell">{{ formatTime(row.createTime) }}</span>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty description="该分类下暂无敏感词" :image-size="64">
                <template #image>
                  <div class="empty-illustration">
                    <BarChart3 :size="26" />
                  </div>
                </template>
              </el-empty>
            </template>
          </el-table>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  BarChart3,
  EyeOff,
  Landmark,
  MoreHorizontal,
  Scale,
  Swords,
} from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import ErrorState from '@/components/ErrorState.vue'
import { sensitiveApi } from '@/api'

const CORE_CATEGORIES = ['政治', '色情', '暴力', '违法']

const allWords = ref([])
const loading = ref(false)
const error = ref('')
const activeCategory = ref('全部')

const categoryMeta = [
  { name: '政治', icon: Landmark, color: 'hsl(0 70% 62%)', soft: 'hsl(0 50% 12%)' },
  { name: '色情', icon: EyeOff, color: 'hsl(0 70% 62%)', soft: 'hsl(0 50% 12%)' },
  { name: '暴力', icon: Swords, color: 'hsl(35 85% 62%)', soft: 'hsl(35 50% 12%)' },
  { name: '违法', icon: Scale, color: 'hsl(187 60% 55%)', soft: 'hsl(187 45% 13%)' },
  { name: '其他', icon: MoreHorizontal, color: 'hsl(221 80% 68%)', soft: 'hsl(221 55% 14%)' },
]

const categories = computed(() => {
  const total = allWords.value.length || 1
  return categoryMeta.map((cat) => {
    const count = cat.name === '其他'
      ? allWords.value.filter((word) => !CORE_CATEGORIES.includes(word.category)).length
      : allWords.value.filter((word) => word.category === cat.name).length
    return { ...cat, count, pct: Math.round((count / total) * 100) }
  })
})

const filtered = computed(() => {
  if (activeCategory.value === '全部') return allWords.value
  if (activeCategory.value === '其他') {
    return allWords.value.filter((word) => !CORE_CATEGORIES.includes(word.category))
  }
  return allWords.value.filter((word) => word.category === activeCategory.value)
})

function formatTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('zh-CN')
}

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    let page = 1
    let total = Infinity
    const collected = []

    while (page <= 20 && collected.length < total) {
      const res = await sensitiveApi.getPage(page, 100, '')
      if (res.code !== 200) {
        throw new Error(res.msg || '加载失败')
      }
      const chunk = res.data?.list || []
      collected.push(...chunk)
      total = res.data?.total ?? collected.length
      if (!chunk.length) break
      page++
    }

    allWords.value = collected
  } catch (e) {
    error.value = e.userMessage || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.category-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
  border-radius: var(--radius);
}

.category-card {
  padding: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.cat-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.cat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
}

.cat-name {
  color: var(--ink-2);
  font-size: 12.5px;
  font-weight: 600;
}

.cat-count {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 500;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.cat-label {
  margin: 4px 0 12px;
  color: var(--ink-3);
  font-size: 11px;
}

.table-card {
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.table-title {
  color: var(--ink);
  font-size: 15px;
  font-weight: 600;
}

.filter-group {
  flex-wrap: wrap;
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

.muted-cell {
  color: var(--ink-3);
  font-size: 12.5px;
}

@media (max-width: 1100px) {
  .category-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .category-card {
    padding: 16px;
  }

  .cat-count {
    font-size: 24px;
  }

  .table-head {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 420px) {
  .category-grid {
    gap: 8px;
  }

  .category-card {
    padding: 12px;
  }

  .cat-count {
    font-size: 22px;
  }
}
</style>
