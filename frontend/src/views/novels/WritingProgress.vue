<template>
  <div class="writing-progress-page page-container page-container-narrow" v-loading="loading">
    <div class="page-header">
      <div>
        <el-button type="text" icon="el-icon-back" class="back-btn" @click="$router.push(`/novels/${novelId}`)">返回</el-button>
        <h1 class="page-title">写作进度</h1>
      </div>
    </div>

    <!-- 进度概览 -->
    <ProgressOverview :data="progressData" class="mb-lg" />

    <!-- 字数趋势图 -->
    <div class="chart-card">
      <h3 class="card-title">字数趋势</h3>
      <div ref="trendChart" class="chart-container" />
    </div>

    <!-- 章节状态 -->
    <div class="status-card">
      <h3 class="card-title">章节状态</h3>
      <div class="chapter-status-list">
        <div v-for="ch in chapters" :key="ch.id" class="chapter-status-item">
          <el-tag :type="statusType(ch.status)" size="mini">{{ statusText(ch.status) }}</el-tag>
          <span class="ch-title">{{ ch.title || `第${ch.sort}章` }}</span>
          <span class="ch-words">{{ ch.wordCount || 0 }}字</span>
          <el-progress
            :percentage="chapterProgress(ch)"
            :stroke-width="6"
            :show-text="false"
            class="ch-progress"
          />
        </div>
      </div>
    </div>

    <!-- 热力图 -->
    <div class="heatmap-card">
      <h3 class="card-title">写作热力图</h3>
      <div class="heatmap-grid">
        <div
          v-for="(day, index) in heatmapData"
          :key="index"
          class="heatmap-cell"
          :style="{ backgroundColor: heatColor(day.count) }"
          :title="`${day.date}: ${day.count}字`"
        />
      </div>
      <div class="heatmap-legend">
        <span>少</span>
        <div class="legend-cells">
          <div class="heatmap-cell legend" :style="{ backgroundColor: heatColor(0) }" />
          <div class="heatmap-cell legend" :style="{ backgroundColor: heatColor(500) }" />
          <div class="heatmap-cell legend" :style="{ backgroundColor: heatColor(1500) }" />
          <div class="heatmap-cell legend" :style="{ backgroundColor: heatColor(3000) }" />
        </div>
        <span>多</span>
      </div>
    </div>
  </div>
</template>

<script>
import ProgressOverview from '@/components/novel/ProgressOverview.vue'
import { getNovelProgress, getChapters } from '@/api/novel'
import * as echarts from 'echarts'

export default {
  name: 'WritingProgress',
  components: { ProgressOverview },
  data() {
    return {
      loading: false,
      progressData: {},
      chapters: [],
      trendChart: null,
      heatmapData: []
    }
  },
  computed: {
    novelId() { return this.$route.params.id }
  },
  async created() {
    await this.fetchData()
    this.initHeatmap()
    this.$nextTick(() => {
      this.initTrendChart()
    })
  },
  beforeDestroy() {
    if (this.trendChart) {
      this.trendChart.dispose()
    }
    window.removeEventListener('resize', this.handleResize)
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const [progress, chapters] = await Promise.all([
          getNovelProgress(this.novelId).catch(() => ({})),
          getChapters(this.novelId).catch(() => [])
        ])
        this.progressData = progress || {}
        this.chapters = chapters || []
        if (progress && progress.trend) {
          // use trend data from API
        }
      } catch (e) { console.error(e) } finally {
        this.loading = false
      }
    },
    initTrendChart() {
      const el = this.$refs.trendChart
      if (!el) return
      this.trendChart = echarts.init(el)

      // 生成近30天的模拟数据
      const days = []
      const wordData = []
      const now = new Date()
      for (let i = 29; i >= 0; i--) {
        const d = new Date(now)
        d.setDate(d.getDate() - i)
        days.push(`${d.getMonth() + 1}/${d.getDate()}`)
        wordData.push(Math.floor(Math.random() * 3000) + 500)
      }

      const option = {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'var(--color-card-bg)',
          borderColor: 'var(--color-border)',
          textStyle: { color: 'var(--color-text)' }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: days,
          axisLine: { lineStyle: { color: '#E4E7ED' } },
          axisLabel: { color: '#6C7293' }
        },
        yAxis: {
          type: 'value',
          axisLine: { lineStyle: { color: '#E4E7ED' } },
          axisLabel: { color: '#6C7293' },
          splitLine: { lineStyle: { color: '#EBEEF5' } }
        },
        series: [{
          name: '字数',
          type: 'line',
          data: wordData,
          smooth: true,
          itemStyle: { color: '#4A6CF7' },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(74, 108, 247, 0.3)' },
                { offset: 1, color: 'rgba(74, 108, 247, 0)' }
              ]
            }
          }
        }]
      }
      this.trendChart.setOption(option)
    },
    initHeatmap() {
      const days = []
      const now = new Date()
      for (let i = 90; i >= 0; i--) {
        const d = new Date(now)
        d.setDate(d.getDate() - i)
        days.push({
          date: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
          count: Math.random() > 0.3 ? Math.floor(Math.random() * 3000) : 0
        })
      }
      this.heatmapData = days
    },
    handleResize() {
      if (this.trendChart) {
        this.trendChart.resize()
      }
    },
    statusText(status) {
      const map = { draft: '草稿', writing: '写作中', completed: '已完成', published: '已发布' }
      return map[status] || '草稿'
    },
    statusType(status) {
      const map = { draft: 'info', writing: 'warning', completed: 'success', published: 'success' }
      return map[status] || 'info'
    },
    chapterProgress(ch) {
      const target = ch.targetWords || 2000
      return Math.min(Math.round(((ch.wordCount || 0) / target) * 100), 100)
    },
    heatColor(count) {
      if (count === 0) return 'var(--color-border)'
      if (count < 500) return 'rgba(74, 108, 247, 0.2)'
      if (count < 1500) return 'rgba(74, 108, 247, 0.5)'
      if (count < 3000) return 'rgba(74, 108, 247, 0.7)'
      return 'rgba(74, 108, 247, 0.9)'
    }
  }
}
</script>

<style lang="scss" scoped>
.writing-progress-page {
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-lg};

    .back-btn { margin-bottom: 4px; padding: 0; color: #4A6CF7; }

    .page-title { margin: 0; }
  }

  .chart-card,
  .status-card,
  .heatmap-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-lg};
    margin-bottom: #{$spacing-lg};

    .card-title {
      font-size: $font-size-md;
      font-weight: 600;
      color: var(--color-text);
      margin-bottom: #{$spacing-md};
    }
  }

  .chart-container {
    width: 100%;
    height: 300px;
  }

  .chapter-status-list {
    .chapter-status-item {
      display: flex;
      align-items: center;
      gap: #{$spacing-sm};
      padding: #{$spacing-sm} 0;
      border-bottom: 1px solid var(--color-border-light);

      &:last-child { border-bottom: none; }

      .ch-title {
        flex: 1;
        font-size: $font-size-sm;
        color: var(--color-text);
      }

      .ch-words {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
        white-space: nowrap;
      }

      .ch-progress {
        width: 100px;
      }
    }
  }

  .heatmap-grid {
    display: grid;
    grid-template-columns: repeat(15, 1fr);
    gap: 4px;
    margin-bottom: #{$spacing-md};

    .heatmap-cell {
      aspect-ratio: 1;
      border-radius: 2px;
      min-height: 20px;
    }
  }

  .heatmap-legend {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    font-size: $font-size-xs;
    color: var(--color-text-secondary);

    .legend-cells {
      display: flex;
      gap: 3px;

      .heatmap-cell {
        width: 14px;
        height: 14px;
        aspect-ratio: unset;
        min-height: unset;
      }
    }
  }
}
</style>
