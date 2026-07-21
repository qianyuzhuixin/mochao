<template>
  <div class="act-page page-container page-container-narrow" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">幕纲管理</h1>
        <p class="page-desc">
          <template v-if="currentVolume">第{{ currentVolume.volumeNumber }}卷 · {{ currentVolume.title }}</template>
          <template v-else>为每卷创建幕，细化情节结构</template>
        </p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedVolumeId" placeholder="选择卷" @change="onVolumeChange" style="width:200px;margin-right:12px">
          <el-option v-for="v in volumes" :key="v.id" :label="'第'+v.volumeNumber+'卷 '+v.title" :value="v.id" />
        </el-select>
        <el-button type="primary" icon="el-icon-plus" @click="openCreateDialog" :disabled="!selectedVolumeId">新建幕</el-button>
      </div>
    </div>

    <div v-if="acts.length === 0 && !loading" class="empty-state">
      <i class="el-icon-collection-tag empty-icon"></i>
      <p>{{ selectedVolumeId ? '该卷还没有幕，点击"新建幕"开始' : '请先选择一卷' }}</p>
    </div>

    <div v-else class="act-list">
      <div v-for="act in acts" :key="act.id" class="act-card">
        <div class="act-header">
          <span class="act-number">第{{ act.actNumber }}幕</span>
          <h4 class="act-title">{{ act.title }}</h4>
          <div class="act-actions">
            <el-button type="text" icon="el-icon-magic-stick" size="small"
              @click="openAIDialog(act)">AI生成</el-button>
            <el-button type="text" icon="el-icon-edit" size="small"
              @click="openEditDialog(act)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" size="small" style="color:#FF4D4F"
              @click="handleDelete(act)">删除</el-button>
          </div>
        </div>
        <div v-if="act.outline" class="act-outline" v-html="renderMarkdown(act.outline)" />
        <div v-else class="act-outline-empty">暂无幕纲内容</div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="幕序号">
          <el-input-number v-model="form.actNumber" :min="1" />
        </el-form-item>
        <el-form-item label="幕标题" required>
          <el-input v-model="form.title" placeholder="如：入门试炼、初遇强敌" maxlength="50" />
        </el-form-item>
        <el-form-item label="幕纲内容">
          <MarkdownInput v-model="form.outline" :rows="10"
            placeholder="本幕核心冲突、场景序列、章节分布建议、关键对话要点..." />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </span>
    </el-dialog>

    <!-- AI生成弹窗 -->
    <el-dialog title="AI 生成幕纲" :visible.sync="aiDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="提示词">
          <el-input v-model="aiPrompt" type="textarea" :rows="4"
            placeholder="描述你对本幕的想法，如：主角在入门试炼中一鸣惊人，结识几位重要同伴..." />
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
import { getVolumes, getActsByVolume, createAct, updateAct, deleteAct } from '@/api/novel'
import { generateActOutline } from '@/api/ai'
import MarkdownInput from '@/components/common/MarkdownInput.vue'

export default {
  name: 'ActList',
  components: { MarkdownInput },
  data() {
    return {
      novelId: null,
      loading: false,
      saving: false,
      volumes: [],
      selectedVolumeId: null,
      currentVolume: null,
      acts: [],
      dialogVisible: false,
      dialogMode: 'create',
      editingActId: null,
      form: { actNumber: 1, title: '', outline: '' },
      aiDialogVisible: false,
      aiPrompt: '',
      aiLoading: false,
      aiTargetActId: null
    }
  },
  computed: {
    dialogTitle() { return this.dialogMode === 'create' ? '新建幕' : '编辑幕' }
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
      try {
        const res = await getVolumes(this.novelId)
        this.volumes = res || []
        if (this.volumes.length > 0 && !this.selectedVolumeId) {
          this.selectedVolumeId = this.volumes[0].id
          this.currentVolume = this.volumes[0]
          this.fetchActs()
        }
      } catch (e) { console.error(e) }
    },
    onVolumeChange(val) {
      this.currentVolume = this.volumes.find(v => v.id === val) || null
      this.fetchActs()
    },
    async fetchActs() {
      if (!this.selectedVolumeId) return
      this.loading = true
      try {
        const res = await getActsByVolume(this.selectedVolumeId)
        this.acts = res || []
      } catch (e) { console.error(e) } finally { this.loading = false }
    },
    openCreateDialog() {
      this.dialogMode = 'create'
      const nextNum = this.acts.length + 1
      this.form = { actNumber: nextNum, title: '', outline: '' }
      this.dialogVisible = true
    },
    openEditDialog(act) {
      this.dialogMode = 'edit'
      this.editingActId = act.id
      this.form = { actNumber: act.actNumber, title: act.title, outline: act.outline || '' }
      this.dialogVisible = true
    },
    resetForm() {
      this.form = { actNumber: 1, title: '', outline: '' }
      this.editingActId = null
    },
    async handleSave() {
      if (!this.form.title.trim()) { this.$message.warning('请输入幕标题'); return }
      this.saving = true
      try {
        if (this.dialogMode === 'create') {
          await createAct(this.novelId, { ...this.form, volumeId: this.selectedVolumeId })
        } else {
          await updateAct(this.editingActId, this.form)
        }
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.fetchActs()
      } catch (e) { console.error(e) } finally { this.saving = false }
    },
    async handleDelete(act) {
      try {
        await this.$confirm('确定删除该幕？', '提示', { type: 'warning' })
        await deleteAct(act.id)
        this.$message.success('删除成功')
        this.fetchActs()
      } catch (e) { /* cancelled */ }
    },
    openAIDialog(act) {
      this.aiTargetActId = act ? act.id : null
      this.aiPrompt = ''
      this.aiDialogVisible = true
    },
    async handleAIGenerate() {
      const prompt = this.aiPrompt.trim()
      if (!prompt) { this.$message.warning('请输入生成提示'); return }
      this.aiLoading = true
      try {
        const params = {
          novelId: this.novelId,
          prompt,
          parentType: 'volume',
          parentId: this.selectedVolumeId
        }
        const res = await generateActOutline(params)
        const content = res?.content || ''
        if (this.aiTargetActId) {
          const act = this.acts.find(a => a.id === this.aiTargetActId)
          if (act) {
            this.openEditDialog(act)
            this.form.outline = (act.outline ? act.outline + '\n\n' : '') + content
          }
        } else {
          this.dialogMode = 'create'
          this.form = { actNumber: this.acts.length + 1, title: 'AI生成幕_' + (this.acts.length + 1), outline: content }
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
.header-actions { display: flex; align-items: center; }
.empty-state { text-align: center; padding: 80px 0; color: #909399; }
.empty-icon { font-size: 64px; margin-bottom: 16px; display: block; }
.act-list { display: flex; flex-direction: column; gap: 12px; }
.act-card { background: #fff; border: 1px solid #EBEEF5; border-radius: 8px; padding: 16px 20px; transition: box-shadow .2s; }
.act-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.act-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.act-number { font-size: 12px; color: #52C41A; background: #F0FFF0; padding: 2px 10px; border-radius: 12px; white-space: nowrap; }
.act-title { font-size: 16px; margin: 0; flex: 1; }
.act-actions { display: flex; gap: 4px; }
.act-outline { color: #606266; font-size: 14px; line-height: 1.8; max-height: 160px; overflow-y: auto; }
.act-outline-empty { color: #C0C4CC; font-size: 13px; font-style: italic; }
.act-outline ::v-deep p { margin: 0 0 6px; }
.act-outline ::v-deep ul,
.act-outline ::v-deep ol { padding-left: 18px; margin: 4px 0; }
.act-outline ::v-deep strong { color: #303133; font-weight: 600; }
.act-outline ::v-deep h1,
.act-outline ::v-deep h2,
.act-outline ::v-deep h3,
.act-outline ::v-deep h4 { font-size: 14px; margin: 8px 0 4px; color: #303133; }
</style>
