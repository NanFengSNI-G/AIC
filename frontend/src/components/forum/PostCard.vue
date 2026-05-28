<script setup lang="ts">
import { computed } from 'vue'
import type { Post } from '@/types'
import Avatar from '@/components/common/Avatar.vue'

const props = defineProps<{
  post: Post
}>()

defineEmits<{
  click: [id: number]
}>()

const hasImages = computed(() => props.post.images && props.post.images.length > 0)
const displayImages = computed(() => {
  if (!hasImages.value) return []
  return props.post.images.slice(0, 4)
})

const formatCount = (count: number) => {
  if (count >= 10000) return `${(count / 10000).toFixed(1)}w`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return String(count)
}

const stripMarkdown = (text: string) => {
  if (!text) return ''
  return text
    .replace(/#{1,6}\s/g, '')
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/`{1,3}[^`]*`{1,3}/g, '')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1')
    .replace(/^[-*+]\s/gm, '')
    .replace(/^>\s/gm, '')
    .replace(/^(\d+)\.\s/gm, '')
    .replace(/~~(.+?)~~/g, '$1')
    .replace(/```[\s\S]*?```/g, '')
    .replace(/\|/g, ' ')
    .replace(/^-{3,}/gm, '')
    .replace(/\n{2,}/g, ' ')
    .trim()
}

const timeAgo = (dateStr: string) => {
  if (!dateStr) return ''
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}m`
  if (hours < 24) return `${hours}h`
  if (days < 7) return `${days}d`
  return new Date(dateStr).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <article
    @click="$emit('click', post.id)"
    class="group bg-white rounded-2xl overflow-hidden cursor-pointer transition-all duration-300 hover:shadow-xl hover:shadow-stone-200/30 hover:-translate-y-0.5 border border-stone-100"
  >
    <!-- Images -->
    <div v-if="hasImages" class="relative overflow-hidden">
      <div :class="[
        'grid gap-0.5',
        displayImages.length === 1 ? 'grid-cols-1' : '',
        displayImages.length === 2 ? 'grid-cols-2' : '',
        displayImages.length >= 3 ? 'grid-cols-2' : ''
      ]">
        <img
          v-for="(img, idx) in displayImages"
          :key="idx"
          :src="img"
          :alt="`post-image-${idx}`"
          class="w-full object-cover bg-stone-100 transition-transform duration-300 group-hover:scale-[1.02]"
          :class="[
            displayImages.length === 1 ? 'h-52' : 'h-28',
            displayImages.length === 3 && idx === 0 ? 'row-span-2 h-56' : ''
          ]"
        />
      </div>
      <!-- Image count badge -->
      <div v-if="post.images.length > 1" class="absolute bottom-2 right-2 px-2 py-1 bg-black/50 backdrop-blur-sm rounded-md text-xs text-white font-medium">
        {{ post.images.length }} 图
      </div>
    </div>

    <!-- Content -->
    <div class="p-4">
      <!-- Section Tag -->
      <div class="flex items-center justify-between mb-2.5">
        <span class="inline-flex items-center px-2 py-0.5 bg-amber-50 text-amber-600 text-[10px] font-medium rounded-md">
          {{ post.sectionName }}
        </span>
        <span class="text-[10px] text-stone-400">{{ timeAgo(post.createdAt) }}</span>
      </div>

      <!-- Title -->
      <h3 class="text-[15px] font-semibold text-stone-900 leading-snug line-clamp-2 mb-2 group-hover:text-amber-600 transition-colors">
        {{ post.title }}
      </h3>

      <!-- Preview -->
      <p class="text-xs text-stone-500 line-clamp-2 mb-4 leading-relaxed">
        {{ stripMarkdown(post.content) }}
      </p>

      <!-- Footer -->
      <div class="flex items-center justify-between pt-3 border-t border-stone-50">
        <!-- Author -->
        <div class="flex items-center gap-2">
          <Avatar :src="post.author.avatar" :alt="post.author.nickname" size="sm" />
          <div>
            <span class="text-xs font-medium text-stone-700">{{ post.author.nickname }}</span>
          </div>
        </div>

        <!-- Stats -->
        <div class="flex items-center gap-4">
          <!-- Like -->
          <div class="flex items-center gap-1.5 text-xs" :class="post.isLiked ? 'text-amber-500' : 'text-stone-400'">
            <svg class="w-4 h-4" :fill="post.isLiked ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
            </svg>
            <span>{{ formatCount(post.likeCount) }}</span>
          </div>

          <!-- Comment -->
          <div class="flex items-center gap-1.5 text-xs text-stone-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span>{{ formatCount(post.commentCount) }}</span>
          </div>
        </div>
      </div>
    </div>
  </article>
</template>
