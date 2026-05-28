<script setup lang="ts">
import { ref } from 'vue'
import CommentItem from './CommentItem.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Modal from '@/components/common/Modal.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useForumStore } from '@/stores/forum'
import notification from '@/utils/notification'

const forumStore = useForumStore()

const replyingTo = ref<{ id: number; authorName: string } | null>(null)
const mainCommentContent = ref('')
const replyCommentContent = ref('')
const showReplyModal = ref(false)

// 删除确认对话框状态
const showDeleteConfirm = ref(false)
const commentToDelete = ref<number | null>(null)

defineProps<{
  postId: number
}>()

const emit = defineEmits<{
  added: []
}>()

const handleReply = (comment: any) => {
  replyingTo.value = { id: comment.id, authorName: comment.author.nickname }
  replyCommentContent.value = ''
  showReplyModal.value = true
}

const handleSubmit = async () => {
  if (!replyCommentContent.value.trim()) return

  await forumStore.addComment(
    forumStore.currentPost!.id,
    replyCommentContent.value,
    replyingTo.value?.id
  )

  replyCommentContent.value = ''
  replyingTo.value = null
  showReplyModal.value = false
  emit('added')
}

const handleCancel = () => {
  showReplyModal.value = false
  replyingTo.value = null
  replyCommentContent.value = ''
}

const handleSubmitMainComment = async () => {
  if (!mainCommentContent.value.trim()) return

  await forumStore.addComment(
    forumStore.currentPost!.id,
    mainCommentContent.value
  )

  mainCommentContent.value = ''
  emit('added')
}

const handleDeleteComment = (commentId: number) => {
  commentToDelete.value = commentId
  showDeleteConfirm.value = true
}

const handleConfirmDelete = async () => {
  if (commentToDelete.value !== null) {
    try {
      await forumStore.deleteComment(commentToDelete.value)
      
      // 递归删除评论（支持一级和二级评论）
      const deleteCommentRecursive = (comments: any[], targetId: number): boolean => {
        const index = comments.findIndex(c => c.id === targetId)
        if (index !== -1) {
          comments.splice(index, 1)
          return true
        }
        
        // 在子评论中查找
        for (const comment of comments) {
          if (comment.replies && comment.replies.length > 0) {
            if (deleteCommentRecursive(comment.replies, targetId)) {
              return true
            }
          }
        }
        
        return false
      }
      
      deleteCommentRecursive(forumStore.comments, commentToDelete.value)
      
      // 显示成功通知
      notification.success('评论已删除', { title: '删除成功' })
      
      // 重置状态
      commentToDelete.value = null
      showDeleteConfirm.value = false
    } catch (error: any) {
      console.error('删除评论失败:', error)
      notification.error(error.message || '删除失败，请重试', { title: '删除失败' })
      // 即使失败也要关闭对话框
      commentToDelete.value = null
      showDeleteConfirm.value = false
    }
  }
}

const handleCancelDelete = () => {
  commentToDelete.value = null
  showDeleteConfirm.value = false
}
</script>

<template>
  <div class="space-y-4">
    <h3 class="text-lg font-medium">评论 ({{ forumStore.comments.length }})</h3>

    <!-- Main Comment Input -->
    <div class="flex gap-3">
      <div class="flex-1">
        <textarea
          v-model="mainCommentContent"
          placeholder="发表你的看法..."
          class="w-full px-3 py-2 bg-bg-tertiary rounded-lg text-sm text-text-primary placeholder-text-tertiary resize-none focus:outline-none focus:ring-1 focus:ring-accent-primary"
          rows="3"
        />
      </div>
      <button
        @click="handleSubmitMainComment"
        :disabled="!mainCommentContent.trim()"
        class="px-4 py-2 bg-accent-primary hover:bg-accent-hover disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors self-end"
      >
        发送
      </button>
    </div>

    <!-- Reply Modal -->
    <Modal v-model:show="showReplyModal" title="回复评论">
      <div class="space-y-4">
        <div v-if="replyingTo" class="text-sm text-text-secondary">
          回复 <span class="font-medium text-text-primary">@{{ replyingTo.authorName }}</span>
        </div>
        <textarea
          v-model="replyCommentContent"
          placeholder="写下你的回复..."
          class="w-full px-3 py-2 bg-bg-tertiary rounded-lg text-sm text-text-primary placeholder-text-tertiary resize-none focus:outline-none focus:ring-1 focus:ring-accent-primary"
          rows="4"
          autofocus
        />
        <div class="flex justify-end gap-2">
          <button
            @click="handleCancel"
            class="px-4 py-2 text-sm text-text-secondary hover:text-text-primary transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSubmit"
            :disabled="!replyCommentContent.trim()"
            class="px-4 py-2 bg-accent-primary hover:bg-accent-hover disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors"
          >
            发送
          </button>
        </div>
      </div>
    </Modal>

    <!-- Comments -->
    <div v-if="forumStore.comments.length > 0" class="space-y-4">
      <CommentItem
        v-for="comment in forumStore.comments"
        :key="comment.id"
        :comment="comment"
        @reply="handleReply"
        @like="() => {}"
        @delete="handleDeleteComment"
      />
    </div>

    <EmptyState
      v-else
      icon="💬"
      title="还没有评论"
      description="来说点什么吧"
    />

    <!-- Delete Confirmation Dialog -->
    <ConfirmDialog
      v-model:show="showDeleteConfirm"
      title="删除评论"
      message="确定要删除这条评论吗？此操作不可恢复。"
      confirm-text="删除"
      cancel-text="取消"
      type="danger"
      @confirm="handleConfirmDelete"
      @cancel="handleCancelDelete"
    />
  </div>
</template>
