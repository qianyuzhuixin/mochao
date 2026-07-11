<template>
  <default-layout>
    <div class="collections-page page-container">
      <div class="page-header">
        <h1 class="page-title">好词好句</h1>
        <div class="header-actions">
          <el-dropdown @command="handleExport">
            <el-button icon="el-icon-download">导出<i class="el-icon-arrow-down el-icon--right" /></el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="txt">导出 TXT</el-dropdown-item>
              <el-dropdown-item command="markdown">导出 Markdown</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建收藏</el-button>
        </div>
      </div>

      <!-- 每日回顾 -->
      <daily-review :list="reviewList" />

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="filters.type" placeholder="全部类型" clearable @change="fetchData">
          <el-option label="好词" value="word" />
          <el-option label="好句" value="sentence" />
        </el-select>
        <el-input
          v-model="filters.bookTitle"
          placeholder="来源书籍"
          clearable
          style="width: 180px"
          @keyup.enter.native="fetchData"
          @clear="fetchData"
        />
        <el-input
          v-model="filters.tag"
          placeholder="标签"
          clearable
          style="width: 140px"
          @keyup.enter.native="fetchData"
          @clear="fetchData"
        />
        <el-input
          v-model="filters.keyword"
          placeholder="搜索内容"
          clearable
          style="width: 240px"
          @keyup.enter.native="fetchData"
          @clear="fetchData"
        >
          <el-button slot="append" icon="el-icon-search" @click="fetchData" />
        </el-input>
      </div>

      <!-- 卡片网格 -->
      <div v-loading="loading" class="collection-grid">
        <collection-card
          v-for="item in list"
          :key="item.id"
          :data="item"
          @edit="handleEdit"
          @delete="handleDelete"
        />
      </div>

      <empty-state v-if="!loading && list.length === 0" text="暂无收藏" description="去练习中收藏好词好句吧" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="total"
          :page-size="pageSize"
          :current-page.sync="currentPage"
          @current-change="fetchData"
        />
      </div>

      <!-- 收藏弹窗 -->
      <collection-dialog
        :visible.sync="dialogVisible"
        :data="editData"
        @submit="handleSubmit"
      />
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import DailyReview from '@/components/collection/DailyReview.vue'
import CollectionCard from '@/components/collection/CollectionCard.vue'
import CollectionDialog from '@/components/collection/CollectionDialog.vue'
import {
  getCollections,
  createCollection,
  updateCollection,
  deleteCollection,
  getDailyReview,
  exportCollections
} from '@/api/collection'

export default {
  name: 'Collections',
  components: { DefaultLayout, EmptyState, DailyReview, CollectionCard, CollectionDialog },
  data() {
    return {
      list: [],
      reviewList: [],
      filters: { type: '', bookTitle: '', tag: '', keyword: '' },
      currentPage: 1,
      pageSize: 12,
      total: 0,
      loading: false,
      dialogVisible: false,
      editData: null
    }
  },
  created() {
    this.fetchData()
    this.fetchReview()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const params = {
          page: this.currentPage,
          pageSize: this.pageSize,
          ...this.filters
        }
        const res = await getCollections(params)
        this.list = res.list || res.records || res || []
        this.total = res.total || this.list.length
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async fetchReview() {
      try {
        this.reviewList = await getDailyReview() || []
      } catch (e) {
        this.reviewList = []
      }
    },
    handleAdd() {
      this.editData = null
      this.dialogVisible = true
    },
    handleEdit(item) {
      this.editData = item
      this.dialogVisible = true
    },
    async handleSubmit(data) {
      try {
        if (data.id) {
          await updateCollection(data.id, data)
          this.$message.success('更新成功')
        } else {
          await createCollection(data)
          this.$message.success('收藏成功')
        }
        this.fetchData()
      } catch (e) {
        // 错误已处理
      }
    },
    handleDelete(item) {
      this.$confirm('确定要删除这条收藏吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteCollection(item.id)
          this.$message.success('删除成功')
          this.fetchData()
        } catch (e) {
          // 错误已处理
        }
      }).catch(() => {})
    },
    async handleExport(format) {
      try {
        const res = await exportCollections(format)
        const blob = new Blob([res], { type: 'text/plain;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = `好词好句_${Date.now()}.${format === 'markdown' ? 'md' : 'txt'}`
        a.click()
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        // 错误已处理
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.collections-page {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: #{$spacing-lg};

    .header-actions {
      display: flex;
      gap: #{$spacing-sm};
    }
  }

  .filter-bar {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    margin-bottom: #{$spacing-lg};
    flex-wrap: wrap;
  }

  .collection-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: #{$spacing-md};
    min-height: 200px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-xl};
  }
}
</style>
