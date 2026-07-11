<template>
  <div class="character-card">
    <div class="card-header" @click="$emit('click', data)">
      <el-avatar :size="48" :style="{ backgroundColor: avatarColor }">
        {{ data.name ? data.name.charAt(0) : '?' }}
      </el-avatar>
      <div class="header-info">
        <h3 class="char-name">{{ data.name || '未命名' }}</h3>
        <span class="char-role">{{ data.role || '角色' }}</span>
      </div>
      <el-dropdown trigger="click" @command="handleCommand">
        <i class="el-icon-more card-more" />
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="edit">编辑</el-dropdown-item>
          <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
    <div v-if="data.description" class="card-body">
      <p class="char-desc">{{ data.description }}</p>
    </div>
    <div v-if="data.tags && data.tags.length" class="card-footer">
      <el-tag v-for="tag in data.tags.slice(0, 3)" :key="tag" size="mini" effect="plain">
        {{ tag }}
      </el-tag>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CharacterCard',
  props: {
    data: { type: Object, required: true }
  },
  computed: {
    avatarColor() {
      const colors = ['#4A6CF7', '#52C41A', '#FAAD14', '#FF4D4F', '#722ED1', '#13C2C2']
      let hash = 0
      const name = this.data.name || ''
      for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    }
  },
  methods: {
    handleCommand(command) {
      this.$emit(command, this.data)
    }
  }
}
</script>

<style lang="scss" scoped>
.character-card {
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  padding: #{$spacing-md};
  transition: all $transition-base;

  &:hover {
    box-shadow: var(--shadow-md);
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: #{$spacing-sm};
    cursor: pointer;

    .header-info {
      flex: 1;
      min-width: 0;

      .char-name {
        font-size: $font-size-md;
        font-weight: 600;
        color: var(--color-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .char-role {
        font-size: $font-size-xs;
        color: var(--color-text-secondary);
      }
    }

    .card-more {
      cursor: pointer;
      color: var(--color-text-secondary);

      &:hover {
        color: var(--color-primary);
      }
    }
  }

  .card-body {
    margin-top: #{$spacing-sm};

    .char-desc {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .card-footer {
    display: flex;
    gap: #{$spacing-xs};
    flex-wrap: wrap;
    margin-top: #{$spacing-sm};
  }
}
</style>
