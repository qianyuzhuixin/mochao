<template>
  <div class="text-comparator">
    <!-- 原文显示区 -->
    <div class="original-text-area" ref="originalRef">
      <div class="original-label">原文</div>
      <div class="original-content" ref="contentRef">
        <template v-for="(para, pIndex) in displayParagraphs">
          <span
            v-if="para.isComplete"
            :key="'done-' + pIndex"
            class="paragraph paragraph-complete"
          >{{ para.text }}</span>
          <span
            v-else-if="para.isCurrent"
            :key="'cur-' + pIndex"
            class="paragraph paragraph-current"
          >
            <span
              v-for="(char, cIndex) in para.chars"
              :key="cIndex"
              class="char"
              :class="getCharClassInPara(cIndex, para)"
            >{{ char === ' ' ? '\u00A0' : char }}</span>
          </span>
          <span
            v-else
            :key="'pending-' + pIndex"
            class="paragraph paragraph-pending"
          >{{ para.text }}</span>
        </template>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-area">
      <div class="input-label">输入</div>
      <textarea
        ref="inputRef"
        v-model="typedText"
        class="custom-input"
        placeholder="开始输入..."
        @input="handleInput"
        @keydown="handleKeydown"
        spellcheck="false"
        autocomplete="off"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'TextComparator',
  props: {
    originalText: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      typedText: '',
      // 缓存 originalText 的字符数组，避免每次 computed 重建
      originalChars: [],
      // 缓存段落分割结果
      paragraphs: []
    }
  },
  computed: {
    displayParagraphs() {
      return this.paragraphs.map(para => {
        const isComplete = para.end <= this.typedText.length
        const isCurrent = !isComplete && para.start <= this.typedText.length
        return {
          ...para,
          chars: isCurrent ? Array.from(para.text) : null,
          isComplete,
          isCurrent,
          isPending: !isComplete && !isCurrent
        }
      })
    }
  },
  watch: {
    originalText: {
      immediate: true,
      handler(newVal) {
        this.typedText = ''
        this.originalChars = Array.from(newVal)
        this.paragraphs = this.splitTextIntoParagraphs(newVal)
      }
    }
  },
  methods: {
    /**
     * 将长文本按固定长度切分成段落（每段最多 300 字符）
     * 已完成/待输入段落用整块文本渲染，只有当前段落逐字渲染
     * 这避免了 18 万字文本产生 18 万个 DOM 节点的性能灾难
     */
    splitTextIntoParagraphs(text) {
      if (!text) return []
      const paragraphs = []
      const MAX_LEN = 300
      for (let i = 0; i < text.length; i += MAX_LEN) {
        const slice = text.slice(i, i + MAX_LEN)
        paragraphs.push({
          text: slice,
          start: i,
          end: i + slice.length
        })
      }
      return paragraphs
    },
    getCharClassInPara(charIndex, paragraph) {
      const globalIndex = paragraph.start + charIndex
      if (globalIndex >= this.typedText.length) {
        if (globalIndex === this.typedText.length) {
          return 'char-current'
        }
        return 'char-pending'
      }
      const typedChar = this.typedText[globalIndex]
      const originalChar = this.originalChars[globalIndex]
      if (typedChar === originalChar) {
        return 'char-correct'
      }
      return 'char-wrong'
    },
    handleInput(e) {
      const value = e.target.value
      const prevLen = this.typedText.length

      // 检查所有新输入的字符（支持粘贴场景）
      if (value.length > prevLen) {
        for (let i = prevLen; i < value.length; i++) {
          if (value[i] !== this.originalChars[i]) {
            this.$emit('error', {
              index: i,
              expected: this.originalChars[i],
              actual: value[i]
            })
            break // 只报第一个错误
          }
        }
      }

      this.$emit('input', value)

      // 检查是否完成（章节模式下 originalText 是章节内容长度）
      if (this.originalText.length > 0 && value.length >= this.originalText.length) {
        this.$emit('complete', {
          typedText: value,
          accuracy: this.calculateAccuracy(),
          totalChars: this.originalText.length
        })
      }

      // 滚动同步：原文与输入框同步滚动，当前输入位置保持在中间偏上
      this.$nextTick(() => {
        this.syncScroll()
      })
    },
    handleKeydown(e) {
      // 禁止回车换行（除非原文包含换行）
      if (e.key === 'Enter' && this.originalChars[this.typedText.length] !== '\n') {
        e.preventDefault()
      }
    },
    calculateAccuracy() {
      if (this.typedText.length === 0) return 100
      let correct = 0
      const len = this.typedText.length
      for (let i = 0; i < len; i++) {
        if (this.typedText[i] === this.originalChars[i]) {
          correct++
        }
      }
      return Math.round((correct / len) * 100)
    },
    setTypedText(text) {
      this.typedText = text || ''
      this.$nextTick(() => {
        this.$refs.inputRef && (this.$refs.inputRef.value = this.typedText)
        this.syncScroll()
      })
    },
    focus() {
      this.$refs.inputRef && this.$refs.inputRef.focus()
    },
    reset() {
      this.typedText = ''
    },
    /**
     * 原文与输入框同步滚动
     * 当前输入位置（光标）保持在可视区域的中间偏上位置
     * 当输入到最后一行时，自动向上滚动使当前行移到中间
     */
    syncScroll() {
      const contentEl = this.$refs.contentRef
      const inputEl = this.$refs.inputRef
      if (!contentEl || !inputEl) return

      // 找到当前光标对应的字符
      const currentChar = contentEl.querySelector('.char-current')
      if (!currentChar) return

      const containerRect = contentEl.getBoundingClientRect()
      const charRect = currentChar.getBoundingClientRect()

      // 当前字符相对于原文容器的位置
      const relativeY = charRect.top - containerRect.top

      // 舒适区域：容器的 25% ~ 70%
      const upperBound = containerRect.height * 0.25
      const lowerBound = containerRect.height * 0.70

      // 如果当前字符在舒适区域内，不滚动（只同步 textarea）
      if (relativeY >= upperBound && relativeY <= lowerBound) {
        inputEl.scrollTop = contentEl.scrollTop
        return
      }

      // 目标位置：40%（中间偏上）
      const targetY = containerRect.height * 0.4
      const scrollDelta = relativeY - targetY

      // 滚动原文区域
      contentEl.scrollTop = Math.max(0, contentEl.scrollTop + scrollDelta)

      // 同步输入框（用户输入的内容是原文前缀，排版完全相同，scrollTop 直接同步）
      inputEl.scrollTop = contentEl.scrollTop
    }
  }
}
</script>

<style lang="scss" scoped>
.text-comparator {
  display: flex;
  gap: #{$spacing-md};
  height: 100%;

  @media (max-width: 768px) {
    flex-direction: column;
  }
}

.original-text-area,
.input-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-card-bg);
  border: 1px solid var(--color-border);
  border-radius: $border-radius-md;
  overflow: hidden;
}

.original-label,
.input-label {
  padding: #{$spacing-sm} #{$spacing-md};
  font-size: $font-size-sm;
  font-weight: 600;
  color: var(--color-text-secondary);
  background-color: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
}

.original-content {
  flex: 1;
  padding: #{$spacing-lg};
  font-size: 17px;
  line-height: 2.2;
  overflow-y: auto;
  font-family: $font-family-base;
  word-break: break-all;
  will-change: scroll-position;
}

.paragraph {
  display: block;
  margin-bottom: 0.8em;
  white-space: pre-wrap;

  &:last-child {
    margin-bottom: 0;
  }
}

.paragraph-complete {
  color: var(--color-success);
}

.paragraph-current {
  .char {
    position: relative;
  }

  .char-correct {
    color: var(--color-success);
  }

  .char-wrong {
    color: var(--color-error);
    text-decoration: underline wavy var(--color-error);
    text-underline-offset: 3px;
  }

  .char-current {
    background-color: var(--color-primary);
    color: #fff;
    border-radius: 2px;
    animation: blink 1s infinite;
  }

  .char-pending {
    color: var(--color-text-placeholder);
  }
}

.paragraph-pending {
  color: var(--color-text-placeholder);
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.6; }
}

.custom-input {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  padding: #{$spacing-lg};
  font-size: 17px;
  line-height: 2.2;
  font-family: $font-family-base;
  color: var(--color-text);
  background-color: var(--color-bg);
  white-space: pre-wrap;
  word-break: break-all;

  &::placeholder {
    color: var(--color-text-placeholder);
  }
}
</style>
