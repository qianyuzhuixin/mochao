<template>
  <div class="markdown-input">
    <div class="markdown-tabs">
      <span
        class="markdown-tab"
        :class="{ active: activeTab === 'edit' }"
        @click="activeTab = 'edit'"
      >
        编辑
      </span>
      <span
        class="markdown-tab"
        :class="{ active: activeTab === 'preview' }"
        @click="activeTab = 'preview'"
      >
        预览
      </span>
    </div>
    <el-input
      v-show="activeTab === 'edit'"
      :value="value"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      resize="vertical"
      @input="handleInput"
    />
    <div
      v-show="activeTab === 'preview'"
      class="markdown-preview"
      v-html="renderedHtml"
    />
  </div>
</template>

<script>
import { marked } from 'marked'

export default {
  name: 'MarkdownInput',
  props: {
    value: { type: String, default: '' },
    rows: { type: Number, default: 4 },
    placeholder: { type: String, default: '支持 Markdown 格式' }
  },
  data() {
    return {
      activeTab: 'edit'
    }
  },
  computed: {
    renderedHtml() {
      if (!this.value || !this.value.trim()) {
        return '<span class="markdown-empty">暂无内容，切换到编辑模式输入...</span>'
      }
      return marked.parse(this.value, { breaks: true, gfm: true })
    }
  },
  methods: {
    handleInput(val) {
      this.$emit('input', val)
    }
  }
}
</script>

<style lang="scss" scoped>
.markdown-input {
  .markdown-tabs {
    display: flex;
    gap: #{$spacing-sm};
    margin-bottom: #{$spacing-xs};
  }

  .markdown-tab {
    font-size: $font-size-sm;
    color: var(--color-text-secondary);
    cursor: pointer;
    padding: 4px 8px;
    border-radius: $border-radius-sm;
    transition: all $transition-base;

    &:hover {
      color: var(--color-primary);
      background-color: var(--color-primary-bg);
    }

    &.active {
      color: var(--color-primary);
      background-color: var(--color-primary-bg);
      font-weight: 500;
    }
  }

  .markdown-preview {
    min-height: 80px;
    padding: 12px;
    border: 1px solid var(--color-border);
    border-radius: $border-radius-sm;
    background-color: var(--color-card-bg);
    color: var(--color-text);
    font-size: $font-size-sm;
    line-height: 1.7;
    overflow: auto;

    ::v-deep .markdown-empty {
      color: var(--color-text-placeholder);
    }

    ::v-deep h1,
    ::v-deep h2,
    ::v-deep h3,
    ::v-deep h4,
    ::v-deep h5,
    ::v-deep h6 {
      margin-top: 16px;
      margin-bottom: 10px;
      color: var(--color-text);
      font-weight: 600;
    }

    ::v-deep p {
      margin-bottom: 10px;
    }

    ::v-deep ul,
    ::v-deep ol {
      padding-left: 20px;
      margin-bottom: 10px;
    }

    ::v-deep li {
      margin-bottom: 4px;
    }

    ::v-deep strong {
      color: var(--color-primary);
      font-weight: 600;
    }

    ::v-deep code {
      background-color: var(--color-primary-bg);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'SFMono-Regular', Consolas, monospace;
    }

    ::v-deep blockquote {
      border-left: 4px solid var(--color-primary);
      padding-left: 12px;
      margin: 10px 0;
      color: var(--color-text-secondary);
    }
  }
}
</style>
