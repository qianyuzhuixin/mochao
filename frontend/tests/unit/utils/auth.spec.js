import { getToken, setToken, removeToken, getUserInfo, setUserInfo, removeUserInfo } from '@/utils/auth'

// Mock localStorage
beforeEach(() => {
  localStorage.clear()
})

describe('auth.js - Token 管理', () => {
  test('setToken 应将 token 存入 localStorage', () => {
    setToken('test-jwt-token-123')
    expect(localStorage.getItem('mochao_token')).toBe('test-jwt-token-123')
  })

  test('getToken 应从 localStorage 读取 token', () => {
    localStorage.setItem('mochao_token', 'my-token')
    expect(getToken()).toBe('my-token')
  })

  test('getToken 未设置时应返回 null', () => {
    expect(getToken()).toBeNull()
  })

  test('removeToken 应清除 localStorage 中的 token', () => {
    setToken('some-token')
    removeToken()
    expect(localStorage.getItem('mochao_token')).toBeNull()
    expect(getToken()).toBeNull()
  })
})

describe('auth.js - UserInfo 管理', () => {
  test('setUserInfo 应将对象 JSON 序列化存入 localStorage', () => {
    const user = { id: 1, username: 'zhangsan', role: 'ADMIN' }
    setUserInfo(user)
    expect(localStorage.getItem('mochao_user')).toBe(JSON.stringify(user))
  })

  test('getUserInfo 应从 localStorage 反序列化读取对象', () => {
    const user = { id: 2, username: 'lisi', role: 'USER' }
    setUserInfo(user)
    expect(getUserInfo()).toEqual(user)
  })

  test('getUserInfo 未设置时应返回 null', () => {
    expect(getUserInfo()).toBeNull()
  })

  test('getUserInfo 遇到损坏 JSON 时应返回 null（不抛异常）', () => {
    localStorage.setItem('mochao_user', '{invalid-json!!!}')
    expect(getUserInfo()).toBeNull()
  })

  test('removeUserInfo 应清除 localStorage 中的用户信息', () => {
    setUserInfo({ id: 1, username: 'test' })
    removeUserInfo()
    expect(localStorage.getItem('mochao_user')).toBeNull()
    expect(getUserInfo()).toBeNull()
  })
})
