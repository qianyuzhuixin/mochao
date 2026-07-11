<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">登录墨抄</h1>
        <p class="login-subtitle">网文创作练笔平台</p>
      </div>
      <el-form ref="loginForm" :model="form" :rules="rules" label-width="0" @submit.native.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="el-icon-user"
            placeholder="用户名或邮箱"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="密码"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名或邮箱', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码至少6位', trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await this.$store.dispatch('auth/login', this.form)
          this.$message.success('登录成功')
          const redirect = this.$route.query.redirect || '/library'
          this.$router.push(redirect)
        } catch (error) {
          // 错误已在拦截器处理
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  padding: #{$spacing-lg};

  .login-card {
    width: 400px;
    max-width: 100%;
    background-color: var(--color-card-bg);
    border-radius: $border-radius-lg;
    box-shadow: var(--shadow-lg);
    padding: #{$spacing-xxl} #{$spacing-xl};

    .login-header {
      text-align: center;
      margin-bottom: #{$spacing-xl};

      .login-title {
        font-size: $font-size-heading;
        font-weight: 700;
        color: var(--color-text);
        margin-bottom: #{$spacing-xs};
      }

      .login-subtitle {
        font-size: $font-size-base;
        color: var(--color-text-secondary);
      }
    }

    .login-btn {
      width: 100%;
      font-size: $font-size-md;
    }

    .login-footer {
      text-align: center;
      font-size: $font-size-sm;
      color: var(--color-text-secondary);

      a {
        margin-left: #{$spacing-xs};
      }
    }
  }
}
</style>
