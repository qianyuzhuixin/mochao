import { getMusicList, getFavoriteMusic, toggleFavorite } from '@/api/music'

const PLAY_MODE_SEQUENCE = 'sequence'
const PLAY_MODE_SHUFFLE = 'shuffle'
const PLAY_MODE_FAVORITE = 'favorite'

const state = {
  musicList: [],
  favoriteList: [],
  currentTrack: null,
  isPlaying: false,
  volume: parseInt(localStorage.getItem('mochao_music_volume') || '60'),
  playMode: localStorage.getItem('mochao_music_play_mode') || PLAY_MODE_SEQUENCE,
  shuffleQueue: [],
  shuffleIndex: -1
}

const mutations = {
  SET_MUSIC_LIST(state, list) {
    state.musicList = list
  },
  SET_FAVORITE_LIST(state, list) {
    state.favoriteList = list
  },
  SET_CURRENT_TRACK(state, track) {
    state.currentTrack = track
    if (track) {
      localStorage.setItem('mochao_music_track_id', track.id)
    } else {
      localStorage.removeItem('mochao_music_track_id')
    }
  },
  SET_PLAYING(state, playing) {
    state.isPlaying = playing
  },
  SET_VOLUME(state, vol) {
    state.volume = vol
    localStorage.setItem('mochao_music_volume', vol)
  },
  SET_PLAY_MODE(state, mode) {
    state.playMode = mode
    localStorage.setItem('mochao_music_play_mode', mode)
    if (mode !== PLAY_MODE_SHUFFLE) {
      state.shuffleQueue = []
      state.shuffleIndex = -1
    }
  },
  SET_SHUFFLE_QUEUE(state, queue) {
    state.shuffleQueue = queue
  },
  SET_SHUFFLE_INDEX(state, idx) {
    state.shuffleIndex = idx
  },
  UPDATE_TRACK_FAVORITE(state, { id, favorite }) {
    const updateFn = (list) => {
      const item = list.find(m => m.id === id)
      if (item) item.favorite = favorite
    }
    updateFn(state.musicList)
    updateFn(state.favoriteList)
    if (state.currentTrack && state.currentTrack.id === id) {
      state.currentTrack = { ...state.currentTrack, favorite }
    }
  }
}

const actions = {
  async fetchMusicList({ commit }) {
    try {
      const res = await getMusicList(1, 200)
      const list = (res && res.records) || []
      commit('SET_MUSIC_LIST', list)
      return list
    } catch {
      return []
    }
  },

  async fetchFavoriteMusic({ commit }) {
    try {
      const res = await getFavoriteMusic(1, 200)
      const list = (res && res.records) || []
      commit('SET_FAVORITE_LIST', list)
      return list
    } catch {
      return []
    }
  },

  async toggleFavoriteTrack({ commit, dispatch }, id) {
    const res = await toggleFavorite(id)
    const favorite = res && res.favorite != null ? res.favorite : 1
    commit('UPDATE_TRACK_FAVORITE', { id, favorite })
    await dispatch('fetchFavoriteMusic')
    return favorite
  },

  playTrack({ commit }, track) {
    commit('SET_CURRENT_TRACK', track)
    commit('SET_PLAYING', true)
  },

  pauseTrack({ commit }) {
    commit('SET_PLAYING', false)
  },

  togglePlay({ commit, state }) {
    commit('SET_PLAYING', !state.isPlaying)
  },

  setPlayMode({ commit }, mode) {
    commit('SET_PLAY_MODE', mode)
  },

  cyclePlayMode({ commit, state }) {
    const modes = [PLAY_MODE_SEQUENCE, PLAY_MODE_SHUFFLE, PLAY_MODE_FAVORITE]
    const idx = modes.indexOf(state.playMode)
    const next = modes[(idx + 1) % modes.length]
    commit('SET_PLAY_MODE', next)
    return next
  },

  playNext({ commit, state, getters }) {
    const list = getters.activeList
    if (!list.length || !state.currentTrack) return

    if (state.playMode === PLAY_MODE_SHUFFLE) {
      // 队列为空或已到末尾，生成新队列
      if (state.shuffleQueue.length === 0 || state.shuffleIndex >= state.shuffleQueue.length - 1) {
        const otherIds = list.filter(m => m.id !== state.currentTrack.id).map(m => m.id)
        shuffleArray(otherIds)
        const queue = [state.currentTrack.id, ...otherIds]
        commit('SET_SHUFFLE_QUEUE', queue)
        commit('SET_SHUFFLE_INDEX', 0)
      }
      const nextIdx = state.shuffleIndex + 1
      const nextId = state.shuffleQueue[nextIdx]
      if (nextId) {
        const next = list.find(m => m.id === nextId)
        if (next) {
          commit('SET_SHUFFLE_INDEX', nextIdx)
          commit('SET_CURRENT_TRACK', next)
          commit('SET_PLAYING', true)
          return
        }
      }
    }

    const idx = list.findIndex(m => m.id === state.currentTrack.id)
    const next = list[(idx + 1) % list.length]
    commit('SET_CURRENT_TRACK', next)
    commit('SET_PLAYING', true)
  },

  playPrev({ commit, state, getters }) {
    const list = getters.activeList
    if (!list.length || !state.currentTrack) return

    if (state.playMode === PLAY_MODE_SHUFFLE && state.shuffleQueue.length > 0 && state.shuffleIndex > 0) {
      const prevIdx = state.shuffleIndex - 1
      const prevId = state.shuffleQueue[prevIdx]
      const prev = list.find(m => m.id === prevId)
      if (prev) {
        commit('SET_SHUFFLE_INDEX', prevIdx)
        commit('SET_CURRENT_TRACK', prev)
        commit('SET_PLAYING', true)
        return
      }
    }

    const idx = list.findIndex(m => m.id === state.currentTrack.id)
    const prev = list[(idx - 1 + list.length) % list.length]
    commit('SET_CURRENT_TRACK', prev)
    commit('SET_PLAYING', true)
  }
}

function shuffleArray(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
}

const getters = {
  musicList: state => state.musicList,
  favoriteList: state => state.favoriteList,
  currentTrack: state => state.currentTrack,
  isPlaying: state => state.isPlaying,
  volume: state => state.volume,
  playMode: state => state.playMode,
  hasTrack: state => !!state.currentTrack,
  activeList: state => {
    if (state.playMode === PLAY_MODE_FAVORITE && state.favoriteList.length > 0) {
      return state.favoriteList
    }
    return state.musicList
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
