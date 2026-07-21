<template>
  <div class="worldview-editor page-container page-container-narrow" v-loading="loading">
    <div class="editor-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">世界观编辑</h1>
      </div>
      <el-button type="primary" size="small" icon="el-icon-magic-stick" :loading="aiLoading" @click="handleGenerate">
        AI补充
      </el-button>
    </div>

    <div class="editor-card">
      <MarkdownInput
        v-model="worldview"
        :rows="20"
        placeholder="在这里编写你的小说世界观设定，支持 Markdown..."
      />
    </div>

    <div class="editor-actions">
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <!-- AI 生成提示词弹窗 -->
    <el-dialog title="AI 补充世界观" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="生成要求">
          <el-input
            v-model="aiPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你对世界观的生成要求，例如：请帮我补充一个修仙世界的详细设定..."
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
import { getWorldview, saveWorldview } from '@/api/novel'
import { generateWorldview } from '@/api/ai'
import MarkdownInput from '@/components/common/MarkdownInput.vue'

export default {
  name: 'WorldviewEditor',
  components: { MarkdownInput },
  data() {
    return {
      worldview: '',
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
        const res = await getWorldview(this.novelId)
        this.worldview = res || res?.content || ''
      } catch (e) {
        // 错误已处理
      } finally {
        this.loading = false
      }
    },
    async handleSave() {
      this.saving = true
      try {
        await saveWorldview(this.novelId, { content: this.worldview })
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
      const prompt = this.aiPrompt.trim() || (this.worldview ? '请基于已有内容优化并完善世界观设定' : '请帮我生成小说世界观设定')
      this.aiLoading = true
      try {
        const res = await generateWorldview({ novelId: this.novelId, prompt })
        const content = res?.content || ''
        if (content) {
          this.aiDialogVisible = false
          this.$confirm('AI已生成世界观内容，是否替换当前内容？', '提示', {
            confirmButtonText: '替换',
            cancelButtonText: '追加',
            type: 'info'
          }).then(() => {
            this.worldview = content
          }).catch(() => {
            this.worldview += '\n\n' + content
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
.worldview-editor {
  .editor-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-lg};

    .back-btn { margin-bottom: 4px; padding: 0; color: #4A6CF7; }

    .page-title {
      margin: 0;
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
