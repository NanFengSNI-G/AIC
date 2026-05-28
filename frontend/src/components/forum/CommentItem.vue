<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import type { Comment } from '@/types'
import Avatar from '@/components/common/Avatar.vue'
import { useForumStore } from '@/stores/forum'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  comment: Comment
  isReply?: boolean
}>()

const emit = defineEmits<{
  like: [id: number]
  reply: [comment: Comment]
  delete: [id: number]
}>()

const forumStore = useForumStore()
const authStore = useAuthStore()

// 判断是否为当前用户的评论
const isOwnComment = computed(() => {
  return authStore.user?.id === props.comment.author.id
})

const handleLike = async () => {
  await forumStore.likeComment(props.comment.id)
  emit('like', props.comment.id)
}

const handleDelete = () => {
  // 触发删除事件，由父组件显示确认对话框
  emit('delete', props.comment.id)
}
</script>

<template>
  <div :class="['flex gap-3', isReply ? 'ml-10' : '']">
    <Avatar :src="comment.author.avatar" :alt="comment.author.nickname" :size="isReply ? 'sm' : 'md'" />
    <div class="flex-1 min-w-0">
      <div class="flex items-center justify-between mb-1">
        <div class="flex items-center gap-2">
          <span class="text-sm font-medium text-text-primary">{{ comment.author.nickname }}</span>
          <span class="text-xs text-text-tertiary">{{ new Date(comment.createdAt).toLocaleDateString() }}</span>
        </div>
        <!-- 删除按钮（仅对自己的评论显示） -->
        <button
          v-if="isOwnComment"
          @click="handleDelete"
          class="text-xs text-stone-400 hover:text-red-500 transition-colors px-2 py-1"
          title="删除评论"
        >
          🗑️ 删除
        </button>
      </div>
      <div class="text-sm text-text-primary mb-2 prose prose-sm prose-amber max-w-none" v-html="marked.parse(comment.content || '')" />
      <div class="flex items-center gap-4">
        <button
          @click="handleLike"
          :class="comment.isLiked ? 'text-amber-500' : 'text-stone-500 hover:text-amber-500'"
          class="text-xs transition-colors flex items-center gap-1"
        >
          {{ comment.isLiked ? '❤️' : '🤍' }}
          <span>{{ comment.likeCount }}</span>
        </button>
        <button
          v-if="!isReply"
          @click="$emit('reply', comment)"
          class="text-xs text-text-secondary hover:text-text-primary transition-colors"
        >
          回复
        </button>
      </div>

      <!-- Replies -->
      <div v-if="comment.replies && comment.replies.length > 0" class="mt-3 space-y-3">
        <CommentItem
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          :is-reply="true"
          @like="$emit('like', $event)"
          @reply="$emit('reply', $event)"
          @delete="$emit('delete', $event)"
        />
      </div>
    </div>
  </div>
</template>
