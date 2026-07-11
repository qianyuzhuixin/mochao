import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

import * as echarts from 'echarts'

import './styles/global.scss'

NProgress.configure({ showSpinner: false })

Vue.prototype.$echarts = echarts
Vue.use(ElementUI)

Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
