<template>
  <div class="summary-practice-page" v-loading="loading">
    <DefaultLayout>
      <div class="sp-container">
        <!-- 顶部导航 -->
        <div class="sp-topbar">
          <el-button type="text" icon="el-icon-arrow-left" @click="handleBack">
            返回
          </el-button>
          <span v-if="bookInfo" class="sp-title">{{ bookInfo.title }}</span>
          <div class="flex-1" />
        </div>

        <!-- 步骤指示器 -->
        <div class="step-indicator">
          <div
            v-for="(step, i) in steps"
            :key="i"
            :class="['step-item', { active: currentStep === i, done: currentStep > i }]"
            @click="i < currentStep && goToStep(i)"
          >
            <div class="step-circle">
              <i v-if="currentStep > i" class="el-icon-check" />
              <span v-else>{{ i + 1 }}</span>
            </div>
            <span class="step-label">{{ step }}</span>
            <div v-if="i < steps.length - 1" class="step-line" :class="{ filled: currentStep > i }" />
          </div>
        </div>

        <!-- 步骤1: 阅读原文写摘要 -->
        <div v-if="currentStep === 0" class="step-content">
          <div class="step-header">
            <h3>第一步：阅读原文，写出摘要</h3>
            <p class="step-desc">仔细阅读左侧原文，在右侧用约300字概括核心内容</p>
          </div>
          <div class="split-panel">
            <!-- 左侧原文（只读） -->
            <div class="panel panel-left">
              <div class="panel-header">
                <i class="el-icon-document" />
                <span>原文</span>
                <span class="word-count">{{ originalText.length }}字</span>
              </div>
              <div class="panel-body original-text" :class="{ 'is-full-book': isFullBook }" ref="originalPanel">
                {{ originalText }}
              </div>
            </div>
            <!-- 右侧摘要输入 -->
            <div class="panel panel-right">
              <div class="panel-header">
                <i class="el-icon-edit-outline" />
                <span>你的摘要</span>
                <span :class="['word-count', { 'over-limit': summaryLength > 350 }]">
                  {{ summaryLength }} / 300字
                </span>
              </div>
              <div class="panel-body">
                <el-input
                  ref="summaryInput"
                  v-model="summaryText"
                  type="textarea"
                  :rows="16"
                  placeholder="在此输入约300字的摘要，概括原文的核心情节、人物关系和关键转折..."
                  class="summary-textarea"
                />
                <div class="panel-tips">
                  <i class="el-icon-info" />
                  建议300字左右，用你自己的语言概括，不要直接抄原文
                </div>
              </div>
            </div>
          </div>
          <div class="step-actions">
            <el-button type="primary" :disabled="!summaryText.trim()" @click="confirmSummary">
              下一步：开始写作
            </el-button>
          </div>
        </div>

        <!-- 步骤2: 根据摘要写自己的章节 -->
        <div v-if="currentStep === 1" class="step-content">
          <div class="step-header">
            <h3>第二步：根据摘要，写出你的章节</h3>
            <p class="step-desc">参考下方摘要，用自己的语言和风格重新创作这段内容</p>
          </div>
          <!-- 摘要参考区 -->
          <div class="summary-ref">
            <div class="ref-header">
              <i class="el-icon-collection-tag" />
              <span>你的摘要</span>
            </div>
            <div class="ref-body">{{ summaryText }}</div>
          </div>
          <!-- 写作区 -->
          <div class="write-area">
            <div class="write-header">
              <i class="el-icon-edit" />
              <span>你的章节内容</span>
              <span class="word-count">{{ selfWrittenLength }}字</span>
            </div>
            <el-input
              ref="writeInput"
              v-model="selfWrittenText"
              type="textarea"
              :rows="18"
              placeholder="根据上面的摘要，发挥你的创意，用自己的语言写出属于你的章节..."
              class="write-textarea"
            />
          </div>
          <div class="step-actions">
            <el-button @click="goToStep(0)">上一步</el-button>
            <el-button type="primary" :disabled="!selfWrittenText.trim()" @click="confirmWriting">
              下一步：对比原文
            </el-button>
          </div>
        </div>

        <!-- 步骤3: 对比原文 -->
        <div v-if="currentStep === 2" class="step-content">
          <div class="step-header">
            <h3>第三步：对比原文，学习提升</h3>
            <p class="step-desc">对比你的作品与原文，发现差异，学习精华</p>
          </div>
          <!-- 统计对比 -->
          <div class="compare-stats">
            <div class="stat-card">
              <span class="stat-label">原文字数</span>
              <span class="stat-value">{{ originalText.length }}</span>
            </div>
            <div class="stat-card highlight">
              <span class="stat-label">你的字数</span>
              <span class="stat-value">{{ selfWrittenLength }}</span>
            </div>
            <div class="stat-card">
              <span class="stat-label">摘录字数</span>
              <span class="stat-value">{{ summaryLength }}</span>
            </div>
          </div>
          <!-- 并排对比 -->
          <div class="split-panel compare-panel">
            <div class="panel panel-left">
              <div class="panel-header">
                <i class="el-icon-document" />
                <span>原文</span>
              </div>
              <div class="panel-body original-text" :class="{ 'is-full-book': isFullBook }">
                <p v-for="(para, i) in originalParagraphs" :key="'orig-' + i" class="text-para">
                  {{ para }}
                </p>
              </div>
            </div>
            <div class="panel panel-right">
              <div class="panel-header">
                <i class="el-icon-edit" />
                <span>你的作品</span>
              </div>
              <div class="panel-body self-text">
                <p v-for="(para, i) in selfWrittenParagraphs" :key="'self-' + i" class="text-para">
                  {{ para }}
                </p>
              </div>
            </div>
          </div>
          <div class="step-actions">
            <el-button @click="goToStep(1)">上一步</el-button>
            <el-button type="primary" @click="goToCopyMode">
              <i class="el-icon-edit" /> 开始1:1抄写原文
            </el-button>
            <el-button type="success" @click="finishPractice">
              <i class="el-icon-circle-check" /> 完成练习
            </el-button>
          </div>
        </div>
      </div>
    </DefaultLayout>
  </div>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getActivePractice, updateProgress, completePractice, startPractice } from '@/api/practice'
import { getBookById, getBookContent } from '@/api/book'

export default {
  name: 'SummaryPractice',
  components: { DefaultLayout },
  data() {
    return {
      loading: true,
      sessionId: null,
      chapterIndex: null,
      bookInfo: null,
      originalText: '',
      summaryText: '',
      selfWrittenText: '',
      currentStep: 0,
      steps: ['写摘要', '自由创作', '对比原文'],
      saving: false
    }
  },
  computed: {
    summaryLength() {
      return this.summaryText.length
    },
    selfWrittenLength() {
      return this.selfWrittenText.length
    },
    originalParagraphs() {
      return this.splitParagraphs(this.originalText)
    },
    selfWrittenParagraphs() {
      return this.splitParagraphs(this.selfWrittenText)
    },
    isFullBook() {
      return this.chapterIndex === null || this.chapterIndex === undefined
    }
  },
  async created() {
    this.sessionId = this.$route.params.sessionId
    await this.initPractice()
  },
  methods: {
    async initPractice() {
      this.loading = true
      try {
        const session = await getActivePractice()
        let bookId

        if (session && session.id == this.sessionId) {
          bookId = session.bookId
          this.chapterIndex = session.chapterIndex != null ? session.chapterIndex : null
          // 恢复进度
          this.summaryText = session.summaryContent || ''
          this.selfWrittenText = session.selfWrittenContent || ''
          // 根据已保存内容推断当前步骤
          if (this.selfWrittenText) {
            this.currentStep = 2
          } else if (this.summaryText) {
            this.currentStep = 1
          }
        } else {
          bookId = this.$route.query.bookId
        }

        if (bookId) {
          this.bookInfo = await getBookById(bookId)
          // 根据 chapterIndex 获取对应的原文内容
          const contentRes = await getBookContent(bookId, this.chapterIndex)
          this.originalText = contentRes.content || ''
        }

        // 自动聚焦
        this.$nextTick(() => {
          this.focusCurrentInput()
        })
      } catch (e) {
        this.$message.error('加载练习失败')
      } finally {
        this.loading = false
      }
    },
    focusCurrentInput() {
      if (this.currentStep === 0 && this.$refs.summaryInput) {
        this.$refs.summaryInput.focus()
      } else if (this.currentStep === 1 && this.$refs.writeInput) {
        this.$refs.writeInput.focus()
      }
    },
    async saveProgress() {
      if (this.saving) return
      this.saving = true
      try {
        await updateProgress(this.sessionId, {
          summaryContent: this.summaryText,
          selfWrittenContent: this.selfWrittenText,
          typedContent: '',
          typedChars: this.selfWrittenLength,
          currentPosition: 0,
          errorCount: 0,
          duration: 0
        })
      } catch (e) {
        // 静默失败
      } finally {
        this.saving = false
      }
    },
    goToStep(step) {
      this.currentStep = step
      this.$nextTick(() => {
        this.focusCurrentInput()
      })
    },
    async confirmSummary() {
      await this.saveProgress()
      this.currentStep = 1
      this.$nextTick(() => {
        this.focusCurrentInput()
      })
    },
    async confirmWriting() {
      await this.saveProgress()
      this.currentStep = 2
    },
    async goToCopyMode() {
      // 完成当前摘要练习，然后创建新的抄写会话
      try {
        await completePractice(this.sessionId, {
          typedContent: '',
          typedChars: this.selfWrittenLength,
          errorCount: 0,
          duration: 0,
          accuracy: 100,
          speed: 0,
          summaryContent: this.summaryText,
          selfWrittenContent: this.selfWrittenText
        })
      } catch (e) {
        // 即使完成失败也尝试跳转
      }
      // 创建新的抄写模式会话
      try {
        const res = await startPractice({ bookId: this.bookInfo.id, mode: 'copy' })
        const sessionId = res.id || res.sessionId
        this.$router.push(`/practice/${sessionId}?bookId=${this.bookInfo.id}`)
      } catch (e) {
        this.$message.error('创建抄写练习失败')
      }
    },
    async finishPractice() {
      try {
        await completePractice(this.sessionId, {
          typedContent: '',
          typedChars: this.selfWrittenLength,
          errorCount: 0,
          duration: 0,
          accuracy: 100,
          speed: 0,
          summaryContent: this.summaryText,
          selfWrittenContent: this.selfWrittenText
        })
        this.$message.success('练习完成！')
      } catch (e) {
        // 静默处理
      }
      this.$router.push('/dashboard')
    },
    handleBack() {
      this.$confirm('确定要退出吗？未保存的内容可能会丢失。', '提示', {
        confirmButtonText: '保存并退出',
        cancelButtonText: '继续练习',
        type: 'warning'
      }).then(async () => {
        await this.saveProgress()
        this.$router.back()
      }).catch(() => {})
    },
    splitParagraphs(text) {
      if (!text) return ['']
      return text.split(/\n+/).filter(p => p.trim())
    }
  }
}
</script>

<style lang="scss" scoped>
.summary-practice-page {
  min-height: 100vh;
  background-color: var(--color-bg);
}

.sp-container {
  max-width: $content-max-width;
  margin: 0 auto;
  padding: #{$spacing-lg};
  display: flex;
  flex-direction: column;
  gap: #{$spacing-md};
  min-height: calc(100vh - #{$header-height} - 60px);
}

.sp-topbar {
  display: flex;
  align-items: center;
  gap: #{$spacing-md};

  .sp-title {
    font-size: $font-size-lg;
    font-weight: 600;
    color: var(--color-text);
  }
}

.flex-1 { flex: 1; }

// 步骤指示器
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: #{$spacing-md} 0;

  .step-item {
    display: flex;
    align-items: center;
    cursor: default;

    &.done { cursor: pointer; }

    .step-circle {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: $font-size-sm;
      font-weight: 600;
      background: var(--color-card-bg);
      border: 2px solid var(--color-border);
      color: var(--color-text-placeholder);
      transition: all 0.3s;
    }

    &.active .step-circle {
      background: var(--color-primary);
      border-color: var(--color-primary);
      color: #fff;
    }

    &.done .step-circle {
      background: var(--color-success);
      border-color: var(--color-success);
      color: #fff;
    }

    .step-label {
      margin-left: 8px;
      font-size: $font-size-sm;
      color: var(--color-text-placeholder);
      white-space: nowrap;
    }

    &.active .step-label {
      color: var(--color-primary);
      font-weight: 600;
    }

    &.done .step-label {
      color: var(--color-success);
    }

    .step-line {
      width: 60px;
      height: 2px;
      background: var(--color-border);
      margin: 0 12px;
      transition: background 0.3s;

      &.filled { background: var(--color-success); }
    }
  }
}

// 步骤内容
.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: #{$spacing-md};
  min-height: 0;
}

.step-header {
  h3 {
    font-size: $font-size-xl;
    font-weight: 600;
    color: var(--color-text);
    margin: 0 0 4px;
  }

  .step-desc {
    color: var(--color-text-secondary);
    margin: 0;
    font-size: $font-size-base;
  }
}

// 分屏面板
.split-panel {
  display: flex;
  gap: #{$spacing-md};
  flex: 1;
  min-height: 0;
}

.panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  overflow: hidden;
  background: var(--color-card-bg);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px #{$spacing-md};
  font-size: $font-size-sm;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);

  .word-count {
    margin-left: auto;
    font-weight: 400;
    font-size: $font-size-xs;
    color: var(--color-text-placeholder);

    &.over-limit { color: var(--color-danger); }
  }
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: #{$spacing-md};
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text);
}

.original-text {
  white-space: pre-wrap;
  word-break: break-all;

  &.is-full-book {
    max-height: 420px;
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--color-border);
      border-radius: 3px;

      &:hover {
        background: var(--color-text-placeholder);
      }
    }
  }
}

.summary-textarea,
.write-textarea {
  ::v-deep textarea {
    font-size: 15px;
    line-height: 1.8;
    border: none !important;
    resize: none;
    background: transparent !important;

    &:focus {
      box-shadow: none !important;
    }
  }
}

.panel-tips {
  margin-top: #{$spacing-sm};
  font-size: $font-size-xs;
  color: var(--color-text-placeholder);
  display: flex;
  align-items: center;
  gap: 4px;
}

// 摘要参考区
.summary-ref {
  border: 1px solid var(--color-primary-light);
  border-radius: $border-radius-md;
  background: var(--color-primary-lighter);
  overflow: hidden;

  .ref-header {
    padding: 8px #{$spacing-md};
    font-size: $font-size-sm;
    font-weight: 600;
    color: var(--color-primary);
    display: flex;
    align-items: center;
    gap: 6px;
    border-bottom: 1px solid var(--color-primary-light);
  }

  .ref-body {
    padding: #{$spacing-md};
    font-size: 15px;
    line-height: 1.8;
    color: var(--color-text);
    white-space: pre-wrap;
  }
}

// 写作区
.write-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  overflow: hidden;
  background: var(--color-card-bg);
  min-height: 300px;

  .write-header {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px #{$spacing-md};
    font-size: $font-size-sm;
    font-weight: 600;
    color: var(--color-text-secondary);
    background: var(--color-bg);
    border-bottom: 1px solid var(--color-border);

    .word-count {
      margin-left: auto;
      font-weight: 400;
      font-size: $font-size-xs;
      color: var(--color-text-placeholder);
    }
  }

  .write-textarea {
    flex: 1;
  }
}

// 对比统计
.compare-stats {
  display: flex;
  gap: #{$spacing-md};
  justify-content: center;

  .stat-card {
    text-align: center;
    padding: #{$spacing-sm} #{$spacing-lg};
    background: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;

    &.highlight {
      border-color: var(--color-primary);
      background: var(--color-primary-lighter);
    }

    .stat-label {
      display: block;
      font-size: $font-size-xs;
      color: var(--color-text-placeholder);
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--color-text);
    }
  }
}

.compare-panel {
  .panel-body {
    .text-para {
      margin: 0 0 12px;
      text-indent: 2em;
    }
  }
}

// 底部操作
.step-actions {
  display: flex;
  justify-content: center;
  gap: #{$spacing-md};
  padding: #{$spacing-md} 0;
}
</style>
