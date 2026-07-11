<template>
  <div class="progress-overview">
    <el-row :gutter="16">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ data.totalWords || 0 }}</div>
          <div class="stat-label">总字数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ data.chapterCount || 0 }}</div>
          <div class="stat-label">总章节</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ data.completedCount || 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-value">{{ data.progress || 0 }}%</div>
          <div class="stat-label">完成度</div>
        </div>
      </el-col>
    </el-row>
    <div class="progress-bar-wrapper">
      <div class="progress-label">
        <span>写作进度</span>
        <span>{{ data.completedCount || 0 }} / {{ data.chapterCount || 0 }} 章</span>
      </div>
      <el-progress :percentage="data.progress || 0" :stroke-width="12" />
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProgressOverview',
  props: {
    data: { type: Object, default: () => ({}) }
  }
}
</script>

<style lang="scss" scoped>
.progress-overview {
  .stat-card {
    text-align: center;
    padding: #{$spacing-md};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;

    .stat-value {
      font-size: $font-size-xxl;
      font-weight: 700;
      color: var(--color-primary);
    }

    .stat-label {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      margin-top: #{$spacing-xs};
    }
  }

  .progress-bar-wrapper {
    margin-top: #{$spacing-md};

    .progress-label {
      display: flex;
      justify-content: space-between;
      margin-bottom: #{$spacing-sm};
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
    }
  }
}
</style>
