import request from '@/utils/request'

export function getMusicList(page = 1, size = 20) {
  return request({
    url: '/music',
    method: 'get',
    params: { page, size }
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
