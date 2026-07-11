<template>
  <div class="music-player" :class="{ 'has-track': trackName }">
    <!-- 播放/暂停 -->
    <el-button
      :icon="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"
      :type="isPlaying ? 'primary' : 'default'"
      size="mini"
      circle
      :disabled="!trackName"
      @click="togglePlay"
    />

    <!-- 曲目名 -->
    <span v-if="trackName" class="track-name" :title="trackName">{{ trackName }}</span>
    <span v-else class="track-placeholder">未选择音乐</span>

    <!-- 音量 -->
    <el-popover
      v-if="trackName"
      placement="top"
      trigger="click"
      :width="36"
    >
      <div class="volume-slider-vertical">
        <el-slider
          v-model="volume"
          vertical
          :min="0"
          :max="100"
          :show-tooltip="false"
          height="100px"
          @input="handleVolume"
        />
      </div>
      <el-button slot="reference" icon="el-icon-bell" size="mini" circle :type="volume > 0 ? '' : 'info'" />
    </el-popover>

    <!-- 导入音乐 -->
    <el-tooltip content="导入本地音乐" placement="bottom">
      <el-button icon="el-icon-folder-add" size="mini" circle @click="$refs.fileInput.click()" />
    </el-tooltip>
    <input
      ref="fileInput"
      type="file"
      accept="audio/*"
      style="display: none"
      @change="handleFileImport"
    />
  </div>
</template>

<script>
export default {
  name: 'MusicPlayer',
  data() {
    return {
      audio: null,
      trackName: '',
      isPlaying: false,
      volume: 60
    }
  },
  beforeDestroy() {
    this.destroyAudio()
  },
  methods: {
    handleFileImport(e) {
      const file = e.target.files[0]
      if (!file) return

      // 清理旧的
      this.destroyAudio()

      const url = URL.createObjectURL(file)
      this.audio = new Audio(url)
      this.audio.volume = this.volume / 100
      this.audio.loop = true

      this.audio.addEventListener('play', () => { this.isPlaying = true })
      this.audio.addEventListener('pause', () => { this.isPlaying = false })
      this.audio.addEventListener('ended', () => { this.isPlaying = false })
      this.audio.addEventListener('error', () => {
        this.$message.warning('音乐文件无法播放')
        this.destroyAudio()
      })

      // 提取文件名（去掉扩展名）
      this.trackName = file.name.replace(/\.[^.]+$/, '')

      // 自动播放
      this.audio.play().catch(() => {
        this.isPlaying = false
      })

      // 重置 file input 以便重复选同一文件
      e.target.value = ''
    },
    togglePlay() {
      if (!this.audio) return
      if (this.isPlaying) {
        this.audio.pause()
      } else {
        this.audio.play().catch(() => {
          this.$message.warning('播放失败，请检查文件')
        })
      }
    },
    handleVolume(val) {
      if (this.audio) {
        this.audio.volume = val / 100
      }
    },
    destroyAudio() {
      if (this.audio) {
        this.audio.pause()
        this.audio.src = ''
        this.audio.load()
        this.audio = null
      }
      this.trackName = ''
      this.isPlaying = false
    }
  }
}
</script>

<style lang="scss" scoped>
.music-player {
  display: flex;
  align-items: center;
  gap: 6px;

  .track-name {
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: $font-size-xs;
    color: var(--color-text);
  }

  .track-placeholder {
    font-size: $font-size-xs;
    color: var(--color-text-placeholder);
  }
}

.volume-slider-vertical {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}
</style>
