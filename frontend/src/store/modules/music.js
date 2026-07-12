import { getMusicList } from '@/api/music'

const state = {
  musicList: [],
  currentTrack: null,
  isPlaying: false,
  volume: parseInt(localStorage.getItem('mochao_music_volume') || '60')
}

const mutations = {
  SET_MUSIC_LIST(state, list) {
    state.musicList = list
  },
  SET_CURRENT_TRACK(state, track) {
    state.currentTrack = track
  },
  SET_PLAYING(state, playing) {
    state.isPlaying = playing
  },
  SET_VOLUME(state, vol) {
    state.volume = vol
    localStorage.setItem('mochao_music_volume', vol)
  }
}

const actions = {
  async fetchMusicList({ commit }) {
    try {
      const res = await getMusicList(1, 100)
      const list = (res && res.records) || []
      commit('SET_MUSIC_LIST', list)
      return list
    } catch {
      return []
    }
  },

  playTrack({ commit, state }, track) {
    commit('SET_CURRENT_TRACK', track)
    commit('SET_PLAYING', true)
  },

  pauseTrack({ commit }) {
    commit('SET_PLAYING', false)
  },

  togglePlay({ commit, state }) {
    commit('SET_PLAYING', !state.isPlaying)
  },

  playNext({ commit, state }) {
    if (!state.musicList.length || !state.currentTrack) return
    const idx = state.musicList.findIndex(m => m.id === state.currentTrack.id)
    const next = state.musicList[(idx + 1) % state.musicList.length]
    commit('SET_CURRENT_TRACK', next)
    commit('SET_PLAYING', true)
  },

  playPrev({ commit, state }) {
    if (!state.musicList.length || !state.currentTrack) return
    const idx = state.musicList.findIndex(m => m.id === state.currentTrack.id)
    const prev = state.musicList[(idx - 1 + state.musicList.length) % state.musicList.length]
    commit('SET_CURRENT_TRACK', prev)
    commit('SET_PLAYING', true)
  }
}

const getters = {
  musicList: state => state.musicList,
  currentTrack: state => state.currentTrack,
  isPlaying: state => state.isPlaying,
  volume: state => state.volume,
  hasTrack: state => !!state.currentTrack
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
