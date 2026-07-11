<template>
  <div class="profile-materials">
    <div class="section-header">
      <h3 class="section-title">我的素材</h3>
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新建素材</el-button>
    </div>

    <el-table :data="list" v-loading="loading" style="width: 100%">
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="bookName" label="书名" width="140" />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="wordCount" label="字数" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="text" icon="el-icon-edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="text" icon="el-icon-delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
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

    <el-dialog :title="editing ? '编辑素材' : '新建素材'" :visible.sync="dialogVisible" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="书名">
          <el-input v-model="form.bookName" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如：玄幻、都市" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" style="width: 100%">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getMyBooks, createBook, updateBook, deleteBook } from '@/api/book'

export default {
  name: 'ProfileMaterials',
  data() {
    return {
      list: [],
      loading: false,
      page: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      editing: null,
      submitting: false,
      form: { title: '', bookName: '', author: '', category: '', difficulty: 'medium', content: '' },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getMyBooks({ page: this.page, pageSize: this.pageSize })
        this.list = res.list || res.records || res || []
        this.total = res.total || this.list.length
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.editing = null
      this.form = { title: '', bookName: '', author: '', category: '', difficulty: 'medium', content: '' }
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.editing = row
      this.form = { ...row }
      this.dialogVisible = true
    },
    async handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          if (this.editing) {
            await updateBook(this.editing.id, this.form)
            this.$message.success('更新成功')
          } else {
            await createBook(this.form)
            this.$message.success('创建成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } catch (e) {} finally {
          this.submitting = false
        }
      })
    },
    handleDelete(row) {
      this.$confirm(`确定要删除"${row.title}"吗？`, '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await deleteBook(row.id)
          this.$message.success('删除成功')
          this.fetchData()
        } catch (e) {}
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-materials {
  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: #{$spacing-md};

    .section-title {
      font-size: $font-size-md;
      font-weight: 600;
      color: var(--color-text);
    }
  }

  .pagination-wrapper {
    display: flex;
    justify-content: center;
    margin-top: #{$spacing-md};
  }
}
</style>
