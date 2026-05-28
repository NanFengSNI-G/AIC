<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PostList from '@/components/forum/PostList.vue'
import { useForumStore } from '@/stores/forum'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const forumStore = useForumStore()
const authStore = useAuthStore()

const searchInput = ref('')
let searchTimer: number | null = null

onMounted(() => {
  forumStore.fetchPosts(true)
  forumStore.fetchSections()
})

const handlePostClick = (id: number) => {
  router.push(`/post/${id}`)
}

const handleOpenCreateModal = () => {
  if (!authStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/forum' } })
    return
  }
  router.push('/post/create')
}

const handleSortChange = (sort: 'latest' | 'hot' | 'relevance') => {
  forumStore.setSortBy(sort)
}

const handleSearchInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  searchInput.value = target.value

  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    forumStore.setSearchKeyword(searchInput.value)
  }, 300)
}

const clearSearch = () => {
  searchInput.value = ''
  forumStore.setSearchKeyword('')
}

const handleSectionChange = (sectionId: number | null) => {
  forumStore.setSelectedSection(sectionId)
}
</script>

<template>
  <div>
    <!-- Hero Section -->
    <div class="mb-8 text-center py-8">
      <h1 class="text-3xl font-bold text-stone-900 mb-2">发现精彩内容</h1>
      <p class="text-stone-500">浏览来自社区的最新帖子</p>
    </div>

    <!-- Search Bar -->
    <div class="mb-6 max-w-2xl mx-auto">
      <div class="relative">
        <input
            v-model="searchInput"
            @input="handleSearchInput"
            type="text"
            placeholder="搜索帖子标题或内容..."
            class="w-full px-4 py-3 pl-12 pr-10 bg-white border border-stone-200 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all duration-200"
        />
        <svg
            class="absolute left-4 top-1/2 transform -translate-y-1/2 w-5 h-5 text-stone-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
        >
          <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <button
            v-if="searchInput"
            @click="clearSearch"
            class="absolute right-4 top-1/2 transform -translate-y-1/2 text-stone-400 hover:text-stone-600 transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      <div v-if="forumStore.isSearching" class="mt-2 text-sm text-stone-500 text-center">
        找到 {{ forumStore.posts.length }} 个相关结果
      </div>
    </div>

    <!-- Section Filter & Sort Selector -->
    <div class="mb-6 flex items-center justify-between gap-4">
      <!-- Section Filter -->
      <div class="flex-1 overflow-x-auto scrollbar-hide">
        <div class="flex items-center gap-2 whitespace-nowrap">
          <button
            @click="handleSectionChange(null)"
            :class="[
              'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 border',
              forumStore.selectedSectionId === null
                ? 'bg-amber-500 text-white border-amber-500 shadow-sm'
                : 'bg-white text-stone-600 border-stone-200 hover:border-amber-300 hover:text-amber-600'
            ]"
          >
            全部版块
          </button>
          <button
            v-for="section in forumStore.sections"
            :key="section.id"
            @click="handleSectionChange(section.id)"
            :class="[
              'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 border',
              forumStore.selectedSectionId === section.id
                ? 'bg-amber-500 text-white border-amber-500 shadow-sm'
                : 'bg-white text-stone-600 border-stone-200 hover:border-amber-300 hover:text-amber-600'
            ]"
          >
            {{ section.name }}
          </button>
        </div>
      </div>

      <!-- Sort Selector -->
      <div class="flex-shrink-0">
        <div class="inline-flex bg-white rounded-lg border border-stone-200 p-1 shadow-sm">
          <button
              v-if="forumStore.isSearching"
              @click="handleSortChange('relevance')"
              :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-all duration-200',
              forumStore.sortBy === 'relevance'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-stone-600 hover:text-stone-900 hover:bg-stone-50'
            ]"
          >
            相关性
          </button>
          <button
              @click="handleSortChange('latest')"
              :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-all duration-200',
              forumStore.sortBy === 'latest'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-stone-600 hover:text-stone-900 hover:bg-stone-50'
            ]"
          >
            最新
          </button>
          <button
              @click="handleSortChange('hot')"
              :class="[
              'px-4 py-2 rounded-md text-sm font-medium transition-all duration-200',
              forumStore.sortBy === 'hot'
                ? 'bg-amber-500 text-white shadow-sm'
                : 'text-stone-600 hover:text-stone-900 hover:bg-stone-50'
            ]"
          >
            热门
          </button>
        </div>
      </div>
    </div>

    <PostList
        @post-click="handlePostClick"
    />

    <!-- Floating Action Button -->
    <button
        @click="handleOpenCreateModal"
        class="fixed bottom-8 right-8 w-14 h-14 bg-amber-500 hover:bg-amber-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-200 flex items-center justify-center text-2xl z-40"
    >
      +
    </button>
  </div>
</template>
