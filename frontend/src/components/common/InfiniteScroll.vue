<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  loading: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  loadMore: []
}>()

const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

onMounted(() => {
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !props.loading && props.hasMore) {
        emit('loadMore')
      }
    },
    { threshold: 0.1 }
  )

  if (sentinelRef.value) {
    observer.observe(sentinelRef.value)
  }
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<template>
  <div>
    <slot />
    <div ref="sentinelRef" class="h-10 flex items-center justify-center">
      <div v-if="loading" class="w-6 h-6 border-2 border-accent-primary border-t-transparent rounded-full animate-spin" />
      <span v-else-if="!hasMore" class="text-sm text-text-tertiary">没有更多了</span>
    </div>
  </div>
</template>
