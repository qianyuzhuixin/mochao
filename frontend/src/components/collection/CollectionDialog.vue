<template>
  <el-dialog :title="isEdit ? '编辑收藏' : '收藏好词好句'" :visible.sync="visible" width="500px" @close="handleClose">
    <el-form ref="form" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="类型" prop="type">
        <el-radio-group v-model="form.type">
          <el-radio label="word">好词</el-radio>
          <el-radio label="sentence">好句</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入内容" />
      </el-form-item>
      <el-form-item label="标签">
        <tag-selector v-model="form.tags" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="form.bookTitle" placeholder="选填，来源书名" />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import TagSelector from './TagSelector.vue'

export default {
  name: 'CollectionDialog',
  components: { TagSelector },
  props: {
    visible: { type: Boolean, default: false },
    data: { type: Object, default: null }
  },
  data() {
    return {
      form: {
        type: 'sentence',
        content: '',
        tags: [],
        bookTitle: ''
      },
      rules: {
        type: [{ required: true, message: '请选择类型', trigger: 'change' }],
        content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
      },
      submitting: false
    }
  },
  computed: {
    isEdit() {
      return this.data && this.data.id
    }
  },
  watch: {
    visible(val) {
      if (val) {
        if (this.data) {
          this.form = {
            type: this.data.type || 'sentence',
            content: this.data.content || '',
            tags: this.data.tags || [],
            bookTitle: this.data.bookTitle || ''
          }
        } else {
          this.form = { type: 'sentence', content: '', tags: [], bookTitle: '' }
        }
      }
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        const payload = { ...this.form }
        if (this.isEdit) {
          payload.id = this.data.id
        }
        this.$emit('submit', payload)
        this.submitting = false
        this.handleClose()
      })
    },
    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>
