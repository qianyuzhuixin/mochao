<template>
  <div class="volume-page page-container page-container-narrow" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">卷纲管理</h1>
        <p class="page-desc">将大纲拆分为卷，每卷设置独立主题和情节任务</p>
      </div>
      <el-button type="primary" icon="el-icon-plus" @click="openCreateDialog">新建卷</el-button>
    </div>

    <!-- 卷列表 -->
    <div v-if="volumes.length === 0 && !loading" class="empty-state">
      <i class="el-icon-notebook-1 empty-icon"></i>
      <p>还没有卷纲，点击"新建卷"开始规划</p>
    </div>

    <div v-else class="volume-list">
      <div v-for="vol in volumes" :key="vol.id" class="volume-card">
        <div class="volume-header">
          <span class="volume-number">第{{ vol.volumeNumber }}卷</span>
          <h3 class="volume-title">{{ vol.title }}</h3>
          <div class="volume-actions">
            <el-button type="text" icon="el-icon-magic-stick" size="small"
              @click="openAIDialog(vol)">AI生成</el-button>
            <el-button type="text" icon="el-icon-edit" size="small"
              @click="openEditDialog(vol)">编辑</el-button>
            <el-button type="text" icon="el-icon-view" size="small"
              @click="$router.push(`/novels/${novelId}/volumes/${vol.id}/acts`)">幕</el-button>
            <el-button type="text" icon="el-icon-delete" size="small" style="color:#FF4D4F"
              @click="handleDelete(vol)">删除</el-button>
          </div>
        </div>
        <div v-if="vol.outline" class="volume-outline" v-html="renderMarkdown(vol.outline)" />
        <div v-else class="volume-outline-empty">暂无卷纲内容，点击"编辑"或"AI生成"添加</div>
      </div>
    </div>

    <!-- 编辑/创建弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="卷序号">
          <el-input-number v-model="form.volumeNumber" :min="1" />
        </el-form-item>
        <el-form-item label="卷标题" required>
          <el-input v-model="form.title" placeholder="如：初入江湖、风云际会" maxlength="50" />
        </el-form-item>
        <el-form-item label="卷纲内容">
          <MarkdownInput v-model="form.outline" :rows="10"
            placeholder="本卷主题、情节任务、关键节点、伏笔设计..." />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </span>
    </el-dialog>

    <!-- AI生成弹窗 -->
    <el-dialog title="AI 生成卷纲" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="提示词">
          <el-input v-model="aiPrompt" type="textarea" :rows="4"
            placeholder="描述你对本卷的想法，如：本卷讲主角进入宗门后的成长与第一次宗门大比..." />
        </el-form-item>
        <el-form-item label="提示模板">
          <el-tag type="info" size="small">可在AI设置→提示词模板中自定义</el-tag>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAIGenerate" :loading="aiLoading">生成</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { marked } from 'marked'
import { getVolumes, createVolume, updateVolume, deleteVolume } from '@/api/novel'
import { generateVolumeOutline } from '@/api/ai'
import MarkdownInput from '@/components/common/MarkdownInput.vue'

export default {
  name: 'VolumeList',
  components: { MarkdownInput },
  data() {
    return {
      novelId: null,
      loading: false,
      saving: false,
      volumes: [],
      dialogVisible: false,
      dialogMode: 'create', // 'create' | 'edit'
      editingVolumeId: null,
      form: { volumeNumber: 1, title: '', outline: '' },
      aiDialogVisible: false,
      aiPrompt: '',
      aiLoading: false,
      aiTargetVolumeId: null
    }
  },
  computed: {
    dialogTitle() { return this.dialogMode === 'create' ? '新建卷' : '编辑卷' }
  },
  created() {
    this.novelId = this.$route.params.id
    this.fetchVolumes()
  },
  methods: {
    renderMarkdown(text) {
      return marked.parse(text || '', { breaks: true, gfm: true })
    },
    async fetchVolumes() {
      this.loading = true
      try {
        const res = await getVolumes(this.novelId)
        this.volumes = res || []
      } catch (e) { console.error(e) } finally { this.loading = false }
    },
    openCreateDialog() {
      this.dialogMode = 'create'
      const nextNum = this.volumes.length + 1
      this.form = { volumeNumber: nextNum, title: '', outline: '' }
      this.dialogVisible = true
    },
    openEditDialog(vol) {
      this.dialogMode = 'edit'
      this.editingVolumeId = vol.id
      this.form = { volumeNumber: vol.volumeNumber, title: vol.title, outline: vol.outline || '' }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { volumeNumber: 1, title: '', outline: '' }
      this.editingVolumeId = null
    },
    async handleSave() {
      if (!this.form.title.trim()) { this.$message.warning('请输入卷标题'); return }
      this.saving = true
      try {
        if (this.dialogMode === 'create') {
          await createVolume(this.novelId, this.form)
        } else {
          await updateVolume(this.editingVolumeId, this.form)
        }
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.fetchVolumes()
      } catch (e) { console.error(e) } finally { this.saving = false }
    },
    async handleDelete(vol) {
      try {
        await this.$confirm('删除该卷将同时删除其下所有幕，确定？', '提示', { type: 'warning' })
        await deleteVolume(vol.id)
        this.$message.success('删除成功')
        this.fetchVolumes()
      } catch (e) { /* cancelled */ }
    },
    openAIDialog(vol) {
      this.aiTargetVolumeId = vol ? vol.id : null
      this.aiPrompt = ''
      this.aiDialogVisible = true
    },
    async handleAIGenerate() {
      const prompt = this.aiPrompt.trim()
      if (!prompt) { this.$message.warning('请输入生成提示'); return }
      this.aiLoading = true
      try {
        const params = { novelId: this.novelId, prompt }
        // 如果有目标卷（编辑模式），传入卷ID作为上下文
        if (this.aiTargetVolumeId) {
          params.parentType = 'volume'
          params.parentId = this.aiTargetVolumeId
        }
        const res = await generateVolumeOutline(params)
        const content = res?.content || ''
        if (this.aiTargetVolumeId) {
          // 编辑模式：追加到目标卷
          const vol = this.volumes.find(v => v.id === this.aiTargetVolumeId)
          if (vol) {
            this.openEditDialog(vol)
            this.form.outline = (vol.outline ? vol.outline + '\n\n' : '') + content
          }
        } else {
          // 新建模式：创建新卷
          this.dialogMode = 'create'
          this.form = { volumeNumber: this.volumes.length + 1, title: 'AI生成卷_' + (this.volumes.length + 1), outline: content }
          this.dialogVisible = true
        }
        this.aiDialogVisible = false
        this.$message.success('AI生成完成，请检查并保存')
      } catch (e) { console.error(e) } finally { this.aiLoading = false }
    }
  }
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.back-btn { margin-bottom: 4px; padding: 0; color: #4A6CF7; }
.page-desc { color: #909399; font-size: 14px; margin-top: 4px; }
.empty-state { text-align: center; padding: 80px 0; color: #909399; }
.empty-icon { font-size: 64px; margin-bottom: 16px; display: block; }
.volume-list { display: flex; flex-direction: column; gap: 16px; }
.volume-card { background: #fff; border: 1px solid #EBEEF5; border-radius: 8px; padding: 20px; transition: box-shadow .2s; }
.volume-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.volume-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.volume-number { font-size: 12px; color: #4A6CF7; background: #ECF0FF; padding: 2px 10px; border-radius: 12px; white-space: nowrap; }
.volume-title { font-size: 18px; margin: 0; flex: 1; }
.volume-actions { display: flex; gap: 4px; }
.volume-outline { color: #606266; font-size: 14px; line-height: 1.8; max-height: 200px; overflow-y: auto; }
.volume-outline-empty { color: #C0C4CC; font-size: 14px; font-style: italic; }
.volume-outline ::v-deep p { margin: 0 0 8px; }
.volume-outline ::v-deep ul,
.volume-outline ::v-deep ol { padding-left: 20px; margin: 6px 0; }
.volume-outline ::v-deep strong { color: #303133; font-weight: 600; }
.volume-outline ::v-deep h1,
.volume-outline ::v-deep h2,
.volume-outline ::v-deep h3,
.volume-outline ::v-deep h4 { font-size: 14px; margin: 10px 0 6px; color: #303133; }
</style>
