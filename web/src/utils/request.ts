import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

function formatTime() {
  return new Date().toLocaleString('zh-CN', { hour12: false })
}

service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    const method = (config.method || 'GET').toUpperCase()
    const fullUrl = config.url?.startsWith('http') ? config.url : `${config.baseURL}${config.url}`
    console.log(`%c[请求] ${formatTime()} ${method} ${fullUrl}`, 'color: #32f08c; font-weight: bold', {
      params: config.params,
      data: config.data,
      headers: { ...config.headers, Authorization: config.headers['Authorization'] ? 'Bearer ***' : undefined },
    })
    return config
  },
  (error) => {
    console.error(`%c[请求异常] ${formatTime()}`, 'color: #f05555; font-weight: bold', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data
    const method = (response.config.method || 'GET').toUpperCase()
    const fullUrl = response.config.url?.startsWith('http') ? response.config.url : `${response.config.baseURL}${response.config.url}`

    if (res.code !== 200) {
      const errInfo = {
        code: res.code,
        message: res.message || '请求失败',
        data: res.data,
        url: fullUrl,
        method,
      }
      console.error(`%c[响应错误] ${formatTime()} ${method} ${fullUrl}`, 'color: #f0c832; font-weight: bold', errInfo)
      ElMessage.error(errInfo.message)
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      const err = new Error(errInfo.message) as Error & { response?: typeof errInfo }
      err.response = errInfo
      return Promise.reject(err)
    }

    console.log(`%c[响应] ${formatTime()} ${method} ${fullUrl}`, 'color: #3298f0; font-weight: bold', {
      code: res.code,
      data: res.data,
    })
    return res
  },
  (error) => {
    const method = (error.config?.method || 'GET').toUpperCase()
    const fullUrl = error.config?.url?.startsWith('http') ? error.config.url : `${error.config?.baseURL || ''}${error.config?.url || ''}`
    const status = error.response?.status
    const responseData = error.response?.data

    console.error(`%c[响应异常] ${formatTime()} ${method} ${fullUrl}`, 'color: #f05555; font-weight: bold', {
      status,
      statusText: error.response?.statusText,
      message: error.message,
      code: error.code,
      responseData,
      requestData: error.config?.data,
      requestParams: error.config?.params,
    })

    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }

    const errorMsg = responseData?.message || error.message || '网络错误'
    ElMessage.error(errorMsg)
    return Promise.reject(error)
  }
)

export default service
