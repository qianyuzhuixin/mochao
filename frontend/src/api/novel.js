import request from '@/utils/request'

// 小说基础操作
export function createNovel(data) {
  return request({ url: '/novels', method: 'post', data })
}

export function getNovels(params) {
  return request({ url: '/novels', method: 'get', params })
}

export function getNovelById(id) {
  return request({ url: `/novels/${id}`, method: 'get' })
}

export function updateNovel(id, data) {
  return request({ url: `/novels/${id}`, method: 'put', data })
}

export function deleteNovel(id) {
  return request({ url: `/novels/${id}`, method: 'delete' })
}

export function getNovelProgress(id) {
  return request({ url: `/novels/${id}/progress`, method: 'get' })
}

// 大纲
export function getOutline(id) {
  return request({ url: `/novels/${id}/outline`, method: 'get' })
}

export function saveOutline(id, data) {
  return request({ url: `/novels/${id}/outline`, method: 'put', data })
}

// 世界观
export function getWorldview(id) {
  return request({ url: `/novels/${id}/worldview`, method: 'get' })
}

export function saveWorldview(id, data) {
  return request({ url: `/novels/${id}/worldview`, method: 'put', data })
}

// 人物
export function getCharacters(id) {
  return request({ url: `/novels/${id}/characters`, method: 'get' })
}

export function createCharacter(id, data) {
  return request({ url: `/novels/${id}/characters`, method: 'post', data })
}

export function getCharacterById(id, cid) {
  return request({ url: `/novels/${id}/characters/${cid}`, method: 'get' })
}

export function updateCharacter(id, cid, data) {
  return request({ url: `/novels/${id}/characters/${cid}`, method: 'put', data })
}

export function deleteCharacter(id, cid) {
  return request({ url: `/novels/${id}/characters/${cid}`, method: 'delete' })
}

// 物品
export function getItems(id) {
  return request({ url: `/novels/${id}/items`, method: 'get' })
}

export function createItem(id, data) {
  return request({ url: `/novels/${id}/items`, method: 'post', data })
}

export function updateItem(id, iid, data) {
  return request({ url: `/novels/${id}/items/${iid}`, method: 'put', data })
}

export function deleteItem(id, iid) {
  return request({ url: `/novels/${id}/items/${iid}`, method: 'delete' })
}

// 章纲
export function getChapterOutlines(id) {
  return request({ url: `/novels/${id}/chapter-outlines`, method: 'get' })
}

export function createChapterOutline(id, data) {
  return request({ url: `/novels/${id}/chapter-outlines`, method: 'post', data })
}

export function updateChapterOutline(id, oid, data) {
  return request({ url: `/novels/${id}/chapter-outlines/${oid}`, method: 'put', data })
}

export function deleteChapterOutline(id, oid) {
  return request({ url: `/novels/${id}/chapter-outlines/${oid}`, method: 'delete' })
}

export function reorderChapterOutlines(id, data) {
  return request({ url: `/novels/${id}/chapter-outlines/reorder`, method: 'put', data })
}

// 章节
export function getChapters(id) {
  return request({ url: `/novels/${id}/chapters`, method: 'get' })
}

export function getChapterById(id, chId) {
  return request({ url: `/novels/${id}/chapters/${chId}`, method: 'get' })
}

export function saveChapter(id, chId, data) {
  return request({ url: `/novels/${id}/chapters/${chId}`, method: 'put', data })
}

export function updateChapterStatus(id, chId, data) {
  return request({ url: `/novels/${id}/chapters/${chId}/status`, method: 'put', data })
}

// 卷纲
export function getVolumes(id) {
  return request({ url: `/novels/${id}/volumes`, method: 'get' })
}

export function createVolume(id, data) {
  return request({ url: `/novels/${id}/volumes`, method: 'post', data })
}

export function updateVolume(volumeId, data) {
  return request({ url: `/novels/volumes/${volumeId}`, method: 'put', data })
}

export function deleteVolume(volumeId) {
  return request({ url: `/novels/volumes/${volumeId}`, method: 'delete' })
}

// 幕
export function getActsByNovel(id) {
  return request({ url: `/novels/${id}/acts`, method: 'get' })
}

export function getActsByVolume(volumeId) {
  return request({ url: `/novels/volumes/${volumeId}/acts`, method: 'get' })
}

export function createAct(id, data) {
  return request({ url: `/novels/${id}/acts`, method: 'post', data })
}

export function updateAct(actId, data) {
  return request({ url: `/novels/acts/${actId}`, method: 'put', data })
}

export function deleteAct(actId) {
  return request({ url: `/novels/acts/${actId}`, method: 'delete' })
}
