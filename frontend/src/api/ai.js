import request from '@/utils/request'

export function optimize(data) {
  return request({ url: '/ai/optimize', method: 'post', data })
}

export function expand(data) {
  return request({ url: '/ai/expand', method: 'post', data })
}

export function condense(data) {
  return request({ url: '/ai/condense', method: 'post', data })
}

export function continueWrite(data) {
  return request({ url: '/ai/continue', method: 'post', data })
}

export function polishDialogue(data) {
  return request({ url: '/ai/polish-dialogue', method: 'post', data })
}

export function predict(data) {
  return request({ url: '/ai/predict', method: 'post', data })
}

export function generateOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'outline' } })
}

export function generateCharacter(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'character' } })
}

export function generateWorldview(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'worldview' } })
}

export function generateChapterOutline(data) {
  return request({ url: '/ai/generate', method: 'post', data: { ...data, type: 'chapter_outline' } })
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
