<template>
  <div id="app" :data-theme="currentTheme">
    <!-- 全局导航栏（单例，跨页面不销毁） -->
    <template v-if="showHeader">
      <el-header class="layout-header">
        <div class="header-inner">
          <div class="header-left">
            <div class="logo" @click="$router.push('/')">
              <span class="logo-text">墨抄</span>
            </div>
            <el-menu
              :default-active="activeMenu"
              mode="horizontal"
              class="header-menu"
              @select="handleSelect"
            >
              <el-menu-item index="/library">书库</el-menu-item>
              <el-menu-item index="/collections">好词好句</el-menu-item>
              <el-menu-item index="/novels">我的小说</el-menu-item>
              <el-menu-item index="/dashboard">数据看板</el-menu-item>
              <el-menu-item index="/music">背景音乐</el-menu-item>
            </el-menu>
          </div>
          <div class="header-right">
            <!-- 音乐播放器：紧跟"背景音乐"菜单项后面 -->
            <music-player v-if="showMusicPlayer" />
            <theme-toggle />
            <el-dropdown v-if="isLoggedIn" @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" icon="el-icon-user-solid" />
                <span class="username">{{ userInfo.nickname || userInfo.username || '用户' }}</span>
                <i class="el-icon-arrow-down" />
              </span>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="/profile/ai-config">AI 模型配置</el-dropdown-item>
                <el-dropdown-item command="/dashboard">数据看板</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <div v-else class="auth-buttons">
              <el-button type="text" @click="$router.push('/login')">登录</el-button>
              <el-button type="primary" size="small" @click="$router.push('/register')">注册</el-button>
            </div>
          </div>
        </div>
      </el-header>

      <!-- 移动端菜单按钮 -->
      <div class="mobile-menu-btn" @click="drawerVisible = true">
        <i class="el-icon-menu" />
      </div>
      <el-drawer
        :visible.sync="drawerVisible"
        direction="ltr"
        size="60%"
        :show-close="false"
        title="导航"
      >
        <el-menu :default-active="activeMenu" @select="handleMobileSelect">
          <el-menu-item index="/library">书库</el-menu-item>
          <el-menu-item index="/collections">好词好句</el-menu-item>
          <el-menu-item index="/novels">我的小说</el-menu-item>
          <el-menu-item index="/dashboard">数据看板</el-menu-item>
          <el-menu-item index="/music">背景音乐</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-menu>
      </el-drawer>
    </template>

    <router-view />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import ThemeToggle from '@/components/common/ThemeToggle.vue'
import MusicPlayer from '@/components/practice/MusicPlayer.vue'

export default {
  name: 'App',
  components: { ThemeToggle, MusicPlayer },
  data() {
    return {
      drawerVisible: false,
      year: new Date().getFullYear()
    }
  },
  computed: {
    ...mapGetters('theme', ['currentTheme']),
    ...mapGetters('auth', ['isLoggedIn', 'userInfo']),
    showHeader() {
      const path = this.$route.path
      if (path === '/') return false
      if (path.startsWith('/login') || path.startsWith('/register')) return false
      if (path.startsWith('/admin')) return false
      return true
    },
    showMusicPlayer() {
      if (!this.isLoggedIn) return false
      const path = this.$route.path
      if (path.startsWith('/login') || path.startsWith('/register')) return false
      if (path.startsWith('/admin')) return false
      return true
    },
    activeMenu() {
      const path = this.$route.path
      if (path.startsWith('/novels')) return '/novels'
      if (path.startsWith('/library')) return '/library'
      if (path.startsWith('/collections')) return '/collections'
      if (path.startsWith('/dashboard')) return '/dashboard'
      if (path.startsWith('/music')) return '/music'
      if (path.startsWith('/profile')) return '/profile'
      return path
    }
  },
  created() {
    this.$store.dispatch('theme/initTheme')
  },
  methods: {
    handleSelect(index) {
      this.$router.push(index).catch(() => {})
    },
    handleMobileSelect(index) {
      this.$router.push(index).catch(() => {})
      this.drawerVisible = false
    },
    async handleCommand(command) {
      if (command === 'logout') {
        this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          await this.$store.dispatch('auth/logout')
          this.$message.success('已退出登录')
          this.$router.push('/')
        }).catch(() => {})
      } else {
        this.$router.push(command).catch(() => {})
      }
    }
  }
}
</script>

<style lang="scss">
#app {
  min-height: 100vh;
  background-color: var(--color-bg);
  color: var(--color-text);
  transition: background-color 0.3s ease, color 0.3s ease;
}

/* === 全局导航栏 === */
.layout-header {
  height: #{$header-height} !important;
  background-color: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: #{$z-index-header};

  .header-inner {
    max-width: #{$content-max-width};
    height: 100%;
    margin: 0 auto;
    padding: 0 #{$spacing-lg};
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    flex: 1;
    min-width: 0;
  }

  .logo {
    cursor: pointer;
    flex-shrink: 0;

    .logo-text {
      font-size: $font-size-xl;
      font-weight: 700;
      color: var(--color-primary);
      letter-spacing: 2px;
    }
  }

  .header-menu {
    border-bottom: none;
    flex-shrink: 0;

    ::v-deep .el-menu-item {
      font-size: 16px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    flex-shrink: 0;

    .user-info {
      display: flex;
      align-items: center;
      gap: #{$spacing-xs};
      cursor: pointer;
      color: var(--color-text);

      .username {
        font-size: $font-size-base;
        max-width: 80px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .auth-buttons {
      display: flex;
      align-items: center;
      gap: #{$spacing-xs};
    }
  }
}

.mobile-menu-btn {
  display: none;
  position: fixed;
  bottom: #{$spacing-lg};
  right: #{$spacing-lg};
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: var(--color-primary);
  color: #fff;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  box-shadow: var(--shadow-lg);
  z-index: #{$z-index-header};
}

@media (max-width: 768px) {
  .layout-header .header-menu {
    display: none;
  }
  .mobile-menu-btn {
    display: flex;
  }
  .layout-header .header-inner {
    padding: 0 #{$spacing-md};
  }
}
</style>
