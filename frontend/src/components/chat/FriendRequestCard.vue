<script setup lang="ts">
import type { FriendRequest } from '@/types'
import Avatar from '@/components/common/Avatar.vue'
import IconButton from '@/components/common/IconButton.vue'

const props = defineProps<{
  request: FriendRequest
  currentUserId: number
}>()

const isSentByMe = props.request.userId === props.currentUserId
</script>

<template>
  <div class="flex items-center gap-3 p-3 rounded-lg bg-bg-tertiary/50">
    <Avatar :src="request.fromUser.avatar" :alt="request.fromUser.nickname" />
    <div class="flex-1 min-w-0">
      <div class="text-sm font-medium text-text-primary">{{ request.fromUser.nickname }}</div>
      <div class="text-xs text-text-secondary">@{{ request.fromUser.username }}</div>
    </div>
    <div v-if="isSentByMe && request.status === 'pending'" class="text-xs text-text-secondary">
      等待对方处理
    </div>
    <div v-else-if="isSentByMe && request.status === 'rejected'" class="text-xs text-text-secondary">
      对方已拒绝
    </div>
    <div v-else-if="!isSentByMe && request.status === 'rejected'" class="text-xs text-text-secondary">
      你已拒绝添加
    </div>
    <div v-else-if="!isSentByMe && request.status === 'pending'" class="flex items-center gap-2">
      <IconButton
        icon="✓"
        size="sm"
        @click="$emit('accept', request.id)"
      />
      <IconButton
        icon="✕"
        size="sm"
        @click="$emit('reject', request.id)"
      />
    </div>
  </div>
</template>
