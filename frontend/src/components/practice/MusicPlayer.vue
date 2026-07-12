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

      <!-- 播放模式 -->
      <el-tooltip :content="playModeLabel" placement="bottom" :open-delay="300">
        <el-button
          :icon="playModeIcon"
          size="mini"
          circle
          :type="playMode === 'favorite' ? 'warning' : 'default'"
          :disabled="!hasTrack"
          @click="handleCycleMode"
        />
      </el-tooltip>
    </div>

    <!-- 当前曲目 + 进度 -->
    <div class="player-track-info">
      <el-popover
        placement="top"
        trigger="click"
        width="300"
        popper-class="music-playlist-popover"
      >
        <div class="playlist-panel">
          <div class="playlist-header">
            <span>播放列表</span>
            <div class="playlist-tabs">
              <span
                class="tab"
                :class="{ active: listFilter === 'all' }"
                @click="listFilter = 'all'"
              >全部 {{ musicList.length }}</span>
              <span
                class="tab"
                :class="{ active: listFilter === 'favorite' }"
                @click="listFilter = 'favorite'"
              >收藏 {{ favoriteCount }}</span>
            </div>
            <el-button type="text" size="mini" @click="goToManager">管理</el-button>
          </div>
          <div class="playlist-body" v-if="displayList.length > 0">
            <div
              v-for="track in displayList"
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
              <i
                class="fav-icon"
                :class="track.favorite === 1 ? 'el-icon-star-on' : 'el-icon-star-off'"
                @click.stop="handleToggleFavorite(track)"
              />
            </div>
          </div>
          <div v-else class="playlist-empty">
            <p>{{ listFilter === 'favorite' ? '还没有收藏音乐' : '还没有音乐，快去添加吧' }}</p>
            <el-button type="primary" size="small" @click="goToManager">
              {{ listFilter === 'favorite' ? '去收藏' : '添加音乐' }}
            </el-button>
          </div>
        </div>
        <span slot="reference" class="track-display">
          <i class="el-icon-headset" />
          <span v-if="hasTrack" class="track-name">{{ currentTrack.title }}</span>
          <span v-else class="track-placeholder">未选择音乐</span>
          <i class="el-icon-arrow-up" />
        </span>
      </el-popover>

      <!-- 进度条 + 时间 -->
      <div class="progress-section" v-if="hasTrack">
        <span class="time-display">{{ formatTime(currentTime) }}</span>
        <el-slider
          v-model="progressPercent"
          :min="0"
          :max="100"
          :step="0.1"
          :show-tooltip="false"
          class="progress-slider"
          @change="handleSeek"
        />
        <span class="time-display">{{ formatTime(duration) }}</span>
      </div>
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
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
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
      audioLoaded: false,
      currentTime: 0,
      duration: 0,
      progressPercent: 0,
      seeking: false,
      listFilter: 'all'
    }
  },
  computed: {
    ...mapGetters('music', [
      'musicList', 'favoriteList', 'currentTrack', 'isPlaying',
      'volume', 'hasTrack', 'playMode', 'activeList'
    ]),
    playModeIcon() {
      if (this.playMode === 'shuffle') return 'el-icon-s-operation'
      if (this.playMode === 'favorite') return 'el-icon-star-on'
      return 'el-icon-sort'
    },
    playModeLabel() {
      if (this.playMode === 'shuffle') return '乱序播放'
      if (this.playMode === 'favorite') return '收藏播放'
      return '顺序播放'
    },
    favoriteCount() {
      return this.musicList.filter(m => m.favorite === 1).length
    },
    displayList() {
      if (this.listFilter === 'favorite') {
        return this.musicList.filter(m => m.favorite === 1)
      }
      return this.musicList
    }
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
    await this.fetchFavoriteMusic()

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
    ...mapActions('music', [
      'fetchMusicList', 'fetchFavoriteMusic', 'playNext', 'playPrev',
      'togglePlay', 'cyclePlayMode', 'toggleFavoriteTrack'
    ]),

    getFileUrl(track) {
      return `${BASE_API}/files/music/${track.id}`
    },

    loadTrack(track) {
      this.destroyAudio()
      const audio = this.$refs.audio
      if (!audio) return

      audio.src = this.getFileUrl(track)
      audio.volume = this.localVolume / 100
      audio.load()

      this.audioLoaded = true
      this.currentTime = 0
      this.duration = track.duration || 0
      this.progressPercent = 0

      if (this.isPlaying) {
        audio.play().catch(() => {})
      }

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

    handleCycleMode() {
      const mode = this.cyclePlayMode()
      const labels = {
        sequence: '顺序播放',
        shuffle: '乱序播放',
        favorite: '收藏播放'
      }
      this.$message({
        message: labels[mode],
        duration: 1500,
        type: 'info'
      })
    },

    async handleToggleFavorite(track) {
      try {
        const favorite = await this.toggleFavoriteTrack(track.id)
        this.$message({
          message: favorite === 1 ? '已收藏' : '已取消收藏',
          duration: 1200,
          type: 'success'
        })
      } catch {
        this.$message.error('操作失败')
      }
    },

    handleSelectTrack(track) {
      if (this.currentTrack && this.currentTrack.id === track.id) {
        this.handleTogglePlay()
      } else {
        this.$store.commit('music/SET_CURRENT_TRACK', track)
        this.$store.commit('music/SET_PLAYING', true)
      }
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
      // 非 ended 状态的暂停由 Vuex 控制
    },

    onAudioEnded() {
      this.playNext()
    },

    onAudioError() {
      this.$message.warning('音乐文件加载失败，请尝试重新上传')
      this.destroyAudio()
    },

    onTimeUpdate() {
      if (this.seeking) return
      const audio = this.$refs.audio
      if (!audio) return
      this.currentTime = audio.currentTime
      if (audio.duration && !isNaN(audio.duration)) {
        this.duration = audio.duration
      }
      this.progressPercent = this.duration > 0
        ? (this.currentTime / this.duration) * 100
        : 0
    },

    onLoadedMetadata() {
      const audio = this.$refs.audio
      if (!audio) return
      if (audio.duration && !isNaN(audio.duration)) {
        this.duration = audio.duration
      }
    },

    handleSeek(val) {
      const audio = this.$refs.audio
      if (!audio || !this.duration) return
      this.seeking = false
      const newTime = (val / 100) * this.duration
      audio.currentTime = newTime
      this.currentTime = newTime
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

    formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '0:00'
      const m = Math.floor(seconds / 60)
      const s = Math.floor(seconds % 60)
      return `${m}:${String(s).padStart(2, '0')}`
    }
  }
}
</script>

<style lang="scss" scoped>
.music-player-bar {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 10px;
  border-radius: 19px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  transition: border-color 0.25s, box-shadow 0.25s;

  &.is-active {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);
  }

  &:hover {
    border-color: var(--color-primary);
  }

  .player-controls {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;
  }

  .player-track-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;

    .track-display {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      color: var(--color-text);
      font-size: $font-size-sm;
      max-width: 110px;

      .el-icon-headset {
        width: 32px;
        text-align: center;
        flex-shrink: 0;
      }

      .track-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        max-width: 70px;
      }

      .track-placeholder {
        color: var(--color-text-placeholder);
        font-size: $font-size-xs;
      }

      .el-icon-arrow-up {
        font-size: $font-size-xs;
        color: var(--color-text-placeholder);
      }
    }

    .progress-section {
      display: flex;
      align-items: center;
      gap: 6px;
      height: 16px;

      .time-display {
        font-size: 10px;
        color: var(--color-text-secondary);
        flex-shrink: 0;
        min-width: 32px;
        text-align: center;
        font-variant-numeric: tabular-nums;
      }

      .progress-slider {
        width: 80px;
        margin: 0 !important;

        ::v-deep .el-slider__runway {
          height: 3px;
          margin: 0;
        }

        ::v-deep .el-slider__bar {
          height: 3px;
          background: var(--color-primary);
        }

        ::v-deep .el-slider__button-wrapper {
          display: none;
        }
      }
    }
  }

  .player-volume {
    display: flex;
    align-items: center;
    gap: 4px;
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
      width: 50px;
    }
  }
}

@media (max-width: 768px) {
  .music-player-bar {
    .player-track-info {
      display: none;
    }

    .player-volume {
      display: none;
    }
  }
}
</style>

<style lang="scss">
.music-playlist-popover {
  padding: 0 !important;
  max-height: 400px;
  overflow: hidden;

  .playlist-panel {
    .playlist-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 14px 6px;
      font-weight: 600;
      border-bottom: 1px solid var(--color-border);
      gap: 8px;

      .playlist-tabs {
        display: flex;
        gap: 6px;
        margin-left: auto;

        .tab {
          font-size: 12px;
          font-weight: 400;
          color: var(--color-text-secondary);
          cursor: pointer;
          padding: 2px 8px;
          border-radius: 10px;
          transition: all 0.2s;

          &:hover {
            background: var(--color-bg);
          }

          &.active {
            color: var(--color-primary);
            background: rgba(64, 158, 255, 0.1);
          }
        }
      }
    }

    .playlist-body {
      max-height: 300px;
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

      .fav-icon {
        font-size: 16px;
        color: var(--color-text-placeholder);
        cursor: pointer;
        flex-shrink: 0;
        transition: color 0.2s, transform 0.15s;

        &:hover {
          transform: scale(1.2);
        }

        &.el-icon-star-on {
          color: #e6a23c;
        }
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
