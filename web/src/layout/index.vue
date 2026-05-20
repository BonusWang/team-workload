<template>
  <div class="layout">
    <div class="sidebar">
      <div class="sidebar-logo">
        <div class="logo-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="url(#logoGrad)" />
            <path d="M2 17L12 22L22 17" stroke="url(#logoGrad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="url(#logoGrad)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="logoGrad" x1="2" y1="2" x2="22" y2="22" gradientUnits="userSpaceOnUse">
                <stop stop-color="#3ee1a3"/>
                <stop offset="1" stop-color="#a0fde7"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="brand-text logo-text">TeamWorkload</span>
      </div>
      <div class="sidebar-menu-wrapper">
        <div class="menu-group">
          <div class="menu-group-label">工作台</div>
          <router-link
            v-for="item in workMenuItems"
            :key="item.path"
            :to="item.path"
            custom
            v-slot="{ isActive, navigate }"
          >
            <div class="nav-item" :class="{ active: isActive }" @click="navigate">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </router-link>
        </div>
        <div v-if="userStore.isLeader()" class="menu-group">
          <div class="menu-group-label">管理</div>
          <router-link
            v-for="item in manageMenuItems"
            :key="item.path"
            :to="item.path"
            custom
            v-slot="{ isActive, navigate }"
          >
            <div class="nav-item" :class="{ active: isActive }" @click="navigate">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </router-link>
        </div>
      </div>
      <div class="sidebar-footer">
        <div class="sidebar-version">v1.0.0</div>
      </div>
    </div>
    <div class="main">
      <div class="header">
        <div class="header-left">
          <span class="header-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <div class="user-avatar">
                {{ (userStore.userInfo?.name || '用')[0] }}
              </div>
              <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
              <el-tag :type="roleTagType" size="small" effect="dark" class="role-tag">{{ roleLabel }}</el-tag>
              <el-icon :size="12" color="#5a5a5a"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><Setting /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserInfo } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const workMenuItems = [
  { path: '/dashboard', label: '个人看板', icon: 'DataBoard' },
  { path: '/weekly', label: '周报管理', icon: 'Document' },
  { path: '/daily/entry', label: '日报录入', icon: 'EditPen' },
  { path: '/leave', label: '请假管理', icon: 'Clock' },
]

const manageMenuItems = computed(() => {
  const items: { path: string; label: string; icon: string }[] = []
  if (userStore.isLeader()) {
    items.push({ path: '/team', label: '团队视图', icon: 'User' })
    items.push({ path: '/monthly', label: '月度汇总', icon: 'Calendar' })
    items.push({ path: '/leave/approve', label: '请假审批', icon: 'Check' })
  }
  if (userStore.isAdmin()) {
    items.push({ path: '/daily/import', label: '日报导入', icon: 'Upload' })
    items.push({ path: '/workday', label: '工作日管理', icon: 'Timer' })
    items.push({ path: '/user', label: '用户管理', icon: 'Avatar' })
  }
  return items
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta.title as string) || '')

const roleTagType = computed(() => {
  const map: Record<string, string> = { ADMIN: 'danger', LEADER: 'warning', MEMBER: 'info' }
  return map[userStore.role] || 'info'
})

const roleLabel = computed(() => {
  const map: Record<string, string> = { ADMIN: '管理员', LEADER: '负责人', MEMBER: '成员' }
  return map[userStore.role] || '成员'
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

// 页面加载时自动获取用户信息
onMounted(async () => {
  // 如果有token但没有用户信息，则自动获取
  if (userStore.token && !userStore.userInfo) {
    try {
      const res = await getUserInfo()
      userStore.setUserInfo(res.data)
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // 获取失败，清除token并跳转到登录页
      userStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    }
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  background-color: $bg-secondary;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  position: relative;
  z-index: 10;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid $border-color;
  padding: 0 20px;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
  white-space: nowrap;
}

.sidebar-menu-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
}

.menu-group {
  margin-bottom: 8px;
}

.menu-group-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: $text-muted;
  padding: 8px 12px 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: $radius-sm;
  color: $text-secondary;
  cursor: pointer;
  transition: all $transition-fast;
  font-size: 14px;
  font-weight: 450;
  margin-bottom: 2px;
  position: relative;

  &:hover {
    color: $text-primary;
    background-color: $bg-hover;
  }

  &.active {
    color: $brand-primary;
    background-color: $brand-primary-dim;
    font-weight: 500;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 20px;
      background: $brand-primary;
      border-radius: 0 3px 3px 0;
    }
  }
}

.sidebar-footer {
  padding: 12px 20px;
  border-top: 1px solid $border-color;
}

.sidebar-version {
  font-size: 11px;
  color: $text-muted;
  font-family: $font-mono;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.header {
  height: 56px;
  background-color: rgba(17, 18, 21, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
  position: relative;
  z-index: 5;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  letter-spacing: -0.2px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: $radius-sm;
  transition: all $transition-fast;

  &:hover {
    background-color: $bg-hover;
  }
}

.user-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: $brand-gradient-subtle;
  border: 1px solid $border-color-brand;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: $brand-primary;
  flex-shrink: 0;
}

.user-name {
  color: $text-primary;
  font-size: 14px;
  font-weight: 500;
}

.role-tag {
  font-size: 11px;
}

.content {
  flex: 1;
  overflow-y: auto;
  background-color: $bg-primary;
}

.page-fade-enter-active {
  animation: fadeInUp 0.3s ease-out;
}

.page-fade-leave-active {
  animation: fadeIn 0.15s ease-in reverse;
}

</style>
