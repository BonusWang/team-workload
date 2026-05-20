import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', noAuth: true },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '个人看板' },
      },
      {
        path: 'weekly',
        name: 'Weekly',
        component: () => import('@/views/weekly/index.vue'),
        meta: { title: '周报管理' },
      },
      {
        path: 'daily/entry',
        name: 'DailyEntry',
        component: () => import('@/views/daily/entry.vue'),
        meta: { title: '日报录入' },
      },
      {
        path: 'leave',
        name: 'Leave',
        component: () => import('@/views/leave/index.vue'),
        meta: { title: '请假管理' },
      },
      {
        path: 'team',
        name: 'Team',
        component: () => import('@/views/team/index.vue'),
        meta: { title: '团队视图', roles: ['LEADER', 'ADMIN'] },
      },
      {
        path: 'monthly',
        name: 'Monthly',
        component: () => import('@/views/monthly/index.vue'),
        meta: { title: '月度汇总', roles: ['LEADER', 'ADMIN'] },
      },
      {
        path: 'leave/approve',
        name: 'LeaveApprove',
        component: () => import('@/views/leave/approve.vue'),
        meta: { title: '请假审批', roles: ['LEADER', 'ADMIN'] },
      },
      {
        path: 'daily/import',
        name: 'DailyImport',
        component: () => import('@/views/daily/import.vue'),
        meta: { title: '日报导入', roles: ['LEADER', 'ADMIN'] },
      },
      {
        path: 'workday',
        name: 'Workday',
        component: () => import('@/views/workday/index.vue'),
        meta: { title: '工作日管理', roles: ['ADMIN'] },
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人设置' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.noAuth) {
    if (token) {
      next('/dashboard')
    } else {
      next()
    }
  } else if (!token) {
    next('/login')
  } else {
    const userStore = useUserStore()
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
      if (!requiredRoles.includes(userStore.role)) {
        next('/dashboard')
      } else {
        next()
      }
    } else {
      next()
    }
  }
})

export default router
