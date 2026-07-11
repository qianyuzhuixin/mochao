<template>
  <default-layout>
    <div class="novel-list-page page-container">
      <div class="page-header">
        <h1 class="page-title">我的小说</h1>
        <el-button type="primary" icon="el-icon-plus" @click="$router.push('/novels/create')">
          创建小说
        </el-button>
      </div>

      <div v-loading="loading" class="novel-grid">
        <div
          v-for="novel in list"
          :key="novel.id"
          class="novel-card"
          @click="goToWorkspace(novel)"
        >
          <div class="novel-cover" :style="{ background: gradientColor(novel.id) }">
            <span class="novel-initial">{{ novel.title ? novel.title.charAt(0) : '?' }}</span>
          </div>
          <div class="novel-info">
            <h3 class="novel-title">{{ novel.title }}</h3>
            <p class="novel-desc">{{ novel.summary || novel.synopsis || novel.description || '暂无简介' }}</p>
            <div class="novel-meta">
              <span class="meta-item">
                <i class="el-icon-document" />
                {{ novel.chapterCount || 0 }} 章
              </span>
              <span class="meta-item">
                <i class="el-icon-edit" />
                {{ novel.totalWords || 0 }} 字
              </span>
              <span class="meta-item">
                <i class="el-icon-time" />
                {{ novel.updatedAt | formatDate }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <empty-state
        v-if="!loading && list.length === 0"
        text="还没有创建小说"
        description="点击右上角按钮开始创作你的第一部小说"
      >
        <template #action>
          <el-button type="primary" @click="$router.push('/novels/create')">创建小说</el-button>
        </template>
      </empty-state>
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getNovels } from '@/api/novel'

export default {
  name: 'NovelList',
  components: { DefaultLayout, EmptyState },
  filters: {
    formatDate(val) {
      if (!val) return '未知'
      const d = new Date(val)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
  },
  data() {
    return {
      list: [],
      loading: false
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getNovels()
        this.list = res.list || res.records || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    goToWorkspace(novel) {
      if (!novel || !novel.id) {
        this.$message.error('小说信息异常，无法打开')
        return
      }
      this.$router.push(`/novels/${novel.id}`)
    },
    gradientColor(id) {
      const gradients = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
      ]
      const hash = String(id || '').split('').reduce((a, b) => a + b.charCodeAt(0), 0)
      return gradients[hash % gradients.length]
    }
  }
}
</script>

<style lang="scss" scoped>
.novel-list-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: #{$spacing-lg};
  }

  .novel-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: #{$spacing-md};
    min-height: 200px;
  }

  .novel-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    overflow: hidden;
    cursor: pointer;
    transition: all $transition-base;

    &:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-2px);
    }

    .novel-cover {
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;

      .novel-initial {
        font-size: 48px;
        font-weight: 800;
        color: rgba(255, 255, 255, 0.9);
      }
    }

    .novel-info {
      padding: #{$spacing-md};

      .novel-title {
        font-size: $font-size-md;
        font-weight: 600;
        color: var(--color-text);
        margin-bottom: #{$spacing-xs};
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .novel-desc {
        font-size: $font-size-sm;
        color: var(--color-text-secondary);
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        margin-bottom: #{$spacing-sm};
      }

      .novel-meta {
        display: flex;
        gap: #{$spacing-md};
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);

        .meta-item {
          display: flex;
          align-items: center;
          gap: 2px;
        }
      }
    }
  }
}
</style>
