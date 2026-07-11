<template>
  <div class="tag-selector">
    <div class="tag-list">
      <el-tag
        v-for="tag in innerTags"
        :key="tag"
        closable
        size="small"
        @close="removeTag(tag)"
        class="mr-sm mb-sm"
      >
        {{ tag }}
      </el-tag>
    </div>
    <el-input
      v-if="inputVisible"
      ref="inputRef"
      v-model="inputValue"
      size="small"
      style="width: 120px"
      @keyup.enter.native="confirmInput"
      @blur="confirmInput"
    />
    <el-button
      v-else
      size="small"
      icon="el-icon-plus"
      @click="showInput"
    >
      添加标签
    </el-button>
  </div>
</template>

<script>
export default {
  name: 'TagSelector',
  props: {
    value: { type: Array, default: () => [] }
  },
  data() {
    return {
      inputVisible: false,
      inputValue: ''
    }
  },
  computed: {
    innerTags: {
      get() {
        return this.value || []
      },
      set(val) {
        this.$emit('input', val)
      }
    }
  },
  methods: {
    showInput() {
      this.inputVisible = true
      this.$nextTick(() => {
        this.$refs.inputRef && this.$refs.inputRef.focus()
      })
    },
    confirmInput() {
      const val = this.inputValue.trim()
      if (val && !this.innerTags.includes(val)) {
        this.innerTags = [...this.innerTags, val]
      }
      this.inputVisible = false
      this.inputValue = ''
    },
    removeTag(tag) {
      this.innerTags = this.innerTags.filter(t => t !== tag)
    }
  }
}
</script>

<style lang="scss" scoped>
.tag-selector {
  .tag-list {
    display: inline-block;
  }
}
</style>
