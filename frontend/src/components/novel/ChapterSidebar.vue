<template>
  <div class="chapter-sidebar">
    <div class="sidebar-header">
      <span class="sidebar-title">章节列表</span>
      <el-button type="text" icon="el-icon-refresh" @click="$emit('refresh')" />
    </div>
    <div class="sidebar-body">
      <div
        v-for="ch in chapters"
        :key="ch.id"
        class="chapter-item"
        :class="{ active: ch.id === currentId }"
        @click="$emit('select', ch)"
      >
        <el-tag :type="statusType(ch.status)" size="mini">{{ statusText(ch.status) }}</el-tag>
        <span class="chapter-title">{{ ch.title || `第${ch.sort || ch.order}章` }}</span>
        <span class="chapter-words">{{ ch.wordCount || 0 }}字</span>
      </div>
      <div v-if="!chapters || chapters.length === 0" class="empty-text">
        暂无章节
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChapterSidebar',
  props: {
    chapters: { type: Array, default: () => [] },
    currentId: { type: [String, Number], default: null }
  },
  methods: {
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
.chapter-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  overflow: hidden;

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: #{$spacing-sm} #{$spacing-md};
    border-bottom: 1px solid var(--color-border);
    background-color: var(--color-bg-secondary);

    .sidebar-title {
      font-size: $font-size-base;
      font-weight: 600;
      color: var(--color-text);
    }
  }

  .sidebar-body {
    flex: 1;
    overflow-y: auto;
    padding: #{$spacing-xs};

    .chapter-item {
      display: flex;
      align-items: center;
      gap: #{$spacing-xs};
      padding: #{$spacing-sm} #{$spacing-xs};
      border-radius: $border-radius-base;
      cursor: pointer;
      transition: background-color $transition-fast;

      &:hover {
        background-color: var(--color-bg-secondary);
      }

      &.active {
        background-color: var(--color-primary-bg);

        .chapter-title {
          color: var(--color-primary);
        }
      }

      .chapter-title {
        flex: 1;
        font-size: $font-size-sm;
        color: var(--color-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .chapter-words {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
        white-space: nowrap;
      }
    }

    .empty-text {
      text-align: center;
      padding: #{$spacing-lg};
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
    }
  }
}
</style>
