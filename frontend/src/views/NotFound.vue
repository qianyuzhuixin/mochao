<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <p class="error-text">页面未找到</p>
      <p class="error-desc">您访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotFound'
}
</script>

<style lang="scss" scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg);

  .not-found-content {
    text-align: center;

    .error-code {
      font-size: 120px;
      font-weight: 800;
      color: var(--color-primary);
      line-height: 1;
    }

    .error-text {
      font-size: $font-size-heading;
      font-weight: 600;
      color: var(--color-text);
      margin-bottom: #{$spacing-sm};
    }

    .error-desc {
      font-size: $font-size-base;
      color: var(--color-text-secondary);
      margin-bottom: #{$spacing-xl};
    }
  }
}
</style>
