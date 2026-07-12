import Vue from 'vue'
import Vuex from 'vuex'
import auth from './modules/auth'
import theme from './modules/theme'
import practice from './modules/practice'
import novel from './modules/novel'
import music from './modules/music'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    auth,
    theme,
    practice,
    novel,
    music
  }
})
