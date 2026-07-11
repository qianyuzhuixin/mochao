<template>
  <div class="app-header">
    <div class="header-logo" @click="$router.push('/')">
      <span class="logo-text">墨抄</span>
    </div>
    <nav class="header-nav">
      <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
        {{ item.label }}
      </router-link>
    </nav>
    <div class="header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'AppHeader',
  data() {
    return {
      navItems: [
        { path: '/library', label: '书库' },
        { path: '/collections', label: '好词好句' },
        { path: '/novels', label: '我的小说' },
        { path: '/dashboard', label: '数据看板' }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: #{$header-height};
  padding: 0 #{$spacing-lg};
  background-color: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);

  .header-logo {
    cursor: pointer;

    .logo-text {
      font-size: $font-size-xl;
      font-weight: 700;
      color: var(--color-primary);
    }
  }

  .header-nav {
    display: flex;
    gap: #{$spacing-xl};

    .nav-item {
      color: var(--color-text-secondary);
      font-size: $font-size-base;
      transition: color $transition-fast;

      &:hover,
      &.router-link-active {
        color: var(--color-primary);
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
  }
}
</style>
