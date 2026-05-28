<script setup lang="ts">
import type { Conversation } from '@/types'
import Avatar from '@/components/common/Avatar.vue'
import Badge from '@/components/common/Badge.vue'

defineProps<{
  conversations: Conversation[]
}>()

defineEmits<{
  select: [id: number]
}>()

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString()
  }
}
</script>

<template>
  <div class="divide-y divide-bg-tertiary">
    <div
      v-for="conv in conversations"
      :key="conv.friend.id"
      @click="$emit('select', conv.friend.id)"
      class="flex items-center gap-3 p-3 hover:bg-bg-tertiary cursor-pointer transition-colors"
    >
      <div class="relative">
        <Avatar :src="conv.friend.avatar" :alt="conv.friend.nickname" />
        <Badge v-if="conv.unreadCount > 0" :value="conv.unreadCount" class="absolute -top-1 -right-1" />
      </div>
      <div class="flex-1 min-w-0">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-text-primary truncate">{{ conv.friend.nickname }}</span>
          <span class="text-xs text-text-tertiary">{{ formatTime(conv.updatedAt) }}</span>
        </div>
        <p class="text-xs text-text-secondary truncate">{{ conv.lastMessage?.content || '暂无消息' }}</p>
      </div>
    </div>
  </div>
</template>
