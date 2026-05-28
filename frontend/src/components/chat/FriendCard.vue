<script setup lang="ts">
import type { Friend } from '@/types'
import Avatar from '@/components/common/Avatar.vue'
import { useChatStore } from '@/stores/chat'

const props = defineProps<{
  friend: Friend
}>()

const emit = defineEmits<{
  click: []
  delete: [friend: Friend]
}>()

const chatStore = useChatStore()

const handleDelete = (event: Event) => {
  event.stopPropagation()
  emit('delete', props.friend)
}
</script>

<template>
  <div
      @click="$emit('click')"
      class="flex items-center gap-3 p-3 rounded-lg hover:bg-bg-tertiary cursor-pointer transition-colors group"
  >
    <Avatar
        :src="friend.user.avatar"
        :alt="friend.user.nickname"
        show-status
        :online="chatStore.isUserOnline(friend.user.id)"
    />
    <div class="flex-1 min-w-0">
      <div class="flex items-center justify-between">
        <span class="text-sm font-medium text-text-primary truncate">{{ friend.user.nickname }}</span>
        <button
            @click="handleDelete"
            class="opacity-0 group-hover:opacity-100 p-1.5 rounded-md hover:bg-red-500/10 hover:text-red-500 text-text-secondary transition-all"
            title="删除好友"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
      <p class="text-xs text-text-secondary truncate">@{{ friend.user.username }}</p>
    </div>
  </div>
</template>
