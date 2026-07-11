<template>
  <div class="collection-card">
    <div class="card-header">
      <el-tag :type="data.type === 'word' ? 'success' : 'primary'" size="mini">
        {{ data.type === 'word' ? '好词' : '好句' }}
      </el-tag>
      <el-dropdown trigger="click" @command="handleCommand">
        <i class="el-icon-more card-more" />
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="edit">编辑</el-dropdown-item>
          <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <div class="card-content">{{ data.content }}</div>
    <div class="card-footer">
      <div class="card-tags">
        <el-tag v-for="tag in (data.tags || []).slice(0, 3)" :key="tag" size="mini" effect="plain">
          {{ tag }}
        </el-tag>
      </div>
      <span v-if="data.bookTitle" class="card-source">{{ data.bookTitle }}</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CollectionCard',
  props: {
    data: { type: Object, required: true }
  },
  methods: {
    handleCommand(command) {
      this.$emit(command, this.data)
    }
  }
}
</script>

<style lang="scss" scoped>
.collection-card {
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  padding: #{$spacing-md};
  transition: all $transition-base;

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: #{$spacing-sm};

    .card-more {
      cursor: pointer;
      color: var(--color-text-secondary);
      font-size: 16px;

      &:hover {
        color: var(--color-primary);
      }
    }
  }

  .card-content {
    font-size: $font-size-base;
    line-height: 1.8;
    color: var(--color-text);
    margin-bottom: #{$spacing-sm};
    display: -webkit-box;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: #{$spacing-sm};

    .card-tags {
      display: flex;
      gap: #{$spacing-xs};
      flex-wrap: wrap;
    }

    .card-source {
      font-size: $font-size-xs;
      color: var(--color-text-placeholder);
      white-space: nowrap;
    }
  }
}
</style>
