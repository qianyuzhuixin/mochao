<template>
  <div class="outline-editor page-container" v-loading="loading">
    <div class="editor-header">
      <el-button type="text" icon="el-icon-arrow-left" @click="$router.push(`/novels/${novelId}`)">
        返回工作台
      </el-button>
      <h1 class="page-title">大纲编辑</h1>
      <el-button type="primary" size="small" icon="el-icon-magic-stick" :loading="aiLoading" @click="handleGenerate">
        AI生成
      </el-button>
    </div>

    <div class="editor-card">
      <el-input
        v-model="outline"
        type="textarea"
        :rows="20"
        placeholder="在这里编写你的小说大纲..."
        resize="vertical"
      />
    </div>

    <div class="editor-actions">
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <!-- AI 生成提示词弹窗 -->
    <el-dialog title="AI 生成大纲" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="生成要求">
          <el-input
            v-model="aiPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你对大纲的生成要求，例如：请帮我生成一个玄幻修仙小说的详细大纲..."
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
import { getOutline, saveOutline } from '@/api/novel'
import { generateOutline } from '@/api/ai'

export default {
  name: 'OutlineEditor',
  data() {
    return {
      outline: '',
      loading: false,
      saving: false,
      aiLoading: false,
      aiDialogVisible: false,
      aiPrompt: ''
    }
  },
  computed: {
    novelId() {
      return this.$route.params.id
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getOutline(this.novelId)
        this.outline = res || res?.content || ''
      } catch (e) {
        // 错误已处理
      } finally {
        this.loading = false
      }
    },
    async handleSave() {
      this.saving = true
      try {
        await saveOutline(this.novelId, { content: this.outline })
        this.$message.success('保存成功')
      } catch (e) {
        // 错误已处理
      } finally {
        this.saving = false
      }
    },
    async handleGenerate() {
      this.aiPrompt = ''
      this.aiDialogVisible = true
    },
    async confirmGenerate() {
      const prompt = this.aiPrompt.trim() || (this.outline ? '请基于已有内容优化并完善小说大纲' : '请帮我生成小说大纲')
      this.aiLoading = true
      try {
        const res = await generateOutline({ novelId: this.novelId, prompt })
        if (res) {
          this.aiDialogVisible = false
          this.$confirm('AI已生成大纲，是否替换当前内容？', '提示', {
            confirmButtonText: '替换',
            cancelButtonText: '追加',
            type: 'info'
          }).then(() => {
            this.outline = res
          }).catch(() => {
            this.outline += '\n\n' + res
          })
        }
      } catch (e) {
        // 错误已处理
      } finally {
        this.aiLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.outline-editor {
  .editor-header {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-lg};

    .page-title {
      margin: 0;
      flex: 1;
    }
  }

  .editor-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-md};

    ::v-deep .el-textarea__inner {
      font-size: $font-size-md;
      line-height: 1.8;
    }
  }

  .editor-actions {
    margin-top: #{$spacing-md};
    text-align: right;
  }
}
</style>
