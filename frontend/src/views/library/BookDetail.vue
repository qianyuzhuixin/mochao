<template>
  <default-layout>
    <div class="book-detail-page page-container" v-loading="loading">
      <div class="back-btn">
        <el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">
          返回书库
        </el-button>
      </div>

      <div v-if="book" class="book-detail-content">
        <!-- 信息卡片 -->
        <div class="info-card">
          <div class="info-header">
            <h1 class="book-title">{{ book.title }}</h1>
            <el-button type="primary" icon="el-icon-edit" @click="handleStartPractice">
              开始练习
            </el-button>
          </div>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">书名：</span>
              <span>{{ book.bookName || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="label">作者：</span>
              <span>{{ book.author || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="label">分类：</span>
              <el-tag v-if="book.category" size="mini">{{ book.category }}</el-tag>
              <span v-else>未分类</span>
            </div>
            <div class="info-item">
              <span class="label">难度：</span>
              <el-tag :type="difficultyType(book.difficulty)" size="mini">
                {{ difficultyText(book.difficulty) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">字数：</span>
              <span>{{ book.wordCount || 0 }} 字</span>
            </div>
          </div>
          <div v-if="book.tags && book.tags.length" class="info-tags">
            <el-tag v-for="tag in book.tags" :key="tag" size="mini" effect="plain" class="mr-sm">
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <!-- 正文内容 -->
        <div class="content-card">
          <div class="content-header">
            <h2 class="content-title">正文预览</h2>
            <span class="content-hint">选中文本可收藏好词好句</span>
          </div>
          <div
            ref="contentRef"
            class="content-body"
            @mouseup="handleTextSelect"
          >
            {{ book.content }}
          </div>
        </div>
      </div>

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

      <!-- 章节选择弹窗 -->
      <el-dialog title="选择练习章节" :visible.sync="chapterDialogVisible" width="600px">
        <div class="chapter-select-hint">请选择练习方式或指定章节</div>
        <div class="chapter-select-list">
          <div
            v-for="(ch, idx) in chapters"
            :key="idx"
            class="chapter-select-item"
            :class="{ active: selectedChapterIndex === idx }"
            @click="selectedChapterIndex = idx"
          >
            <div class="chapter-select-title">
              <span v-if="ch.index === -1" class="chapter-index full-book">整本</span>
              <span v-else-if="ch.index === 0" class="chapter-index">前言</span>
              <span v-else class="chapter-index">第{{ ch.index }}章</span>
              <span class="chapter-name">{{ ch.title }}</span>
              <span class="chapter-words">{{ ch.wordCount }} 字</span>
            </div>
            <div class="chapter-select-preview">{{ ch.preview }}</div>
          </div>
        </div>
        <div slot="footer">
          <el-button @click="chapterDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="selectedChapterIndex === null"
            @click="handleChapterConfirm"
          >
            开始练习本章
          </el-button>
        </div>
      </el-dialog>
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getBookById, getChapters } from '@/api/book'
import { startPractice } from '@/api/practice'
import { createCollection } from '@/api/collection'
import { mapGetters } from 'vuex'

export default {
  name: 'BookDetail',
  components: { DefaultLayout },
  data() {
    return {
      book: null,
      loading: false,
      collectDialogVisible: false,
      collecting: false,
      collectForm: {
        content: '',
        type: 'sentence',
        tagsStr: ''
      },
      chapterDialogVisible: false,
      chapters: [],
      selectedChapterIndex: null
    }
  },
  computed: {
    ...mapGetters('auth', ['isLoggedIn'])
  },
  created() {
    this.fetchBook()
  },
  methods: {
    async fetchBook() {
      this.loading = true
      try {
        this.book = await getBookById(this.$route.params.id)
      } catch (e) {
        // 错误已处理
      } finally {
        this.loading = false
      }
    },
    async handleStartPractice() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push(`/login?redirect=${encodeURIComponent(this.$route.fullPath)}`)
        return
      }
      try {
        // 获取章节列表，有章节就弹窗让用户选择
        const chapters = await getChapters(this.book.id)
        if (chapters && chapters.length > 0) {
          // 在列表前面插入"整本练习"选项
          this.chapters = [
            { index: -1, title: '整本练习（不分章节）', wordCount: this.book.wordCount || 0, preview: '从头到尾完整练习' },
            ...chapters
          ]
          this.selectedChapterIndex = 0
          this.chapterDialogVisible = true
          return
        }
        // 无章节，直接开始
        this.doStartPractice()
      } catch (e) {
        // 解析失败或无章节，直接开始整本练习
        this.doStartPractice()
      }
    },
    async doStartPractice(chapterIndex) {
      try {
        const params = { bookId: this.book.id }
        if (chapterIndex !== undefined && chapterIndex !== null) {
          params.chapterIndex = chapterIndex
        }
        const res = await startPractice(params)
        const query = (chapterIndex !== undefined && chapterIndex !== null)
          ? `?bookId=${this.book.id}&chapterIndex=${chapterIndex}`
          : `?bookId=${this.book.id}`
        this.$router.push(`/practice/${res.id || res.sessionId}${query}`)
      } catch (e) {
        // 错误已处理
      }
    },
    async handleChapterConfirm() {
      if (this.selectedChapterIndex === null) return
      const ch = this.chapters[this.selectedChapterIndex]
      // -1 表示整本练习，否则传递章节的实际 index 字段
      const chapterIndex = (ch && ch.index === -1) ? null : (ch ? ch.index : null)
      this.chapterDialogVisible = false
      await this.doStartPractice(chapterIndex)
    },
    handleTextSelect() {
      const selection = window.getSelection()
      const text = selection.toString().trim()
      if (text && text.length > 0) {
        this.collectForm.content = text
        this.collectForm.tagsStr = ''
        this.collectDialogVisible = true
      }
    },
    async handleCollect() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录')
        this.$router.push('/login')
        return
      }
      this.collecting = true
      try {
        const tags = this.collectForm.tagsStr
          ? this.collectForm.tagsStr.split(',').map(t => t.trim()).filter(Boolean)
          : []
        await createCollection({
          content: this.collectForm.content,
          type: this.collectForm.type,
          tags,
          bookId: this.book.id,
          bookTitle: this.book.title
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
    difficultyText(d) {
      const map = { 1: '简单', 2: '中等', 3: '困难' }
      return map[d] || '中等'
    },
    difficultyType(d) {
      const map = { 1: 'success', 2: 'warning', 3: 'danger' }
      return map[d] || 'info'
    }
  }
}
</script>

<style lang="scss" scoped>
.book-detail-page {
  .back-btn {
    margin-bottom: #{$spacing-md};
  }

  .info-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-xl};
    margin-bottom: #{$spacing-lg};

    .info-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: #{$spacing-lg};

      .book-title {
        font-size: $font-size-xxl;
        font-weight: 700;
        color: var(--color-text);
      }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: #{$spacing-md};

      .info-item {
        font-size: $font-size-base;
        color: var(--color-text);

        .label {
          color: var(--color-text-secondary);
        }
      }
    }

    .info-tags {
      margin-top: #{$spacing-md};
    }
  }

  .content-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-xl};

    .content-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: #{$spacing-md};

      .content-title {
        font-size: $font-size-lg;
        font-weight: 600;
        color: var(--color-text);
      }

      .content-hint {
        font-size: $font-size-sm;
        color: var(--color-text-placeholder);
      }
    }

    .content-body {
      font-size: $font-size-md;
      line-height: 2;
      color: var(--color-text);
      white-space: pre-wrap;
      user-select: text;
    }
  }

  .chapter-select-hint {
    font-size: $font-size-sm;
    color: var(--color-text-secondary);
    margin-bottom: #{$spacing-md};
  }

  .chapter-select-list {
    max-height: 400px;
    overflow-y: auto;
  }

  .chapter-select-item {
    padding: #{$spacing-sm} #{$spacing-md};
    border: 1px solid var(--color-border);
    border-radius: $border-radius-base;
    margin-bottom: #{$spacing-sm};
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: var(--color-primary);
      background-color: rgba(74, 108, 247, 0.04);
    }

    &.active {
      border-color: var(--color-primary);
      background-color: rgba(74, 108, 247, 0.08);
    }

    .chapter-select-title {
      display: flex;
      align-items: center;
      gap: #{$spacing-sm};
      margin-bottom: 4px;

      .chapter-index {
        font-size: $font-size-xs;
        color: var(--color-primary);
        font-weight: 600;
        white-space: nowrap;

        &.full-book {
          color: var(--color-success, #67c23a);
          background: rgba(103, 194, 58, 0.1);
          padding: 1px 6px;
          border-radius: 3px;
        }
      }

      .chapter-name {
        flex: 1;
        font-size: $font-size-base;
        font-weight: 500;
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

    .chapter-select-preview {
      font-size: $font-size-xs;
      color: var(--color-text-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      padding-left: 48px;
    }
  }
}
</style>
