import request from '@/utils/request'

export function getMusicList(page = 1, size = 20) {
  return request({
    url: '/music',
    method: 'get',
    params: { page, size }
  })
}

export function getFavoriteMusic(page = 1, size = 100) {
  return request({
    url: '/music/favorites',
    method: 'get',
    params: { page, size }
  })
}

export function toggleFavorite(id) {
  return request({
    url: `/music/${id}/favorite`,
    method: 'post'
  })
}

export function uploadMusic(file, title, artist) {
  const formData = new FormData()
  formData.append('file', file)
  if (title) formData.append('title', title)
  if (artist) formData.append('artist', artist)
  return request({
    url: '/music/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function deleteMusic(id) {
  return request({
    url: `/music/${id}`,
    method: 'delete'
  })
}
