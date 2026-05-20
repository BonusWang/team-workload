<template>
  <div class="page-container">
    <div class="card">
      <div class="toolbar">
        <div>
          <h2 class="page-title">个人设置</h2>
          <p class="page-desc">管理您的账户信息与密码</p>
        </div>
      </div>

      <div class="settings-section">
        <div class="section-header">
          <el-icon :size="20" color="#32f08c"><Lock /></el-icon>
          <span class="section-title">密码修改</span>
        </div>
        <div class="section-desc">修改您的登录密码，修改成功后需使用新密码重新登录</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="password-form"
          @submit.prevent
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleSubmit">
              保存
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
})

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword,
      })
      ElMessage.success('密码修改成功，请重新登录')
      userStore.logout()
      router.push('/login')
    } catch (error: any) {
      ElMessage.error(error.message || '密码修改失败')
    } finally {
      loading.value = false
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.settings-section {
  max-width: 520px;
  padding: 24px 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.section-desc {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 24px;
  padding-left: 28px;
}

.password-form {
  :deep(.el-form-item__label) {
    color: $text-secondary;
    font-weight: 500;
  }

  :deep(.el-input__wrapper) {
    background-color: $bg-input;
    border: 1px solid $border-color;
    box-shadow: none;

    &:hover {
      border-color: $border-color-light;
    }

    &.is-focus {
      border-color: $brand-primary;
    }
  }

  :deep(.el-input__inner) {
    color: $text-primary;

    &::placeholder {
      color: $text-muted;
    }
  }
}
</style>
