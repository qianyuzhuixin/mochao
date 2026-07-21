/**
 * Vuex auth store 测试
 *
 * 覆盖: mutations(SET_TOKEN/SET_USER_INFO/CLEAR_AUTH) + getters(isLoggedIn/isAdmin)
 * + actions(login/logout/getProfile 的 mutation dispatch)
 *
 * 编写指南 (给团队成员):
 *  1. 测试命名: 方法名_场景_预期结果
 *  2. 结构: Given(准备数据) -> When(执行操作) -> Then(验证结果)
 *  3. Actions 用异步测试 (async/await 或 done 回调)
 *  4. 需要 Mock 的依赖在 beforeEach 中 mock，afterEach 中清理
 */

import authModule from '@/store/modules/auth'

// Mock API 调用
jest.mock('@/api/auth', () => ({
  login: jest.fn(),
  logout: jest.fn(),
  getProfile: jest.fn()
}))

// Mock 工具函数
jest.mock('@/utils/auth', () => ({
  getToken: jest.fn(() => null),
  setToken: jest.fn(),
  removeToken: jest.fn(),
  getUserInfo: jest.fn(() => null),
  setUserInfo: jest.fn(),
  removeUserInfo: jest.fn()
}))

const { login, logout, getProfile } = require('@/api/auth')
const { setToken, removeToken, setUserInfo, removeUserInfo } = require('@/utils/auth')

describe('auth store - mutations', () => {
  let state

  beforeEach(() => {
    state = { token: '', userInfo: {} }
  })

  test('SET_TOKEN_更新token', () => {
    authModule.mutations.SET_TOKEN(state, 'new-jwt-token')
    expect(state.token).toBe('new-jwt-token')
  })

  test('SET_TOKEN_空字符串_更新成功', () => {
    state.token = 'old-token'
    authModule.mutations.SET_TOKEN(state, '')
    expect(state.token).toBe('')
  })

  test('SET_USER_INFO_更新用户信息', () => {
    const userInfo = { id: 1, username: 'testuser', role: 'USER' }
    authModule.mutations.SET_USER_INFO(state, userInfo)
    expect(state.userInfo).toEqual(userInfo)
  })

  test('CLEAR_AUTH_清除token和用户信息', () => {
    state.token = 'some-token'
    state.userInfo = { id: 1, username: 'testuser', role: 'ADMIN' }
    authModule.mutations.CLEAR_AUTH(state)
    expect(state.token).toBe('')
    expect(state.userInfo).toEqual({})
  })
})

describe('auth store - getters', () => {
  test('isLoggedIn_有token时返回true', () => {
    const state = { token: 'valid-token', userInfo: {} }
    expect(authModule.getters.isLoggedIn(state)).toBe(true)
  })

  test('isLoggedIn_空token时返回false', () => {
    const state = { token: '', userInfo: {} }
    expect(authModule.getters.isLoggedIn(state)).toBe(false)
  })

  test('isAdmin_管理员角色返回true', () => {
    const state = { token: 'valid-token', userInfo: { id: 1, role: 'ADMIN' } }
    expect(authModule.getters.isAdmin(state)).toBe(true)
  })

  test('isAdmin_普通用户角色返回false', () => {
    const state = { token: 'valid-token', userInfo: { id: 1, role: 'USER' } }
    expect(authModule.getters.isAdmin(state)).toBe(false)
  })

  test('isAdmin_无userInfo时返回false', () => {
    const state = { token: 'valid-token', userInfo: null }
    // `null && anything` 返回 null，但 null 是 falsy，对业务逻辑来说等同于 false
    expect(authModule.getters.isAdmin(state)).toBeFalsy()
  })
})

describe('auth store - actions', () => {
  let commit

  beforeEach(() => {
    commit = jest.fn()
    jest.clearAllMocks()
  })

  test('login_成功_提交token和userInfo并持久化', async () => {
    const mockResponse = { token: 'login-token', userInfo: { id: 1, username: 'admin', role: 'ADMIN' } }
    login.mockResolvedValue(mockResponse)

    const result = await authModule.actions.login({ commit }, { username: 'admin', password: 'pass123' })

    expect(commit).toHaveBeenCalledWith('SET_TOKEN', 'login-token')
    expect(commit).toHaveBeenCalledWith('SET_USER_INFO', mockResponse.userInfo)
    expect(setToken).toHaveBeenCalledWith('login-token')
    expect(setUserInfo).toHaveBeenCalledWith(mockResponse.userInfo)
    expect(result).toEqual(mockResponse)
  })

  test('logout_成功_清除认证信息', async () => {
    logout.mockResolvedValue({})

    await authModule.actions.logout({ commit })

    expect(commit).toHaveBeenCalledWith('CLEAR_AUTH')
    expect(removeToken).toHaveBeenCalled()
    expect(removeUserInfo).toHaveBeenCalled()
  })

  test('logout_API失败_仍然清除本地认证', async () => {
    logout.mockRejectedValue(new Error('Network error'))

    await authModule.actions.logout({ commit })

    // 即使 API 报错，也应清除本地认证信息
    expect(commit).toHaveBeenCalledWith('CLEAR_AUTH')
    expect(removeToken).toHaveBeenCalled()
    expect(removeUserInfo).toHaveBeenCalled()
  })

  test('getProfile_成功_更新用户信息', async () => {
    const userProfile = { id: 1, username: 'admin', role: 'ADMIN', email: 'admin@test.com' }
    getProfile.mockResolvedValue(userProfile)

    const result = await authModule.actions.getProfile({ commit })

    expect(commit).toHaveBeenCalledWith('SET_USER_INFO', userProfile)
    expect(setUserInfo).toHaveBeenCalledWith(userProfile)
    expect(result).toEqual(userProfile)
  })
})
