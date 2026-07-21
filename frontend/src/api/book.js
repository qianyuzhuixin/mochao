import request from '@/utils/request'

export function getBooks(params) {
  return request({
    url: '/books',
    method: 'get',
    params
  })
}

export function getBookById(id) {
  return request({
    url: `/books/${id}`,
    method: 'get'
  })
}

export function getCategories() {
  return request({
    url: '/books/categories',
    method: 'get'
  })
}

export function getMyBooks(params) {
  return request({
    url: '/books/my',
    method: 'get',
    params
  })
}

export function createBook(data) {
  return request({
    url: '/books',
    method: 'post',
    data
  })
}

export function updateBook(id, data) {
  return request({
    url: `/books/${id}`,
    method: 'put',
    data
  })
}

export function deleteBook(id) {
  return request({
    url: `/books/${id}`,
    method: 'delete'
  })
}

export function parseFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/books/parse-file',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getChapters(bookId) {
  return request({
    url: `/books/${bookId}/chapters`,
    method: 'get'
  })
}

export function getBookContent(bookId, chapterIndex) {
  return request({
    url: `/books/${bookId}/content`,
    method: 'get',
    params: { chapterIndex }
  })
}
