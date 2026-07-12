<template>
  <div class="music-player-bar" :class="{ 'is-active': hasTrack }">
    <div class="player-controls">
      <!-- 上一首 -->
      <el-button
        icon="el-icon-d-arrow-left"
        size="mini"
        circle
        :disabled="!hasTrack"
        @click="handlePrev"
      />

      <!-- 播放/暂停 -->
      <el-button
        :icon="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"
        :type="isPlaying ? 'primary' : 'default'"
        size="mini"
        circle
        :disabled="!hasTrack"
        @click="handleTogglePlay"
      />

      <!-- 下一首 -->
      <el-button
        icon="el-icon-d-arrow-right"
        size="mini"
        circle
        :disabled="!hasTrack"
        @click="handleNext"
      />
    </div>

    <!-- 当前曲目 -->
    <div class="player-track-info">
      <el-popover
        placement="top"
        trigger="click"
        width="280"
        popper-class="music-playlist-popover"
      >
        <div class="playlist-panel">
          <div class="playlist-header">
            <span>我的音乐库</span>
            <el-button type="text" size="mini" @click="goToManager">管理</el-button>
          </div>
          <div class="playlist-body" v-if="musicList.length > 0">
            <div
              v-for="track in musicList"
              :key="track.id"
              class="playlist-item"
              :class="{ active: currentTrack && currentTrack.id === track.id }"
              @click="handleSelectTrack(track)"
            >
              <i
                :class="currentTrack && currentTrack.id === track.id && isPlaying
                  ? 'el-icon-video-pause'
                  : 'el-icon-caret-right'"
                class="play-icon"
              />
              <div class="track-info">
                <span class="track-title">{{ track.title }}</span>
                <span v-if="track.artist" class="track-artist">{{ track.artist }}</span>
              </div>
              <span class="track-size">{{ formatSize(track.fileSize) }}</span>
            </div>
          </div>
          <div v-else class="playlist-empty">
            <p>还没有音乐，快去添加吧</p>
            <el-button type="primary" size="small" @click="goToManager">添加音乐</el-button>
          </div>
        </div>
        <span slot="reference" class="track-display">
          <i class="el-icon-headset" />
          <span v-if="hasTrack" class="track-name">{{ currentTrack.title }}</span>
          <span v-else class="track-placeholder">未选择音乐</span>
          <i class="el-icon-arrow-up" />
        </span>
      </el-popover>
    </div>

    <!-- 音量 -->
    <div class="player-volume">
      <i
        class="el-icon-bell"
        :class="{ muted: volume === 0 }"
        @click="toggleMute"
      />
      <el-slider
        v-model="localVolume"
        :min="0"
        :max="100"
        :show-tooltip="false"
        class="volume-slider"
        @input="handleVolume"
      />
    </div>

    <!-- 隐藏的 audio -->
    <audio
      ref="audio"
      @play="onAudioPlay"
      @pause="onAudioPause"
      @ended="onAudioEnded"
      @error="onAudioError"
    />
  </div>
</template>

<script>
import { mapGetters, mapActions } from 'vuex'

const BASE_API = process.env.VUE_APP_API_BASE_URL || '/api/v1'

export default {
  name: 'MusicPlayer',
  data() {
    return {
      localVolume: 60,
      previousVolume: 60,
      audioLoaded: false
    }
  },
  computed: {
    ...mapGetters('music', ['musicList', 'currentTrack', 'isPlaying', 'volume', 'hasTrack'])
  },
  watch: {
    currentTrack: {
      immediate: true,
      handler(track) {
        if (track) {
          this.loadTrack(track)
        }
      }
    },
    isPlaying(val) {
      if (!this.$refs.audio) return
      if (val && this.audioLoaded) {
        this.$refs.audio.play().catch(() => {})
      } else if (!val) {
        this.$refs.audio.pause()
      }
    },
    volume: {
      immediate: true,
      handler(val) {
        this.localVolume = val
        if (this.$refs.audio) {
          this.$refs.audio.volume = val / 100
        }
      }
    }
  },
  async created() {
    await this.fetchMusicList()
    // 恢复上次播放
    const savedTrackId = localStorage.getItem('mochao_music_track_id')
    if (savedTrackId && this.musicList.length > 0) {
      const track = this.musicList.find(m => m.id === parseInt(savedTrackId))
      if (track) {
        this.$store.commit('music/SET_CURRENT_TRACK', track)
      }
    }
  },
  beforeDestroy() {
    this.destroyAudio()
  },
  methods: {
    ...mapActions('music', ['fetchMusicList', 'playNext', 'playPrev', 'togglePlay']),

    getFileUrl(track) {
      return `${BASE_API}/files/music/${track.filePath}`
    },

    loadTrack(track) {
      this.destroyAudio()
      const audio = this.$refs.audio
      if (!audio) return

      audio.src = this.getFileUrl(track)
      audio.volume = this.localVolume / 100
      audio.load()

      this.audioLoaded = true
      localStorage.setItem('mochao_music_track_id', track.id)

      if (this.isPlaying) {
        audio.play().catch(() => {})
      }

      // 更新浏览器媒体会话（支持媒体键控制）
      if ('mediaSession' in navigator) {
        navigator.mediaSession.metadata = new MediaMetadata({
          title: track.title,
          artist: track.artist || '未知艺术家'
        })
        navigator.mediaSession.setActionHandler('previoustrack', () => this.handlePrev())
        navigator.mediaSession.setActionHandler('nexttrack', () => this.handleNext())
        navigator.mediaSession.setActionHandler('pause', () => this.handlePause())
        navigator.mediaSession.setActionHandler('play', () => this.handlePlay())
      }
    },

    handleTogglePlay() {
      this.$store.commit('music/SET_PLAYING', !this.isPlaying)
    },

    handlePlay() {
      this.$store.commit('music/SET_PLAYING', true)
    },

    handlePause() {
      this.$store.commit('music/SET_PLAYING', false)
    },

    handlePrev() {
      this.playPrev()
    },

    handleNext() {
      this.playNext()
    },

    handleSelectTrack(track) {
      if (this.currentTrack && this.currentTrack.id === track.id) {
        this.handleTogglePlay()
      } else {
        this.playTrack(track)
      }
    },

    playTrack(track) {
      this.$store.commit('music/SET_CURRENT_TRACK', track)
      this.$store.commit('music/SET_PLAYING', true)
    },

    handleVolume(val) {
      this.localVolume = val
      if (this.$refs.audio) {
        this.$refs.audio.volume = val / 100
      }
      this.$store.commit('music/SET_VOLUME', val)
    },

    toggleMute() {
      if (this.localVolume > 0) {
        this.previousVolume = this.localVolume
        this.handleVolume(0)
      } else {
        this.handleVolume(this.previousVolume || 60)
      }
    },

    onAudioPlay() {
      if (!this.isPlaying) {
        this.$store.commit('music/SET_PLAYING', true)
      }
    },

    onAudioPause() {
      if (this.$refs.audio && !this.$refs.audio.ended) {
        // 只在非结束状态手动暂停时更新
      }
    },

    onAudioEnded() {
      this.playNext()
    },

    onAudioError() {
      this.$message.warning('音乐文件加载失败，请尝试重新上传')
      this.destroyAudio()
    },

    goToManager() {
      this.$router.push('/music').catch(() => {})
    },

    destroyAudio() {
      if (this.$refs.audio) {
        this.$refs.audio.pause()
        this.$refs.audio.removeAttribute('src')
        this.$refs.audio.load()
      }
      this.audioLoaded = false
    },

    formatSize(bytes) {
      if (!bytes) return '--'
      if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    }
  }
}
</script>

<style lang="scss" scoped>
.music-player-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 16px;
  background: var(--color-card-bg);
  border-top: 1px solid var(--color-border);
  opacity: 0.5;
  transition: opacity 0.3s;

  &.is-active,
  &:hover {
    opacity: 1;
  }

  .player-controls {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }

  .player-track-info {
    flex: 1;
    min-width: 0;

    .track-display {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      color: var(--color-text);
      font-size: $font-size-sm;
      padding: 4px 8px;
      border-radius: 4px;
      max-width: 100%;

      &:hover {
        background: var(--color-border);
      }

      .track-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 200px;
      }

      .track-placeholder {
        color: var(--color-text-placeholder);
        font-size: $font-size-xs;
      }

      .el-icon-arrow-up {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
        margin-left: 2px;
      }
    }
  }

  .player-volume {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;

    .el-icon-bell {
      cursor: pointer;
      color: var(--color-text-secondary);
      font-size: 14px;
      transition: color 0.2s;

      &:hover {
        color: var(--color-primary);
      }

      &.muted {
        color: var(--color-text-placeholder);
      }
    }

    .volume-slider {
      width: 80px;
    }
  }
}

@media (max-width: 768px) {
  .music-player-bar {
    padding: 6px 10px;

    .player-track-info .track-display .track-name {
      max-width: 100px;
    }

    .player-volume .volume-slider {
      width: 50px;
    }
  }
}
</style>

<style lang="scss">
.music-playlist-popover {
  padding: 0 !important;
  max-height: 360px;
  overflow: hidden;

  .playlist-panel {
    .playlist-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px 6px;
      font-weight: 600;
      border-bottom: 1px solid var(--color-border);
    }

    .playlist-body {
      max-height: 280px;
      overflow-y: auto;
    }

    .playlist-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      cursor: pointer;
      transition: background 0.15s;

      &:hover {
        background: var(--color-bg);
      }

      &.active {
        color: var(--color-primary);
        background: var(--color-bg);

        .track-title {
          color: var(--color-primary);
        }
      }

      .play-icon {
        font-size: 12px;
        flex-shrink: 0;
      }

      .track-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .track-title {
          font-size: 13px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .track-artist {
          font-size: 11px;
          color: var(--color-text-secondary);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .track-size {
        font-size: 11px;
        color: var(--color-text-placeholder);
        flex-shrink: 0;
      }
    }

    .playlist-empty {
      padding: 30px 14px;
      text-align: center;

      p {
        color: var(--color-text-placeholder);
        font-size: 13px;
        margin-bottom: 12px;
      }
    }
  }
}
</style>
