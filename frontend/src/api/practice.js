import request from '@/utils/request'

export function startPractice(data) {
  return request({
    url: '/practice/start',
    method: 'post',
    data
  })
}

export function updateProgress(id, data) {
  return request({
    url: `/practice/${id}/progress`,
    method: 'put',
    data
  })
}

export function pausePractice(id) {
  return request({
    url: `/practice/${id}/pause`,
    method: 'post'
  })
}

export function resumePractice(id) {
  return request({
    url: `/practice/${id}/resume`,
    method: 'post'
  })
}

export function completePractice(id, data) {
  return request({
    url: `/practice/${id}/complete`,
    method: 'post',
    data
  })
}

export function getActivePractice() {
  return request({
    url: '/practice/active',
    method: 'get'
  })
}

export function getPracticeHistory(params) {
  return request({
    url: '/practice/history',
    method: 'get',
    params
  })
}
