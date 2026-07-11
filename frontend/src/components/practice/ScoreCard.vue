<template>
  <div class="score-card">
    <div class="score-header">
      <h2 class="score-title">练习完成</h2>
      <p class="score-subtitle">恭喜你完成了本次抄写练习</p>
    </div>

    <div class="score-ring-wrapper">
      <div class="score-ring" :style="{ '--score': score }">
        <svg width="140" height="140" viewBox="0 0 140 140">
          <circle cx="70" cy="70" r="60" class="ring-bg" />
          <circle cx="70" cy="70" r="60" class="ring-progress" :style="ringStyle" />
        </svg>
        <div class="ring-text">
          <span class="ring-score">{{ score }}</span>
          <span class="ring-label">综合评分</span>
        </div>
      </div>
    </div>

    <div class="score-details">
      <div class="detail-item">
        <div class="detail-icon">
          <i class="el-icon-circle-check" style="color: var(--color-success)" />
        </div>
        <div class="detail-value">{{ accuracy }}%</div>
        <div class="detail-label">正确率</div>
      </div>
      <div class="detail-item">
        <div class="detail-icon">
          <i class="el-icon-lightning" style="color: var(--color-warning)" />
        </div>
        <div class="detail-value">{{ speed }}</div>
        <div class="detail-label">字/分钟</div>
      </div>
      <div class="detail-item">
        <div class="detail-icon">
          <i class="el-icon-time" style="color: var(--color-primary)" />
        </div>
        <div class="detail-value">{{ formattedDuration }}</div>
        <div class="detail-label">耗时</div>
      </div>
    </div>

    <div class="score-actions">
      <el-button @click="$emit('retry')">再练一次</el-button>
      <el-button type="primary" @click="$emit('confirm')">完成</el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ScoreCard',
  props: {
    accuracy: { type: Number, default: 0 },
    speed: { type: Number, default: 0 },
    duration: { type: Number, default: 0 },
    score: { type: Number, default: 0 }
  },
  computed: {
    formattedDuration() {
      const mins = Math.floor(this.duration / 60)
      const secs = this.duration % 60
      return `${mins}分${secs}秒`
    },
    ringStyle() {
      const circumference = 2 * Math.PI * 60
      const offset = circumference * (1 - this.score / 100)
      return {
        strokeDasharray: `${circumference}`,
        strokeDashoffset: offset
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.score-card {
  text-align: center;
  padding: #{$spacing-xl};

  .score-header {
    margin-bottom: #{$spacing-lg};

    .score-title {
      font-size: $font-size-xxl;
      font-weight: 700;
      color: var(--color-text);
    }

    .score-subtitle {
      font-size: $font-size-base;
      color: var(--color-text-secondary);
      margin-top: #{$spacing-xs};
    }
  }

  .score-ring-wrapper {
    display: flex;
    justify-content: center;
    margin-bottom: #{$spacing-xl};

    .score-ring {
      position: relative;
      width: 140px;
      height: 140px;

      svg {
        transform: rotate(-90deg);

        .ring-bg {
          fill: none;
          stroke: var(--color-border);
          stroke-width: 8;
        }

        .ring-progress {
          fill: none;
          stroke: var(--color-primary);
          stroke-width: 8;
          stroke-linecap: round;
          transition: stroke-dashoffset 1s ease;
        }
      }

      .ring-text {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .ring-score {
          font-size: 36px;
          font-weight: 800;
          color: var(--color-primary);
        }

        .ring-label {
          font-size: $font-size-xs;
          color: var(--color-text-secondary);
        }
      }
    }
  }

  .score-details {
    display: flex;
    justify-content: center;
    gap: #{$spacing-xl};
    margin-bottom: #{$spacing-xl};

    .detail-item {
      text-align: center;

      .detail-icon {
        font-size: 24px;
        margin-bottom: #{$spacing-xs};
      }

      .detail-value {
        font-size: $font-size-lg;
        font-weight: 600;
        color: var(--color-text);
      }

      .detail-label {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }
  }

  .score-actions {
    display: flex;
    justify-content: center;
    gap: #{$spacing-md};
  }
}
</style>
