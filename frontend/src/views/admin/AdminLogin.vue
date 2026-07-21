<template>
  <div class="admin-login-page">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">墨抄管理后台</h1>
        <p class="login-subtitle">管理员登录</p>
      </div>
      <el-form ref="loginForm" :model="form" :rules="rules" @submit.native.prevent="handleLogin">
        <el-form-item prop="account">
          <el-input v-model="form.account" prefix-icon="el-icon-user" placeholder="管理员账号" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" prefix-icon="el-icon-lock" type="password" placeholder="密码" show-password @keyup.enter.native="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">登 录</el-button>
        </el-form-item>
        <div class="login-footer">
          <router-link to="/">返回前台</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminLogin',
  data() {
    return {
      form: { account: '', password: '' },
      rules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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
          const res = await this.$store.dispatch('auth/login', { ...this.form, isAdmin: true })
          if (!res.userInfo || res.userInfo.role !== 'ADMIN') {
            this.$message.error('该账号没有管理员权限')
            await this.$store.dispatch('auth/logout')
            return
          }
          this.$message.success('登录成功')
          this.$router.push('/admin/dashboard')
        } catch (e) { console.error(e) } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.admin-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1A1A2E 0%, #16213E 100%);
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
      }

      .login-subtitle {
        font-size: $font-size-base;
        color: var(--color-text-secondary);
        margin-top: #{$spacing-xs};
      }
    }

    .login-btn {
      width: 100%;
      font-size: $font-size-md;
    }

    .login-footer {
      text-align: center;
      font-size: $font-size-sm;
    }
  }
}
</style>
