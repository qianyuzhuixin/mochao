<template>
  <div class="chapter-outline-list-page page-container page-container-narrow" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">章纲管理</h1>
      </div>
      <div>
        <el-button type="primary" size="small" icon="el-icon-magic-stick" :loading="aiLoading" @click="handleGenerate">
          AI生成
        </el-button>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">
          新建章纲
        </el-button>
      </div>
    </div>

    <div class="drag-hint">
      <i class="el-icon-rank" />
      <span>拖拽可调整章节顺序</span>
    </div>

    <draggable
      v-model="list"
      v-loading="loading"
      class="outline-list"
      handle=".drag-handle"
      @end="handleDragEnd"
    >
      <div v-for="(item, index) in list" :key="item.id" class="outline-item">
        <i class="el-icon-rank drag-handle" />
        <span class="outline-index">第{{ index + 1 }}章</span>
        <span class="outline-title">{{ item.title || '未命名' }}</span>
        <span class="outline-summary">{{ item.summary || item.content || '暂无摘要' }}</span>
        <div class="outline-actions">
          <el-button type="text" icon="el-icon-edit" @click="handleEdit(item)">编辑</el-button>
          <el-button type="text" icon="el-icon-delete" @click="handleDelete(item)">删除</el-button>
        </div>
      </div>
    </draggable>

    <EmptyState v-if="!loading && list.length === 0" text="暂无章纲" description="创建你的第一个章纲" />

    <el-dialog :title="editing ? '编辑章纲' : '新建章纲'" :visible.sync="dialogVisible" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="章节标题" />
        </el-form-item>
        <el-form-item label="摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="5" placeholder="章节摘要/大纲" />
        </el-form-item>
        <el-form-item label="关键点">
          <el-input v-model="form.keyPoints" type="textarea" :rows="3" placeholder="章节关键情节" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

    <!-- AI 生成提示词弹窗 -->
    <el-dialog title="AI 生成章纲" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="生成要求">
          <el-input
            v-model="aiPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你对章纲的生成要求，例如：请根据大纲生成30章的详细章纲..."
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="confirmGenerate">生成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import EmptyState from '@/components/common/EmptyState.vue'
import {
  getChapterOutlines, createChapterOutline, updateChapterOutline,
  deleteChapterOutline, reorderChapterOutlines
} from '@/api/novel'
import { generateChapterOutline } from '@/api/ai'

export default {
  name: 'ChapterOutlineList',
  components: { draggable, EmptyState },
  data() {
    return {
      list: [],
      loading: false,
      dialogVisible: false,
      editing: null,
      submitting: false,
      aiLoading: false,
      aiDialogVisible: false,
      aiPrompt: '',
      form: { title: '', summary: '', keyPoints: '' },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
      }
    }
  },
  computed: {
    novelId() { return this.$route.params.id }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        this.list = await getChapterOutlines(this.novelId) || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.editing = null
      this.form = { title: '', summary: '', keyPoints: '' }
      this.dialogVisible = true
    },
    handleEdit(item) {
      this.editing = item
      this.form = {
        title: item.title || '',
        summary: item.summary || '',
        keyPoints: item.keyPoints || ''
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          if (this.editing) {
            await updateChapterOutline(this.novelId, this.editing.id, this.form)
            this.$message.success('更新成功')
          } else {
            await createChapterOutline(this.novelId, this.form)
            this.$message.success('创建成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } catch (e) { console.error(e) } finally {
          this.submitting = false
        }
      })
    },
    handleDelete(item) {
      this.$confirm(`确定要删除"${item.title}"吗？`, '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await deleteChapterOutline(this.novelId, item.id)
          this.$message.success('删除成功')
          this.fetchData()
        } catch (e) { console.error(e) }
      }).catch(() => {})
    },
    async handleDragEnd() {
      const order = this.list.map((item, index) => ({ id: item.id, sort: index + 1 }))
      try {
        await reorderChapterOutlines(this.novelId, { order })
        this.$message.success('排序已更新')
      } catch (e) { console.error(e) }
    },
    async handleGenerate() {
      this.aiPrompt = ''
      this.aiDialogVisible = true
    },
    async confirmGenerate() {
      const prompt = this.aiPrompt.trim() || '请根据小说大纲生成章节规划'
      this.aiLoading = true
      try {
        const res = await generateChapterOutline({ novelId: this.novelId, prompt })
        if (res) {
          this.aiDialogVisible = false
          this.$message.success('AI章纲已生成')
          this.fetchData()
        }
      } catch (e) { console.error(e) } finally {
        this.aiLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.chapter-outline-list-page {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-md};

    .back-btn { margin-bottom: 4px; padding: 0; color: #4A6CF7; }

    .page-title { margin: 0; }
  }

  .drag-hint {
    display: flex;
    align-items: center;
    gap: #{$spacing-xs};
    font-size: $font-size-sm;
    color: var(--color-text-placeholder);
    margin-bottom: #{$spacing-md};
  }

  .outline-list {
    min-height: 200px;
  }

  .outline-item {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    padding: #{$spacing-md};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    margin-bottom: #{$spacing-sm};
    transition: all $transition-fast;

    &:hover {
      box-shadow: var(--shadow-sm);
    }

    .drag-handle {
      cursor: move;
      color: var(--color-text-placeholder);
      font-size: 18px;
    }

    .outline-index {
      font-size: $font-size-sm;
      font-weight: 600;
      color: var(--color-primary);
      white-space: nowrap;
    }

    .outline-title {
      font-size: $font-size-base;
      font-weight: 500;
      color: var(--color-text);
      white-space: nowrap;
    }

    .outline-summary {
      flex: 1;
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .outline-actions {
      display: flex;
      gap: #{$spacing-xs};
    }
  }
}
</style>
