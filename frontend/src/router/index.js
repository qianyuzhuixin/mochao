import Vue from 'vue'
import VueRouter from 'vue-router'
import NProgress from 'nprogress'
import { getToken } from '@/utils/auth'
import store from '@/store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '墨抄 - 网文创作练笔平台' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/library',
    name: 'Library',
    component: () => import('@/views/library/Library.vue'),
    meta: { title: '书库', requiresAuth: true }
  },
  {
    path: '/library/:id',
    name: 'BookDetail',
    component: () => import('@/views/library/BookDetail.vue'),
    meta: { title: '素材详情', requiresAuth: true }
  },
  {
    path: '/practice/:sessionId',
    name: 'Practice',
    component: () => import('@/views/practice/Practice.vue'),
    meta: { title: '抄书练习', requiresAuth: true }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/Dashboard.vue'),
    meta: { title: '数据看板', requiresAuth: true }
  },
  {
    path: '/collections',
    name: 'Collections',
    component: () => import('@/views/collections/Collections.vue'),
    meta: { title: '好词好句', requiresAuth: true }
  },
  {
    path: '/music',
    name: 'MusicManager',
    component: () => import('@/views/music/MusicManager.vue'),
    meta: { title: '背景音乐', requiresAuth: true }
  },
  {
    path: '/novels',
    name: 'NovelList',
    component: () => import('@/views/novels/NovelList.vue'),
    meta: { title: '我的小说', requiresAuth: true }
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('@/views/ranking/Ranking.vue'),
    meta: { title: '扫榜分析', requiresAuth: true }
  },
  {
    path: '/novels/create',
    name: 'NovelCreate',
    component: () => import('@/views/novels/NovelCreate.vue'),
    meta: { title: '创建小说', requiresAuth: true }
  },
  {
    path: '/novels/:id',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'NovelWorkspace',
        component: () => import('@/views/novels/NovelWorkspace.vue'),
        meta: { title: '工作台', requiresAuth: true }
      },
      {
        path: 'outline',
        name: 'OutlineEditor',
        component: () => import('@/views/novels/OutlineEditor.vue'),
        meta: { title: '大纲编辑', requiresAuth: true }
      },
      {
        path: 'worldview',
        name: 'WorldviewEditor',
        component: () => import('@/views/novels/WorldviewEditor.vue'),
        meta: { title: '世界观编辑', requiresAuth: true }
      },
      {
        path: 'characters',
        name: 'CharacterList',
        component: () => import('@/views/novels/CharacterList.vue'),
        meta: { title: '人物设定', requiresAuth: true }
      },
      {
        path: 'items',
        name: 'ItemList',
        component: () => import('@/views/novels/ItemList.vue'),
        meta: { title: '物品设定', requiresAuth: true }
      },
      {
        path: 'chapter-outlines',
        name: 'ChapterOutlineList',
        component: () => import('@/views/novels/ChapterOutlineList.vue'),
        meta: { title: '章纲管理', requiresAuth: true }
      },
      {
        path: 'chapters/:chId',
        name: 'ChapterEditor',
        component: () => import('@/views/novels/ChapterEditor.vue'),
        meta: { title: '章节编辑', requiresAuth: true }
      },
      {
        path: 'progress',
        name: 'WritingProgress',
        component: () => import('@/views/novels/WritingProgress.vue'),
        meta: { title: '写作进度', requiresAuth: true }
      }
    ]
  },
  {
    path: '/profile',
    component: () => import('@/views/profile/Profile.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/profile/info'
      },
      {
        path: 'info',
        name: 'ProfileInfo',
        component: () => import('@/views/profile/ProfileInfo.vue'),
        meta: { title: '个人信息', requiresAuth: true }
      },
      {
        path: 'materials',
        name: 'ProfileMaterials',
        component: () => import('@/views/profile/ProfileMaterials.vue'),
        meta: { title: '我的素材', requiresAuth: true }
      },
      {
        path: 'history',
        name: 'ProfileHistory',
        component: () => import('@/views/profile/ProfileHistory.vue'),
        meta: { title: '练习历史', requiresAuth: true }
      },
      {
        path: 'settings',
        name: 'ProfileSettings',
        component: () => import('@/views/profile/ProfileSettings.vue'),
        meta: { title: '偏好设置', requiresAuth: true }
      },
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('@/views/profile/AiConfig.vue'),
        meta: { title: 'AI 模型配置', requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/AdminLogin.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { title: '管理后台', requiresAdmin: true }
      },
      {
        path: 'books',
        name: 'AdminBooks',
        component: () => import('@/views/admin/AdminBooks.vue'),
        meta: { title: '素材管理', requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { title: '用户管理', requiresAdmin: true }
      }
    ]
  },
  {
    path: '*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes,
  scrollBehavior() {
    return { x: 0, y: 0 }
  }
})

const DEFAULT_TITLE = '墨抄 - 网文创作练笔平台'

router.beforeEach((to, from, next) => {
  NProgress.start()
  document.title = to.meta.title ? `${to.meta.title} - 墨抄` : DEFAULT_TITLE

  const token = getToken()

  // 已登录访问登录/注册页，跳转首页
  if (token && (to.path === '/login' || to.path === '/register')) {
    next('/')
    return
  }

  // 需要登录的路由
  if (to.meta.requiresAuth && !token) {
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  // 需要管理员权限（统一从 Vuex store 读取，避免 localStorage 角色判断不一致）
  if (to.meta.requiresAdmin) {
    if (!token || !store.getters['auth/isAdmin']) {
      next('/admin/login')
      return
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
