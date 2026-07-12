<template>
  <div class="practice-page" v-loading="loading">
    <default-layout>
      <div class="practice-container" :class="{ 'focus-mode': focusMode }">
        <!-- 返回按钮 -->
        <div v-if="!focusMode" class="practice-topbar">
          <el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">
            返回
          </el-button>
          <span v-if="bookInfo" class="practice-title">
            {{ bookInfo.title }}
            <template v-if="hasChapter">
              <span class="chapter-sep">/</span>
              <span class="chapter-sub-title">{{ chapterTitle }}</span>
            </template>
          </span>
          <div class="flex-1" />
          <el-tooltip content="专注模式 (Esc 退出)" placement="bottom">
            <el-button type="text" icon="el-icon-full-screen" @click="enterFocusMode">
              专注
            </el-button>
          </el-tooltip>
        </div>

        <!-- 实时数据栏 -->
        <realtime-stats
          v-if="!focusMode"
          :duration="duration"
          :accuracy="accuracy"
          :speed="currentSpeed"
        />

        <!-- 抄写对照区 -->
        <div class="practice-main">
          <text-comparator
            v-if="originalText"
            ref="comparator"
            :original-text="originalText"
            @input="handleInput"
            @complete="handleComplete"
            @error="handleError"
          />

        <!-- 专注模式提示条 -->
        <div v-if="focusMode" class="focus-mode-bar">
          <span>专注模式中</span>
          <el-button type="text" size="small" @click="exitFocusMode">按 Esc 退出</el-button>
        </div>
        </div>

        <!-- 底部操作栏 -->
        <div v-if="!focusMode" class="practice-actions">
          <el-button
            :type="isPaused ? 'primary' : 'warning'"
            :icon="isPaused ? 'el-icon-video-play' : 'el-icon-video-pause'"
            @click="togglePause"
          >
            {{ isPaused ? '继续' : '暂停' }}
          </el-button>
          <el-button icon="el-icon-refresh-left" @click="handleRestart">重新开始</el-button>
          <el-button type="success" icon="el-icon-circle-check" @click="handleFinish">
            完成练习
          </el-button>
          <div class="flex-1" />
          <div class="progress-group">
            <!-- 整本模式：单进度条 -->
            <template v-if="!hasChapter">
              <span class="progress-text">已输入 {{ typedLength }} / {{ totalLength }} 字</span>
              <el-progress :percentage="progressPercent" :stroke-width="8" class="progress-bar" />
            </template>
            <!-- 章节模式：双进度条 -->
            <template v-else>
              <div class="progress-row">
                <span class="progress-label">本章节</span>
                <span class="progress-text">{{ typedLength }} / {{ chapterLength }} 字</span>
                <el-progress :percentage="chapterProgressPercent" :stroke-width="6" class="progress-bar-sm" />
              </div>
              <div class="progress-row">
                <span class="progress-label">总计</span>
                <span class="progress-text">{{ typedLength }} / {{ bookTotalLength }} 字</span>
                <el-progress :percentage="bookProgressPercent" :stroke-width="6" class="progress-bar-sm" color="#8b9cbe" />
              </div>
            </template>
          </div>
        </div>

        <!-- 完成评分弹窗 -->
        <el-dialog
          :visible.sync="scoreDialogVisible"
          :show-close="false"
          :close-on-click-modal="false"
          width="480px"
          center
        >
          <score-card
            v-if="scoreDialogVisible"
            :accuracy="finalAccuracy"
            :speed="finalSpeed"
            :duration="duration"
            :score="totalScore"
            @retry="handleRetry"
            @confirm="handleScoreConfirm"
          />
        </el-dialog>

        <!-- 收藏弹窗 -->
        <el-dialog title="收藏好词好句" :visible.sync="collectDialogVisible" width="500px">
          <el-form :model="collectForm" label-width="80px">
            <el-form-item label="内容">
              <el-input v-model="collectForm.content" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item label="类型">
              <el-radio-group v-model="collectForm.type">
                <el-radio label="word">好词</el-radio>
                <el-radio label="sentence">好句</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="标签">
              <el-input v-model="collectForm.tagsStr" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-form>
          <div slot="footer">
            <el-button @click="collectDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="collecting" @click="handleCollect">收藏</el-button>
          </div>
        </el-dialog>
      </div>
    </default-layout>
  </div>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import TextComparator from '@/components/practice/TextComparator.vue'
import RealtimeStats from '@/components/practice/RealtimeStats.vue'
import ScoreCard from '@/components/practice/ScoreCard.vue'
import { startPractice, getActivePractice, updateProgress, completePractice, pausePractice, resumePractice } from '@/api/practice'
import { getBookById, getChapters } from '@/api/book'
import { createCollection } from '@/api/collection'

export default {
  name: 'Practice',
  components: { DefaultLayout, TextComparator, RealtimeStats, ScoreCard },
  data() {
    return {
      loading: true,
      sessionId: null,
      bookInfo: null,
      originalText: '',
      typedText: '',
      duration: 0,
      errorCount: 0,
      isPaused: false,
      timer: null,
      startTime: null,
      pausedDuration: 0,
      lastPauseTime: null,
      scoreDialogVisible: false,
      collectDialogVisible: false,
      collecting: false,
      collectForm: {
        content: '',
        type: 'sentence',
        tagsStr: ''
      },
      finalAccuracy: 0,
      finalSpeed: 0,
      totalScore: 0,
      progressSaveTimer: null,
      originalChars: [],
      focusMode: false,
      // 章节模式
      chapterIndex: null,
      chapterTitle: '',
      chapterLength: 0,
      bookTotalLength: 0
    }
  },
  computed: {
    typedLength() {
      return this.typedText.length
    },
    totalLength() {
      return this.originalText.length
    },
    progressPercent() {
      if (this.totalLength === 0) return 0
      return Math.min(Math.round((this.typedLength / this.totalLength) * 100), 100)
    },
    accuracy() {
      if (this.typedText.length === 0) return 100
      let correct = 0
      const len = this.typedText.length
      for (let i = 0; i < len; i++) {
        if (this.typedText[i] === this.originalChars[i]) {
          correct++
        }
      }
      return Math.round((correct / len) * 100)
    },
    currentSpeed() {
      const minutes = this.duration / 60
      if (minutes === 0) return 0
      return Math.round(this.typedLength / minutes)
    },
    chapterProgressPercent() {
      if (this.chapterLength === 0) return this.progressPercent
      return Math.min(Math.round((this.typedLength / this.chapterLength) * 100), 100)
    },
    bookProgressPercent() {
      if (this.bookTotalLength === 0) return this.progressPercent
      return Math.min(Math.round((this.typedLength / this.bookTotalLength) * 100), 100)
    },
    hasChapter() {
      return this.chapterIndex !== null && this.chapterTitle !== ''
    }
  },
  async created() {
    this.sessionId = this.$route.params.sessionId
    this.handleKeydown = this.handleGlobalKeydown.bind(this)
    document.addEventListener('keydown', this.handleKeydown)
    await this.initPractice()
  },
  beforeDestroy() {
    this.stopTimer()
    this.stopProgressSave()
    document.removeEventListener('keydown', this.handleKeydown)
  },
  methods: {
    async initPractice() {
      this.loading = true
      try {
        // 获取练习会话信息
        const session = await getActivePractice()
        let bookId

        if (session && session.id == this.sessionId) {
          // 恢复进行中的会话
          bookId = session.bookId
          this.typedText = session.progress || session.typedText || ''
          this.duration = session.duration || 0
          this.pausedDuration = this.duration
          // 恢复章节模式 — 优先从 session 获取，fallback 到 URL query
          this.chapterIndex = (session.chapterIndex !== undefined && session.chapterIndex !== null)
            ? session.chapterIndex
            : (this.$route.query.chapterIndex !== undefined
                ? Number(this.$route.query.chapterIndex)
                : null)
          this.chapterTitle = session.chapterTitle || ''
        } else {
          // 新会话
          bookId = this.$route.query.bookId
          this.chapterIndex = this.$route.query.chapterIndex !== undefined
            ? Number(this.$route.query.chapterIndex)
            : null
        }

        // 获取素材内容
        if (bookId) {
          this.bookInfo = await getBookById(bookId)
          const fullContent = this.bookInfo.content || ''
          this.bookTotalLength = fullContent.length

          if (this.chapterIndex !== null && this.chapterIndex !== undefined) {
            // 章节模式：解析章节，按 chapter.index 精确匹配
            try {
              const chapters = await getChapters(bookId)
              if (chapters && chapters.length > 0) {
                // 用 == 避免类型差异（后端返回 int，route.query 是 string 转 number）
                const ch = chapters.find(c => c.index == this.chapterIndex)
                if (ch) {
                  this.originalText = ch.content || ''
                  this.chapterTitle = ch.title || ''
                  this.chapterLength = this.originalText.length
                } else {
                  console.warn('[Practice] 未找到匹配章节 index=' + this.chapterIndex + '，可用索引: ' + chapters.map(c => c.index).join(','))
                  this.originalText = fullContent
                }
              } else {
                console.warn('[Practice] getChapters 返回空数组')
                this.originalText = fullContent
              }
            } catch (e) {
              console.error('[Practice] 获取章节失败:', e)
              this.originalText = fullContent
            }
          } else {
            // 整本模式
            this.originalText = fullContent
          }

          // 恢复进度截断：避免 typedText 超过 originalText 导致立即触发 complete
          if (this.typedText.length > this.originalText.length) {
            this.typedText = this.typedText.substring(0, this.originalText.length)
            console.warn('[Practice] 恢复进度超过素材长度，已截断到', this.originalText.length)
          }

          this.originalChars = Array.from(this.originalText)
        }

        this.$nextTick(() => {
          if (this.$refs.comparator) {
            this.$refs.comparator.setTypedText(this.typedText)
            this.$refs.comparator.focus()
          }
        })

        this.startTimer()
        this.startProgressSave()
      } catch (e) {
        this.$message.error('加载练习失败')
      } finally {
        this.loading = false
      }
    },
    startTimer() {
      this.startTime = Date.now()
      this.timer = setInterval(() => {
        if (!this.isPaused) {
          this.duration++
        }
      }, 1000)
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    startProgressSave() {
      this.progressSaveTimer = setInterval(() => {
        if (!this.isPaused && this.typedText.length > 0) {
          this.saveProgress()
        }
      }, 10000)
    },
    stopProgressSave() {
      if (this.progressSaveTimer) {
        clearInterval(this.progressSaveTimer)
        this.progressSaveTimer = null
      }
    },
    async saveProgress() {
      try {
        await updateProgress(this.sessionId, {
          progress: this.typedText,
          duration: this.duration,
          accuracy: this.accuracy
        })
      } catch (e) {
        // 静默失败
      }
    },
    handleInput(value) {
      this.typedText = value
    },
    handleError() {
      this.errorCount++
    },
    handleComplete(data) {
      this.finishPractice(data.accuracy)
    },
    async togglePause() {
      this.isPaused = !this.isPaused
      try {
        if (this.isPaused) {
          await pausePractice(this.sessionId)
        } else {
          await resumePractice(this.sessionId)
          this.$refs.comparator && this.$refs.comparator.focus()
        }
      } catch (e) {
        // 静默处理
      }
    },
    handleRestart() {
      this.$confirm('确定要重新开始吗？当前进度将被清空。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.typedText = ''
        this.duration = 0
        this.errorCount = 0
        this.isPaused = false
        this.$refs.comparator && this.$refs.comparator.reset()
        this.$refs.comparator && this.$refs.comparator.focus()
        this.saveProgress()
      }).catch(() => {})
    },
    handleFinish() {
      this.finishPractice(this.accuracy)
    },
    async finishPractice(accuracy) {
      this.stopTimer()
      this.stopProgressSave()
      this.isPaused = true

      this.finalAccuracy = accuracy || this.accuracy
      this.finalSpeed = this.currentSpeed
      this.totalScore = this.calculateScore()

      try {
        await completePractice(this.sessionId, {
          progress: this.typedText,
          typedChars: this.typedText.length,
          errorCount: this.errorCount,
          duration: this.duration,
          accuracy: this.finalAccuracy,
          speed: this.finalSpeed,
          score: this.totalScore
        })
      } catch (e) {
        // 静默处理
      }

      this.scoreDialogVisible = true
    },
    calculateScore() {
      // 综合评分：正确率50% + 速度30% + 完成度20%
      const speedScore = Math.min(this.finalSpeed / 100 * 100, 100)
      const completeness = this.typedLength / this.totalLength * 100
      return Math.round(this.finalAccuracy * 0.5 + speedScore * 0.3 + completeness * 0.2)
    },
    handleRetry() {
      this.scoreDialogVisible = false
      this.typedText = ''
      this.duration = 0
      this.errorCount = 0
      this.isPaused = false
      this.$refs.comparator && this.$refs.comparator.reset()
      this.startTimer()
      this.startProgressSave()
      this.$refs.comparator && this.$refs.comparator.focus()
    },
    handleScoreConfirm() {
      this.scoreDialogVisible = false
      this.$router.push('/dashboard')
    },
    handleTextSelect() {
      const selection = window.getSelection()
      const text = selection.toString().trim()
      if (text) {
        this.collectForm.content = text
        this.collectForm.tagsStr = ''
        this.collectDialogVisible = true
      }
    },
    async handleCollect() {
      this.collecting = true
      try {
        const tags = this.collectForm.tagsStr
          ? this.collectForm.tagsStr.split(',').map(t => t.trim()).filter(Boolean)
          : []
        await createCollection({
          content: this.collectForm.content,
          type: this.collectForm.type,
          tags,
          bookId: this.bookInfo ? this.bookInfo.id : null,
          bookTitle: this.bookInfo ? this.bookInfo.title : null
        })
        this.$message.success('收藏成功')
        this.collectDialogVisible = false
        window.getSelection().removeAllRanges()
      } catch (e) {
        // 错误已处理
      } finally {
        this.collecting = false
      }
    },
    enterFocusMode() {
      this.focusMode = true
      this.$nextTick(() => {
        this.$refs.comparator && this.$refs.comparator.focus()
      })
    },
    exitFocusMode() {
      this.focusMode = false
    },
    handleGlobalKeydown(e) {
      if (e.key === 'Escape' && this.focusMode) {
        e.preventDefault()
        this.exitFocusMode()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.practice-page {
  min-height: 100vh;
  background-color: var(--color-bg);
}

.practice-container {
  max-width: $content-max-width;
  margin: 0 auto;
  padding: #{$spacing-lg};
  display: flex;
  flex-direction: column;
  gap: #{$spacing-md};
  height: calc(100vh - #{$header-height} - 60px);

  .practice-topbar {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};

    .practice-title {
      font-size: $font-size-lg;
      font-weight: 600;
      color: var(--color-text);

      .chapter-sep {
        margin: 0 4px;
        color: var(--color-text-placeholder);
        font-weight: 400;
      }

      .chapter-sub-title {
        font-size: $font-size-base;
        font-weight: 500;
        color: var(--color-text-secondary);
      }
    }
  }

  .practice-main {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  .flex-1 {
    flex: 1;
  }

  .focus-mode-bar {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: #{$spacing-sm};
    padding: 4px #{$spacing-md};
    font-size: $font-size-xs;
    color: var(--color-text-placeholder);
    opacity: 0.6;
    transition: opacity 0.3s;

    &:hover {
      opacity: 1;
    }
  }

  .practice-actions {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    padding: #{$spacing-md};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    box-shadow: var(--shadow-sm);

    .progress-text {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      white-space: nowrap;
    }

    .progress-bar {
      width: 200px;
    }

    .progress-group {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 260px;
    }

    .progress-row {
      display: flex;
      align-items: center;
      gap: 6px;

      .progress-label {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
        width: 36px;
        text-align: right;
      }

      .progress-text {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
        white-space: nowrap;
        min-width: 90px;
      }

      .progress-bar-sm {
        flex: 1;
        min-width: 80px;
      }
    }
  }

  // 专注模式覆盖：固定定位覆盖整个视口，突破布局约束
  &.focus-mode {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 1050;
    max-width: 100%;
    padding: 0;
    height: 100vh;
    gap: 0;
    background-color: var(--color-bg);
  }
}
</style>
