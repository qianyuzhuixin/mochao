const TOKEN_KEY = 'mochao_token'
const USER_KEY = 'mochao_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  return localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  return localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo() {
  const info = localStorage.getItem(USER_KEY)
  try {
    return info ? JSON.parse(info) : null
  } catch (e) {
    return null
  }
}

export function setUserInfo(info) {
  return localStorage.setItem(USER_KEY, JSON.stringify(info))
}

export function removeUserInfo() {
  return localStorage.removeItem(USER_KEY)
}
