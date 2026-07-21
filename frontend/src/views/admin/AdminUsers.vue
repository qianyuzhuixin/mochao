<template>
  <div class="admin-users" v-loading="loading">
    <h2 class="page-title">用户管理</h2>

    <div class="filter-bar mb-md">
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱" clearable style="width: 240px" @keyup.enter.native="fetchData" @clear="fetchData">
        <el-button slot="append" icon="el-icon-search" @click="fetchData" />
      </el-input>
      <el-select v-model="statusFilter" placeholder="状态" clearable @change="fetchData" style="width: 120px">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
    </div>

    <el-table :data="list" style="width: 100%">
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="mini">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
            {{ row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column prop="lastLoginAt" label="最后登录" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="text"
            :icon="row.status === 1 ? 'el-icon-circle-close' : 'el-icon-circle-check'"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > pageSize" class="pagination-wrapper">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page.sync="page"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script>
import { getUsers, updateUserStatus } from '@/api/admin'

export default {
  name: 'AdminUsers',
  data() {
    return {
      list: [],
      loading: false,
      keyword: '',
      statusFilter: '',
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
        const res = await getUsers({ page: this.page, pageSize: this.pageSize, keyword: this.keyword, status: this.statusFilter })
        this.list = res.list || res.records || res || []
        this.total = res.total || this.list.length
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleToggleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const action = newStatus === 1 ? '启用' : '禁用'
      this.$confirm(`确定要${action}用户"${row.username}"吗？`, '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await updateUserStatus(row.id, newStatus)
          this.$message.success(`${action}成功`)
          this.fetchData()
        } catch (e) { console.error(e) }
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-users {
  .filter-bar {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-md};
  }
}
</style>
