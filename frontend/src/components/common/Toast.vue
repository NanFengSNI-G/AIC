<script setup lang="ts">
import { ref } from 'vue'

interface NotificationItem {
  id: number
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
  title?: string
}

const notifications = ref<NotificationItem[]>([])
let nextId = 1

const show = (message: string, type: NotificationItem['type'] = 'info', title?: string) => {
  const id = nextId++
  const notification: NotificationItem = { id, message, type, title }
  notifications.value.push(notification)
  
  // 5秒后自动移除
  setTimeout(() => {
    remove(id)
  }, 5000)
}

// 获取通知的创建时间
const getNotificationAge = (notification: NotificationItem) => {
  return 0 // 简化版本，实际可以记录时间戳
}

const remove = (id: number) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 暴露方法供外部调用
defineExpose({ show })

// 提供全局方法
if (typeof window !== 'undefined') {
  ;(window as any).$notification = {
    success: (message: string, title?: string) => show(message, 'success', title),
    error: (message: string, title?: string) => show(message, 'error', title),
    warning: (message: string, title?: string) => show(message, 'warning', title),
    info: (message: string, title?: string) => show(message, 'info', title)
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[99999] space-y-3 max-w-sm w-full pointer-events-none">
      <TransitionGroup name="notification">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="pointer-events-auto"
        >
          <div
            class="relative overflow-hidden rounded-2xl bg-black/5 backdrop-blur-xl border border-white/10 transition-all duration-300"
            style="box-shadow: inset 0 1px 0 0 rgba(255, 255, 255, 0.1), 0 8px 32px rgba(0, 0, 0, 0.08)"
          >
            <!-- 关闭按钮 -->
            <button
              @click="remove(notification.id)"
              class="absolute top-3 right-3 w-7 h-7 flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 transition-all duration-200 backdrop-blur-sm text-stone-600 hover:text-stone-900"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>

            <!-- 内容区域 -->
            <div class="pl-4 pr-12 py-4">
              <div v-if="notification.title" class="font-semibold text-base mb-1.5 text-stone-900">
                {{ notification.title }}
              </div>
              <div class="text-sm leading-relaxed text-stone-700">
                {{ notification.message }}
              </div>
            </div>

            <!-- 进度条 -->
            <div class="absolute bottom-0 left-0 right-0 h-0.5 bg-stone-200/50">
              <div 
                class="h-full bg-stone-600/40"
                style="animation: toast-progress 5s linear forwards"
              ></div>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.notification-enter-active,
.notification-leave-active {
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

.notification-enter-from {
  transform: translateX(100%) scale(0.9) translateY(-10px);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.95) translateY(-5px);
}

.notification-move {
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
</style>

<style>
/* 全局样式 - 确保进度条动画正常工作 */
@keyframes toast-progress {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}

@-webkit-keyframes toast-progress {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}
</style>
