<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import type { Message } from '@/types'
import MessageBubble from './MessageBubble.vue'
import IconButton from '@/components/common/IconButton.vue'

const props = defineProps<{
  messages: Message[]
  currentUserId: number
  friendAvatar?: string
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const messageListRef = ref<HTMLElement | null>(null)
const inputContent = ref('')

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom, { immediate: true })
watch(() => props.messages, (msgs) => {
  console.log('Messages in ChatWindow:', msgs.map(m => ({
    id: m.id,
    senderId: m.senderId,
    content: m.content,
    isOwn: m.senderId === props.currentUserId
  })))
}, { deep: true })

const handleSend = () => {
  if (!inputContent.value.trim()) return
  emit('send', inputContent.value)
  inputContent.value = ''
}
</script>

<template>
  <div class="flex flex-col h-full bg-stone-50">
    <!-- Messages -->
    <div ref="messageListRef" class="flex-1 overflow-y-auto p-4 space-y-1">
      <div v-if="messages.length === 0" class="flex items-center justify-center h-full text-stone-400 text-sm">
        暂无消息记录
      </div>
      <MessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
        :is-own="msg.senderId === currentUserId"
        :sender-avatar="msg.senderId === currentUserId ? undefined : friendAvatar"
      />
    </div>

    <!-- Input -->
    <div class="border-t border-stone-200 p-3 bg-white">
      <div class="flex items-center gap-2">
        <IconButton icon="📎" size="sm" />
        <input
          v-model="inputContent"
          @keyup.enter="handleSend"
          type="text"
          placeholder="输入消息..."
          class="flex-1 px-4 py-2 bg-stone-100 rounded-full text-sm text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500"
        />
        <button
          @click="handleSend"
          :disabled="!inputContent.trim()"
          class="w-10 h-10 flex items-center justify-center bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white rounded-full transition-colors"
        >
          ➤
        </button>
      </div>
    </div>
  </div>
</template>
