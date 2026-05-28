<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import Avatar from '@/components/common/Avatar.vue'
import Badge from '@/components/common/Badge.vue'
import IconButton from '@/components/common/IconButton.vue'

const route = useRoute()
const authStore = useAuthStore()
const chatStore = useChatStore()

const totalUnread = computed(() => chatStore.totalUnread + chatStore.pendingRequestsCount)
const isLoggedIn = computed(() => authStore.isLoggedIn)
const user = computed(() => authStore.user)

const handleLogout = async () => {
  await authStore.logout()
  setTimeout(() => {
    window.location.href = '/'
  }, 100)
}

const navLinkClass = (path: string) => [
  'relative text-sm font-medium transition-colors px-1 py-0.5',
  route.path === path
    ? 'text-amber-600'
    : 'text-stone-500 hover:text-stone-800',
]
const navIndicator = (path: string) =>
  route.path === path
    ? 'absolute -bottom-1 left-1/2 -translate-x-1/2 w-5 h-0.5 bg-amber-500 rounded-full'
    : ''
</script>

<template>
  <div class="min-h-screen relative">
    <!-- Ambient background decoration -->
    <div class="fixed inset-0 pointer-events-none z-0 overflow-hidden">
      <div class="absolute -top-40 -right-40 w-[500px] h-[500px] rounded-full bg-amber-100/20 blur-3xl" />
      <div class="absolute -bottom-20 -left-20 w-[400px] h-[400px] rounded-full bg-stone-200/30 blur-3xl" />
    </div>

    <!-- Navbar -->
    <header class="sticky top-0 z-40 bg-white/80 backdrop-blur-xl border-b border-stone-200/60">
      <nav class="max-w-6xl mx-auto px-5 h-16 flex items-center justify-between">
        <!-- Left: Logo + Nav -->
        <div class="flex items-center gap-8">
          <RouterLink
            to="/"
            class="text-xl font-bold tracking-tight transition-colors"
          >
            <span class="text-amber-600">Dev</span><span class="text-stone-800">Forum</span>
          </RouterLink>

          <div class="flex items-center gap-1">
            <RouterLink to="/forum" :class="navLinkClass('/forum')">
              论坛
              <span :class="navIndicator('/forum')" />
            </RouterLink>
            <RouterLink to="/interview" :class="navLinkClass('/interview')" class="ml-4">
              模拟面试
              <span :class="navIndicator('/interview')" />
            </RouterLink>
            <RouterLink to="/blog" :class="navLinkClass('/blog')" class="ml-4">
              博客助手
              <span :class="navIndicator('/blog')" />
            </RouterLink>
          </div>
        </div>

        <!-- Right: User -->
        <div class="flex items-center gap-3">
          <template v-if="isLoggedIn">
            <RouterLink to="/chat" class="relative p-2 rounded-xl hover:bg-stone-100 transition-colors">
              <svg class="w-5 h-5 text-stone-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <Badge v-if="totalUnread > 0" :value="totalUnread" class="absolute -top-0.5 -right-0.5" />
            </RouterLink>

            <div class="relative group">
              <button class="rounded-full hover:ring-2 hover:ring-amber-200 hover:ring-offset-2 transition-all">
                <Avatar :src="user?.avatar" :alt="user?.nickname" size="sm" />
              </button>

              <div class="absolute right-0 mt-2 w-48 py-1.5 bg-white rounded-2xl shadow-lg border border-stone-200/80 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200">
                <div class="px-3 py-1.5 border-b border-stone-100">
                  <p class="text-xs font-medium text-stone-800 truncate">{{ user?.nickname }}</p>
                  <p class="text-[10px] text-stone-400 truncate">@{{ user?.username }}</p>
                </div>
                <RouterLink to="/profile" class="block px-3 py-2 text-sm text-stone-600 hover:bg-stone-50 transition-colors">
                  个人资料
                </RouterLink>
                <button @click="handleLogout()" class="w-full text-left px-3 py-2 text-sm text-red-500 hover:bg-red-50 transition-colors">
                  退出登录
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <RouterLink to="/auth/login" class="px-4 py-2 text-sm text-stone-500 hover:text-stone-800 transition-colors font-medium">
              登录
            </RouterLink>
            <RouterLink to="/auth/register" class="px-4 py-2 text-sm bg-amber-500 hover:bg-amber-600 text-white rounded-xl transition-all font-medium shadow-sm hover:shadow-md">
              注册
            </RouterLink>
          </template>
        </div>
      </nav>
    </header>

    <!-- Main Content -->
    <main class="relative z-10 max-w-6xl mx-auto px-5 py-6">
      <slot />
    </main>
  </div>
</template>