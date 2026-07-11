<template>
  <div class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">墨抄</h1>
        <p class="hero-subtitle">网文创作练笔平台</p>
        <p class="hero-desc">抄写经典，积累素材，创作属于你的网文世界</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/register')">
            免费注册
          </el-button>
          <el-button size="large" @click="$router.push('/library')">
            浏览书库
          </el-button>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="features-section">
      <div class="page-container">
        <h2 class="section-title">核心功能</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :sm="12" :md="8" v-for="feature in features" :key="feature.title">
            <div class="feature-card">
              <div class="feature-icon" :style="{ backgroundColor: feature.color + '20', color: feature.color }">
                <i :class="feature.icon" />
              </div>
              <h3 class="feature-title">{{ feature.title }}</h3>
              <p class="feature-desc">{{ feature.desc }}</p>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>

    <!-- Steps Section -->
    <section class="steps-section">
      <div class="page-container">
        <h2 class="section-title">如何开始</h2>
        <el-row :gutter="24">
          <el-col :xs="24" :sm="8" v-for="(step, index) in steps" :key="index">
            <div class="step-card">
              <div class="step-number">{{ index + 1 }}</div>
              <h3 class="step-title">{{ step.title }}</h3>
              <p class="step-desc">{{ step.desc }}</p>
            </div>
          </el-col>
        </el-row>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <div class="cta-content">
        <h2 class="cta-title">准备好开始你的创作之旅了吗？</h2>
        <el-button type="primary" size="large" @click="$router.push('/register')">
          立即加入
        </el-button>
      </div>
    </section>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'

export default {
  name: 'Home',
  computed: {
    ...mapGetters('auth', ['isLoggedIn'])
  },
  data() {
    return {
      features: [
        {
          icon: 'el-icon-edit',
          title: '抄书练习',
          desc: '逐字对照抄写经典网文，实时高亮反馈，提升打字速度与正确率',
          color: '#4A6CF7'
        },
        {
          icon: 'el-icon-collection-tag',
          title: '好词好句收藏',
          desc: '练习中随时收藏精彩段落，标签分类管理，支持每日回顾',
          color: '#52C41A'
        },
        {
          icon: 'el-icon-reading',
          title: '小说创作',
          desc: '从大纲到章节，完整创作工作流，AI辅助写作让灵感不断',
          color: '#FAAD14'
        },
        {
          icon: 'el-icon-data-line',
          title: '数据看板',
          desc: '练习数据可视化，连续打卡记录，趋势图表一目了然',
          color: '#FF4D4F'
        },
        {
          icon: 'el-icon-magic-stick',
          title: 'AI写作助手',
          desc: '智能优化、扩写、缩写、续写、润色对话，辅助创作更轻松',
          color: '#722ED1'
        },
        {
          icon: 'el-icon-skin',
          title: '多主题切换',
          desc: '亮色、暗色、护眼绿三种主题，保护你的创作灵感与视力',
          color: '#13C2C2'
        }
      ],
      steps: [
        {
          title: '注册登录',
          desc: '创建你的墨抄账号，开启网文创作练笔之旅'
        },
        {
          title: '选择素材',
          desc: '从书库中选择经典网文章节，开始抄写练习'
        },
        {
          title: '创作小说',
          desc: '积累素材后，创建小说，利用AI辅助完成你的作品'
        }
      ]
    }
  },
  created() {
    if (this.isLoggedIn) {
      this.$router.push('/library')
    }
  }
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background-color: var(--color-bg);
}

.hero-section {
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  text-align: center;
  padding: #{$spacing-xxl} #{$spacing-lg};

  .hero-content {
    .hero-title {
      font-size: 56px;
      font-weight: 800;
      color: #fff;
      letter-spacing: 8px;
      margin-bottom: #{$spacing-md};
    }

    .hero-subtitle {
      font-size: $font-size-xl;
      color: rgba(255, 255, 255, 0.9);
      margin-bottom: #{$spacing-sm};
    }

    .hero-desc {
      font-size: $font-size-md;
      color: rgba(255, 255, 255, 0.7);
      margin-bottom: #{$spacing-xl};
    }

    .hero-actions {
      display: flex;
      gap: #{$spacing-md};
      justify-content: center;

      .el-button--large {
        padding: 12px 32px;
        font-size: $font-size-md;
      }
    }
  }
}

.features-section {
  padding: #{$spacing-xxl} 0;

  .section-title {
    text-align: center;
    font-size: $font-size-heading;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: #{$spacing-xl};
  }

  .feature-card {
    text-align: center;
    padding: #{$spacing-xl} #{$spacing-lg};
    border-radius: $border-radius-lg;
    background-color: var(--color-card-bg);
    border: 1px solid var(--color-border);
    height: 100%;
    transition: all $transition-base;

    &:hover {
      box-shadow: var(--shadow-md);
      transform: translateY(-4px);
    }

    .feature-icon {
      width: 64px;
      height: 64px;
      border-radius: $border-radius-lg;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto #{$spacing-md};

      i {
        font-size: 32px;
      }
    }

    .feature-title {
      font-size: $font-size-lg;
      font-weight: 600;
      color: var(--color-text);
      margin-bottom: #{$spacing-sm};
    }

    .feature-desc {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      line-height: 1.6;
    }
  }
}

.steps-section {
  padding: #{$spacing-xxl} 0;
  background-color: var(--color-bg-secondary);

  .section-title {
    text-align: center;
    font-size: $font-size-heading;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: #{$spacing-xl};
  }

  .step-card {
    text-align: center;
    padding: #{$spacing-xl};

    .step-number {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background-color: var(--color-primary);
      color: #fff;
      font-size: $font-size-xl;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto #{$spacing-md};
    }

    .step-title {
      font-size: $font-size-lg;
      font-weight: 600;
      color: var(--color-text);
      margin-bottom: #{$spacing-sm};
    }

    .step-desc {
      font-size: $font-size-sm;
      color: var(--color-text-secondary);
      line-height: 1.6;
    }
  }
}

.cta-section {
  padding: #{$spacing-xxl} #{$spacing-lg};
  text-align: center;
  background-color: var(--color-card-bg);

  .cta-title {
    font-size: $font-size-xxl;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: #{$spacing-lg};
  }
}
</style>
