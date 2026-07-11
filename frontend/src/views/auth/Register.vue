<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-header">
        <h1 class="register-title">注册墨抄</h1>
        <p class="register-subtitle">开始你的网文创作之旅</p>
      </div>
      <el-form ref="registerForm" :model="form" :rules="rules" label-width="0" @submit.native.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="el-icon-user"
            placeholder="用户名"
            clearable
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            prefix-icon="el-icon-message"
            placeholder="邮箱"
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
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            prefix-icon="el-icon-lock"
            type="password"
            placeholder="确认密码"
            show-password
            @keyup.enter.native="handleRegister"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/auth'

export default {
  name: 'Register',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      form: {
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度2-20个字符', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleRegister() {
      this.$refs.registerForm.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await register({
            username: this.form.username,
            email: this.form.email,
            password: this.form.password
          })
          this.$message.success('注册成功，请登录')
          this.$router.push('/login')
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
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  padding: #{$spacing-lg};

  .register-card {
    width: 400px;
    max-width: 100%;
    background-color: var(--color-card-bg);
    border-radius: $border-radius-lg;
    box-shadow: var(--shadow-lg);
    padding: #{$spacing-xxl} #{$spacing-xl};

    .register-header {
      text-align: center;
      margin-bottom: #{$spacing-xl};

      .register-title {
        font-size: $font-size-heading;
        font-weight: 700;
        color: var(--color-text);
        margin-bottom: #{$spacing-xs};
      }

      .register-subtitle {
        font-size: $font-size-base;
        color: var(--color-text-secondary);
      }
    }

    .register-btn {
      width: 100%;
      font-size: $font-size-md;
    }

    .register-footer {
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
