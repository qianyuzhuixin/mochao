<template>
  <default-layout>
    <div class="library-page page-container">
      <div class="library-header">
        <h1 class="page-title">书库</h1>
      </div>

      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane label="内置书库" name="builtin" />
        <el-tab-pane label="我的素材" name="mine" />
      </el-tabs>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="filters.category" placeholder="全部分类" clearable @change="fetchData">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filters.difficulty" placeholder="全部难度" clearable @change="fetchData">
          <el-option label="简单" value="easy" />
          <el-option label="中等" value="medium" />
          <el-option label="困难" value="hard" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索标题、书名或作者"
          clearable
          style="width: 280px"
          @keyup.enter.native="fetchData"
          @clear="fetchData"
        >
          <el-button slot="append" icon="el-icon-search" @click="fetchData" />
        </el-input>
        <div class="flex-1" />
        <el-button v-if="activeTab === 'mine'" type="primary" icon="el-icon-plus" @click="openCreateDialog">
          新建素材
        </el-button>
      </div>

      <!-- 卡片网格 -->
      <div v-loading="loading" class="book-grid">
        <div
          v-for="book in bookList"
          :key="book.id"
          class="book-card"
          @click="$router.push(`/library/${book.id}`)"
        >
          <el-button
            v-if="activeTab === 'mine'"
            class="card-delete-btn"
            type="text"
            icon="el-icon-delete"
            size="mini"
            @click.stop="handleDelete(book)"
          />
          <div class="book-card-header">
            <h3 class="book-title">{{ book.title }}</h3>
            <el-tag :type="difficultyType(book.difficulty)" size="mini">
              {{ difficultyText(book.difficulty) }}
            </el-tag>
          </div>
          <p class="book-info">
            <span class="info-label">书名：</span>{{ book.bookName || '未知' }}
          </p>
          <p class="book-info">
            <span class="info-label">作者：</span>{{ book.author || '未知' }}
          </p>
          <div class="book-card-footer">
            <el-tag v-if="book.category" size="mini" effect="plain">{{ book.category }}</el-tag>
            <span class="word-count">{{ book.wordCount || 0 }} 字</span>
          </div>
        </div>
      </div>

      <empty-state v-if="!loading && bookList.length === 0" text="暂无素材" description="换个筛选条件试试" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          @current-change="fetchData"
        />
      </div>

      <!-- 新建素材弹窗 -->
      <el-dialog title="新建素材" :visible.sync="showCreateDialog" width="500px">
        <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="80px">
          <el-form-item label="标题" prop="title">
            <el-input v-model="createForm.title" placeholder="请输入素材标题" />
          </el-form-item>
          <el-form-item label="书名" prop="bookName">
            <el-input v-model="createForm.bookName" placeholder="请输入书名" />
          </el-form-item>
          <el-form-item label="作者" prop="author">
            <el-input v-model="createForm.author" placeholder="请输入作者" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-input v-model="createForm.category" placeholder="如：玄幻、都市、科幻" />
          </el-form-item>
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="createForm.difficulty" placeholder="请选择难度" style="width: 100%">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>
          <el-form-item label="正文" prop="content">
            <div class="content-import">
              <input
                ref="fileInput"
                type="file"
                accept=".txt"
                style="display:none"
                @change="handleFileImport"
              />
              <el-button size="small" type="info" icon="el-icon-upload2" @click="$refs.fileInput.click()">
                导入文件
              </el-button>
              <span class="import-hint" v-if="!createForm.content">
                支持上传 TXT 文件，自动按章节拆分，可勾选要导入的章节
              </span>
            </div>
            <el-input v-model="createForm.content" type="textarea" :rows="6" placeholder="请输入正文内容" />
          </el-form-item>
        </el-form>
        <div slot="footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreate">创建</el-button>
        </div>
      </el-dialog>

      <!-- 章节选择弹窗 -->
      <el-dialog
        title="选择要导入的章节"
        :visible.sync="chapterDialogVisible"
        width="680px"
        :close-on-click-modal="false"
      >
        <div v-loading="parsing" class="chapter-dialog-body">
          <div v-if="!parsing && chapterResult" class="chapter-summary">
            <span><strong>{{ chapterResult.fileName }}</strong></span>
            <span class="summary-divider">|</span>
            <span>共 {{ chapterResult.totalWords || 0 }} 字</span>
            <span class="summary-divider">|</span>
            <span>{{ chapterResult.chapters.length }} 个章节</span>
            <el-button type="text" size="small" style="float:right" @click="toggleSelectAll">
              {{ allSelected ? '取消全选' : '全选' }}
            </el-button>
          </div>

          <div v-if="!parsing && chapterResult" class="chapter-list">
            <el-checkbox-group v-model="selectedChapters">
              <div
                v-for="ch in chapterResult.chapters"
                :key="ch.index"
                class="chapter-item"
                :class="{ selected: selectedChapters.includes(ch.index) }"
              >
                <el-checkbox :label="ch.index" class="chapter-checkbox">
                  <span class="chapter-title">{{ ch.title }}</span>
                  <span class="chapter-words">（{{ ch.wordCount }} 字）</span>
                </el-checkbox>
                <div class="chapter-preview">{{ ch.preview }}</div>
              </div>
            </el-checkbox-group>
          </div>

          <div v-if="!parsing && !chapterResult" class="parse-empty">
            暂无解析结果，请重新选择文件
          </div>
        </div>

        <div slot="footer">
          <el-button @click="chapterDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmChapters" :disabled="selectedChapters.length === 0">
            确认导入（已选 {{ selectedChapters.length }} 章）
          </el-button>
        </div>
      </el-dialog>
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getBooks, getMyBooks, getCategories, createBook, deleteBook, parseFile } from '@/api/book'

export default {
  name: 'Library',
  components: { DefaultLayout, EmptyState },
  data() {
    return {
      activeTab: 'builtin',
      bookList: [],
      categories: [],
      filters: {
        category: '',
        difficulty: '',
        keyword: ''
      },
      currentPage: 1,
      pageSize: 12,
      total: 0,
      loading: false,
      showCreateDialog: false,
      submitting: false,
      createForm: {
        title: '',
        bookName: '',
        author: '',
        category: '',
        difficulty: 'medium',
        content: '',
        chapters: null  // 文件导入时前端已解析的章节列表
      },
      createRules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }]
      },
      // 文件导入相关
      chapterDialogVisible: false,
      parsing: false,
      chapterResult: null,
      selectedChapters: []
    }
  },
  computed: {
    allSelected() {
      if (!this.chapterResult || !this.chapterResult.chapters) return false
      return this.selectedChapters.length === this.chapterResult.chapters.length
    }
  },
  created() {
    this.fetchCategories()
    this.fetchData()
  },
  methods: {
    async fetchCategories() {
      try {
        this.categories = await getCategories() || []
      } catch (e) {
        this.categories = []
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          pageSize: this.pageSize,
          ...this.filters
        }
        const res = this.activeTab === 'builtin'
          ? await getBooks(params)
          : await getMyBooks(params)
        this.bookList = res.list || res.records || res || []
        this.total = res.total || this.bookList.length
      } catch (e) {
        this.bookList = []
      } finally {
        this.loading = false
      }
    },
    handleTabChange() {
      this.currentPage = 1
      this.fetchData()
    },
    difficultyText(d) {
      const map = { 1: '简单', 2: '中等', 3: '困难' }
      return map[d] || '中等'
    },
    difficultyType(d) {
      const map = { 1: 'success', 2: 'warning', 3: 'danger' }
      return map[d] || 'info'
    },
    // === 文件导入 ===
    async handleFileImport(e) {
      const file = e.target.files[0]
      if (!file) return
      // 重置 input，允许重复选择同一文件
      e.target.value = ''

      if (!file.name.toLowerCase().endsWith('.txt')) {
        this.$message.warning('仅支持 TXT 文件')
        return
      }
      if (file.size > 10 * 1024 * 1024) {
        this.$message.warning('文件大小不能超过 10MB')
        return
      }

      this.parsing = true
      this.chapterResult = null
      this.selectedChapters = []
      this.chapterDialogVisible = true

      try {
        this.chapterResult = await parseFile(file)
        // 默认全选
        this.selectedChapters = this.chapterResult.chapters.map(c => c.index)
      } catch (e) {
        this.chapterDialogVisible = false
        this.$message.error(e.message || '文件解析失败')
      } finally {
        this.parsing = false
      }
    },
    toggleSelectAll() {
      if (!this.chapterResult) return
      if (this.allSelected) {
        this.selectedChapters = []
      } else {
        this.selectedChapters = this.chapterResult.chapters.map(c => c.index)
      }
    },
    handleConfirmChapters() {
      if (this.selectedChapters.length === 0) return
      const chapters = this.chapterResult.chapters.filter(
        c => this.selectedChapters.includes(c.index)
      )
      // 拼接选中章节的内容（包含标题）
      const content = chapters.map(c => c.title + '\n\n' + c.content).join('\n\n')
      this.createForm.content = content
      // 传递已解析的章节列表，避免后端重复拆分（重复拆分会因分隔线丢失导致合为单章）
      this.createForm.chapters = chapters

      // 自动填充表单（如果为空）
      if (!this.createForm.title && this.chapterResult.fileName) {
        this.createForm.title = this.chapterResult.fileName.replace(/\.txt$/i, '')
      }
      // 重置 content 校验状态
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.validateField('content')
        }
      })

      this.chapterDialogVisible = false
      this.$message.success(`已导入 ${chapters.length} 个章节`)
    },
    openCreateDialog() {
      this.createForm.chapters = null
      this.showCreateDialog = true
    },
    handleCreate() {
      this.$refs.createForm.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          await createBook(this.createForm)
          this.$message.success('创建成功')
          this.showCreateDialog = false
          this.$refs.createForm.resetFields()
          this.fetchData()
        } catch (e) {
          // 错误已处理
        } finally {
          this.submitting = false
        }
      })
    },
    handleDelete(book) {
      this.$confirm(`确定要删除素材「${book.title}」吗？删除后不可恢复。`, '删除确认', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteBook(book.id)
          this.$message.success('已删除')
          this.fetchData()
        } catch (e) {
          // 错误已处理
        }
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.library-page {
  .filter-bar {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    margin-bottom: #{$spacing-lg};
    flex-wrap: wrap;
  }

  .book-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: #{$spacing-md};
    min-height: 200px;
  }

  .book-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-md};
    cursor: pointer;
    transition: all $transition-base;
    position: relative;

    &:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-2px);
      border-color: var(--color-primary);

      .card-delete-btn {
        opacity: 1;
      }
    }

    .card-delete-btn {
      position: absolute;
      top: 8px;
      right: 8px;
      padding: 4px;
      color: var(--color-text-placeholder);
      opacity: 0;
      transition: opacity 0.2s, color 0.2s;
      z-index: 1;

      &:hover {
        color: var(--color-error, #f56c6c);
      }
    }

    .book-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: #{$spacing-sm};

      .book-title {
        font-size: $font-size-md;
        font-weight: 600;
        color: var(--color-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
        margin-right: #{$spacing-sm};
      }
    }

    .book-info {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      margin-bottom: #{$spacing-xs};

      .info-label {
        color: var(--color-text-placeholder);
      }
    }

    .book-card-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: #{$spacing-sm};

      .word-count {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
      }
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-xl};
  }
}

// 导入文件样式
.content-import {
  margin-bottom: #{$spacing-sm};
  display: flex;
  align-items: center;
  gap: #{$spacing-sm};

  .import-hint {
    font-size: $font-size-xs;
    color: var(--color-text-placeholder);
  }
}

// 章节选择弹窗
.chapter-dialog-body {
  min-height: 200px;
}

.chapter-summary {
  padding: #{$spacing-sm} 0 #{$spacing-md};
  border-bottom: 1px solid var(--color-border);
  margin-bottom: #{$spacing-md};
  font-size: $font-size-sm;
  color: var(--color-text-secondary);

  .summary-divider {
    margin: 0 #{$spacing-sm};
    color: var(--color-border);
  }
}

.chapter-list {
  max-height: 400px;
  overflow-y: auto;
}

.chapter-item {
  border: 1px solid var(--color-border);
  border-radius: $border-radius-sm;
  padding: #{$spacing-sm} #{$spacing-md};
  margin-bottom: #{$spacing-sm};
  transition: border-color .2s;
  cursor: pointer;

  &:hover {
    border-color: var(--color-primary-light);
  }
  &.selected {
    border-color: var(--color-primary);
    background: rgba(64, 158, 255, 0.03);
  }

  .chapter-checkbox {
    display: flex;
    align-items: center;
    width: 100%;

    .chapter-title {
      font-weight: 500;
      color: var(--color-text);
      margin-right: #{$spacing-xs};
    }
    .chapter-words {
      font-size: $font-size-xs;
      color: var(--color-text-placeholder);
    }
  }

  .chapter-preview {
    font-size: $font-size-xs;
    color: var(--color-text-placeholder);
    margin-top: 4px;
    padding-left: 24px;
    line-height: 1.5;
  }
}

.parse-empty {
  text-align: center;
  padding: #{$spacing-xxl};
  color: var(--color-text-placeholder);
}
</style>
