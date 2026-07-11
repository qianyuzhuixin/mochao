<template>
  <el-container class="admin-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="admin-aside">
      <div class="admin-logo">
        <span v-if="!collapsed">墨抄管理后台</span>
        <span v-else>墨</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <i class="el-icon-data-line" />
          <span slot="title">数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/books">
          <i class="el-icon-reading" />
          <span slot="title">素材管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <i class="el-icon-user" />
          <span slot="title">用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <i
            :class="collapsed ? 'el-icon-s-unfold' : 'el-icon-s-fold'"
            class="collapse-btn"
            @click="collapsed = !collapsed"
          />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="admin-info">
              <el-avatar :size="32" icon="el-icon-user-solid" />
              <span class="admin-name">{{ userInfo.nickname || userInfo.username || '管理员' }}</span>
              <i class="el-icon-arrow-down" />
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="home">返回前台</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'AdminLayout',
  data() {
    return {
      collapsed: false
    }
  },
  computed: {
    ...mapGetters('auth', ['userInfo']),
    activeMenu() {
      return this.$route.path
    }
  },
  methods: {
    handleCommand(command) {
      if (command === 'logout') {
        this.$store.dispatch('auth/logout').then(() => {
          this.$router.push('/admin/login')
        })
      } else if (command === 'home') {
        this.$router.push('/')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-aside {
  background-color: var(--color-card-bg);
  border-right: 1px solid var(--color-border);
  transition: width 0.3s ease;
  overflow: hidden;

  .admin-logo {
    height: #{$header-height};
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: $font-size-lg;
    font-weight: 700;
    color: var(--color-primary);
    border-bottom: 1px solid var(--color-border);
    white-space: nowrap;
  }

  .admin-menu {
    border-right: none;
    background-color: transparent;
  }
}

.admin-header {
  height: #{$header-height} !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  padding: 0 #{$spacing-lg};

  .collapse-btn {
    font-size: 20px;
    cursor: pointer;
    color: var(--color-text-secondary);

    &:hover {
      color: var(--color-primary);
    }
  }

  .admin-info {
    display: flex;
    align-items: center;
    gap: #{$spacing-xs};
    cursor: pointer;
    color: var(--color-text);
  }
}

.admin-main {
  background-color: var(--color-bg);
  padding: #{$spacing-lg};
}
</style>
