<template>
  <div class="profile-history">
    <h3 class="section-title">练习历史</h3>
    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="bookTitle" label="素材" min-width="160" />
      <el-table-column prop="wordCount" label="字数" width="80" />
      <el-table-column label="正确率" width="100">
        <template #default="{ row }">{{ row.accuracy }}%</template>
      </el-table-column>
      <el-table-column label="速度" width="100">
        <template #default="{ row }">{{ row.speed }} 字/分</template>
      </el-table-column>
      <el-table-column label="耗时" width="100">
        <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
      </el-table-column>
      <el-table-column prop="score" label="评分" width="80" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>

    <div v-if="total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="page"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script>
import { getPracticeHistory } from '@/api/practice'

export default {
  name: 'ProfileHistory',
  data() {
    return {
      list: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getPracticeHistory({ page: this.page, pageSize: this.pageSize })
        this.list = res.list || res.records || res || []
        this.total = res.total || this.list.length
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    formatDuration(seconds) {
      if (!seconds) return '0秒'
      const mins = Math.floor(seconds / 60)
      const secs = seconds % 60
      return mins > 0 ? `${mins}分${secs}秒` : `${secs}秒`
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-history {
  .section-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: #{$spacing-md};
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-md};
  }
}
</style>
