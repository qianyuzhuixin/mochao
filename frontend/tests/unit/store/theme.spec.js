/**
 * Vuex theme store 测试
 *
 * 覆盖: mutations(SET_THEME) + getters(currentTheme) + actions(initTheme/changeTheme)
 */

import themeModule from '@/store/modules/theme'

// Mock 工具函数
jest.mock('@/utils/theme', () => ({
  getTheme: jest.fn(() => 'light'),
  setTheme: jest.fn(),
  initTheme: jest.fn()
}))

const { getTheme, setTheme } = require('@/utils/theme')

describe('theme store - mutations', () => {
  test('SET_THEME_切换到dark主题', () => {
    const state = { currentTheme: 'light' }
    themeModule.mutations.SET_THEME(state, 'dark')
    expect(state.currentTheme).toBe('dark')
  })

  test('SET_THEME_切换到eye-care主题', () => {
    const state = { currentTheme: 'dark' }
    themeModule.mutations.SET_THEME(state, 'eye-care')
    expect(state.currentTheme).toBe('eye-care')
  })
})

describe('theme store - getters', () => {
  test('currentTheme_返回当前主题', () => {
    const state = { currentTheme: 'eye-care' }
    expect(themeModule.getters.currentTheme(state)).toBe('eye-care')
  })
})

describe('theme store - actions', () => {
  let commit, dispatch

  beforeEach(() => {
    commit = jest.fn()
    dispatch = jest.fn()
    jest.clearAllMocks()
  })

  test('initTheme_从localStorage读取主题', () => {
    getTheme.mockReturnValue('dark')

    themeModule.actions.initTheme({ commit, dispatch })

    expect(commit).toHaveBeenCalledWith('SET_THEME', 'dark')
    // setTheme 在 mutation SET_THEME 中调用，mock commit 不会触发真实 mutation
  })

  test('initTheme_无存储时默认light', () => {
    // 真实场景: getTheme() 在 localStorage 无值时返回 'light'
    getTheme.mockReturnValue('light')

    themeModule.actions.initTheme({ commit, dispatch })

    expect(commit).toHaveBeenCalledWith('SET_THEME', 'light')
  })

  test('changeTheme_切换到新主题', async () => {
    await themeModule.actions.changeTheme({ commit, dispatch }, 'eye-care')

    expect(commit).toHaveBeenCalledWith('SET_THEME', 'eye-care')
    // action 通过 commit mutation 间接触发 setTheme
    // 真正触发 setTheme 的是 mutation SET_THEME，这里验证 commit 参数即可
  })
})
