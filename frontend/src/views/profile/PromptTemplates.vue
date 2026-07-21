<template>
  <div class="prompt-templates-page page-container" v-loading="loading">
    <div class="page-header">
      <div>
        <h1 class="page-title">AI 提示词模板</h1>
        <p class="page-desc">自定义各功能的 AI 系统提示词，让 AI 更懂你的创作风格</p>
      </div>
    </div>

    <el-alert
      title="提示"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:20px">
      <template slot="default">
        系统已为每种生成类型提供默认模板。你可以根据需求定制专属提示词。
        修改后即时生效，无需重启。<br/>
        <strong>提示词中可使用以下占位符（由系统自动填充）：</strong>
        【小说基本信息】【小说大纲】【所属卷纲】【所属幕纲】— 这些上下文会自动注入到用户提示词中。
      </template>
    </el-alert>

    <div class="template-grid">
      <div v-for="tpl in templates" :key="tpl.feature" class="template-card">
        <div class="template-header">
          <div class="template-feature">
            <i :class="featureIcon(tpl.feature)" class="feature-icon"></i>
            <div>
              <h4 class="feature-name">{{ featureLabel(tpl.feature) }}</h4>
              <span class="feature-code">{{ tpl.feature }}</span>
            </div>
          </div>
          <el-tag v-if="tpl.userId !== 0" type="success" size="mini">已自定义</el-tag>
          <el-tag v-else type="info" size="mini">系统默认</el-tag>
        </div>

        <div class="template-preview">
          {{ tpl.systemPrompt || '(空模板)' }}
        </div>

        <div class="template-actions">
          <el-button type="primary" size="small" icon="el-icon-edit" @click="openEditDialog(tpl)">编辑</el-button>
          <el-button v-if="tpl.userId !== 0" type="warning" size="small" icon="el-icon-refresh-left"
            @click="handleReset(tpl)">恢复默认</el-button>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog :title="'编辑模板 - ' + editingFeature" :visible.sync="dialogVisible" width="700px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="editForm.name" placeholder="如：我的大纲风格" />
        </el-form-item>
        <el-form-item label="系统提示词" required>
          <el-input v-model="editForm.systemPrompt" type="textarea" :rows="12"
            placeholder="输入 AI 的系统提示词，告诉 AI 你的创作偏好、风格、具体要求..." />
        </el-form-item>
        <el-form-item label="生效范围">
          <el-tag type="success" size="small">仅对你生效</el-tag>
          <span style="color:#909399;font-size:12px;margin-left:8px">修改后，生成{{ featureLabel(editingFeature) }}时将使用此模板</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getPromptTemplates, savePromptTemplate, resetPromptTemplate } from '@/api/ai'

const FEATURE_MAP = {
  'generate-outline': { label: '大纲生成', icon: 'el-icon-document' },
  'generate-volume-outline': { label: '卷纲生成', icon: 'el-icon-notebook-1' },
  'generate-act-outline': { label: '幕纲生成', icon: 'el-icon-collection-tag' },
  'generate-detailed-outline': { label: '细纲生成', icon: 'el-icon-s-order' },
  'generate-character': { label: '人物设定', icon: 'el-icon-user' },
  'generate-worldview': { label: '世界观设定', icon: 'el-icon-picture-outline' },
  'generate-chapter_outline': { label: '章纲生成', icon: 'el-icon-notebook-2' }
}

const DEFAULT_TEMPLATES = {
  'generate-outline': '你是一位资深的网文大纲策划专家，精通各类网络小说流派（玄幻、都市、修仙、科幻、悬疑、历史、言情等）。\\n请根据提供的小说基本信息（书名、类型、简介）和用户的具体要求，直接为这本小说生成完整的故事大纲正文。\\n\\n输出要求：\\n1. 使用 Markdown 格式输出\\n2. 不要添加任何开场白（如"好的，根据您的要求"）、结束语、总结性陈述或解释性语句\\n3. 只输出大纲正文\\n\\n内容要求：\\n1. 先进行故事核心设定（主线冲突、世界观定位、主角成长路线）\\n2. 规划主要情节节点（开端、发展、转折、高潮、结局）\\n3. 设计分卷结构（每卷应有独立主题和任务）\\n4. 注意网文节奏感：每3-5章一个小高潮，每卷一个大高潮\\n5. 人物关系网和伏笔铺设建议',
  'generate-volume-outline': '你是一位网文卷纲策划专家。\\n请根据提供的小说大纲和用户要求，直接为指定卷生成详细的卷纲正文。\\n\\n输出要求：\\n1. 使用 Markdown 格式输出\\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\\n3. 只输出卷纲正文\\n\\n内容要求：\\n1. 明确本卷的主题和任务（承上启下的作用）\\n2. 规划本卷的幕结构（每幕的核心冲突和情感走向）\\n3. 设计关键情节和转折点\\n4. 控制节奏：开局吸引→发展推进→小高潮→铺垫下一卷\\n5. 标注需要回收和新增的伏笔',
  'generate-act-outline': '你是一位网文幕纲策划专家。\\n请根据提供的小说大纲、卷纲和用户要求，直接为指定幕生成详细的幕纲正文。\\n\\n输出要求：\\n1. 使用 Markdown 格式输出\\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\\n3. 只输出幕纲正文\\n\\n内容要求：\\n1. 明确本幕的核心冲突和情感主题\\n2. 规划本幕的场景序列（每个场景的功能：推进/展示/伏笔）\\n3. 设计具体的章节分布建议（每章要完成什么任务）\\n4. 标注场景之间的衔接和情绪曲线\\n5. 关键对话和动作场面的要点提示',
  'generate-detailed-outline': '你是一位网文细纲策划专家。\\n请根据提供的小说大纲、卷纲、幕纲和用户要求，直接为指定范围生成详细的章节细纲正文。\\n\\n输出要求：\\n1. 使用 Markdown 格式输出\\n2. 不要添加任何开场白、结束语、总结性陈述或解释性语句\\n3. 只输出细纲正文\\n\\n内容要求：\\n1. 每章标注：核心任务、情感目标、信息揭示\\n2. 开头钩子设计（吸引读者继续阅读的悬念/冲突/金句）\\n3. 每章2-3个场景分解，每个场景标注功能和字数建议\\n4. 关键对话要点和动作场面的节奏控制\\n5. 章节结尾钩子（预告/悬念/情绪余韵）',
  'generate-character': '你是一位人物设定专家。请根据提供的信息生成一个完整的人物设定，包括外貌、性格、背景、关系等。',
  'generate-worldview': '你是一位世界观架构专家。请根据提供的信息生成一个完整的世界观设定，包括力量体系、地理格局、历史背景、势力分布等。',
  'generate-chapter_outline': '你是一位章节大纲专家。请根据提供的信息生成章节大纲。'
}

export default {
  name: 'PromptTemplates',
  data() {
    return {
      loading: false,
      saving: false,
      templates: [],
      dialogVisible: false,
      editingFeature: '',
      editForm: { name: '', systemPrompt: '' }
    }
  },
  created() { this.fetchTemplates() },
  methods: {
    featureLabel(feature) {
      return (FEATURE_MAP[feature] || {}).label || feature
    },
    featureIcon(feature) {
      return (FEATURE_MAP[feature] || {}).icon || 'el-icon-set-up'
    },
    async fetchTemplates() {
      this.loading = true
      try {
        const res = await getPromptTemplates()
        const userTemplates = res || []

        // 合并：所有 feature 类型 + 用户自定义 > 系统默认
        const merged = {}
        for (const key of Object.keys(DEFAULT_TEMPLATES)) {
          merged[key] = { feature: key, name: '系统默认-' + this.featureLabel(key), systemPrompt: DEFAULT_TEMPLATES[key], userId: 0 }
        }
        for (const tpl of userTemplates) {
          merged[tpl.feature] = tpl
        }
        this.templates = Object.values(merged)
      } catch (e) { console.error(e) } finally { this.loading = false }
    },
    openEditDialog(tpl) {
      this.editingFeature = tpl.feature
      this.editForm = {
        name: tpl.name || '',
        systemPrompt: tpl.systemPrompt || ''
      }
      this.dialogVisible = true
    },
    async handleSave() {
      if (!this.editForm.systemPrompt.trim()) {
        this.$message.warning('提示词不能为空')
        return
      }
      this.saving = true
      try {
        await savePromptTemplate({
          feature: this.editingFeature,
          name: this.editForm.name,
          systemPrompt: this.editForm.systemPrompt
        })
        this.$message.success('模板已保存，立即生效')
        this.dialogVisible = false
        this.fetchTemplates()
      } catch (e) { console.error(e) } finally { this.saving = false }
    },
    async handleReset(tpl) {
      try {
        await this.$confirm('将恢复为系统默认模板，确定？', '提示', { type: 'warning' })
        await resetPromptTemplate(tpl.feature)
        this.$message.success('已恢复默认')
        this.fetchTemplates()
      } catch (e) { /* cancelled */ }
    }
  }
}
</script>

<style scoped>
.prompt-templates-page { max-width: 1000px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-desc { color: #909399; font-size: 14px; margin-top: 4px; }
.template-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.template-card { background: #fff; border: 1px solid #EBEEF5; border-radius: 8px; padding: 16px; transition: box-shadow .2s; }
.template-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.template-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.template-feature { display: flex; align-items: center; gap: 10px; }
.feature-icon { font-size: 22px; color: #4A6CF7; }
.feature-name { margin: 0; font-size: 15px; }
.feature-code { font-size: 11px; color: #C0C4CC; }
.template-preview { color: #606266; font-size: 13px; line-height: 1.8; max-height: 120px; overflow-y: hidden; position: relative; }
.template-preview::after { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 40px; background: linear-gradient(transparent, #fff); }
.template-actions { margin-top: 12px; display: flex; gap: 8px; }
</style>
