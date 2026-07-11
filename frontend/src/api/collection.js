import request from '@/utils/request'

export function createCollection(data) {
  return request({
    url: '/collections',
    method: 'post',
    data
  })
}

export function getCollections(params) {
  return request({
    url: '/collections',
    method: 'get',
    params
  })
}

export function getCollectionById(id) {
  return request({
    url: `/collections/${id}`,
    method: 'get'
  })
}

export function updateCollection(id, data) {
  return request({
    url: `/collections/${id}`,
    method: 'put',
    data
  })
}

export function deleteCollection(id) {
  return request({
    url: `/collections/${id}`,
    method: 'delete'
  })
}

export function getTags() {
  return request({
    url: '/collections/tags',
    method: 'get'
  })
}

export function getDailyReview() {
  return request({
    url: '/collections/daily',
    method: 'get'
  })
}

export function exportCollections(format) {
  return request({
    url: '/collections/export',
    method: 'get',
    params: { format },
    responseType: 'blob'
  })
}

export function getCollectionStats() {
  return request({
    url: '/collections/stats',
    method: 'get'
  })
}
