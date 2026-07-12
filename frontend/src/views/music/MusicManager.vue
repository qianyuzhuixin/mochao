<template>
  <div class="music-manager">
    <div class="page-header">
      <h2 class="page-title">
        <i class="el-icon-headset" />
        背景音乐管理
      </h2>
      <p class="page-desc">上传和管理你的背景音乐，在练习或写作时播放</p>
    </div>

    <!-- 上传区域 -->
    <div class="upload-section">
      <el-upload
        class="upload-area"
        drag
        action="#"
        :auto-upload="false"
        :show-file-list="false"
        :accept="acceptFormats"
        :on-change="handleFileChange"
      >
        <i class="el-icon-upload" />
        <div class="el-upload__text">
          将音频文件拖到此处，或<em>点击选择</em>
        </div>
        <div class="el-upload__tip" slot="tip">
          支持 MP3 / WAV / OGG / FLAC / AAC / M4A，单个文件不超过 50MB
        </div>
      </el-upload>

      <!-- 上传表单 -->
      <div v-if="pendingFile" class="upload-form">
        <el-form :model="uploadForm" label-width="60px" size="small">
          <el-form-item label="标题">
            <el-input
              v-model="uploadForm.title"
              placeholder="默认使用文件名"
              maxlength="100"
            />
          </el-form-item>
          <el-form-item label="艺术家">
            <el-input
              v-model="uploadForm.artist"
              placeholder="可选"
              maxlength="100"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="uploading" @click="handleUpload">
              上传
            </el-button>
            <el-button @click="handleCancelUpload">取消</el-button>
          </el-form-item>
        </el-form>
        <div class="file-preview">
          <i class="el-icon-document" />
          <span>{{ pendingFile.name }}</span>
          <span class="file-size">{{ formatSize(pendingFile.size) }}</span>
        </div>
      </div>
    </div>

    <!-- 音乐列表 -->
    <div class="music-list-section">
      <div class="list-header">
        <h3>我的音乐库（{{ musicList.length }} 首）</h3>
        <el-button
          v-if="musicList.length > 0"
          type="danger"
          size="small"
          plain
          :disabled="!musicList.length"
          @click="handleClearAll"
        >
          清空全部
        </el-button>
      </div>

      <div v-if="loading" class="loading-state">
        <i class="el-icon-loading" />
        <span>加载中...</span>
      </div>

      <div v-else-if="musicList.length === 0" class="empty-state">
        <el-empty description="还没有音乐，上传你的第一首曲目吧" />
      </div>

      <div v-else class="music-table-wrap">
        <el-table :data="musicList" style="width: 100%" size="medium">
          <el-table-column width="60" align="center">
            <template slot-scope="{ row }">
              <el-button
                :icon="isCurrentTrack(row) && isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"
                :type="isCurrentTrack(row) ? 'primary' : 'default'"
                size="mini"
                circle
                @click="handlePlayRow(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="标题" min-width="160">
            <template slot-scope="{ row }">
              <div class="track-cell">
                <span class="track-title" :class="{ active: isCurrentTrack(row) }">
                  {{ row.title }}
                </span>
                <span v-if="row.artist" class="track-artist">{{ row.artist }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="大小" width="90" align="right">
            <template slot-scope="{ row }">
              <span class="cell-muted">{{ formatSize(row.fileSize) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="上传时间" width="160" align="center">
            <template slot-scope="{ row }">
              <span class="cell-muted">{{ formatDate(row.createdAt) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="{ row }">
              <el-button
                type="danger"
                size="mini"
                icon="el-icon-delete"
                circle
                @click="handleDelete(row)"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'
import { uploadMusic, deleteMusic } from '@/api/music'

export default {
  name: 'MusicManager',
  data() {
    return {
      loading: false,
      uploading: false,
      pendingFile: null,
      uploadForm: {
        title: '',
        artist: ''
      }
    }
  },
  computed: {
    ...mapGetters('music', ['musicList', 'currentTrack', 'isPlaying']),
    acceptFormats() {
      return '.mp3,.wav,.ogg,.flac,.aac,.m4a,.wma'
    }
  },
  created() {
    this.loadMusicList()
  },
  methods: {
    ...mapActions('music', ['fetchMusicList']),

    async loadMusicList() {
      this.loading = true
      await this.fetchMusicList()
      this.loading = false
    },

    handleFileChange(file) {
      this.pendingFile = file.raw
      this.uploadForm.title = file.name.replace(/\.[^.]+$/, '')
      this.uploadForm.artist = ''
    },

    async handleUpload() {
      if (!this.pendingFile) return
      this.uploading = true
      try {
        await uploadMusic(
          this.pendingFile,
          this.uploadForm.title || undefined,
          this.uploadForm.artist || undefined
        )
        this.$message.success('上传成功')
        this.pendingFile = null
        this.uploadForm.title = ''
        this.uploadForm.artist = ''
        await this.fetchMusicList()
      } catch (e) {
        const msg = (e && e.message) || '上传失败'
        this.$message.error(msg)
      } finally {
        this.uploading = false
      }
    },

    handleCancelUpload() {
      this.pendingFile = null
      this.uploadForm.title = ''
      this.uploadForm.artist = ''
    },

    handlePlayRow(track) {
      if (this.isCurrentTrack(track)) {
        this.$store.commit('music/SET_PLAYING', !this.isPlaying)
      } else {
        this.$store.commit('music/SET_CURRENT_TRACK', track)
        this.$store.commit('music/SET_PLAYING', true)
      }
    },

    async handleDelete(row) {
      try {
        await this.$confirm(`确定要删除「${row.title}」吗？删除后不可恢复。`, '删除确认', {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }

      try {
        await deleteMusic(row.id)
        this.$message.success('已删除')
        // 如果删除的是当前播放曲目，停止播放
        if (this.isCurrentTrack(row)) {
          this.$store.commit('music/SET_CURRENT_TRACK', null)
          this.$store.commit('music/SET_PLAYING', false)
        }
        await this.fetchMusicList()
      } catch (e) {
        const msg = (e && e.message) || '删除失败'
        this.$message.error(msg)
      }
    },

    async handleClearAll() {
      try {
        await this.$confirm('确定要清空全部音乐吗？此操作不可恢复！', '清空确认', {
          confirmButtonText: '确定清空',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return
      }

      let hasError = false
      for (const track of this.musicList) {
        try {
          await deleteMusic(track.id)
        } catch {
          hasError = true
        }
      }
      this.$store.commit('music/SET_CURRENT_TRACK', null)
      this.$store.commit('music/SET_PLAYING', false)
      await this.fetchMusicList()

      if (hasError) {
        this.$message.warning('部分文件删除失败')
      } else {
        this.$message.success('已清空全部音乐')
      }
    },

    isCurrentTrack(track) {
      return this.currentTrack && this.currentTrack.id === track.id
    },

    formatSize(bytes) {
      if (!bytes) return '--'
      if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    formatDate(dateStr) {
      if (!dateStr) return '--'
      return dateStr.replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
.music-manager {
  max-width: 800px;
  margin: 0 auto;
  padding: $spacing-xl $spacing-lg;

  .page-header {
    margin-bottom: $spacing-xl;

    .page-title {
      font-size: $font-size-xl;
      color: var(--color-text);
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: $spacing-xs;
    }

    .page-desc {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
    }
  }

  .upload-section {
    margin-bottom: $spacing-xl;

    .upload-area {
      width: 100%;

      ::v-deep .el-upload-dragger {
        width: 100%;
        background: var(--color-card-bg);
        border: 2px dashed var(--color-border);
        border-radius: 8px;
        transition: border-color 0.3s;

        &:hover {
          border-color: var(--color-primary);
        }
      }
    }

    .upload-form {
      margin-top: $spacing-md;
      padding: $spacing-md;
      background: var(--color-card-bg);
      border: 1px solid var(--color-border);
      border-radius: 8px;
      display: flex;
      gap: $spacing-lg;
      align-items: flex-end;

      .el-form {
        flex: 1;

        .el-form-item {
          margin-bottom: 12px;

          &:last-child {
            margin-bottom: 0;
          }
        }
      }

      .file-preview {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: $font-size-sm;
        color: var(--color-text-secondary);
        padding-bottom: 6px;

        .file-size {
          color: var(--color-text-placeholder);
          font-size: $font-size-xs;
        }
      }
    }
  }

  .music-list-section {
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: $spacing-md;

      h3 {
        font-size: $font-size-lg;
        color: var(--color-text);
      }
    }

    .loading-state {
      text-align: center;
      padding: $spacing-xxl;
      color: var(--color-text-secondary);

      i {
        font-size: 24px;
        margin-right: 8px;
      }
    }

    .empty-state {
      padding: $spacing-xxl 0;
    }

    .music-table-wrap {
      background: var(--color-card-bg);
      border-radius: 8px;
      border: 1px solid var(--color-border);
      overflow: hidden;

      .track-cell {
        display: flex;
        flex-direction: column;

        .track-title {
          font-size: $font-size-base;
          color: var(--color-text);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;

          &.active {
            color: var(--color-primary);
            font-weight: 500;
          }
        }

        .track-artist {
          font-size: $font-size-xs;
          color: var(--color-text-secondary);
        }
      }

      .cell-muted {
        font-size: $font-size-sm;
        color: var(--color-text-secondary);
      }
    }
  }
}

@media (max-width: 768px) {
  .music-manager {
    padding: $spacing-md $spacing-sm;

    .upload-section .upload-form {
      flex-direction: column;

      .file-preview {
        padding-bottom: 0;
      }
    }
  }
}
</style>
