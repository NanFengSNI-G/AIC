<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Tabs from '@/components/common/Tabs.vue'
import FriendCard from '@/components/chat/FriendCard.vue'
import FriendRequestCard from '@/components/chat/FriendRequestCard.vue'
import ChatList from '@/components/chat/ChatList.vue'
import AddFriendPanel from '@/components/chat/AddFriendPanel.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()

const tabs = [
  { key: 'friends', label: '好友列表' },
  { key: 'requests', label: '申请列表' },
  { key: 'conversations', label: '会话列表' },
  { key: 'add', label: '添加好友' }
]

const activeTab = ref('friends')
const showDeleteConfirm = ref(false)
const friendToDelete = ref<{ id: number; user: { nickname: string } } | null>(null)

onMounted(async () => {
  await Promise.all([
    chatStore.fetchFriends(),
    chatStore.fetchRequests(),
    chatStore.fetchConversations()
  ])
})

const handleFriendClick = (friend: { user: { id: number } }) => {
  router.push(`/chat/${friend.user.id}`)
}

const handleConversationSelect = (friendId: number) => {
  router.push(`/chat/${friendId}`)
}

const handleAccept = async (id: number) => {
  await chatStore.acceptRequest(id)
}

const handleReject = async (id: number) => {
  await chatStore.rejectRequest(id)
}

const handleDeleteFriend = (friend: { id: number; user: { nickname: string } }) => {
  friendToDelete.value = friend
  showDeleteConfirm.value = true
}

const handleConfirmDelete = async () => {
  if (friendToDelete.value) {
    await chatStore.deleteFriend(friendToDelete.value.id)
    showDeleteConfirm.value = false
    friendToDelete.value = null
  }
}

const handleCancelDelete = () => {
  showDeleteConfirm.value = false
  friendToDelete.value = null
}

const getDeleteMessage = () => {
  return friendToDelete.value ? `确定要删除好友 "${friendToDelete.value.user.nickname}" 吗？此操作不可恢复。` : ''
}
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <h1 class="text-xl font-bold mb-4">聊天</h1>

    <Tabs v-model="activeTab" :tabs="tabs" />

    <div class="mt-4">
      <!-- Friends Tab -->
      <div v-if="activeTab === 'friends'">
        <div v-if="chatStore.friends.length > 0" class="space-y-1">
          <FriendCard
            v-for="friend in chatStore.friends"
            :key="friend.id"
            :friend="friend"
            @click="handleFriendClick(friend)"
            @delete="handleDeleteFriend"
          />
        </div>
        <EmptyState
          v-else
          icon="👥"
          title="还没有好友"
          description="去添加一些好友吧"
        />
      </div>

      <!-- Requests Tab -->
      <div v-else-if="activeTab === 'requests'">
        <div v-if="chatStore.requests.length > 0" class="space-y-2">
          <FriendRequestCard
            v-for="request in chatStore.requests"
            :key="request.id"
            :request="request"
            :current-user-id="authStore.user?.id ?? 0"
            @accept="handleAccept"
            @reject="handleReject"
          />
        </div>
        <EmptyState
          v-else
          icon="📨"
          title="没有待处理申请"
          description=""
        />
      </div>

      <!-- Conversations Tab -->
      <div v-else-if="activeTab === 'conversations'">
        <ChatList
          v-if="chatStore.conversations.length > 0"
          :conversations="chatStore.conversations"
          @select="handleConversationSelect"
        />
        <EmptyState
          v-else
          icon="💬"
          title="没有会话"
          description="开始聊天吧"
        />
      </div>

      <!-- Add Friend Tab -->
      <div v-else-if="activeTab === 'add'">
        <AddFriendPanel />
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      v-model:show="showDeleteConfirm"
      title="删除好友"
      :message="getDeleteMessage()"
      confirm-text="删除"
      cancel-text="取消"
      type="danger"
      @confirm="handleConfirmDelete"
      @cancel="handleCancelDelete"
    />
  </div>
</template>
