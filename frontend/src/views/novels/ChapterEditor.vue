<template>
  <div class="chapter-editor-page" v-loading="loading">
    <div class="editor-topbar">
      <el-button type="text" icon="el-icon-arrow-left" @click="$router.push(`/novels/${novelId}`)">
        返回工作台
      </el-button>
      <span v-if="chapter" class="chapter-name">{{ chapter.title }}</span>
      <div class="flex-1" />
      <el-button
        size="small"
        :type="autoSaveEnabled ? 'success' : 'info'"
        :icon="autoSaveIcon"
        @click="toggleAutoSave"
      >
        {{ autoSaveText }}
      </el-button>
      <el-button size="small" type="primary" icon="el-icon-document-checked" @click="handleSave">
        保存
      </el-button>
    </div>

    <div class="editor-body">
      <!-- 左侧侧边栏 -->
      <div class="editor-sidebar">
        <chapter-sidebar
          :chapters="chapters"
          :current-id="chapterId"
          @select="handleSelectChapter"
          @refresh="fetchChapters"
        />
        <div v-if="chapter" class="sidebar-info">
          <div v-if="chapter.outlineSummary" class="info-section">
            <div class="info-label">章纲摘要</div>
            <p class="info-text">{{ chapter.outlineSummary }}</p>
          </div>
          <div v-if="chapter.characters && chapter.characters.length" class="info-section">
            <div class="info-label">出场人物</div>
            <div class="info-tags">
              <el-tag v-for="c in chapter.characters" :key="c.id || c" size="mini">
                {{ c.name || c }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间编辑区 -->
      <div class="editor-main">
        <textarea
          ref="textareaRef"
          v-model="content"
          class="chapter-textarea"
          placeholder="开始写作..."
          @mouseup="handleTextSelect"
          @blur="handleBlur"
          spellcheck="false"
        />
      </div>
    </div>

    <!-- 底部状态栏 -->
    <div class="editor-footer">
      <span class="word-count">{{ wordCount }} 字</span>
      <span v-if="targetWords" class="word-target">目标：{{ targetWords }} 字</span>
      <el-progress
        v-if="targetWords"
        :percentage="targetProgress"
        :stroke-width="6"
        class="footer-progress"
      />
      <div class="flex-1" />
      <el-button
        v-if="prevChapter"
        size="small"
        icon="el-icon-arrow-left"
        @click="navigateChapter(prevChapter.id)"
      >
        上一章
      </el-button>
      <el-button
        v-if="nextChapter"
        size="small"
        @click="navigateChapter(nextChapter.id)"
      >
        下一章<i class="el-icon-arrow-right el-icon--right" />
      </el-button>
    </div>

    <!-- AI工具条 -->
    <ai-toolbar
      :visible="aiToolbarVisible"
      :position="aiToolbarPosition"
      @action="handleAiAction"
      @close="aiToolbarVisible = false"
    />

    <!-- AI结果面板 -->
    <ai-result-panel
      :visible="aiPanelVisible"
      :result="aiResult"
      :loading="aiLoading"
      @adopt="handleAdopt"
      @cancel="handleCancelAi"
      @retry="handleRetryAi"
      @close="handleCancelAi"
    />
  </div>
</template>

<script>
import ChapterSidebar from '@/components/novel/ChapterSidebar.vue'
import AiToolbar from '@/components/novel/AiToolbar.vue'
import AiResultPanel from '@/components/novel/AiResultPanel.vue'
import { getChapters, getChapterById, saveChapter } from '@/api/novel'
import { optimize, expand, condense, continueWrite, polishDialogue, predict, adopt } from '@/api/ai'

export default {
  name: 'ChapterEditor',
  components: { ChapterSidebar, AiToolbar, AiResultPanel },
  data() {
    return {
      loading: false,
      chapter: null,
      chapters: [],
      content: '',
      autoSaveEnabled: true,
      autoSaveTimer: null,
      lastSavedContent: '',
      aiToolbarVisible: false,
      aiToolbarPosition: { x: 0, y: 0 },
      aiPanelVisible: false,
      aiResult: '',
      aiLoading: false,
      aiAction: '',
      selectedText: '',
      aiLogId: null
    }
  },
  computed: {
    novelId() { return this.$route.params.id },
    chapterId() { return this.$route.params.chId },
    wordCount() {
      return (this.content || '').length
    },
    targetWords() {
      return this.chapter ? (this.chapter.targetWords || 2000) : 2000
    },
    targetProgress() {
      if (!this.targetWords) return 0
      return Math.min(Math.round((this.wordCount / this.targetWords) * 100), 100)
    },
    autoSaveText() {
      if (!this.autoSaveEnabled) return '自动保存关'
      return this.lastSavedContent === this.content ? '已保存' : '自动保存中'
    },
    autoSaveIcon() {
      if (!this.autoSaveEnabled) return 'el-icon-close'
      return this.lastSavedContent === this.content ? 'el-icon-circle-check' : 'el-icon-loading'
    },
    currentIndex() {
      return this.chapters.findIndex(ch => String(ch.id) === String(this.chapterId))
    },
    prevChapter() {
      const i = this.currentIndex
      return i > 0 ? this.chapters[i - 1] : null
    },
    nextChapter() {
      const i = this.currentIndex
      return i >= 0 && i < this.chapters.length - 1 ? this.chapters[i + 1] : null
    }
  },
  watch: {
    chapterId() {
      this.fetchChapter()
    },
    content() {
      if (this.autoSaveEnabled && this.lastSavedContent !== this.content) {
        this.scheduleAutoSave()
      }
    }
  },
  created() {
    this.fetchChapters()
    this.fetchChapter()
  },
  beforeDestroy() {
    this.stopAutoSave()
  },
  methods: {
    async fetchChapters() {
      try {
        this.chapters = await getChapters(this.novelId) || []
      } catch (e) {
        this.chapters = []
      }
    },
    async fetchChapter() {
      this.loading = true
      try {
        const res = await getChapterById(this.novelId, this.chapterId)
        this.chapter = res
        this.content = res.content || ''
        this.lastSavedContent = this.content
      } catch (e) {} finally {
        this.loading = false
      }
    },
    handleSelectChapter(ch) {
      this.$router.push(`/novels/${this.novelId}/chapters/${ch.id}`)
    },
    navigateChapter(chId) {
      this.$router.push(`/novels/${this.novelId}/chapters/${chId}`)
    },
    scheduleAutoSave() {
      this.stopAutoSave()
      this.autoSaveTimer = setTimeout(() => {
        this.handleSave(true)
      }, 30000)
    },
    stopAutoSave() {
      if (this.autoSaveTimer) {
        clearTimeout(this.autoSaveTimer)
        this.autoSaveTimer = null
      }
    },
    toggleAutoSave() {
      this.autoSaveEnabled = !this.autoSaveEnabled
      if (this.autoSaveEnabled) {
        this.$message.success('自动保存已开启')
      } else {
        this.stopAutoSave()
        this.$message.warning('自动保存已关闭')
      }
    },
    handleBlur() {
      if (this.autoSaveEnabled && this.lastSavedContent !== this.content) {
        this.handleSave(true)
      }
    },
    async handleSave(silent = false) {
      if (this.lastSavedContent === this.content && silent) return
      try {
        await saveChapter(this.novelId, this.chapterId, { content: this.content })
        this.lastSavedContent = this.content
        if (!silent) {
          this.$message.success('保存成功')
        }
      } catch (e) {}
    },
    handleTextSelect() {
      const textarea = this.$refs.textareaRef
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      const text = this.content.substring(start, end).trim()

      if (text.length > 0) {
        this.selectedText = text
        // 计算工具条位置
        const rect = textarea.getBoundingClientRect()
        this.aiToolbarPosition = {
          x: rect.left + rect.width / 2 - 150,
          y: rect.top + 20
        }
        this.aiToolbarVisible = true
      } else {
        this.aiToolbarVisible = false
      }
    },
    async handleAiAction(action) {
      this.aiToolbarVisible = false
      this.aiAction = action
      this.aiPanelVisible = true
      this.aiLoading = true
      this.aiResult = ''

      try {
        const data = {
          novelId: parseInt(this.novelId),
          chapterId: parseInt(this.chapterId),
          selectedText: this.selectedText
        }

        let res
        switch (action) {
          case 'optimize':
            res = await optimize(data); break
          case 'expand':
            res = await expand(data); break
          case 'condense':
            res = await condense(data); break
          case 'continue':
            res = await continueWrite(data); break
          case 'polish':
            res = await polishDialogue(data); break
          case 'predict':
            res = await predict(data); break
        }

        this.aiResult = (res && res.content) || ''
        this.aiLogId = (res && res.logId) || null
      } catch (e) {} finally {
        this.aiLoading = false
      }
    },
    async handleAdopt() {
      try {
        if (this.aiLogId) {
          await adopt(this.aiLogId)
        }

        // 替换选中文本
        const textarea = this.$refs.textareaRef
        const start = textarea.selectionStart
        const end = textarea.selectionEnd
        this.content = this.content.substring(0, start) + this.aiResult + this.content.substring(end)

        this.aiPanelVisible = false
        this.$message.success('已采纳AI结果')
        this.handleSave(true)
      } catch (e) {}
    },
    handleCancelAi() {
      this.aiPanelVisible = false
      this.aiResult = ''
    },
    async handleRetryAi() {
      await this.handleAiAction(this.aiAction)
    }
  }
}
</script>

<style lang="scss" scoped>
.chapter-editor-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: var(--color-bg);

  .editor-topbar {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    padding: #{$spacing-sm} #{$spacing-md};
    background-color: var(--color-card-bg);
    border-bottom: 1px solid var(--color-border);
    height: #{$header-height};

    .chapter-name {
      font-size: $font-size-md;
      font-weight: 600;
      color: var(--color-text);
    }
  }

  .editor-body {
    flex: 1;
    display: flex;
    overflow: hidden;

    .editor-sidebar {
      width: 260px;
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--color-border);
      background-color: var(--color-bg);

      .sidebar-info {
        padding: #{$spacing-md};
        border-top: 1px solid var(--color-border);
        overflow-y: auto;

        .info-section {
          margin-bottom: #{$spacing-md};

          .info-label {
            font-size: $font-size-xs;
            font-weight: 600;
            color: var(--color-text-secondary);
            margin-bottom: #{$spacing-xs};
          }

          .info-text {
            font-size: $font-size-sm;
            color: var(--color-text);
            line-height: 1.6;
          }

          .info-tags {
            display: flex;
            flex-wrap: wrap;
            gap: #{$spacing-xs};
          }
        }
      }
    }

    .editor-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .chapter-textarea {
        flex: 1;
        border: none;
        outline: none;
        resize: none;
        padding: #{$spacing-xl};
        font-size: $font-size-md;
        line-height: 2;
        font-family: $font-family-base;
        color: var(--color-text);
        background-color: var(--color-bg);
        width: 100%;
        height: 100%;

        &::placeholder {
          color: var(--color-text-placeholder);
        }
      }
    }
  }

  .editor-footer {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    padding: #{$spacing-sm} #{$spacing-md};
    background-color: var(--color-card-bg);
    border-top: 1px solid var(--color-border);
    height: 48px;

    .word-count {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
    }

    .word-target {
      font-size: $font-size-sm;
      color: var(--color-text-placeholder);
    }

    .footer-progress {
      width: 120px;
    }
  }
}
</style>
