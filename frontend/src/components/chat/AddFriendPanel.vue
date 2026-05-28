<script setup lang="ts">
import { ref } from 'vue'
import Avatar from '@/components/common/Avatar.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'

const authStore = useAuthStore()
const chatStore = useChatStore()

const searchKeyword = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)
const addingId = ref<number | null>(null)
const searchDone = ref(false)

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) return

  searching.value = true
  searchDone.value = false
  searchResults.value = []

  try {
    const result = await authStore.searchUsers(searchKeyword.value.trim())
    searchResults.value = result.list.filter((u: any) => u.userId !== authStore.user?.id)
    searchDone.value = true
  } catch (e) {
    console.error('Search failed:', e)
  } finally {
    searching.value = false
  }
}

const handleAddFriend = async (userId: number) => {
  addingId.value = userId
  try {
    await chatStore.sendFriendRequest(userId)
    // Mark as already sent
    const result = await authStore.searchUsers(searchKeyword.value.trim())
    searchResults.value = result.list.filter((u: any) => u.userId !== authStore.user?.id)
  } catch (e) {
    console.error('Add friend failed:', e)
  } finally {
    addingId.value = null
  }
}

const handleKeyup = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleSearch()
  }
}

// Get button text and state based on friendStatus
const getFriendStatusInfo = (status: string) => {
  switch (status) {
    case 'friends':
      return { text: '已是好友', disabled: true, className: 'text-green-600' }
    case 'request_sent':
      return { text: '已发送', disabled: true, className: 'text-stone-500' }
    case 'request_received':
      return { text: '待处理', disabled: true, className: 'text-amber-600' }
    default:
      return { text: '添加好友', disabled: false, className: '' }
  }
}
</script>

<template>
  <div class="space-y-4">
    <!-- Search Input -->
    <div class="flex gap-2">
      <input
        v-model="searchKeyword"
        @keyup="handleKeyup"
        type="text"
        placeholder="搜索用户名..."
        class="flex-1 px-4 py-2 bg-stone-100 border border-stone-200 rounded-xl text-sm text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent"
      />
      <button
        @click="handleSearch"
        :disabled="searching || !searchKeyword.trim()"
        class="px-4 py-2 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white text-sm font-medium rounded-xl transition-colors"
      >
        {{ searching ? '搜索中...' : '搜索' }}
      </button>
    </div>

    <!-- Search Results -->
    <div v-if="searchDone" class="space-y-2">
      <div v-if="searchResults.length === 0" class="text-center text-stone-400 text-sm py-4">
        未找到用户
      </div>
      <div
        v-for="user in searchResults"
        :key="user.userId"
        class="flex items-center gap-3 p-3 bg-stone-50 rounded-xl"
      >
        <Avatar :src="user.avatar" :alt="user.username" />
        <div class="flex-1 min-w-0">
          <div class="text-sm font-medium text-stone-900 truncate">
            {{ user.username }}
          </div>
          <div class="text-xs text-stone-500 truncate">@{{ user.username }}</div>
        </div>
        <button
            v-if="!getFriendStatusInfo(user.friendStatus).disabled"
            @click="handleAddFriend(user.userId)"
            :disabled="addingId === user.userId"
            class="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white text-xs font-medium rounded-lg transition-colors"
        >
          {{ addingId === user.userId ? '发送中...' : '添加好友' }}
        </button>
        <span
            v-else
            :class="['text-xs font-medium', getFriendStatusInfo(user.friendStatus).className]"
        >
          {{ getFriendStatusInfo(user.friendStatus).text }}
        </span>
      </div>
    </div>
  </div>
</template>
