<script setup lang="ts">
import {onMounted, computed, ref, onUnmounted} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { marked } from 'marked'
import Avatar from '@/components/common/Avatar.vue'
import IconButton from '@/components/common/IconButton.vue'
import CommentList from '@/components/forum/CommentList.vue'
import Skeleton from '@/components/common/Skeleton.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useForumStore } from '@/stores/forum'
import { useChatStore } from '@/stores/chat'
import { useAuthStore } from '@/stores/auth'
import notification from '@/utils/notification'

const route = useRoute()
const router = useRouter()
const forumStore = useForumStore()
const chatStore = useChatStore()
const authStore = useAuthStore()

const postId = computed(() => Number(route.params.id))

// Check if post author is already a friend
const isFriend = computed(() => {
  if (!forumStore.currentPost) return false
  const authorId = forumStore.currentPost.author.id
  return chatStore.friends.some(f => f.user.id === authorId)
})

// Check if the author is the current user
const isSelf = computed(() => {
  if (!forumStore.currentPost) return false
  return forumStore.currentPost.author.id === authStore.user?.id
})

// Check if can add friend (not self, not already friend)
const canAddFriend = computed(() => {
  return !isSelf.value && !isFriend.value
})

const addingFriend = ref(false)

// Dropdown menu state
const showMenu = ref(false)
const menuRef = ref<HTMLElement | null>(null)

// Delete confirmation dialog state
const showDeleteConfirm = ref(false)

const toggleMenu = () => {
  showMenu.value = !showMenu.value
}

const closeMenu = () => {
  showMenu.value = false
}

// Close menu when clicking outside
const handleClickOutside = (event: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    closeMenu()
  }
}

onMounted(async () => {
  await forumStore.fetchPostById(postId.value)
  await forumStore.fetchComments(postId.value)
  // Fetch friends list to check friendship status
  await chatStore.fetchFriends()

  // Add click outside listener
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  // Remove click outside listener
  document.removeEventListener('click', handleClickOutside)
})

const handleLike = async () => {
  await forumStore.likePost(postId.value)
}

const handleCollect = async () => {
  await forumStore.collectPost(postId.value)
}

const handleAddFriend = async () => {
  if (!forumStore.currentPost || !canAddFriend.value) return

  addingFriend.value = true
  try {
    await chatStore.sendFriendRequest(forumStore.currentPost.author.id)
    // Refresh friends list
    await chatStore.fetchFriends()
    notification.success('好友申请已发送', { title: '发送成功' })
  } catch (e: any) {
    notification.error(e.message || '好友申请发送失败', { title: '发送失败' })
    console.error('Add friend failed:', e)
  } finally {
    addingFriend.value = false
  }
}

const handleEdit = () => {
  closeMenu()
  router.push(`/post/${postId.value}/edit`)
}

const handleDelete = () => {
  closeMenu()
  showDeleteConfirm.value = true
}

const handleConfirmDelete = async () => {
  try {
    await forumStore.deletePost(postId.value)
    notification.success('帖子已删除', { title: '删除成功' })
    showDeleteConfirm.value = false
    router.back()
  } catch (e: any) {
    notification.error(e.message || '删除失败，请重试', { title: '删除失败' })
    console.error('Delete post failed:', e)
    showDeleteConfirm.value = false
  }
}

const handleCancelDelete = () => {
  showDeleteConfirm.value = false
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="max-w-3xl mx-auto">
    <!-- Loading -->
    <div v-if="forumStore.loading && !forumStore.currentPost" class="space-y-4">
      <Skeleton variant="text" :lines="1" height="32px" />
      <div class="flex items-center gap-3">
        <Skeleton variant="circular" width="40px" height="40px" />
        <Skeleton variant="text" :lines="1" width="120px" />
      </div>
      <Skeleton variant="rectangular" height="300px" />
      <Skeleton variant="text" lines="5" />
    </div>

    <!-- Post Content -->
    <article v-else-if="forumStore.currentPost" class="space-y-5">
      <!-- Back & Actions -->
      <div class="flex items-center justify-between">
        <button @click="router.back()" class="text-stone-500 hover:text-stone-700 transition-colors flex items-center gap-1">
          ← 返回
        </button>
        <!-- More Actions Menu -->
        <div class="relative" ref="menuRef">
          <IconButton icon="⋮" @click="toggleMenu" />

          <!-- Dropdown Menu -->
          <div
              v-if="showMenu && isSelf"
              class="absolute right-0 mt-2 w-40 bg-white rounded-lg shadow-lg border border-stone-200 py-1 z-50"
          >
            <button
                @click="handleEdit"
                class="w-full px-4 py-2 text-left text-sm text-stone-700 hover:bg-stone-100 transition-colors flex items-center gap-2"
            >
              ✏️ 编辑帖子
            </button>
            <button
                @click="handleDelete"
                class="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 transition-colors flex items-center gap-2"
            >
              🗑️ 删除帖子
            </button>
          </div>
        </div>
      </div>

      <!-- Author Info -->
      <div class="flex items-center gap-3">
        <Avatar :src="forumStore.currentPost.author.avatar" :alt="forumStore.currentPost.author.nickname" size="lg" />
        <div class="flex-1">
          <div class="font-semibold text-stone-900">{{ forumStore.currentPost.author.nickname }}</div>
          <div class="text-sm text-stone-500">{{ formatDate(forumStore.currentPost.createdAt) }}</div>
        </div>
        <!-- Add Friend Button -->
        <button
          v-if="canAddFriend"
          @click="handleAddFriend"
          :disabled="addingFriend"
          class="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 text-white text-xs font-medium rounded-lg transition-colors"
        >
          {{ addingFriend ? '发送中...' : '+ 添加好友' }}
        </button>
        <span v-else-if="isFriend" class="text-xs text-green-600 font-medium px-3 py-1.5">
          已好友
        </span>
        <span v-else-if="isSelf" class="text-xs text-blue-600 font-medium px-3 py-1.5">
          我自己
        </span>
      </div>

      <!-- Section Tag -->
      <span class="inline-block px-3 py-1 bg-amber-50 text-amber-600 text-xs font-medium rounded-full">
        {{ forumStore.currentPost.sectionName }}
      </span>

      <!-- Title & Content -->
      <h1 class="text-2xl font-bold text-stone-900">{{ forumStore.currentPost.title }}</h1>
      <div class="prose prose-amber max-w-none" v-html="marked.parse(forumStore.currentPost.content || '')" />

      <!-- Images -->
      <div v-if="forumStore.currentPost.images.length > 0" class="grid grid-cols-2 gap-2">
        <img
          v-for="(img, idx) in forumStore.currentPost.images"
          :key="idx"
          :src="img"
          :alt="`image-${idx}`"
          class="w-full rounded-xl object-cover bg-stone-100"
        />
      </div>

      <!-- Action Bar -->
      <div class="flex items-center gap-6 py-4 border-y border-stone-200">
        <button
          @click="handleLike"
          :class="forumStore.currentPost.isLiked ? 'text-amber-500' : 'text-stone-500 hover:text-amber-500'"
          class="flex items-center gap-2 transition-colors"
        >
          {{ forumStore.currentPost.isLiked ? '❤️' : '🤍' }}
          <span>{{ forumStore.currentPost.likeCount }}</span>
        </button>
        <button class="flex items-center gap-2 text-stone-500 hover:text-stone-700 transition-colors">
          💬 <span>{{ forumStore.currentPost.commentCount }}</span>
        </button>
        <button
          @click="handleCollect"
          :class="forumStore.currentPost.isCollected ? 'text-amber-500' : 'text-stone-500 hover:text-amber-500'"
          class="flex items-center gap-2 transition-colors"
        >
          {{ forumStore.currentPost.isCollected ? '⭐' : '☆' }}
          <span>{{ forumStore.currentPost.isCollected ? '已收藏' : '收藏' }}</span>
        </button>
        <button class="text-stone-500 hover:text-stone-700 transition-colors ml-auto">
          分享 ➦
        </button>
      </div>

      <!-- Comments -->
      <CommentList :post-id="postId" />
    </article>

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      v-model:show="showDeleteConfirm"
      title="删除帖子"
      message="确定要删除这个帖子吗？此操作不可恢复。"
      confirm-text="删除"
      cancel-text="取消"
      type="danger"
      @confirm="handleConfirmDelete"
      @cancel="handleCancelDelete"
    />
  </div>
</template>
