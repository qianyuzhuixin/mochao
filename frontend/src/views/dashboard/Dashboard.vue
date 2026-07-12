<template>
  <default-layout>
    <div class="dashboard-page page-container" v-loading="loading">
      <h1 class="page-title">数据看板</h1>

      <!-- 今日概览 -->
      <el-row :gutter="16" class="mb-lg">
        <el-col :xs="12" :sm="8">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: rgba(74, 108, 247, 0.1); color: var(--color-primary)">
              <i class="el-icon-edit" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayWords || 0 }}</div>
              <div class="stat-label">今日练习字数</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: rgba(82, 196, 26, 0.1); color: var(--color-success)">
              <i class="el-icon-time" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayDuration || 0 }}分</div>
              <div class="stat-label">今日耗时</div>
            </div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="8">
          <div class="stat-card">
            <div class="stat-icon" style="background-color: rgba(250, 173, 20, 0.1); color: var(--color-warning)">
              <i class="el-icon-circle-check" />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.todayAccuracy || 0 }}%</div>
              <div class="stat-label">今日正确率</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 打卡 -->
      <div class="check-in-card mb-lg">
        <div class="check-in-info">
          <div class="check-in-days">
            <span class="days-number">{{ overview.streakDays || 0 }}</span>
            <span class="days-label">天连续打卡</span>
          </div>
          <div class="check-in-total">
            累计练习 {{ overview.totalWords || 0 }} 字 · {{ overview.totalSessions || 0 }} 次
          </div>
        </div>
        <el-button type="primary" :disabled="overview.checkedIn" @click="handleCheckIn">
          {{ overview.checkedIn ? '今日已打卡' : '今日打卡' }}
        </el-button>
      </div>

      <!-- 日历热力图 -->
      <div class="card-base mb-lg">
        <div class="calendar-header">
          <h3 class="card-title">练习日历</h3>
          <div class="calendar-nav">
            <el-button type="text" icon="el-icon-arrow-left" @click="changeMonth(-1)" />
            <span class="calendar-month-label">{{ currentYear }}年 {{ currentMonth }}月</span>
            <el-button type="text" icon="el-icon-arrow-right" @click="changeMonth(1)" />
          </div>
        </div>
        <div class="calendar-weekdays">
          <span v-for="d in weekDays" :key="d" class="weekday-label">{{ d }}</span>
        </div>
        <div class="calendar-grid-month">
          <div
            v-for="(day, index) in monthCalendarData"
            :key="index"
            class="calendar-cell-month"
            :class="{ 'other-month': !day.isCurrentMonth, 'today': day.isToday }"
            :style="{ backgroundColor: day.isCurrentMonth ? calendarColor(day.totalChars) : 'transparent' }"
            :title="day.title"
          >
            <span class="day-number">{{ day.day }}</span>
            <span v-if="day.isCurrentMonth && day.totalChars > 0" class="day-count">{{ day.totalChars }}</span>
          </div>
        </div>
        <div class="calendar-legend">
          <span>少</span>
          <div class="legend-cells">
            <div class="calendar-cell legend" :style="{ backgroundColor: calendarColor(0) }" />
            <div class="calendar-cell legend" :style="{ backgroundColor: calendarColor(500) }" />
            <div class="calendar-cell legend" :style="{ backgroundColor: calendarColor(1500) }" />
            <div class="calendar-cell legend" :style="{ backgroundColor: calendarColor(3000) }" />
          </div>
          <span>多</span>
        </div>
      </div>

      <!-- 趋势图表 -->
      <div class="card-base mb-lg">
        <h3 class="card-title">近30天趋势</h3>
        <div ref="trendChart" class="chart-container" />
      </div>

      <!-- 练习历史 -->
      <div class="card-base">
        <h3 class="card-title">练习历史</h3>
        <el-table :data="historyList" style="width: 100%">
          <el-table-column prop="bookTitle" label="素材" min-width="160" />
          <el-table-column prop="wordCount" label="字数" width="100" />
          <el-table-column prop="accuracy" label="正确率" width="100">
            <template #default="{ row }">{{ row.accuracy }}%</template>
          </el-table-column>
          <el-table-column prop="speed" label="速度" width="100">
            <template #default="{ row }">{{ row.speed }} 字/分</template>
          </el-table-column>
          <el-table-column prop="duration" label="耗时" width="100">
            <template #default="{ row }">{{ row.duration }}秒</template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="80" />
          <el-table-column prop="createdAt" label="时间" width="160" />
        </el-table>
        <div v-if="historyTotal > pageSize" class="pagination-wrapper">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="historyTotal"
            :page-size="pageSize"
            :current-page.sync="historyPage"
            @current-change="fetchHistory"
          />
        </div>
      </div>
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getOverview, getTrend, getCheckIn, getCalendar } from '@/api/statistics'
import { getPracticeHistory } from '@/api/practice'

export default {
  name: 'Dashboard',
  components: { DefaultLayout },
  data() {
    return {
      loading: false,
      overview: {},
      calendarData: [],
      currentYear: new Date().getFullYear(),
      currentMonth: new Date().getMonth() + 1,
      weekDays: ['日', '一', '二', '三', '四', '五', '六'],
      historyList: [],
      historyPage: 1,
      historyTotal: 0,
      pageSize: 10,
      trendChart: null
    }
  },
  computed: {
    monthCalendarData() {
      return this.buildMonthCalendar(this.currentYear, this.currentMonth, this.calendarData)
    }
  },
  async created() {
    await this.fetchData()
    this.$nextTick(() => {
      this.initTrendChart()
    })
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    if (this.trendChart) this.trendChart.dispose()
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const [overview, calendarRes] = await Promise.all([
          getOverview().catch(() => null),
          getCalendar({ year: this.currentYear, month: this.currentMonth }).catch(() => null)
        ])
        this.overview = overview || {}

        // 使用后端真实数据，不再生成假数据
        const backendCalendar = calendarRes && calendarRes.calendar ? calendarRes.calendar : []
        this.calendarData = backendCalendar

        this.fetchHistory()
      } catch (e) {
        console.error('Dashboard fetchData error:', e)
      } finally {
        this.loading = false
      }
    },
    async fetchHistory() {
      try {
        const res = await getPracticeHistory({ page: this.historyPage, pageSize: this.pageSize })
        this.historyList = res.list || res.records || res || []
        this.historyTotal = res.total || this.historyList.length
      } catch (e) {
        this.historyList = []
      }
    },
    generateCalendar() {
      // 【已废弃】不再生成假数据，改为从后端 API 获取真实日历数据
      // 如果后端未返回数据，日历将显示为空白（均为 0 字）
      return []
    },
    changeMonth(delta) {
      let newMonth = this.currentMonth + delta
      let newYear = this.currentYear
      if (newMonth > 12) {
        newMonth = 1
        newYear++
      } else if (newMonth < 1) {
        newMonth = 12
        newYear--
      }
      this.currentMonth = newMonth
      this.currentYear = newYear
      this.fetchData()
    },
    buildMonthCalendar(year, month, backendData) {
      const dataMap = {}
      backendData.forEach(item => {
        dataMap[item.date] = item
      })

      const firstDay = new Date(year, month - 1, 1)
      const lastDay = new Date(year, month, 0)
      const startWeekDay = firstDay.getDay() // 0=Sunday
      const daysInMonth = lastDay.getDate()

      const today = new Date()
      const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`

      const result = []

      // 前置补位：上月日期
      const prevMonthLastDay = new Date(year, month - 1, 0).getDate()
      for (let i = startWeekDay - 1; i >= 0; i--) {
        const day = prevMonthLastDay - i
        result.push({
          day,
          isCurrentMonth: false,
          isToday: false,
          totalChars: 0,
          title: ''
        })
      }

      // 当月日期
      for (let d = 1; d <= daysInMonth; d++) {
        const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
        const data = dataMap[dateStr]
        const totalChars = data ? (data.totalChars || 0) : 0
        result.push({
          day: d,
          isCurrentMonth: true,
          isToday: dateStr === todayStr,
          totalChars,
          title: data ? `${dateStr}: ${totalChars}字` : `${dateStr}: 0字`
        })
      }

      // 后置补位：下月日期，凑满整行
      const remaining = (7 - (result.length % 7)) % 7
      for (let d = 1; d <= remaining; d++) {
        result.push({
          day: d,
          isCurrentMonth: false,
          isToday: false,
          totalChars: 0,
          title: ''
        })
      }

      return result
    },
    async handleCheckIn() {
      try {
        const res = await getCheckIn()
        this.$message.success('打卡成功')
        this.overview.checkedIn = true
        this.overview.streakDays = res.streakDays || this.overview.streakDays
      } catch (e) {
        console.error('打卡失败:', e)
      }
    },
    async initTrendChart() {
      const el = this.$refs.trendChart
      if (!el) return
      this.trendChart = this.$echarts.init(el)

      // 从后端获取真实趋势数据
      let trendData = null
      try {
        trendData = await getTrend()
      } catch (e) {
        console.error('趋势数据加载失败:', e)
      }

      const days = []
      const wordData = []
      const accuracyData = []
      const speedData = []

      if (trendData && trendData.days && trendData.days.length > 0) {
        // 使用后端真实数据
        trendData.days.forEach((item, i) => {
          days.push(item.date || `第${i + 1}天`)
          wordData.push(item.totalChars || 0)
          accuracyData.push(item.accuracy || 0)
          speedData.push(item.speed || 0)
        })
      } else {
        // 后端无数据时显示空图表（不再生成假数据）
        for (let i = 29; i >= 0; i--) {
          const d = new Date()
          d.setDate(d.getDate() - i)
          days.push(`${d.getMonth() + 1}/${d.getDate()}`)
          wordData.push(0)
          accuracyData.push(0)
          speedData.push(0)
        }
      }

      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['字数', '正确率', '速度'], textStyle: { color: '#6C7293' } },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: days, axisLabel: { color: '#6C7293' } },
        yAxis: [
          { type: 'value', name: '字数', axisLabel: { color: '#6C7293' } },
          { type: 'value', name: '%', axisLabel: { color: '#6C7293' } }
        ],
        series: [
          { name: '字数', type: 'bar', data: wordData, itemStyle: { color: '#4A6CF7' } },
          { name: '正确率', type: 'line', yAxisIndex: 1, data: accuracyData, itemStyle: { color: '#52C41A' }, smooth: true },
          { name: '速度', type: 'line', data: speedData, itemStyle: { color: '#FAAD14' }, smooth: true }
        ]
      })
    },
    handleResize() {
      if (this.trendChart) this.trendChart.resize()
    },
    calendarColor(count) {
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
.dashboard-page {
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

  .check-in-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: #{$spacing-lg};
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
    border-radius: $border-radius-md;

    .check-in-info {
      .check-in-days {
        display: flex;
        align-items: baseline;
        gap: #{$spacing-xs};

        .days-number {
          font-size: $font-size-heading;
          font-weight: 800;
          color: #fff;
        }

        .days-label {
          font-size: $font-size-base;
          color: rgba(255, 255, 255, 0.8);
        }
      }

      .check-in-total {
        font-size: $font-size-sm;
        color: rgba(255, 255, 255, 0.6);
        margin-top: #{$spacing-xs};
      }
    }
  }

  .calendar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: #{$spacing-md};

    .card-title {
      margin-bottom: 0;
    }

    .calendar-nav {
      display: flex;
      align-items: center;
      gap: #{$spacing-md};

      .calendar-month-label {
        font-size: $font-size-base;
        font-weight: 600;
        color: var(--color-text);
        min-width: 100px;
        text-align: center;
      }
    }
  }

  .calendar-weekdays {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    margin-bottom: 4px;

    .weekday-label {
      text-align: center;
      font-size: $font-size-xs;
      color: var(--color-text-secondary);
      padding: 4px 0;
    }
  }

  .calendar-grid-month {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 3px;
    margin-bottom: #{$spacing-md};

    .calendar-cell-month {
      aspect-ratio: 1;
      border-radius: 4px;
      min-height: 28px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      position: relative;
      transition: transform 0.15s ease;
      cursor: pointer;

      &:hover {
        transform: scale(1.05);
        z-index: 1;
      }

      &.other-month {
        opacity: 0.3;
      }

      &.today {
        border: 2px solid var(--color-primary);
        font-weight: 700;
      }

      .day-number {
        font-size: $font-size-xs;
        color: var(--color-text);
        line-height: 1.2;
      }

      .day-count {
        font-size: 9px;
        color: var(--color-text-secondary);
        line-height: 1.2;
        margin-top: 1px;
      }
    }
  }

  .calendar-legend {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    font-size: $font-size-xs;
    color: var(--color-text-secondary);

    .legend-cells {
      display: flex;
      gap: 3px;

      .calendar-cell {
        width: 14px;
        height: 14px;
        aspect-ratio: unset;
        min-height: unset;
      }
    }
  }

  .chart-container {
    width: 100%;
    height: 300px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-md};
  }
}
</style>
