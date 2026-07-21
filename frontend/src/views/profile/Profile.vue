<template>
  <DefaultLayout>
    <div class="profile-page page-container">
      <h1 class="page-title">个人中心</h1>
      <el-row :gutter="24">
        <el-col :xs="24" :sm="6">
          <div class="profile-sidebar">
            <div class="profile-avatar">
              <el-avatar :size="80" icon="el-icon-user-solid" />
              <h3 class="profile-name">{{ userInfo.nickname || userInfo.username || '用户' }}</h3>
              <p class="profile-email">{{ userInfo.email || '' }}</p>
            </div>
            <el-menu :default-active="activeMenu" router class="profile-menu">
              <el-menu-item index="/profile/info">
                <i class="el-icon-user" />
                <span>个人信息</span>
              </el-menu-item>
              <el-menu-item index="/profile/materials">
                <i class="el-icon-reading" />
                <span>我的素材</span>
              </el-menu-item>
              <el-menu-item index="/profile/history">
                <i class="el-icon-time" />
                <span>练习历史</span>
              </el-menu-item>
              <el-menu-item index="/profile/settings">
                <i class="el-icon-setting" />
                <span>偏好设置</span>
              </el-menu-item>
              <el-menu-item index="/profile/ai-config">
                <i class="el-icon-cpu" />
                <span>AI 模型配置</span>
              </el-menu-item>
              <el-menu-item index="/profile/prompt-templates">
                <i class="el-icon-edit-outline" />
                <span>AI 提示词模板</span>
              </el-menu-item>
            </el-menu>
          </div>
        </el-col>
        <el-col :xs="24" :sm="18">
          <div class="profile-content">
            <router-view />
          </div>
        </el-col>
      </el-row>
    </div>
  </DefaultLayout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { mapGetters } from 'vuex'

export default {
  name: 'Profile',
  components: { DefaultLayout },
  computed: {
    ...mapGetters('auth', ['userInfo']),
    activeMenu() {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-page {
  .profile-sidebar {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    overflow: hidden;

    .profile-avatar {
      text-align: center;
      padding: #{$spacing-xl} #{$spacing-md};
      border-bottom: 1px solid var(--color-border);

      .profile-name {
        font-size: $font-size-md;
        font-weight: 600;
        color: var(--color-text);
        margin-top: #{$spacing-sm};
      }

      .profile-email {
        font-size: $font-size-sm;
        color: var(--color-text-secondary);
      }
    }

    .profile-menu {
      border-right: none;
    }
  }

  .profile-content {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-lg};
    min-height: 500px;
  }
}
</style>
