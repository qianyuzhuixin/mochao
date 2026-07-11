import { startPractice, updateProgress, completePractice } from '@/api/practice'

const state = {
  activeSession: null,
  currentBook: null
}

const mutations = {
  SET_ACTIVE_SESSION(state, session) {
    state.activeSession = session
  },
  SET_CURRENT_BOOK(state, book) {
    state.currentBook = book
  }
}

const actions = {
  async startPractice({ commit }, data) {
    const res = await startPractice(data)
    commit('SET_ACTIVE_SESSION', res)
    return res
  },

  async saveProgress({ state }, data) {
    if (state.activeSession) {
      return await updateProgress(state.activeSession.id, data)
    }
  },

  async completePractice({ commit, state }, data) {
    if (state.activeSession) {
      const res = await completePractice(state.activeSession.id, data)
      commit('SET_ACTIVE_SESSION', null)
      return res
    }
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
