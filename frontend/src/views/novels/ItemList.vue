<template>
  <div class="item-list-page page-container" v-loading="loading">
    <div class="page-header">
      <el-button type="text" icon="el-icon-arrow-left" @click="$router.push(`/novels/${novelId}`)">
        返回工作台
      </el-button>
      <h1 class="page-title">物品设定</h1>
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">
        新建物品
      </el-button>
    </div>

    <div class="item-grid">
      <item-card
        v-for="item in list"
        :key="item.id"
        :data="item"
        @edit="handleEdit"
        @delete="handleDelete"
      />
    </div>

    <empty-state v-if="!loading && list.length === 0" text="暂无物品" description="创建你的第一个物品" />

    <el-dialog :title="editing ? '编辑物品' : '新建物品'" :visible.sync="dialogVisible" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="物品名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
            <el-option label="武器" value="weapon" />
            <el-option label="防具" value="armor" />
            <el-option label="道具" value="item" />
            <el-option label="功法" value="skill" />
            <el-option label="材料" value="material" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="物品描述" />
        </el-form-item>
        <el-form-item label="属性">
          <el-input v-model="form.attributes" type="textarea" :rows="3" placeholder="物品特殊属性" />
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
import ItemCard from '@/components/novel/ItemCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getItems, createItem, updateItem, deleteItem } from '@/api/novel'

export default {
  name: 'ItemList',
  components: { ItemCard, EmptyState },
  data() {
    return {
      list: [],
      loading: false,
      dialogVisible: false,
      editing: null,
      submitting: false,
      form: { name: '', type: '', description: '', attributes: '' },
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    novelId() { return this.$route.params.id }
  },
  created() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        this.list = await getItems(this.novelId) || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.editing = null
      this.form = { name: '', type: '', description: '', attributes: '' }
      this.dialogVisible = true
    },
    handleEdit(item) {
      this.editing = item
      this.form = {
        name: item.name || '',
        type: item.type || '',
        description: item.description || '',
        attributes: item.attributes || ''
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          if (this.editing) {
            await updateItem(this.novelId, this.editing.id, this.form)
            this.$message.success('更新成功')
          } else {
            await createItem(this.novelId, this.form)
            this.$message.success('创建成功')
          }
          this.dialogVisible = false
          this.fetchData()
        } catch (e) {} finally {
          this.submitting = false
        }
      })
    },
    handleDelete(item) {
      this.$confirm(`确定要删除物品"${item.name}"吗？`, '提示', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      }).then(async () => {
        try {
          await deleteItem(this.novelId, item.id)
          this.$message.success('删除成功')
          this.fetchData()
        } catch (e) {}
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.item-list-page {
  .page-header {
    display: flex;
    align-items: center;
    gap: #{$spacing-md};
    margin-bottom: #{$spacing-lg};

    .page-title { margin: 0; flex: 1; }
  }

  .item-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: #{$spacing-md};
  }
}
</style>
