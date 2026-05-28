<script setup lang="ts">
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import PostCard from './PostCard.vue'
import InfiniteScroll from '@/components/common/InfiniteScroll.vue'
import Skeleton from '@/components/common/Skeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useForumStore } from '@/stores/forum'

const forumStore = useForumStore()

defineEmits<{
  postClick: [id: number]
}>()
</script>

<template>
  <div>
    <!-- Loading Skeleton -->
    <MasonryGrid v-if="forumStore.loading && forumStore.posts.length === 0" :column-count="3">
      <div v-for="i in 9" :key="i" class="bg-white rounded-2xl overflow-hidden border border-stone-100">
        <Skeleton variant="rectangular" height="150px" />
        <div class="p-4 space-y-2">
          <Skeleton variant="text" :lines="2" />
          <Skeleton variant="text" :lines="1" />
        </div>
      </div>
    </MasonryGrid>

    <!-- Posts -->
    <MasonryGrid v-else-if="forumStore.posts.length > 0">
      <PostCard
        v-for="post in forumStore.posts"
        :key="post.id"
        :post="post"
        @click="$emit('postClick', $event)"
      />
    </MasonryGrid>

    <!-- Empty State -->
    <EmptyState
      v-else
      icon="📝"
      title="还没有帖子"
      description="成为第一个发帖的人吧"
    />

    <!-- Infinite Scroll -->
    <InfiniteScroll
      v-if="forumStore.posts.length > 0"
      :loading="forumStore.loading"
      :has-more="forumStore.hasMore"
      @load-more="forumStore.fetchPosts()"
    />
  </div>
</template>
