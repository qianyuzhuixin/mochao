<template>
  <div class="empty-state">
    <el-image v-if="image" :src="image" class="empty-image" fit="contain" />
    <i v-else :class="'el-icon-' + (icon || 'folder-opened')" class="empty-icon" />
    <p class="empty-text">{{ text }}</p>
    <p v-if="description" class="empty-description">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'EmptyState',
  props: {
    icon: {
      type: String,
      default: 'folder-opened'
    },
    text: {
      type: String,
      default: '暂无数据'
    },
    description: {
      type: String,
      default: ''
    },
    image: {
      type: String,
      default: ''
    }
  }
}
</script>

<style lang="scss" scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: #{$spacing-xxl} #{$spacing-lg};

  .empty-image {
    width: 120px;
    height: 120px;
    margin-bottom: #{$spacing-md};
  }

  .empty-icon {
    font-size: 64px;
    color: var(--color-text-placeholder);
    margin-bottom: #{$spacing-md};
  }

  .empty-text {
    font-size: $font-size-md;
    color: var(--color-text-secondary);
    margin-bottom: #{$spacing-xs};
  }

  .empty-description {
    font-size: $font-size-sm;
    color: var(--color-text-placeholder);
  }

  .empty-action {
    margin-top: #{$spacing-md};
  }
}
</style>
