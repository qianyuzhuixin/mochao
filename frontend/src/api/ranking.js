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

/** 下载整本小说到素材库 */
export function downloadBook(data) {
  return request({ url: '/ranking/download-book', method: 'post', data, timeout: 360000 })
}
