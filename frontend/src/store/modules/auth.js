import { login as loginApi, logout as logoutApi, getProfile } from '@/api/auth'
import { getToken, setToken, removeToken, getUserInfo, setUserInfo, removeUserInfo } from '@/utils/auth'

const state = {
  token: getToken() || '',
  userInfo: getUserInfo() || {}
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token
  },
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo
  },
  CLEAR_AUTH(state) {
    state.token = ''
    state.userInfo = {}
  }
}

const actions = {
  async login({ commit }, data) {
    const res = await loginApi(data)
    const { token, userInfo } = res
    commit('SET_TOKEN', token)
    commit('SET_USER_INFO', userInfo)
    setToken(token)
    setUserInfo(userInfo)
    return res
  },

  async logout({ commit }) {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略登出接口错误
    }
    commit('CLEAR_AUTH')
    removeToken()
    removeUserInfo()
  },

  async getProfile({ commit }) {
    const res = await getProfile()
    commit('SET_USER_INFO', res)
    setUserInfo(res)
    return res
  }
}

const getters = {
  isLoggedIn: state => !!state.token,
  isAdmin: state => state.userInfo && state.userInfo.role === 'admin',
  userInfo: state => state.userInfo
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
