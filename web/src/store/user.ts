import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(null)
  const role = ref('')
  const permissions = ref<string[]>([])

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(info: any) {
    userInfo.value = info.user || info
    role.value = (info.user ? info.user.role : info.role) || ''
    permissions.value = info.permissions || []
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    role.value = ''
    permissions.value = []
    localStorage.removeItem('token')
  }

  function isLeader() {
    return role.value === 'LEADER' || role.value === 'ADMIN'
  }

  function isAdmin() {
    return role.value === 'ADMIN'
  }

  function hasPermission(permissionCode: string) {
    return permissions.value.includes(permissionCode)
  }

  return { token, userInfo, role, permissions, setToken, setUserInfo, logout, isLeader, isAdmin, hasPermission }
})
