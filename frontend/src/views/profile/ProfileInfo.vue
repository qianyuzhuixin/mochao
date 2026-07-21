<template>
  <div class="profile-info">
    <h3 class="section-title">基本信息</h3>
    <el-form ref="form" :model="form" :rules="rules" label-width="100px" style="max-width: 500px">
      <el-form-item label="用户名">
        <el-input :value="userInfo.username" disabled />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="form.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="个性签名">
        <el-input v-model="form.bio" type="textarea" :rows="3" placeholder="介绍一下自己" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <h3 class="section-title">修改密码</h3>
    <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 500px">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { updateProfile, changePassword } from '@/api/auth'

export default {
  name: 'ProfileInfo',
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.pwdForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      form: { nickname: '', email: '', bio: '' },
      rules: {
        email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }]
      },
      pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
        newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
        confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validateConfirm, trigger: 'blur' }]
      },
      saving: false,
      changingPwd: false
    }
  },
  computed: {
    ...mapGetters('auth', ['userInfo'])
  },
  created() {
    this.form.nickname = this.userInfo.nickname || ''
    this.form.email = this.userInfo.email || ''
    this.form.bio = this.userInfo.bio || ''
  },
  methods: {
    async handleSave() {
      this.saving = true
      try {
        await updateProfile(this.form)
        this.$store.dispatch('auth/getProfile')
        this.$message.success('保存成功')
      } catch (e) { console.error(e) } finally {
        this.saving = false
      }
    },
    handleChangePassword() {
      this.$refs.pwdForm.validate(async valid => {
        if (!valid) return
        this.changingPwd = true
        try {
          await changePassword(this.pwdForm)
          this.$message.success('密码修改成功')
          this.$refs.pwdForm.resetFields()
        } catch (e) { console.error(e) } finally {
          this.changingPwd = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.profile-info {
  .section-title {
    font-size: $font-size-md;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: #{$spacing-md};
  }
}
</style>
