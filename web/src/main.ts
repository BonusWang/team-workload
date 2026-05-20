import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import './styles/index.scss'
import { useUserStore } from './store/user'
import { getUserInfo } from './api'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn
})
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 页面刷新时恢复用户信息
const userStore = useUserStore()
const token = localStorage.getItem('token')
if (token) {
  // 如果有token，尝试获取用户信息
  getUserInfo().then((res: any) => {
    if (res.data) {
      userStore.setUserInfo(res.data)
    }
  }).catch(() => {
    // 获取用户信息失败，清除token
    userStore.logout()
  })
}

app.mount('#app')
