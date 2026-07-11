<template>
  <div class="item-card">
    <div class="card-header" @click="$emit('click', data)">
      <i class="el-icon-present item-icon" />
      <div class="header-info">
        <h3 class="item-name">{{ data.name || '未命名' }}</h3>
        <span class="item-type">{{ data.type || '物品' }}</span>
      </div>
      <el-dropdown trigger="click" @command="handleCommand">
        <i class="el-icon-more card-more" />
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="edit">编辑</el-dropdown-item>
          <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <div v-if="data.description" class="card-body">
      <p class="item-desc">{{ data.description }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ItemCard',
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
.item-card {
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  padding: #{$spacing-md};
  transition: all $transition-base;

  &:hover {
    box-shadow: var(--shadow-md);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    cursor: pointer;

    .item-icon {
      font-size: 24px;
      color: var(--color-warning);
    }

    .header-info {
      flex: 1;
      min-width: 0;

      .item-name {
        font-size: $font-size-md;
        font-weight: 600;
        color: var(--color-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .item-type {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }

    .card-more {
      cursor: pointer;
      color: var(--color-text-secondary);

      &:hover {
        color: var(--color-primary);
      }
    }
  }

  .card-body {
    margin-top: #{$spacing-sm};

    .item-desc {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }
}
</style>
