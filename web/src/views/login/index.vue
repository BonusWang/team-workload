<template>
  <div class="login-page">
    <div class="login-bg-grid"></div>
    <div class="login-bg-glow"></div>
    <div class="login-container">
      <div class="login-header">
        <div class="login-logo">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="url(#loginGrad)" />
            <path d="M2 17L12 22L22 17" stroke="url(#loginGrad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="url(#loginGrad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="loginGrad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse">
                <stop stop-color="#3ee1a3"/>
                <stop offset="1" stop-color="#a0fde7"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="brand-text login-title">TeamWorkload</span>
        <p class="login-subtitle">智能无限，协作无间</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>团队工作量管理系统</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { login, getUserInfo } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res: any = await login(form)
    console.log('[登录] 登录接口返回:', JSON.stringify(res))
    if (!res?.data?.token) {
      console.error('[登录] 返回数据中缺少token:', JSON.stringify(res))
      ElMessage.error('登录返回数据异常，请联系管理员')
      return
    }
    userStore.setToken(res.data.token)
    const infoRes: any = await getUserInfo()
    console.log('[登录] 用户信息接口返回:', JSON.stringify(infoRes))
    if (!infoRes?.data) {
      console.error('[登录] 获取用户信息失败:', JSON.stringify(infoRes))
      ElMessage.error('获取用户信息失败，请联系管理员')
      return
    }
    userStore.setUserInfo(infoRes.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    console.error('[登录] 登录失败:', JSON.stringify({
      message: error?.message,
      code: error?.response?.code,
      data: error?.response?.data,
      url: error?.response?.url,
      method: error?.response?.method,
      httpStatus: error?.response?.status,
      httpStatusText: error?.response?.statusText,
      stack: error?.stack,
    }))
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: $bg-primary;
  position: relative;
  overflow: hidden;
}

.login-bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
}

.login-bg-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(50, 240, 140, 0.06) 0%, transparent 70%);
  animation: glowPulse 6s ease-in-out infinite;
  pointer-events: none;
}

@keyframes glowPulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.15); opacity: 1; }
}

.login-container {
  width: 420px;
  padding: 48px 40px 36px;
  background: $bg-card;
  border: 1px solid $border-color-light;
  border-radius: $radius-xl;
  box-shadow: $shadow-elevated;
  position: relative;
  z-index: 1;
  animation: fadeInUp 0.5s ease-out;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.login-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
  display: block;
}

.login-subtitle {
  margin-top: 8px;
  font-size: 14px;
  color: $text-secondary;
  font-weight: 400;
  letter-spacing: 2px;
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper) {
    background-color: $bg-input;
    border: 1px solid $border-color;
    box-shadow: none;
    border-radius: $radius-sm;
    height: 46px;
    transition: all $transition-fast;

    &:hover {
      border-color: $border-color-light;
    }

    &.is-focus {
      border-color: $brand-primary;
      box-shadow: 0 0 0 3px rgba(50, 240, 140, 0.08);
    }
  }

  :deep(.el-input__inner) {
    color: $text-primary;
    font-size: 14px;

    &::placeholder {
      color: $text-muted;
    }
  }

  :deep(.el-input__prefix .el-icon) {
    color: $text-muted;
    font-size: 16px;
  }
}

.login-btn {
  width: 100%;
  background: $brand-gradient;
  border: none;
  font-weight: 600;
  letter-spacing: 4px;
  font-size: 15px;
  height: 46px;
  border-radius: $radius-sm;
  transition: all $transition-normal;
  color: #0a0b0d;

  &:hover {
    box-shadow: 0 0 24px rgba(50, 240, 140, 0.35);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 12px;
  color: $text-muted;
  letter-spacing: 0.5px;
}
</style>
