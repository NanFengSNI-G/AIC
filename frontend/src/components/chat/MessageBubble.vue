<script setup lang="ts">
import type { Message } from '@/types'
import Avatar from '@/components/common/Avatar.vue'

defineProps<{
  message: Message
  isOwn: boolean
  senderAvatar?: string
}>()

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div :class="['flex gap-2 mb-3', isOwn ? 'flex-row-reverse' : '']">
    <Avatar v-if="!isOwn" :src="senderAvatar" size="sm" />
    <div :class="['max-w-[70%] flex flex-col', isOwn ? 'items-end' : 'items-start']">
      <div
        :class="[
          'px-4 py-2 rounded-2xl text-sm',
          isOwn ? 'bg-amber-500 text-white rounded-tr-sm' : 'bg-white text-stone-800 rounded-tl-sm shadow-sm'
        ]"
      >
        {{ message.type === 'text' ? message.content : '[图片]' }}
      </div>
      <span class="text-xs text-stone-400 mt-1">{{ formatTime(message.createdAt) }}</span>
    </div>
  </div>
</template>
