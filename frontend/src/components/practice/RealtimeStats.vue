<template>
  <div class="realtime-stats">
    <div class="stat-item">
      <div class="stat-icon timer-icon">
        <i class="el-icon-time" />
      </div>
      <div class="stat-content">
        <div class="stat-value">{{ formattedDuration }}</div>
        <div class="stat-label">计时</div>
      </div>
    </div>
    <div class="stat-divider" />
    <div class="stat-item">
      <div class="stat-icon accuracy-icon">
        <i class="el-icon-circle-check" />
      </div>
      <div class="stat-content">
        <div class="stat-value">{{ animatedAccuracy }}%</div>
        <div class="stat-label">正确率</div>
      </div>
    </div>
    <div class="stat-divider" />
    <div class="stat-item">
      <div class="stat-icon speed-icon">
        <i class="el-icon-lightning" />
      </div>
      <div class="stat-content">
        <div class="stat-value">{{ animatedSpeed }}</div>
        <div class="stat-label">字/分钟</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RealtimeStats',
  props: {
    duration: { type: Number, default: 0 },
    accuracy: { type: Number, default: 100 },
    speed: { type: Number, default: 0 }
  },
  data() {
    return {
      animatedAccuracy: 100,
      animatedSpeed: 0
    }
  },
  computed: {
    formattedDuration() {
      const mins = Math.floor(this.duration / 60)
      const secs = this.duration % 60
      return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    }
  },
  watch: {
    accuracy(newVal, oldVal) {
      this.animateNumber('animatedAccuracy', oldVal || 0, newVal, 300)
    },
    speed(newVal, oldVal) {
      this.animateNumber('animatedSpeed', oldVal || 0, newVal, 300)
    }
  },
  methods: {
    animateNumber(key, from, to, duration) {
      const start = performance.now()
      const step = (now) => {
        const elapsed = now - start
        const progress = Math.min(elapsed / duration, 1)
        const eased = 1 - Math.pow(1 - progress, 3)
        this[key] = Math.round(from + (to - from) * eased)
        if (progress < 1) {
          requestAnimationFrame(step)
        }
      }
      requestAnimationFrame(step)
    }
  }
}
</script>

<style lang="scss" scoped>
.realtime-stats {
  display: flex;
  align-items: center;
  gap: #{$spacing-lg};
  padding: #{$spacing-md} #{$spacing-lg};
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  box-shadow: var(--shadow-sm);

  .stat-item {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};

    .stat-icon {
      width: 40px;
      height: 40px;
      border-radius: $border-radius-base;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;

      &.timer-icon {
        background-color: rgba(74, 108, 247, 0.1);
        color: var(--color-primary);
      }

      &.accuracy-icon {
        background-color: rgba(82, 196, 26, 0.1);
        color: var(--color-success);
      }

      &.speed-icon {
        background-color: rgba(250, 173, 20, 0.1);
        color: var(--color-warning);
      }
    }

    .stat-content {
      .stat-value {
        font-size: $font-size-xl;
        font-weight: 700;
        color: var(--color-text);
        line-height: 1.2;
      }

      .stat-label {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }
  }

  .stat-divider {
    width: 1px;
    height: 32px;
    background-color: var(--color-border);
  }
}

@media (max-width: 768px) {
  .realtime-stats {
    flex-wrap: wrap;
    gap: #{$spacing-sm};

    .stat-divider {
      display: none;
    }
  }
}
</style>
