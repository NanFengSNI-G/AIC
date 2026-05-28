<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import notification from '@/utils/notification'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const email = ref('')
const verifyCode = ref('')
const loginMode = ref<'password' | 'email'>('password')
const error = ref('')
const loading = ref(false)
const sendingCode = ref(false)

const handleSendCode = async () => {
  if (!email.value) return
  sendingCode.value = true
  try {
    await authStore.sendVerifyCode(email.value, 'login')
    notification.success('验证码已发送', { title: '发送成功' })
  } catch (e: any) {
    notification.error(e.message || '发送验证码失败', { title: '发送失败' })
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async () => {
  if (loginMode.value === 'password') {
    if (!username.value || !password.value) return
  } else {
    if (!email.value || !verifyCode.value) return
  }

  loading.value = true
  error.value = ''

  try {
    if (loginMode.value === 'password') {
      await authStore.login(username.value, password.value)
    } else {
      await authStore.loginByEmail(email.value, verifyCode.value)
    }
    notification.success('欢迎回来！', { title: '登录成功' })
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e: any) {
    notification.error(e.message || '登录失败', { title: '登录失败' })
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

const toggleMode = () => {
  loginMode.value = loginMode.value === 'password' ? 'email' : 'password'
  error.value = ''
}
</script>

<template>
  <div class="max-w-sm mx-auto mt-20">
    <div class="text-center mb-8">
      <h1 class="text-2xl font-bold text-stone-900 mb-2">欢迎回来</h1>
      <p class="text-stone-500">登录到社交论坛</p>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-5">
      <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
        {{ error }}
      </div>

      <!-- Login Mode Toggle -->
      <div class="flex rounded-xl bg-stone-100 p-1">
        <button
          type="button"
          @click="loginMode = 'password'"
          :class="loginMode === 'password' ? 'bg-white shadow-sm' : ''"
          class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
        >
          密码登录
        </button>
        <button
          type="button"
          @click="loginMode = 'email'"
          :class="loginMode === 'email' ? 'bg-white shadow-sm' : ''"
          class="flex-1 py-2 text-sm font-medium rounded-lg transition-all"
        >
          验证码登录
        </button>
      </div>

      <!-- Password Login -->
      <template v-if="loginMode === 'password'">
        <div>
          <label class="block text-sm font-medium text-stone-700 mb-2">用户名</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            class="w-full px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-stone-700 mb-2">密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="w-full px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
          />
        </div>
      </template>

      <!-- Email Code Login -->
      <template v-else>
        <div>
          <label class="block text-sm font-medium text-stone-700 mb-2">邮箱</label>
          <div class="flex gap-2">
            <input
              v-model="email"
              type="email"
              placeholder="请输入邮箱"
              class="flex-1 px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
            />
            <button
              type="button"
              @click="handleSendCode"
              :disabled="sendingCode || !email"
              class="px-4 py-3 bg-stone-100 hover:bg-stone-200 disabled:opacity-50 text-sm font-medium text-stone-700 rounded-xl transition-colors whitespace-nowrap"
            >
              {{ sendingCode ? '发送中...' : '获取验证码' }}
            </button>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-stone-700 mb-2">验证码</label>
          <input
            v-model="verifyCode"
            type="text"
            placeholder="请输入验证码"
            class="w-full px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
          />
        </div>
      </template>

      <button
        type="submit"
        :disabled="loading"
        class="w-full py-3 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-colors shadow-sm"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>

    <div class="flex justify-between mt-4 text-sm">
      <button @click="toggleMode" class="text-amber-600 hover:text-amber-700 font-medium">
        {{ loginMode === 'password' ? '验证码登录' : '密码登录' }}
      </button>
      <RouterLink to="/auth/forgot-password" class="text-stone-500 hover:text-stone-700">
        忘记密码?
      </RouterLink>
    </div>

    <p class="text-center mt-6 text-sm text-stone-500">
      还没有账号?
      <RouterLink to="/auth/register" class="text-amber-600 hover:text-amber-700 font-medium">立即注册</RouterLink>
    </p>
  </div>
</template>
