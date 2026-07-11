import request from '@/utils/request'

export function getDashboard() {
  return request({ url: '/admin/dashboard', method: 'get' })
}

export function getUsers(params) {
  return request({ url: '/admin/users', method: 'get', params })
}

export function updateUserStatus(id, status) {
  return request({ url: `/admin/users/${id}/status`, method: 'put', data: { status } })
}

export function createBook(data) {
  return request({ url: '/admin/books', method: 'post', data })
}

export function updateBook(id, data) {
  return request({ url: `/admin/books/${id}`, method: 'put', data })
}

export function deleteBook(id) {
  return request({ url: `/admin/books/${id}`, method: 'delete' })
}

export function importBooks(data) {
  return request({ url: '/admin/books/import', method: 'post', data })
}
