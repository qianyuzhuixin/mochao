<template>
  <div class="ai-result-panel" v-if="visible">
    <div class="panel-header">
      <span class="panel-title">AI结果</span>
      <div class="header-actions">
        <el-button size="mini" @click="$emit('retry')" :loading="loading">再试一次</el-button>
        <i class="el-icon-close panel-close" @click="$emit('close')" />
      </div>
    </div>
    <div class="panel-body" v-loading="loading">
      <div v-if="!loading && result" class="result-content">{{ result }}</div>
      <div v-if="!loading && !result" class="result-empty">
        <span>暂无结果</span>
      </div>
    </div>
    <div class="panel-footer" v-if="result && !loading">
      <el-button size="small" @click="$emit('cancel')">放弃</el-button>
      <el-button size="small" type="primary" @click="$emit('adopt')">采纳</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiResultPanel',
  props: {
    visible: { type: Boolean, default: false },
    result: { type: String, default: '' },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style lang="scss" scoped>
.ai-result-panel {
  position: fixed;
  right: #{$spacing-lg};
  top: 50%;
  transform: translateY(-50%);
  width: 420px;
  max-width: 90vw;
  max-height: 70vh;
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-lg;
  box-shadow: var(--shadow-lg);
  z-index: $z-index-dropdown;
  display: flex;
  flex-direction: column;

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: #{$spacing-md};
    border-bottom: 1px solid var(--color-border);

    .panel-title {
      font-size: $font-size-base;
      font-weight: 600;
      color: var(--color-text);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: #{$spacing-sm};
    }

    .panel-close {
      cursor: pointer;
      color: var(--color-text-secondary);

      &:hover {
        color: var(--color-error);
      }
    }
  }

  .panel-body {
    flex: 1;
    overflow-y: auto;
    padding: #{$spacing-md};

    .result-content {
      font-size: $font-size-base;
      line-height: 1.8;
      color: var(--color-text);
      white-space: pre-wrap;
    }

    .result-empty {
      text-align: center;
      padding: #{$spacing-xl};
      color: var(--color-text-secondary);
    }
  }

  .panel-footer {
    display: flex;
    justify-content: flex-end;
    gap: #{$spacing-sm};
    padding: #{$spacing-md};
    border-top: 1px solid var(--color-border);
  }
}
</style>
