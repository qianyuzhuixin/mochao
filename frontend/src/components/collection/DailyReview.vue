<template>
  <div class="daily-review">
    <div class="review-card" @click="handleNext">
      <div class="review-header">
        <i class="el-icon-star-on review-icon" />
        <span class="review-title">每日回顾</span>
        <span class="review-hint">点击换一条</span>
      </div>
      <div v-if="current" class="review-content">
        <p class="review-text">{{ current.content }}</p>
        <div class="review-meta">
          <el-tag size="mini" :type="current.type === 'word' ? 'success' : 'primary'">
            {{ current.type === 'word' ? '好词' : '好句' }}
          </el-tag>
          <span v-if="current.bookTitle" class="review-source">— {{ current.bookTitle }}</span>
        </div>
      </div>
      <div v-else class="review-empty">
        <p>暂无收藏内容，快去练习中收藏好词好句吧</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DailyReview',
  props: {
    list: { type: Array, default: () => [] }
  },
  data() {
    return {
      currentIndex: 0
    }
  },
  computed: {
    current() {
      if (!this.list || this.list.length === 0) return null
      return this.list[this.currentIndex % this.list.length]
    }
  },
  methods: {
    handleNext() {
      if (this.list.length > 0) {
        this.currentIndex++
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.daily-review {
  margin-bottom: #{$spacing-lg};

  .review-card {
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
    border-radius: $border-radius-lg;
    padding: #{$spacing-lg} #{$spacing-xl};
    cursor: pointer;
    transition: all $transition-base;

    &:hover {
      box-shadow: var(--shadow-lg);
      transform: translateY(-2px);
    }

    .review-header {
      display: flex;
      align-items: center;
      gap: #{$spacing-sm};
      margin-bottom: #{$spacing-sm};

      .review-icon {
        font-size: 20px;
        color: rgba(255, 255, 255, 0.9);
      }

      .review-title {
        font-size: $font-size-md;
        font-weight: 600;
        color: #fff;
      }

      .review-hint {
        margin-left: auto;
        font-size: $font-size-xs;
        color: rgba(255, 255, 255, 0.6);
      }
    }

    .review-content {
      .review-text {
        font-size: $font-size-md;
        line-height: 1.8;
        color: #fff;
        margin-bottom: #{$spacing-sm};
      }

      .review-meta {
        display: flex;
        align-items: center;
        gap: #{$spacing-sm};

        .review-source {
          font-size: $font-size-sm;
          color: rgba(255, 255, 255, 0.7);
        }
      }
    }

    .review-empty {
      p {
        color: rgba(255, 255, 255, 0.7);
        font-size: $font-size-sm;
      }
    }
  }
}
</style>
