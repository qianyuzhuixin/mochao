import { getTheme, setTheme } from '@/utils/theme'

const state = {
  currentTheme: getTheme()
}

const mutations = {
  SET_THEME(state, theme) {
    state.currentTheme = theme
    setTheme(theme)
  }
}

const actions = {
  initTheme({ commit }) {
    const theme = getTheme()
    commit('SET_THEME', theme)
  },

  changeTheme({ commit }, theme) {
    commit('SET_THEME', theme)
  }
}

const getters = {
  currentTheme: state => state.currentTheme
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
