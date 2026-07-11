<template>
  <div class="ai-toolbar" v-show="visible" :style="positionStyle">
    <div class="toolbar-inner">
      <el-button size="mini" @click="handleAction('optimize')">优化</el-button>
      <el-button size="mini" @click="handleAction('expand')">扩写</el-button>
      <el-button size="mini" @click="handleAction('condense')">缩写</el-button>
      <el-button size="mini" @click="handleAction('continue')">续写</el-button>
      <el-button size="mini" @click="handleAction('polish')">润色对话</el-button>
      <el-button size="mini" @click="handleAction('predict')">预判</el-button>
      <i class="el-icon-close toolbar-close" @click="$emit('close')" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiToolbar',
  props: {
    visible: { type: Boolean, default: false },
    position: { type: Object, default: () => ({ x: 0, y: 0 }) }
  },
  computed: {
    positionStyle() {
      return {
        left: `${this.position.x}px`,
        top: `${this.position.y}px`
      }
    }
  },
  methods: {
    handleAction(type) {
      this.$emit('action', type)
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-toolbar {
  position: fixed;
  z-index: $z-index-dropdown;

  .toolbar-inner {
    display: flex;
    align-items: center;
    gap: #{$spacing-xs};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-base;
    box-shadow: var(--shadow-md);
    padding: #{$spacing-xs} #{$spacing-sm};

    .el-button--mini {
      padding: 4px 10px;
      font-size: $font-size-xs;
    }

    .toolbar-close {
      cursor: pointer;
      color: var(--color-text-secondary);
      margin-left: #{$spacing-xs};

      &:hover {
        color: var(--color-error);
      }
    }
  }
}
</style>
