<template>
  <div id="app" :data-theme="currentTheme">
    <router-view />
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'App',
  computed: {
    ...mapGetters('theme', ['currentTheme'])
  },
  created() {
    this.$store.dispatch('theme/initTheme')
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
</style>
