import { getNovels, getNovelById } from '@/api/novel'

const state = {
  currentNovel: null,
  novelList: []
}

const mutations = {
  SET_CURRENT_NOVEL(state, novel) {
    state.currentNovel = novel
  },
  SET_NOVEL_LIST(state, list) {
    state.novelList = list
  }
}

const actions = {
  async fetchNovels({ commit }, params) {
    const res = await getNovels(params)
    commit('SET_NOVEL_LIST', res.list || res || [])
    return res
  },

  async fetchNovelDetail({ commit }, id) {
    const res = await getNovelById(id)
    commit('SET_CURRENT_NOVEL', res)
    return res
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
