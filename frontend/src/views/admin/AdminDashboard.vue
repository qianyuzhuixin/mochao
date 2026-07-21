<template>
  <div class="admin-dashboard" v-loading="loading">
    <h2 class="page-title">数据概览</h2>

    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: rgba(74, 108, 247, 0.1); color: var(--color-primary)">
            <i class="el-icon-user" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ data.totalUsers || 0 }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: rgba(82, 196, 26, 0.1); color: var(--color-success)">
            <i class="el-icon-reading" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ data.totalBooks || 0 }}</div>
            <div class="stat-label">素材总数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: rgba(250, 173, 20, 0.1); color: var(--color-warning)">
            <i class="el-icon-edit" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ data.totalPractices || 0 }}</div>
            <div class="stat-label">练习总次数</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card">
          <div class="stat-icon" style="background-color: rgba(255, 77, 79, 0.1); color: var(--color-error)">
            <i class="el-icon-document" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ data.totalNovels || 0 }}</div>
            <div class="stat-label">小说总数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="24" :sm="12">
        <div class="card-base">
          <h3 class="card-title">近7天新增用户</h3>
          <div ref="userChart" class="chart-container" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="12">
        <div class="card-base">
          <h3 class="card-title">近7天练习量</h3>
          <div ref="practiceChart" class="chart-container" />
        </div>
      </el-col>
    </el-row>

    <div class="card-base">
      <h3 class="card-title">最近注册用户</h3>
      <el-table :data="data.recentUsers || []" style="width: 100%">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="createdAt" label="注册时间" width="180" />
      </el-table>
    </div>
  </div>
</template>

<script>
import { getDashboard } from '@/api/admin'
import * as echarts from 'echarts'

export default {
  name: 'AdminDashboard',
  data() {
    return {
      loading: false,
      data: {},
      userChart: null,
      practiceChart: null
    }
  },
  async created() {
    await this.fetchData()
    this.$nextTick(() => {
      this.initCharts()
    })
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.userChart) this.userChart.dispose()
    if (this.practiceChart) this.practiceChart.dispose()
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        this.data = await getDashboard() || {}
      } catch (e) { console.error(e) } finally {
        this.loading = false
      }
    },
    initCharts() {
      const days = []
      const now = new Date()
      for (let i = 6; i >= 0; i--) {
        const d = new Date(now)
        d.setDate(d.getDate() - i)
        days.push(`${d.getMonth() + 1}/${d.getDate()}`)
      }

      // 用户增长图（使用后端真实数据，无数据时显示 0）
      if (this.$refs.userChart) {
        const userGrowth = this.data.recentUserGrowth || days.map(() => 0)
        this.userChart = echarts.init(this.$refs.userChart)
        this.userChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
          xAxis: { type: 'category', data: days },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'bar',
            data: userGrowth,
            itemStyle: { color: '#4A6CF7' }
          }]
        })
      }

      // 练习量图（使用后端真实数据，无数据时显示 0）
      if (this.$refs.practiceChart) {
        const practiceCount = this.data.recentPracticeCount || days.map(() => 0)
        this.practiceChart = echarts.init(this.$refs.practiceChart)
        this.practiceChart.setOption({
          tooltip: { trigger: 'axis' },
          grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
          xAxis: { type: 'category', data: days },
          yAxis: { type: 'value', minInterval: 1 },
          series: [{
            type: 'line',
            data: practiceCount,
            smooth: true,
            itemStyle: { color: '#52C41A' },
            areaStyle: { color: 'rgba(82, 196, 26, 0.2)' }
          }]
        })
      }
    },
    handleResize() {
      if (this.userChart) this.userChart.resize()
      if (this.practiceChart) this.practiceChart.resize()
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-dashboard {
  .stat-card {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    padding: #{$spacing-lg};
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: $border-radius-md;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .stat-info {
      .stat-value {
        font-size: $font-size-xl;
        font-weight: 700;
        color: var(--color-text);
      }

      .stat-label {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }
  }

  .card-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: #{$spacing-md};
  }

  .chart-container {
    width: 100%;
    height: 250px;
  }
}
</style>
