<template>
  <div class="character-list-page page-container page-container-narrow" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">人物设定</h1>
      </div>
      <div>
        <el-button type="primary" size="small" icon="el-icon-magic-stick" :loading="aiLoading" @click="handleGenerate">
          AI生成
        </el-button>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">
          新建角色
        </el-button>
      </div>
    </div>

    <div class="character-grid">
      <CharacterCard
        v-for="char in list"
        :key="char.id"
        :data="char"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>

    <EmptyState v-if="!loading && list.length === 0" text="暂无角色" description="创建你的第一个角色" />

    <!-- 编辑弹窗 -->
    <el-dialog :title="editing ? '编辑角色' : '新建角色'" :visible.sync="dialogVisible" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="角色姓名" />
        </el-form-item>
        <el-form-item label="角色定位" prop="role">
          <el-select v-model="form.role" placeholder="选择角色定位" style="width: 100%">
            <el-option label="主角" value="protagonist" />
            <el-option label="重要配角" value="major" />
            <el-option label="配角" value="supporting" />
            <el-option label="反派" value="antagonist" />
            <el-option label="龙套" value="minor" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <MarkdownInput v-model="form.description" :rows="4" placeholder="角色描述，支持 Markdown" />
        </el-form-item>
        <el-form-item label="背景">
          <MarkdownInput v-model="form.background" :rows="3" placeholder="角色背景，支持 Markdown" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tagsStr" placeholder="多个标签用逗号分隔" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

    <!-- AI 生成提示词弹窗 -->
    <el-dialog title="AI 生成角色" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="生成要求">
          <el-input
            v-model="aiPrompt"
            type="textarea"
            :rows="4"
            placeholder="描述你对角色的生成要求，例如：请生成一个冷酷的反派角色..."
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
import CharacterCard from '@/components/novel/CharacterCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import MarkdownInput from '@/components/common/MarkdownInput.vue'
import { getCharacters, createCharacter, updateCharacter, deleteCharacter } from '@/api/novel'
import { generateCharacter } from '@/api/ai'

export default {
  name: 'CharacterList',
  components: { CharacterCard, EmptyState, MarkdownInput },
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
      form: {
        name: '', role: '', description: '', background: '', tagsStr: ''
      },
      rules: {
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
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
        this.list = await getCharacters(this.novelId) || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.editing = null
      this.form = { name: '', role: '', description: '', background: '', tagsStr: '' }
      this.dialogVisible = true
    },
    handleEdit(item) {
      this.editing = item
      this.form = {
        name: item.name || '',
        role: item.role || '',
        description: item.description || '',
        background: item.background || '',
        tagsStr: (item.tags || []).join(',')
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const tags = this.form.tagsStr ? this.form.tagsStr.split(',').map(t => t.trim()).filter(Boolean) : []
          const data = { ...this.form, tags }
          delete data.tagsStr
          if (this.editing) {
            await updateCharacter(this.novelId, this.editing.id, data)
            this.$message.success('更新成功')
          } else {
            await createCharacter(this.novelId, data)
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
      this.$confirm(`确定要删除角色"${item.name}"吗？`, '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await deleteCharacter(this.novelId, item.id)
          this.$message.success('删除成功')
          this.fetchData()
        } catch (e) { console.error(e) }
      }).catch(() => {})
    },
    async handleGenerate() {
      this.aiPrompt = ''
      this.aiDialogVisible = true
    },
    async confirmGenerate() {
      const prompt = this.aiPrompt.trim() || '请根据小说信息生成主要人物设定'
      this.aiLoading = true
      try {
        const res = await generateCharacter({ novelId: this.novelId, prompt })
        if (res) {
          this.aiDialogVisible = false
          this.$message.success('AI角色已生成')
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
.character-list-page {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-lg};

    .back-btn { margin-bottom: 4px; padding: 0; color: #4A6CF7; }

    .page-title { margin: 0; }
  }

  .character-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: #{$spacing-md};
  }
}
</style>
