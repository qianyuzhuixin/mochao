<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="theme-toggle">
      <i :class="themeIcon" />
      <span class="theme-name">{{ currentThemeName }}</span>
    </span>
    <el-dropdown-menu slot="dropdown">
      <el-dropdown-item
        v-for="t in themes"
        :key="t"
        :command="t"
        :class="{ 'is-active': t === currentTheme }"
      >
        <i :class="themeIcons[t]" />
        {{ themeNames[t] }}
        <i v-if="t === currentTheme" class="el-icon-check check-icon" />
      </el-dropdown-item>
    </el-dropdown-menu>
  </el-dropdown>
</template>

<script>
import { mapGetters } from 'vuex'
import { themes, themeNames } from '@/utils/theme'

export default {
  name: 'ThemeToggle',
  data() {
    return {
      themes,
      themeNames,
      themeIcons: {
        light: 'el-icon-sunny',
        dark: 'el-icon-moon',
        'eye-care': 'el-icon-cpu'
      }
    }
  },
  computed: {
    ...mapGetters('theme', ['currentTheme']),
    currentThemeName() {
      return themeNames[this.currentTheme]
    },
    themeIcon() {
      return this.themeIcons[this.currentTheme]
    }
  },
  methods: {
    handleCommand(theme) {
      this.$store.dispatch('theme/changeTheme', theme)
    }
  }
}
</script>

<style lang="scss" scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: #{$spacing-xs};
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: $font-size-base;
  padding: #{$spacing-xs} #{$spacing-sm};
  border-radius: $border-radius-base;
  transition: all $transition-fast;

  &:hover {
    color: var(--color-primary);
    background-color: var(--color-bg-secondary);
  }

  i {
    font-size: 16px;
  }
}

.el-dropdown-menu__item {
  &.is-active {
    color: var(--color-primary);
  }

  .check-icon {
    float: right;
    margin-left: #{$spacing-sm};
  }
}
</style>
