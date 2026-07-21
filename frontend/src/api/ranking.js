import request from '@/utils/request'

/** 查询榜单快照 */
export function getRanking(params) {
  return request({ url: '/ranking', method: 'get', params })
}

/** 触发抓取（已有当天数据则服务端跳过） */
export function triggerScrape(platform, rankType) {
  return request({ url: '/ranking/scrape', method: 'post', params: { platform, rankType } })
}

/** 检查当天是否已有数据（控制抓取按钮显隐） */
export function checkTodayData(platform, rankType) {
  return request({ url: '/ranking/check-today', method: 'get', params: { platform, rankType } })
}

/** 获取某平台+榜单有数据的日期列表 */
export function getAvailableDates(platform, rankType) {
  return request({ url: '/ranking/available-dates', method: 'get', params: { platform, rankType } })
}

/** 搜索小说（按书名/作者，支持全平台或指定平台） */
export function searchBooks(params) {
  return request({ url: '/ranking/search', method: 'get', params })
}

/** 下载整本小说到素材库 */
export function downloadBook(data) {
  return request({ url: '/ranking/download-book', method: 'post', data, timeout: 360000 })
}

/** so-novel 全平台搜索（11 书源并发） */
export function sonovelSearch(data) {
  return request({ url: '/ranking/sonovel/search', method: 'post', data, timeout: 60000 })
}

/** so-novel 下载整本小说 */
export function sonovelDownloadBook(data) {
  return request({ url: '/ranking/sonovel/download-book', method: 'post', data, timeout: 360000 })
}

/**
 * so-novel 下载文件（TXT / HTML / PDF），直接返回文件 Blob，不存入数据库
 * @param {Object} data - { bookUrl, sourceName, format, maxChapters? }
 * @returns {Promise<Blob>} 文件 Blob
 */
export function sonovelDownloadFile(data) {
  return request({
    url: '/ranking/sonovel/download-file',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 360000
  })
}

/**
 * 下载小说文件（TXT / HTML / PDF）
 * @param {string} bookId - 书籍 ID
 * @param {string} format - 格式: 'txt' | 'html' | 'pdf'
 * @param {number} maxChapters - 最大章节数 (0 = 全部)
 * @returns {Promise<Blob>} 文件 Blob
 */
export function downloadFile(bookId, format, maxChapters = 0) {
  return request({
    url: '/ranking/download-file',
    method: 'get',
    params: { bookId, format, maxChapters },
    responseType: 'blob',
    timeout: 360000
  })
}
