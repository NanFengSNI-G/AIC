<script setup lang="ts">
import {computed, onMounted, ref, watch} from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useForumStore } from '@/stores/forum'
import Avatar from '@/components/common/Avatar.vue'
import PostCard from '@/components/forum/PostCard.vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import type { Post } from '@/types'
import notification from '@/utils/notification'

const router = useRouter()
const authStore = useAuthStore()
const forumStore = useForumStore()
const user = computed(() => authStore.user)

// Tab state
const activeTab = ref<'posts' | 'favorites'>('posts')

// Profile edit state
const isEditing = ref(false)
const editForm = ref({
  username:  '',
  avatar:  '',
  bio: ''
})
const saving = ref(false)
const error = ref('')
const loading = ref(false)

// Posts state
const myPosts = ref<Post[]>([])
const favoritePosts = ref<Post[]>([])
const postsLoading = ref(false)
const favoritesLoading = ref(false)
const postsPage = ref(1)
const favoritesPage = ref(1)
const pageSize = 20
const hasMorePosts = ref(true)
const hasMoreFavorites = ref(true)



onMounted(async () => {
  if (!user.value && authStore.isLoggedIn) {
    loading.value = true
    try {
      await authStore.fetchCurrentUser()
    } finally {
      loading.value = false
    }
  }
  
  // After user is loaded, automatically load my posts if on posts tab
  if (user.value && activeTab.value === 'posts') {
    await loadMyPosts(true)
  }
})

// Watch for user changes and update editForm
watch(user, (newUser) => {
  if (newUser) {
    editForm.value.username = newUser.nickname
    editForm.value.avatar = newUser.avatar || ''
    editForm.value.bio = newUser.bio || ''
  }
}, { immediate: true })



const loadMyPosts = async (reset = false) => {
  if (!user.value || postsLoading.value || (!hasMorePosts.value && !reset)) return
  
  postsLoading.value = true
  if (reset) {
    postsPage.value = 1
    myPosts.value = []
  }
  
  try {
    const data = await forumStore.getUserPosts(user.value.id, postsPage.value, pageSize)
    const mappedPosts = data.list.map(mapPost)
    
    if (reset) {
      myPosts.value = mappedPosts
    } else {
      myPosts.value.push(...mappedPosts)
    }
    
    hasMorePosts.value = myPosts.value.length < data.total
    postsPage.value++
  } catch (err) {
    console.error('Failed to load my posts:', err)
  } finally {
    postsLoading.value = false
  }
}

const loadFavoritePosts = async (reset = false) => {
  if (favoritesLoading.value || (!hasMoreFavorites.value && !reset)) return
  
  favoritesLoading.value = true
  if (reset) {
    favoritesPage.value = 1
    favoritePosts.value = []
  }
  
  try {
    const data = await forumStore.getUserFavorites(favoritesPage.value, pageSize)
    const mappedPosts = data.list.map(mapPost)
    
    if (reset) {
      favoritePosts.value = mappedPosts
    } else {
      favoritePosts.value.push(...mappedPosts)
    }
    
    hasMoreFavorites.value = favoritePosts.value.length < data.total
    favoritesPage.value++
  } catch (err) {
    console.error('Failed to load favorite posts:', err)
  } finally {
    favoritesLoading.value = false
  }
}

// Helper function to map backend post to frontend format
function mapPost(raw: any): Post {
  return {
    id: raw.id,
    author: {
      id: raw.userId,
      username: raw.authorUsername,
      nickname: raw.authorUsername,
      avatar: raw.authorAvatar || '',
      createdAt: raw.createTime
    },
    title: raw.title,
    content: raw.content,
    images: raw.images || [],
    sectionId: raw.sectionId,
    sectionName: raw.sectionName,
    likeCount: raw.likeCount || 0,
    commentCount: raw.commentCount || 0,
    viewCount: raw.viewCount || 0,
    collectCount: 0,
    isLiked: raw.isLiked || false,
    isCollected: raw.isFavorited || false,
    createdAt: raw.createTime
  }
}

// Watch for tab changes to load data
watch(activeTab, async (newTab) => {
  if (newTab === 'posts' && myPosts.value.length === 0 && user.value) {
    await loadMyPosts(true)
  } else if (newTab === 'favorites' && favoritePosts.value.length === 0 && user.value) {
    await loadFavoritePosts(true)
  }
}, { immediate: true })

const handleSave = async () => {
  saving.value = true
  error.value = ''
  try {
    await authStore.updateProfile({
      username: editForm.value.username,
      avatar: editForm.value.avatar,
      bio: editForm.value.bio
    })
    if (user.value) {
      editForm.value.username = user.value.nickname
      editForm.value.avatar = user.value.avatar || ''
      editForm.value.bio = user.value.bio || ''
    }
    isEditing.value = false
    notification.success('个人资料已更新', { title: '保存成功' })
  } catch (e: any) {
    notification.error(e.message || '更新失败', { title: '保存失败' })
    error.value = e.message || '更新失败'
  } finally {
    saving.value = false
  }
}

const handleLogout = async () => {
  await authStore.logout()
  window.location.href = '/'
}

const handlePostClick = (postId: number) => {
  router.push(`/post/${postId}`)
}


</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <!-- Loading State -->
    <div v-if="loading" class="flex justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-500"></div>
    </div>

    <!-- Profile Content -->
    <div v-if="user" class="space-y-6">
      <!-- Profile Header Card -->
      <div class="bg-white rounded-2xl shadow-sm border border-stone-100 overflow-hidden">
        <!-- Cover Background -->
        <div class="h-32 bg-gradient-to-r from-amber-400 via-orange-400 to-pink-400"></div>
        
        <!-- Profile Info -->
        <div class="px-6 pb-6">
          <div class="relative flex items-end -mt-12 mb-4">
            <Avatar :src="user.avatar" :alt="user.nickname" size="lg" class="w-24 h-24 border-4 border-white shadow-lg" />
            <div class="ml-4 mb-1 flex-1">
              <h1 class="text-2xl font-bold text-stone-900">{{ user.nickname }}</h1>
              <p class="text-sm text-stone-500">@{{ user.username }}</p>
            </div>
            <button
              @click="isEditing = true"
              class="px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors text-sm font-medium"
            >
              编辑资料
            </button>
          </div>
          
          <p v-if="user.bio" class="text-stone-600">{{ user.bio }}</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="bg-white rounded-2xl shadow-sm border border-stone-100 overflow-hidden">
        <div class="flex border-b border-stone-100">
          <button
            @click="activeTab = 'posts'"
            :class="[
              'flex-1 py-4 text-sm font-medium transition-colors relative',
              activeTab === 'posts' ? 'text-amber-600' : 'text-stone-500 hover:text-stone-700'
            ]"
          >
            我的帖子
            <div v-if="activeTab === 'posts'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-amber-500"></div>
          </button>
          <button
            @click="activeTab = 'favorites'"
            :class="[
              'flex-1 py-4 text-sm font-medium transition-colors relative',
              activeTab === 'favorites' ? 'text-amber-600' : 'text-stone-500 hover:text-stone-700'
            ]"
          >
            我的收藏
            <div v-if="activeTab === 'favorites'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-amber-500"></div>
          </button>
        </div>

        <!-- Tab Content -->
        <div class="p-6">
          <!-- My Posts Tab -->
          <div v-if="activeTab === 'posts'" class="space-y-4">
            <div v-if="myPosts.length === 0 && !postsLoading" class="text-center py-12">
              <div class="text-stone-400 mb-2">
                <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <p class="text-stone-500">还没有发布过帖子</p>
              <router-link
                to="/forum/create"
                class="inline-block mt-4 px-6 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-lg transition-colors"
              >
                去发布
              </router-link>
            </div>

            <MasonryGrid v-else class="space-y-4">
              <PostCard
                v-for="post in myPosts"
                :key="post.id"
                :post="post"
                @click="handlePostClick"
              />
            </MasonryGrid>

            <!-- Loading More -->
            <div v-if="postsLoading" class="flex justify-center py-4">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-amber-500"></div>
            </div>

            <!-- Load More Button -->
            <div v-if="myPosts.length > 0 && hasMorePosts && !postsLoading" class="text-center">
              <button
                @click="loadMyPosts()"
                class="px-6 py-2 bg-stone-100 hover:bg-stone-200 text-stone-700 rounded-lg transition-colors"
              >
                加载更多
              </button>
            </div>
          </div>

          <!-- Favorites Tab -->
          <div v-if="activeTab === 'favorites'" class="space-y-4">
            <div v-if="favoritePosts.length === 0 && !favoritesLoading" class="text-center py-12">
              <div class="text-stone-400 mb-2">
                <svg class="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </div>
              <p class="text-stone-500">还没有收藏任何帖子</p>
            </div>

            <MasonryGrid v-else class="space-y-4">
              <PostCard
                v-for="post in favoritePosts"
                :key="post.id"
                :post="post"
                @click="handlePostClick"
              />
            </MasonryGrid>

            <!-- Loading More -->
            <div v-if="favoritesLoading" class="flex justify-center py-4">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-amber-500"></div>
            </div>

            <!-- Load More Button -->
            <div v-if="favoritePosts.length > 0 && hasMoreFavorites && !favoritesLoading" class="text-center">
              <button
                @click="loadFavoritePosts()"
                class="px-6 py-2 bg-stone-100 hover:bg-stone-200 text-stone-700 rounded-lg transition-colors"
              >
                加载更多
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Edit Profile Modal -->
    <div v-if="isEditing" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" @click.self="isEditing = false">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
        <!-- Modal Header -->
        <div class="px-6 py-4 border-b border-stone-100 flex items-center justify-between">
          <h3 class="text-lg font-semibold text-stone-900">编辑个人资料</h3>
          <button @click="isEditing = false" class="text-stone-400 hover:text-stone-600 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <!-- Modal Body -->
        <div class="px-6 py-4 space-y-4">
          <div v-if="error" class="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {{ error }}
          </div>

          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">用户名</label>
            <input
              v-model="editForm.username"
              type="text"
              class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent outline-none transition-all"
              placeholder="请输入用户名"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">头像URL</label>
            <input
              v-model="editForm.avatar"
              type="text"
              placeholder="https://example.com/avatar.jpg"
              class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent outline-none transition-all"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-stone-700 mb-2">个人简介</label>
            <textarea
              v-model="editForm.bio"
              rows="4"
              placeholder="介绍一下自己吧..."
              class="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent outline-none transition-all resize-none"
            />
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="px-6 py-4 bg-stone-50 border-t border-stone-100 flex gap-3">
          <button
            @click="handleSave"
            :disabled="saving"
            class="flex-1 py-2.5 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg transition-colors font-medium"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
          <button
            @click="isEditing = false"
            class="flex-1 py-2.5 bg-white border border-stone-200 hover:bg-stone-50 text-stone-700 rounded-lg transition-colors font-medium"
          >
            取消
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
