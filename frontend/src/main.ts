import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 初始化：有 accessToken 直接用；只有 refreshToken 则自动续期
const authStore = useAuthStore()
async function initAuth() {
  if (authStore.isLoggedIn) {
    // 有 accessToken → 直接获取用户信息
    await authStore.fetchCurrentUser().catch(() => authStore.logout())
  } else if (authStore.refreshToken) {
    // 只有 refreshToken → 自动换取新 accessToken
    const newToken = await authStore.refreshAccessToken()
    if (newToken) {
      await authStore.fetchCurrentUser().catch(() => authStore.logout())
    }
  }
}

initAuth().then(() => {
  app.mount('#app')
})
