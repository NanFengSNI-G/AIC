<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ChatWindow from '@/components/chat/ChatWindow.vue'
import Avatar from '@/components/common/Avatar.vue'
import IconButton from '@/components/common/IconButton.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()

const friendId = computed(() => Number(route.params.friendId))

const currentUserId = computed(() => {
  if (authStore.user?.id) return authStore.user.id
  const stored = localStorage.getItem('userId')
  if (stored) return parseInt(stored, 10)
  return 0
})

// Get messages for this friend - messages array stores all messages
const messages = computed(() => {
  return chatStore.messages.filter(m => m.conversationId === friendId.value)
})

onMounted(async () => {
  if (!authStore.user && authStore.token) {
    await authStore.fetchCurrentUser()
  }

  console.log('PrivateMessagePage mounted:', {
    friendId: friendId.value,
    currentUserId: currentUserId.value
  })

  // Fetch messages for this friend (don't clear global messages)
  await chatStore.fetchMessages(friendId.value)
  await chatStore.markAsRead(friendId.value)
})

const handleSend = (content: string) => {
  console.log('handleSend:', content)
  chatStore.sendMessage(friendId.value, content)
}

const friend = computed(() => {
  const f = chatStore.friends.find(f => f.user.id === friendId.value)
  if (f) return f.user
  const conv = chatStore.conversations.find(c => c.friend.id === friendId.value)
  return conv?.friend
})
</script>

<template>
  <div class="flex flex-col h-[calc(100vh-8rem)] bg-white rounded-lg shadow-sm">
    <!-- Header -->
    <div class="flex items-center gap-3 pb-3 border-b border-stone-200">
      <button @click="router.back()" class="text-stone-500 hover:text-stone-700 transition-colors">
        ←
      </button>
      <Avatar v-if="friend" :src="friend.avatar" :alt="friend.nickname" size="sm" />
      <span class="font-medium text-stone-900">{{ friend?.nickname || friend?.username || '聊天' }}</span>
      <div class="flex-1" />
      <IconButton icon="⋮" size="sm" />
    </div>

    <!-- Chat Window -->
    <div class="flex-1 overflow-hidden">
      <ChatWindow
        :messages="messages"
        :current-user-id="currentUserId"
        :friend-avatar="friend?.avatar"
        @send="handleSend"
      />
    </div>
  </div>
</template>
