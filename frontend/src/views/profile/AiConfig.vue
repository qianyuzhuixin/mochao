<template>
  <div class="ai-config-page">
    <div class="page-header">
      <h2>AI 模型配置</h2>
      <p class="page-desc">配置你的 AI 写作助手，支持 OpenAI / DeepSeek / Moonshot / 阿里百炼 / 硅基流动等主流模型</p>
    </div>

    <el-row :gutter="20">
      <!-- 配置列表 -->
      <el-col :span="14" :xs="24">
        <div class="config-card">
          <div class="card-header">
            <span>我的配置</span>
            <el-button type="primary" size="small" @click="openDialog()">添加配置</el-button>
          </div>

          <div v-loading="loading" class="config-list">
            <div v-if="list.length === 0" class="empty-hint">
              <i class="el-icon-setting" style="font-size:48px;color:#c0c4cc" />
              <p>还没有 AI 配置，点击"添加配置"开始</p>
            </div>

            <div
              v-for="item in list"
              :key="item.id"
              class="config-item"
              :class="{ active: item.isActive }"
            >
              <div class="config-main">
                <div class="config-info">
                  <div class="config-name">
                    <span class="provider-badge" :class="getProviderClass(item.providerName)">
                      {{ getProviderLabel(item.providerName) }}
                    </span>
                    <span class="model-name">{{ item.model }}</span>
                    <el-tag v-if="item.isActive" type="success" size="mini">使用中</el-tag>
                  </div>
                  <div class="config-meta">
                    <span class="config-url">{{ item.apiUrl }}</span>
                  </div>
                </div>
                <div class="config-actions">
                  <el-button
                    v-if="!item.isActive"
                    type="text"
                    size="small"
                    @click="handleActivate(item)"
                  >启用</el-button>
                  <el-button type="text" size="small" @click="openDialog(item)">编辑</el-button>
                  <el-popconfirm
                    title="确定删除此配置？"
                    :disabled="item.isActive"
                    @confirm="handleDelete(item)"
                  >
                    <el-button
                      slot="reference"
                      type="text"
                      size="small"
                      :disabled="item.isActive"
                      style="color:#f56c6c"
                    >删除</el-button>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 预设指南 -->
      <el-col :span="10" :xs="24">
        <div class="guide-card">
          <h3>主流模型提供商</h3>
          <p class="guide-desc">以下提供商均兼容 OpenAI API 格式，可直接使用</p>

          <div class="provider-list">
            <div
              v-for="provider in providers"
              :key="provider.name"
              class="provider-item"
              @click="fillPreset(provider)"
            >
              <div class="provider-header">
                <strong>{{ provider.label }}</strong>
                <el-tag size="mini" type="info">{{ provider.model }}</el-tag>
              </div>
              <div class="provider-url">{{ provider.apiUrl }}</div>
              <div class="provider-tip">{{ provider.tip }}</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      :title="isEdit ? '编辑配置' : '添加配置'"
      :visible.sync="dialogVisible"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" size="small">
        <el-form-item label="提供商" prop="providerName">
          <el-select v-model="form.providerName" placeholder="选择或输入" filterable allow-create style="width:100%">
            <el-option
              v-for="p in providers"
              :key="p.name"
              :label="p.label"
              :value="p.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API 地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://api.xxx.com/v1/chat/completions" />
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" placeholder="sk-..." type="password" show-password />
        </el-form-item>

        <el-form-item label="模型" prop="model">
          <el-input v-model="form.model" placeholder="gpt-4o / deepseek-chat / moonshot-v1-8k" />
        </el-form-item>

        <el-form-item label="最大 Token">
          <el-input-number v-model="form.maxTokens" :min="100" :max="128000" :step="500" style="width:100%" />
        </el-form-item>

        <el-form-item label="温度">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleTest" :loading="testing" size="small">
            <i class="el-icon-connection" /> 测试连接
          </el-button>
          <span v-if="testResult !== null" class="test-result" :class="testResult.success ? 'success' : 'fail'">
            {{ testResult.success ? '✓ 连接成功' : '✗ ' + testResult.message }}
            <template v-if="testResult.success">（{{ testResult.latency }}ms）</template>
          </span>
        </el-form-item>

        <el-collapse v-model="proxyCollapse" style="margin-top: 8px">
          <el-collapse-item title="代理设置（国内访问 OpenAI 需填写）" name="proxy">
            <el-form-item label="代理地址">
              <el-input v-model="form.proxyHost" placeholder="127.0.0.1" />
            </el-form-item>

            <el-form-item label="代理端口">
              <el-input-number v-model="form.proxyPort" :min="1" :max="65535" placeholder="7890" style="width:100%" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isEdit ? '保存' : '添加' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAiConfigs, createAiConfig, updateAiConfig, deleteAiConfig,
  activateAiConfig, testAiConnection
} from '@/api/ai'

export default {
  name: 'AiConfig',
  data() {
    return {
      loading: false,
      saving: false,
      testing: false,
      list: [],
      dialogVisible: false,
      isEdit: false,
      testResult: null,
      proxyCollapse: [],
      form: {
        id: null,
        providerName: '',
        apiUrl: '',
        apiKey: '',
        model: '',
        maxTokens: 2000,
        temperature: 0.8,
        proxyHost: '',
        proxyPort: null
      },
      rules: {
        providerName: [{ required: true, message: '请选择提供商', trigger: 'blur' }],
        apiUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
        apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
        model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      },
      providers: [
        {
          name: 'openai',
          label: 'OpenAI',
          apiUrl: 'https://api.openai.com/v1/chat/completions',
          model: 'gpt-4o',
          tip: '需代理访问，推荐 gpt-4o / gpt-4o-mini'
        },
        {
          name: 'deepseek',
          label: 'DeepSeek',
          apiUrl: 'https://api.deepseek.com/v1/chat/completions',
          model: 'deepseek-chat',
          tip: '国内直连，性价比高，写作能力强'
        },
        {
          name: 'moonshot',
          label: 'Moonshot (Kimi)',
          apiUrl: 'https://api.moonshot.cn/v1/chat/completions',
          model: 'moonshot-v1-8k',
          tip: '国内直连，长文本处理能力突出'
        },
        {
          name: 'qwen',
          label: '阿里百炼 (通义千问)',
          apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
          model: 'qwen-plus',
          tip: '国内直连，阿里云生态，中文能力强'
        },
        {
          name: 'siliconflow',
          label: '硅基流动',
          apiUrl: 'https://api.siliconflow.cn/v1/chat/completions',
          model: 'deepseek-ai/DeepSeek-V3',
          tip: '国内直连，聚合多模型，免费额度充足'
        },
        {
          name: 'zhipu',
          label: '智谱 GLM',
          apiUrl: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
          model: 'glm-4-flash',
          tip: '国内直连，GLM 系列模型'
        },
        {
          name: 'ollama',
          label: 'Ollama（本地）',
          apiUrl: 'http://localhost:11434/v1/chat/completions',
          model: 'qwen2.5:7b',
          tip: '本地免费部署，支持 qwen2.5 / llama3 / mistral 等'
        },
        {
          name: 'lmstudio',
          label: 'LM Studio（本地）',
          apiUrl: 'http://localhost:1234/v1/chat/completions',
          model: 'local-model',
          tip: '本地免费运行，图形化界面管理模型'
        }
      ]
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        this.list = await getAiConfigs() || []
      } catch (e) {
        // handled by interceptor
      } finally {
        this.loading = false
      }
    },

    openDialog(item) {
      this.testResult = null
      if (item) {
        this.isEdit = true
        this.form = {
          id: item.id,
          providerName: item.providerName || '',
          apiUrl: item.apiUrl || '',
          apiKey: item.apiKey || '',
          model: item.model || '',
          maxTokens: item.maxTokens || 2000,
          temperature: item.temperature != null ? item.temperature : 0.8,
          proxyHost: item.proxyHost || '',
          proxyPort: item.proxyPort || null
        }
      } else {
        this.isEdit = false
        this.form = {
          id: null,
          providerName: '',
          apiUrl: '',
          apiKey: '',
          model: '',
          maxTokens: 2000,
          temperature: 0.8,
          proxyHost: '',
          proxyPort: null
        }
      }
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
      this.dialogVisible = true
    },

    fillPreset(provider) {
      this.openDialog()
      this.$nextTick(() => {
        this.form.providerName = provider.name
        this.form.apiUrl = provider.apiUrl
        this.form.model = provider.model
      })
    },

    async handleTest() {
      this.testing = true
      this.testResult = null
      try {
        this.testResult = await testAiConnection(this.form)
      } catch (e) {
        this.testResult = { success: false, message: e.message || '请求失败', latency: 0 }
      } finally {
        this.testing = false
      }
    },

    async handleSave() {
      try {
        await this.$refs.form.validate()
      } catch {
        return
      }
      this.saving = true
      try {
        if (this.isEdit) {
          await updateAiConfig(this.form.id, this.form)
          this.$message.success('配置已更新')
        } else {
          await createAiConfig(this.form)
          this.$message.success('配置已添加')
        }
        this.dialogVisible = false
        this.fetchList()
      } catch (e) {
        // handled by interceptor
      } finally {
        this.saving = false
      }
    },

    async handleActivate(item) {
      try {
        await activateAiConfig(item.id)
        this.$message.success(`已切换到 ${item.providerName}`)
        this.fetchList()
      } catch (e) {
        // handled by interceptor
      }
    },

    async handleDelete(item) {
      try {
        await deleteAiConfig(item.id)
        this.$message.success('已删除')
        this.fetchList()
      } catch (e) {
        // handled by interceptor
      }
    },

    getProviderLabel(name) {
      const map = {
        openai: 'OpenAI', deepseek: 'DeepSeek', moonshot: 'Kimi',
        qwen: '通义千问', siliconflow: '硅基流动', zhipu: '智谱 GLM',
        ollama: 'Ollama', lmstudio: 'LM Studio'
      }
      return map[name] || name
    },

    getProviderClass(name) {
      const map = {
        openai: 'openai', deepseek: 'deepseek', moonshot: 'moonshot',
        qwen: 'qwen', siliconflow: 'silicon', zhipu: 'zhipu',
        ollama: 'ollama', lmstudio: 'lmstudio'
      }
      return map[name] || 'default'
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-config-page {
  max-width: $content-max-width;
  margin: 0 auto;
  padding: $spacing-xl $spacing-lg;
}

.page-header {
  margin-bottom: $spacing-xl;
  h2 { margin: 0 0 $spacing-xs 0; font-size: 24px; color: var(--color-text); }
  .page-desc { margin: 0; color: var(--color-text-secondary); font-size: 14px; }
}

.config-card, .guide-card {
  background: var(--color-card-bg);
  border-radius: $border-radius-lg;
  padding: $spacing-lg;
  box-shadow: var(--shadow-sm);
  margin-bottom: $spacing-lg;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: $spacing-md;
  font-weight: 600;
  color: var(--color-text);
}

.empty-hint {
  text-align: center;
  padding: $spacing-xxl 0;
  color: var(--color-text-secondary);
}

.config-item {
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  transition: all .2s;

  &:hover { border-color: var(--color-primary); }
  &.active { border-color: var(--color-success); background: rgba(103,194,58,0.04); }

  .config-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .config-info { flex: 1; min-width: 0; }
  .config-name {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    margin-bottom: 4px;
  }
  .provider-badge {
    padding: 1px 8px;
    border-radius: 4px;
    font-size: 12px;
    color: #fff;
    &.openai { background: #10a37f; }
    &.deepseek { background: #4d6bfe; }
    &.moonshot { background: #6c3ce1; }
    &.qwen { background: #ff6a00; }
    &.silicon { background: #6366f1; }
    &.zhipu { background: #3859ff; }
    &.ollama { background: #4a9d8f; }
    &.lmstudio { background: #8b5cf6; }
    &.default { background: #909399; }
  }
  .model-name { font-size: 13px; color: var(--color-text-secondary); }
  .config-url { font-size: 12px; color: var(--color-text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .config-actions { flex-shrink: 0; margin-left: $spacing-md; }
}

.guide-card {
  h3 { margin: 0 0 4px 0; font-size: 16px; }
  .guide-desc { margin: 0 0 $spacing-md 0; font-size: 13px; color: var(--color-text-secondary); }
}

.provider-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
}

.provider-item {
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  padding: $spacing-sm $spacing-md;
  cursor: pointer;
  transition: all .2s;

  &:hover {
    border-color: var(--color-primary);
    background: rgba(64,158,255,0.04);
  }

  .provider-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2px;
  }
  .provider-url {
    font-size: 12px;
    color: var(--color-text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .provider-tip {
    font-size: 12px;
    color: var(--color-text-placeholder);
    margin-top: 2px;
  }
}

.test-result {
  margin-left: $spacing-md;
  font-size: 13px;
  &.success { color: var(--color-success); }
  &.fail { color: var(--color-error); }
}

@media (max-width: 768px) {
  .ai-config-page { padding: $spacing-md; }
  .config-item .config-main { flex-direction: column; align-items: flex-start; gap: $spacing-sm; }
  .config-actions { margin-left: 0 !important; }
}
</style>
