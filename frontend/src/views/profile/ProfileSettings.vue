<template>
  <div class="profile-settings">
    <h3 class="section-title">偏好设置</h3>

    <el-form label-width="120px" style="max-width: 500px">
      <el-form-item label="主题">
        <el-radio-group v-model="theme" @change="handleThemeChange">
          <el-radio-button label="light">亮色</el-radio-button>
          <el-radio-button label="dark">暗色</el-radio-button>
          <el-radio-button label="eye-care">护眼绿</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="字体大小">
        <el-slider v-model="fontSize" :min="12" :max="20" :step="1" show-stops @change="handleFontSizeChange" />
      </el-form-item>

      <el-form-item label="预览">
        <div class="preview-text" :style="{ fontSize: fontSize + 'px' }">
          这是一段预览文字，用于展示当前字体大小效果。墨抄 — 网文创作练笔平台。
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'ProfileSettings',
  data() {
    return {
      theme: 'light',
      fontSize: 14
    }
  },
  computed: {
    ...mapGetters('theme', ['currentTheme'])
  },
  created() {
    this.theme = this.currentTheme
    this.fontSize = parseInt(localStorage.getItem('mochao_font_size')) || 14
    this.applyFontSize()
  },
  methods: {
    handleThemeChange(theme) {
      this.$store.dispatch('theme/changeTheme', theme)
    },
    handleFontSizeChange(size) {
      localStorage.setItem('mochao_font_size', size)
      this.applyFontSize()
    },
    applyFontSize() {
      document.documentElement.style.setProperty('--font-size-base', this.fontSize + 'px')
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-settings {
  .section-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: #{$spacing-md};
  }

  .preview-text {
    padding: #{$spacing-md};
    background-color: var(--color-bg-secondary);
    border-radius: $border-radius-base;
    line-height: 1.8;
    color: var(--color-text);
  }
}
</style>
