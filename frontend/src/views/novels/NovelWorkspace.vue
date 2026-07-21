<template>
  <div class="novel-workspace page-container" v-loading="loading">
    <div v-if="novel" class="workspace-content">
      <div class="workspace-header">
        <div>
          <h1 class="page-title">{{ novel.title }}</h1>
          <div class="novel-meta">
            <el-tag size="mini">{{ novel.genre || '未分类' }}</el-tag>
            <span v-for="tag in (novel.tags || [])" :key="tag">
              <el-tag size="mini" effect="plain" class="ml-xs">{{ tag }}</el-tag>
            </span>
          </div>
        </div>
        <el-button type="text" icon="el-icon-arrow-left" @click="$router.push('/novels')">
          返回列表
        </el-button>
      </div>

      <p v-if="novel.summary || novel.synopsis" class="novel-synopsis">{{ novel.summary || novel.synopsis }}</p>

      <!-- 进度概览 -->
      <ProgressOverview :data="progressData" class="mb-lg" />

      <!-- 功能入口 -->
      <h3 class="section-title">创作工作台</h3>
      <div class="feature-grid">
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/outline`)">
          <i class="el-icon-document icon" style="color: #4A6CF7" />
          <div class="entry-info">
            <span class="entry-title">大纲</span>
            <span class="entry-desc">故事大纲规划</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/volumes`)">
          <i class="el-icon-notebook-1 icon" style="color: #EB2F96" />
          <div class="entry-info">
            <span class="entry-title">卷纲</span>
            <span class="entry-desc">分卷大纲规划</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/acts`)">
          <i class="el-icon-collection-tag icon" style="color: #52C41A" />
          <div class="entry-info">
            <span class="entry-title">幕</span>
            <span class="entry-desc">幕纲情节细化</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/worldview`)">
          <i class="el-icon-picture-outline icon" style="color: #FAAD14" />
          <div class="entry-info">
            <span class="entry-title">世界观</span>
            <span class="entry-desc">设定世界规则</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/characters`)">
          <i class="el-icon-user icon" style="color: #FA8C16" />
          <div class="entry-info">
            <span class="entry-title">人物设定</span>
            <span class="entry-desc">管理角色信息</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/items`)">
          <i class="el-icon-present icon" style="color: #FF4D4F" />
          <div class="entry-info">
            <span class="entry-title">物品设定</span>
            <span class="entry-desc">管理道具物品</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/chapter-outlines`)">
          <i class="el-icon-notebook-2 icon" style="color: #722ED1" />
          <div class="entry-info">
            <span class="entry-title">章纲管理</span>
            <span class="entry-desc">规划章节大纲</span>
          </div>
        </div>
        <div class="feature-entry" @click="$router.push(`/novels/${novelId}/progress`)">
          <i class="el-icon-data-line icon" style="color: #13C2C2" />
          <div class="entry-info">
            <span class="entry-title">写作进度</span>
            <span class="entry-desc">查看创作数据</span>
          </div>
        </div>
      </div>

      <!-- 最近章节 -->
      <h3 class="section-title mt-xl">最近章节</h3>
      <div v-if="chapters && chapters.length" class="chapter-list">
        <div
          v-for="ch in chapters.slice(0, 5)"
          :key="ch.id"
          class="chapter-item"
          @click="$router.push(`/novels/${novelId}/chapters/${ch.id}`)"
        >
          <el-tag :type="statusType(ch.status)" size="mini">{{ statusText(ch.status) }}</el-tag>
          <span class="chapter-title">{{ ch.title || `第${ch.sort}章` }}</span>
          <span class="chapter-words">{{ ch.wordCount || 0 }}字</span>
          <i class="el-icon-arrow-right" />
        </div>
      </div>
      <EmptyState v-else text="暂无章节" description="去章纲管理中创建章节吧" />
    </div>
  </div>
</template>

<script>
import ProgressOverview from '@/components/novel/ProgressOverview.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getNovelById, getNovelProgress, getChapters } from '@/api/novel'

export default {
  name: 'NovelWorkspace',
  components: { ProgressOverview, EmptyState },
  data() {
    return {
      novel: null,
      progressData: {},
      chapters: [],
      loading: false
    }
  },
  computed: {
    novelId() {
      const id = this.$route.params.id
      // 校验 ID 是有效数字（防止 "undefined" / "null" 等无效值）
      if (!id || id === 'undefined' || id === 'null' || isNaN(Number(id))) {
        return null
      }
      return id
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      // 前置校验：novelId 无效则直接跳回列表
      if (!this.novelId) {
        this.$message.error('小说ID无效，请重新选择')
        this.$router.replace('/novels')
        return
      }

      this.loading = true
      try {
        // 先获取小说基本信息，成功后再并行获取进度和章节
        const novel = await getNovelById(this.novelId)
        this.novel = novel

        // 并行获取进度和章节，等待两者完成后再结束 loading
        await Promise.all([
          getNovelProgress(this.novelId).then(res => {
            this.progressData = res || {}
          }).catch(() => {
            this.progressData = {}
          }),
          getChapters(this.novelId).then(res => {
            this.chapters = (res && (res.records || res.list)) || []
          }).catch(() => {
            this.chapters = []
          })
        ])
      } catch (e) {
        this.$message.error('加载小说信息失败，可能不存在或无权访问')
        setTimeout(() => this.$router.replace('/novels'), 1500)
      } finally {
        this.loading = false
      }
    },
    statusText(status) {
      const map = { draft: '草稿', writing: '写作中', completed: '已完成', published: '已发布' }
      return map[status] || '草稿'
    },
    statusType(status) {
      const map = { draft: 'info', writing: 'warning', completed: 'success', published: 'success' }
      return map[status] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.novel-workspace {
  .workspace-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: #{$spacing-md};

    .novel-meta {
      display: flex;
      align-items: center;
      gap: #{$spacing-xs};
      margin-top: #{$spacing-xs};
    }
  }

  .novel-synopsis {
    font-size: $font-size-base;
    color: var(--color-text-secondary);
    line-height: 1.8;
    margin-bottom: #{$spacing-lg};
    padding: #{$spacing-md};
    background-color: var(--color-bg-secondary);
    border-radius: $border-radius-md;
  }

  .section-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: #{$spacing-md};
  }

  .feature-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: #{$spacing-md};
  }

  .feature-entry {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    padding: #{$spacing-lg};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    cursor: pointer;
    transition: all $transition-base;

    &:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-2px);
      border-color: var(--color-primary);
    }

    .icon {
      font-size: 28px;
    }

    .entry-info {
      display: flex;
      flex-direction: column;

      .entry-title {
        font-size: $font-size-base;
        font-weight: 600;
        color: var(--color-text);
      }

      .entry-desc {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }
  }

  .chapter-list {
    .chapter-item {
      display: flex;
      align-items: center;
      gap: #{$spacing-sm};
      padding: #{$spacing-sm} #{$spacing-md};
      background-color: var(--color-card-bg);
      border: 1px solid var(--color-border);
      border-radius: $border-radius-base;
      margin-bottom: #{$spacing-xs};
      cursor: pointer;
      transition: all $transition-fast;

      &:hover {
        background-color: var(--color-bg-secondary);
      }

      .chapter-title {
        flex: 1;
        font-size: $font-size-base;
        color: var(--color-text);
      }

      .chapter-words {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
      }
    }
  }
}
</style>
