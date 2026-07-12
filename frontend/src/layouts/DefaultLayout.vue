<template>
  <div class="default-layout">
    <main class="layout-main">
      <slot>
        <router-view />
      </slot>
    </main>

    <footer class="layout-footer">
      <span>墨抄 MoChao &copy; {{ year }} - 网文创作练笔平台</span>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'DefaultLayout',
  data() {
    return {
      year: new Date().getFullYear()
    }
  }
}
</script>

<style lang="scss" scoped>
.default-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-main {
  flex: 1;
  min-height: calc(100vh - #{$header-height} - 60px);
  padding: 0;
  background-color: var(--color-bg);
}

.layout-footer {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-card-bg);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: $font-size-sm;
}
</style>
