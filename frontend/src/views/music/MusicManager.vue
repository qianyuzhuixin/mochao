<template>
  <DefaultLayout>
    <div class="music-manager">
      <!-- ====== Hero Banner ====== -->
      <div class="hero-banner">
        <div class="hero-content">
          <div class="hero-icon-wrap">
            <i class="el-icon-headset" />
          </div>
          <div class="hero-text">
            <h2 class="hero-title">背景音乐</h2>
            <p class="hero-subtitle">打造你的专属写作氛围，让音乐伴随每一次灵感迸发</p>
          </div>
        </div>
        <div class="hero-wave" />
      </div>

      <!-- ====== Stats 统计卡片 ====== -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="el-icon-document" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ musicList.length }}</div>
            <div class="stat-label">曲库数量</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon orange">
            <i class="el-icon-time" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatDuration(totalDuration) }}</div>
            <div class="stat-label">累计时长</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            <i class="el-icon-s-data" />
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatSize(totalSize) }}</div>
            <div class="stat-label">总大小</div>
          </div>
        </div>
      </div>

      <!-- ====== 主 Tab ====== -->
      <div class="main-tabs">
        <span class="main-tab active">
          <i class="el-icon-service" /> 我的音乐库
        </span>
      </div>

      <!-- ====== 我的音乐库 ====== -->

      <!-- ====== 上传区域 ====== -->
      <div class="upload-section">
        <div class="section-title">
          <i class="el-icon-upload2" />
          <span>上传音乐</span>
        </div>

        <el-upload
          class="upload-area"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :accept="acceptFormats"
          :on-change="handleFileChange"
        >
          <div class="upload-inner">
            <div class="upload-icon-wrap">
              <i class="el-icon-upload" />
            </div>
            <div class="upload-text">
              <span class="upload-drag-hint">将音频文件拖到此处</span>
              <span class="upload-or">或</span>
              <span class="upload-click">点击选择</span>
            </div>
            <div class="upload-formats">
              <span class="format-tag">MP3</span>
              <span class="format-tag">WAV</span>
              <span class="format-tag">FLAC</span>
              <span class="format-tag">AAC</span>
              <span class="format-tag">M4A</span>
              <span class="format-tag">OGG</span>
            </div>
          </div>
        </el-upload>

        <!-- 上传表单 -->
        <div v-if="pendingFile" class="upload-form-card">
          <div class="upload-form-header">
            <i class="el-icon-document" />
            <span class="file-name">{{ pendingFile.name }}</span>
            <span class="file-size">{{ formatSize(pendingFile.size) }}</span>
          </div>
          <el-form :model="uploadForm" label-width="60px" size="small">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="标题">
                  <el-input
                    v-model="uploadForm.title"
                    placeholder="默认使用文件名"
                    maxlength="100"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="艺术家">
                  <el-input
                    v-model="uploadForm.artist"
                    placeholder="可选"
                    maxlength="100"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" :loading="uploading" @click="handleUpload">
                <i class="el-icon-upload" /> 确认上传
              </el-button>
              <el-button @click="handleCancelUpload">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- ====== 音乐列表 ====== -->
      <div class="music-list-section">
        <div class="section-title">
          <i class="el-icon-service" />
          <span>我的音乐库</span>
          <span v-if="musicList.length > 0" class="list-count">{{ musicList.length }} 首</span>
          <el-button
            v-if="musicList.length > 0"
            class="clear-btn"
            type="danger"
            size="mini"
            plain
            circle
            icon="el-icon-delete-solid"
            :disabled="!musicList.length"
            @click="handleClearAll"
          />
        </div>

        <!-- 筛选标签 -->
        <div v-if="musicList.length > 0" class="filter-tabs">
          <span
            class="filter-tab"
            :class="{ active: listFilter === 'all' }"
            @click="listFilter = 'all'"
          >
            <i class="el-icon-menu" /> 全部 ({{ musicList.length }})
          </span>
          <span
            class="filter-tab"
            :class="{ active: listFilter === 'favorite' }"
            @click="listFilter = 'favorite'"
          >
            <i class="el-icon-star-on" /> 收藏 ({{ favoriteCount }})
          </span>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="loading-state">
          <div class="loading-spin">
            <i class="el-icon-loading" />
          </div>
          <span>正在加载音乐库...</span>
        </div>

        <!-- Empty -->
        <div v-else-if="filteredList.length === 0" class="empty-state">
          <div class="empty-illustration">
            <i :class="listFilter === 'favorite' ? 'el-icon-star-off' : 'el-icon-headset'" />
          </div>
          <div class="empty-title">{{ listFilter === 'favorite' ? '还没有收藏音乐' : '还没有音乐' }}</div>
          <div class="empty-desc">{{ listFilter === 'favorite' ? '在列表中点击星星收藏喜欢的音乐' : '上传你的第一首曲目，为写作时光增添氛围' }}</div>
        </div>

        <!-- List -->
        <div v-else class="music-list">
          <div
            v-for="row in filteredList"
            :key="row.id"
            class="music-row"
            :class="{ active: isCurrentTrack(row) }"
            @click="handlePlayRow(row)"
          >
            <div class="row-play-btn">
              <i
                :class="isCurrentTrack(row) && isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"
              />
            </div>
            <div class="row-info">
              <div class="row-title">
                <span class="title-text" :class="{ active: isCurrentTrack(row) }">{{ row.title }}</span>
                <span v-if="row.artist" class="row-artist">— {{ row.artist }}</span>
              </div>
              <div class="row-meta">
                <span class="meta-item">{{ formatSize(row.fileSize) }}</span>
                <span class="meta-dot">·</span>
                <span class="meta-item">{{ formatDate(row.createdAt) }}</span>
              </div>
            </div>
            <div class="row-actions" @click.stop>
              <el-button
                :type="row.favorite === 1 ? 'warning' : 'default'"
                size="mini"
                :icon="row.favorite === 1 ? 'el-icon-star-on' : 'el-icon-star-off'"
                circle
                plain
                @click="handleToggleFavorite(row)"
              />
              <el-button
                type="danger"
                size="mini"
                icon="el-icon-delete"
                circle
                plain
                @click="handleDelete(row)"
              />
            </div>
          </div>
        </div>
      </div>

    </div>
  </DefaultLayout>
</template>

<script>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { mapGetters, mapActions } from 'vuex'
import { uploadMusic, deleteMusic } from '@/api/music'

export default {
  name: 'MusicManager',
  components: { DefaultLayout },
  data() {
    return {
      loading: false,
      uploading: false,
      pendingFile: null,
      listFilter: 'all',
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
    },
    totalSize() {
      return this.musicList.reduce((sum, m) => sum + (m.fileSize || 0), 0)
    },
    totalDuration() {
      return this.musicList.reduce((sum, m) => sum + (m.duration || 0), 0)
    },
    favoriteCount() {
      return this.musicList.filter(m => m.favorite === 1).length
    },
    filteredList() {
      if (this.listFilter === 'favorite') {
        return this.musicList.filter(m => m.favorite === 1)
      }
      return this.musicList
    }
  },
  created() {
    this.loadMusicList()
  },
  methods: {
    ...mapActions('music', ['fetchMusicList', 'fetchFavoriteMusic', 'toggleFavoriteTrack']),

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

    async handleToggleFavorite(row) {
      try {
        const favorite = await this.toggleFavoriteTrack(row.id)
        this.$message({
          message: favorite === 1 ? '已收藏' : '已取消收藏',
          duration: 1200,
          type: 'success'
        })
      } catch {
        this.$message.error('操作失败')
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
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    formatDate(dateStr) {
      if (!dateStr) return '--'
      return dateStr.replace('T', ' ').substring(0, 16)
    },

    formatDuration(seconds) {
      if (!seconds) return '0:00'
      const m = Math.floor(seconds / 60)
      const s = Math.floor(seconds % 60)
      return `${m}:${String(s).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.music-manager {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px 48px;
}

/* ====== Hero Banner ====== */
.hero-banner {
  position: relative;
  margin: 0 -24px 28px;
  padding: 40px 24px 48px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light, #a0cfff) 100%);
  overflow: hidden;

  .hero-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    gap: 20px;
    max-width: 900px;
    margin: 0 auto;
  }

  .hero-icon-wrap {
    width: 64px;
    height: 64px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    color: #fff;
    flex-shrink: 0;
  }

  .hero-text {
    .hero-title {
      font-size: 28px;
      font-weight: 600;
      color: #fff;
      margin: 0 0 6px;
      letter-spacing: -0.5px;
    }
    .hero-subtitle {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.85);
      margin: 0;
    }
  }

  .hero-wave {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 40px;
    background: var(--color-bg);
    clip-path: polygon(
      0 100%, 5% 60%, 10% 75%, 15% 55%, 20% 70%,
      25% 50%, 30% 65%, 35% 45%, 40% 60%, 45% 40%,
      50% 55%, 55% 35%, 60% 50%, 65% 30%, 70% 45%,
      75% 25%, 80% 40%, 85% 20%, 90% 35%, 95% 15%,
      100% 30%, 100% 100%
    );
    opacity: 0.08;
  }
}

/* ====== Stats Row ====== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 28px;

  .stat-card {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 18px 20px;
    background: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: 12px;
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    }

    .stat-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: rgba(64, 158, 255, 0.1);
      color: var(--color-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;

      &.orange {
        background: rgba(230, 162, 60, 0.1);
        color: #e6a23c;
      }
      &.purple {
        background: rgba(144, 147, 153, 0.1);
        color: #909399;
      }
    }

    .stat-info {
      .stat-value {
        font-size: 22px;
        font-weight: 600;
        color: var(--color-text);
        line-height: 1.2;
      }
      .stat-label {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 2px;
      }
    }
  }
}

/* ====== Section Title ====== */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 14px;

  i {
    font-size: 18px;
    color: var(--color-primary);
  }

  .list-count {
    font-size: 12px;
    font-weight: 400;
    color: var(--color-text-placeholder);
    background: var(--color-card-bg);
    border: 1px solid var(--color-border);
    padding: 1px 10px;
    border-radius: 12px;
    margin-left: 4px;
  }

  .clear-btn {
    margin-left: auto;
  }
}

/* ====== Main Tabs ====== */
.main-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  background: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;

  .main-tab {
    flex: 1;
    text-align: center;
    padding: 10px 0;
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all 0.2s;
    border-bottom: 2px solid transparent;

    i {
      margin-right: 4px;
      font-size: 16px;
      vertical-align: -1px;
    }

    &:first-child {
      border-right: 1px solid var(--color-border);
    }

    &:hover {
      color: var(--color-primary);
      background: rgba(64, 158, 255, 0.03);
    }

    &.active {
      color: var(--color-primary);
      background: rgba(64, 158, 255, 0.06);
      border-bottom-color: var(--color-primary);
    }
  }
}

/* ====== Upload Section ====== */
.upload-section {
  margin-bottom: 32px;

  .upload-area {
    ::v-deep .el-upload {
      width: 100%;
    }
    ::v-deep .el-upload-dragger {
      width: 100%;
      height: auto;
      padding: 32px 20px;
      background: var(--color-card-bg);
      border: 2px dashed var(--color-border);
      border-radius: 12px;
      transition: all 0.3s ease;

      &:hover {
        border-color: var(--color-primary);
        background: rgba(64, 158, 255, 0.03);
      }
      &.is-dragover {
        border-color: var(--color-primary);
        background: rgba(64, 158, 255, 0.06);
        transform: scale(1.005);
      }
    }
  }

  .upload-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .upload-icon-wrap {
      width: 56px;
      height: 56px;
      border-radius: 16px;
      background: rgba(64, 158, 255, 0.08);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: var(--color-primary);
      transition: transform 0.3s ease;
    }

    .upload-text {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      color: var(--color-text-secondary);

      .upload-drag-hint { color: var(--color-text); }
      .upload-or { color: var(--color-text-placeholder); font-size: 12px; }
      .upload-click {
        color: var(--color-primary);
        cursor: pointer;
        font-weight: 500;
      }
    }

    .upload-formats {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      justify-content: center;
      margin-top: 4px;

      .format-tag {
        font-size: 11px;
        color: var(--color-text-placeholder);
        background: var(--color-bg);
        border: 1px solid var(--color-border);
        padding: 2px 8px;
        border-radius: 6px;
      }
    }
  }

  .upload-form-card {
    margin-top: 16px;
    padding: 20px 24px;
    background: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: 12px;
    animation: slideDown 0.3s ease;

    .upload-form-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid var(--color-border);

      i {
        font-size: 18px;
        color: var(--color-primary);
      }
      .file-name {
        font-size: 14px;
        color: var(--color-text);
        font-weight: 500;
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      .file-size {
        font-size: 12px;
        color: var(--color-text-placeholder);
        flex-shrink: 0;
      }
    }
  }
}

/* ====== Music List ====== */
.music-list-section {
  .filter-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;

    .filter-tab {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: var(--color-text-secondary);
      background: var(--color-card-bg);
      border: 1px solid var(--color-border);
      padding: 4px 14px;
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        border-color: var(--color-primary);
        color: var(--color-primary);
      }

      &.active {
        background: var(--color-primary);
        border-color: var(--color-primary);
        color: #fff;
      }
    }
  }

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    padding: 48px 0;
    color: var(--color-text-secondary);

    .loading-spin {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: var(--color-card-bg);
      border: 1px solid var(--color-border);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: var(--color-primary);
      animation: pulse 2s infinite;
    }
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 48px 0;

    .empty-illustration {
      width: 80px;
      height: 80px;
      border-radius: 24px;
      background: rgba(64, 158, 255, 0.06);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36px;
      color: var(--color-primary);
      margin-bottom: 16px;
      opacity: 0.6;
    }
    .empty-title {
      font-size: 16px;
      font-weight: 500;
      color: var(--color-text);
      margin-bottom: 4px;
    }
    .empty-desc {
      font-size: 13px;
      color: var(--color-text-placeholder);
    }
  }

  .music-list {
    background: var(--color-card-bg);
    border: 1px solid var(--color-border);
    border-radius: 12px;
    overflow: hidden;

    .music-row {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      border-bottom: 1px solid var(--color-border);
      cursor: pointer;
      transition: background 0.2s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: var(--color-hover-bg, rgba(64, 158, 255, 0.04));
      }

      &.active {
        background: rgba(64, 158, 255, 0.06);
      }

      .row-play-btn {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: var(--color-bg);
        border: 1px solid var(--color-border);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        color: var(--color-text-secondary);
        flex-shrink: 0;
        transition: all 0.2s;

        .active & {
          background: var(--color-primary);
          border-color: var(--color-primary);
          color: #fff;
        }
      }

      .row-info {
        flex: 1;
        min-width: 0;

        .row-title {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 2px;

          .title-text {
            font-size: 14px;
            font-weight: 500;
            color: var(--color-text);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;

            &.active {
              color: var(--color-primary);
            }
          }
          .row-artist {
            font-size: 12px;
            color: var(--color-text-placeholder);
            white-space: nowrap;
          }
        }

        .row-meta {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 12px;
          color: var(--color-text-placeholder);

          .meta-dot { opacity: 0.5; }
        }
      }

      .row-actions {
        display: flex;
        align-items: center;
        gap: 4px;
        opacity: 0;
        transition: opacity 0.2s;
        flex-shrink: 0;
      }

      &:hover .row-actions {
        opacity: 1;
      }
    }
  }
}

/* ====== Animations ====== */
@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* ====== Responsive ====== */
@media (max-width: 768px) {
  .music-manager {
    padding: 0 16px 32px;
  }

  .hero-banner {
    margin: 0 -16px 24px;
    padding: 28px 16px 36px;

    .hero-icon-wrap {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      font-size: 22px;
    }
    .hero-text .hero-title {
      font-size: 22px;
    }
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .upload-section .upload-form-card {
    padding: 16px;
  }

  .music-list-section .music-list .music-row {
    .row-actions {
      opacity: 1;
    }
  }
}
</style>
