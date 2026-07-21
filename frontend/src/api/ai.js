import request from '@/utils/request'

// AI 生成类请求超时（毫秒）— 大纲/卷纲等复杂内容生成可能需要较长时间
const AI_GENERATE_TIMEOUT = 300000

export function optimize(data) {
  return request({ url: '/ai/optimize', method: 'post', data, timeout: 60000 })
}

export function expand(data) {
  return request({ url: '/ai/expand', method: 'post', data, timeout: 60000 })
}

export function condense(data) {
  return request({ url: '/ai/condense', method: 'post', data, timeout: 60000 })
}

export function continueWrite(data) {
  return request({ url: '/ai/continue', method: 'post', data, timeout: 60000 })
}

export function polishDialogue(data) {
  return request({ url: '/ai/polish-dialogue', method: 'post', data, timeout: 60000 })
}

export function predict(data) {
  return request({ url: '/ai/predict', method: 'post', data, timeout: 60000 })
}

export function generateOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'outline' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateVolumeOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'volume_outline' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateActOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'act_outline' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateDetailedOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'detailed_outline' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateCharacter(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'character' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateWorldview(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'worldview' }, timeout: AI_GENERATE_TIMEOUT })
}

export function generateChapterOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'chapter_outline' }, timeout: AI_GENERATE_TIMEOUT })
}

export function adopt(logId) {
  return request({ url: `/ai/adopt/${logId}`, method: 'post' })
}

export function getHistory(params) {
  return request({ url: '/ai/history', method: 'get', params })
}

// ==================== AI 配置管理 ====================

export function getAiConfigs() {
  return request({ url: '/ai-config', method: 'get' })
}

export function getAiConfig(id) {
  return request({ url: `/ai-config/${id}`, method: 'get' })
}

export function createAiConfig(data) {
  return request({ url: '/ai-config', method: 'post', data })
}

export function updateAiConfig(id, data) {
  return request({ url: `/ai-config/${id}`, method: 'put', data })
}

export function deleteAiConfig(id) {
  return request({ url: `/ai-config/${id}`, method: 'delete' })
}

export function activateAiConfig(id) {
  return request({ url: `/ai-config/${id}/activate`, method: 'post' })
}

export function testAiConnection(data) {
  return request({ url: '/ai-config/test', method: 'post', data })
}

// ==================== AI 提示词模板管理 ====================

export function getPromptTemplates() {
  return request({ url: '/ai/prompt-templates', method: 'get' })
}

export function savePromptTemplate(data) {
  return request({ url: '/ai/prompt-templates', method: 'put', data })
}

export function resetPromptTemplate(feature) {
  return request({ url: `/ai/prompt-templates/${feature}`, method: 'delete' })
}
