import request from '@/utils/request'

export function getOverview() {
  return request({ url: '/statistics/overview', method: 'get' })
}

export function getTrend() {
  return request({ url: '/statistics/trend', method: 'get' })
}

export function getCheckIn() {
  return request({ url: '/statistics/check-in', method: 'get' })
}

export function getCalendar(params) {
  return request({ url: '/statistics/calendar', method: 'get', params })
}
