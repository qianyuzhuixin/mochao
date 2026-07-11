<template>
  <default-layout>
    <div class="novel-create-page page-container">
      <div class="back-btn">
        <el-button type="text" icon="el-icon-arrow-left" @click="$router.back()">返回</el-button>
      </div>
      <div class="form-card">
        <h1 class="page-title">创建小说</h1>
        <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 600px">
          <el-form-item label="小说名称" prop="title">
            <el-input v-model="form.title" placeholder="请输入小说名称" />
          </el-form-item>
          <el-form-item label="类型" prop="genre">
            <el-select v-model="form.genre" placeholder="请选择类型" style="width: 100%">
              <el-option label="玄幻" value="玄幻" />
              <el-option label="都市" value="都市" />
              <el-option label="科幻" value="科幻" />
              <el-option label="历史" value="历史" />
              <el-option label="悬疑" value="悬疑" />
              <el-option label="言情" value="言情" />
              <el-option label="武侠" value="武侠" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="简介" prop="synopsis">
            <el-input v-model="form.synopsis" type="textarea" :rows="4" placeholder="简要描述小说内容" />
          </el-form-item>
          <el-form-item label="目标字数">
            <el-input-number v-model="form.targetWords" :min="10000" :step="10000" />
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="form.tagsStr" placeholder="多个标签用逗号分隔" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">创建</el-button>
            <el-button @click="$router.back()">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </default-layout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { createNovel } from '@/api/novel'

export default {
  name: 'NovelCreate',
  components: { DefaultLayout },
  data() {
    return {
      form: {
        title: '',
        genre: '',
        synopsis: '',
        targetWords: 100000,
        tagsStr: ''
      },
      rules: {
        title: [{ required: true, message: '请输入小说名称', trigger: 'blur' }],
        genre: [{ required: true, message: '请选择类型', trigger: 'change' }]
      },
      submitting: false
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const tags = this.form.tagsStr
            ? this.form.tagsStr.split(',').map(t => t.trim()).filter(Boolean)
            : []
          const res = await createNovel({
            title: this.form.title,
            genre: this.form.genre,
            summary: this.form.synopsis,
            targetWords: this.form.targetWords,
            tags
          })
          this.$message.success('创建成功')
          if (res && res.id) {
            this.$router.push(`/novels/${res.id}`)
          } else {
            this.$router.push('/novels')
          }
        } catch (e) {
          // 错误已处理
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.novel-create-page {
  .back-btn {
    margin-bottom: #{$spacing-md};
  }

  .form-card {
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: $border-radius-md;
    padding: #{$spacing-xl};
    max-width: 700px;
  }
}
</style>
