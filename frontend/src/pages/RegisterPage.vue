<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import notification from '@/utils/notification'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const verifyCode = ref('')
const error = ref('')
const loading = ref(false)
const sendingCode = ref(false)

const handleSendCode = async () => {
  if (!email.value) return
  sendingCode.value = true
  try {
    await authStore.sendVerifyCode(email.value, 'register')
    notification.success('验证码已发送', { title: '发送成功' })
  } catch (e: any) {
    notification.error(e.message || '发送验证码失败', { title: '发送失败' })
    error.value = e.message || '发送验证码失败'
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async () => {
  if (!username.value || !email.value || !password.value || !verifyCode.value) return
  if (password.value !== confirmPassword.value) {
    error.value = '两次密码不一致'
    notification.error('两次密码不一致', { title: '注册失败' })
    return
  }

  loading.value = true
  error.value = ''

  try {
    await authStore.register(username.value, email.value, password.value, verifyCode.value)
    notification.success('注册成功，请登录', { title: '注册成功' })
    router.push('/auth/login')
  } catch (e: any) {
    notification.error(e.message || '注册失败', { title: '注册失败' })
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="max-w-sm mx-auto mt-16">
    <div class="text-center mb-8">
      <h1 class="text-2xl font-bold text-stone-900 mb-2">创建账号</h1>
      <p class="text-stone-500">加入社交论坛</p>
    </div>

    <form @submit.prevent="handleSubmit" class="space-y-4">
      <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
        {{ error }}
      </div>

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

      <div>
        <label class="block text-sm font-medium text-stone-700 mb-2">确认密码</label>
        <input
          v-model="confirmPassword"
          type="password"
          placeholder="请再次输入密码"
          class="w-full px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
        />
      </div>

      <button
        type="submit"
        :disabled="loading || !username || !email || !password || !verifyCode"
        class="w-full py-3 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-colors shadow-sm"
      >
        {{ loading ? '注册中...' : '注册' }}
      </button>
    </form>

    <p class="text-center mt-6 text-sm text-stone-500">
      已有账号?
      <RouterLink to="/auth/login" class="text-amber-600 hover:text-amber-700 font-medium">立即登录</RouterLink>
    </p>
  </div>
</template>
