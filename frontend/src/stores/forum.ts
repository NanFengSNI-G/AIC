import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Post, Section, Comment } from '@/types'
import api from '@/services/api'

interface BackendPost {
  id: number
  userId: number
  authorUsername: string
  authorAvatar: string
  sectionId: number
  sectionName: string
  title: string
  content: string
  images: string[]
  likeCount: number
  commentCount: number
  viewCount: number
  isLiked: boolean
  isFavorited: boolean
  createTime: string
  updateTime: string
}

interface BackendComment {
  id: number
  postId: number
  userId: number
  authorUsername: string
  authorAvatar: string
  parentId?: number
  content: string
  likeCount: number
  isLiked?: boolean
  createTime: string
  replies?: BackendComment[]
}

function mapBackendPost(raw: BackendPost): Post {
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

function mapBackendComment(c: BackendComment, postId: number): Comment {
  return {
    id: c.id,
    postId: c.postId || postId,
    author: {
      id: c.userId,
      username: c.authorUsername || '',
      nickname: c.authorUsername || '',
      avatar: c.authorAvatar || '',
      createdAt: c.createTime || ''
    },
    content: c.content,
    parentId: c.parentId,
    likeCount: c.likeCount || 0,
    isLiked: c.isLiked || false,
    createdAt: c.createTime || '',
    replies: c.replies?.map(r => mapBackendComment(r, postId)) || []
  }
}

export const useForumStore = defineStore('forum', () => {
  const posts = ref<Post[]>([])
  const currentPost = ref<Post | null>(null)
  const sections = ref<Section[]>([])
  const comments = ref<Comment[]>([])
  const loading = ref(false)
  const page = ref(1)
  const pageSize = ref(20)
  const hasMore = ref(true)
  const sortBy = ref<'latest' | 'hot' | 'relevance'>('latest')
  const searchKeyword = ref('')
  const isSearching = ref(false)
  const selectedSectionId = ref<number | null>(null)

  const fetchPosts = async (reset = false) => {
    if (loading.value || (!hasMore.value && !reset)) return

    loading.value = true
    if (reset) {
      page.value = 1
      posts.value = []
    }

    try {
      const response = await api.get<{ data: { list: BackendPost[]; total: number } }>('/forum/posts', {
        params: { sectionId: selectedSectionId.value, sortBy: sortBy.value, page: page.value, size: pageSize.value }
      })

      const mapped = response.data.data.list.map(mapBackendPost)

      if (reset) {
        posts.value = mapped
      } else {
        posts.value.push(...mapped)
      }

      hasMore.value = posts.value.length < response.data.data.total
      page.value++
    } finally {
      loading.value = false
    }
  }

  const fetchPostById = async (id: number) => {
    loading.value = true
    try {
      const response = await api.get<{ data: BackendPost }>(`/forum/post/${id}`)
      currentPost.value = mapBackendPost(response.data.data)
      return currentPost.value
    } finally {
      loading.value = false
    }
  }

  const fetchSections = async () => {
    const response = await api.get<{ data: Section[] }>('/forum/sections')
    sections.value = response.data.data
  }

  const createPost = async (data: { title: string; content: string; images: string[]; sectionId: number }) => {
    const requestData = {
      ...data,
      images: data.images && data.images.length > 0 ? data.images.join(',') : null
    }
    const response = await api.post<{ data: BackendPost }>('/forum/post', requestData)
    return mapBackendPost(response.data.data)
  }

  const likePost = async (postId: number) => {
    const response = await api.post<{ data: { liked: boolean } }>(`/forum/post/${postId}/like`)
    const liked = response.data.data.liked

    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.isLiked = liked
      post.likeCount += liked ? 1 : -1
    }
    if (currentPost.value?.id === postId) {
      currentPost.value.isLiked = liked
      currentPost.value.likeCount += liked ? 1 : -1
    }
  }

  const collectPost = async (postId: number) => {
    const response = await api.post<{ data: { favorited: boolean } }>(`/forum/post/${postId}/favorite`)
    const favorited = response.data.data.favorited

    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.isCollected = favorited
      post.collectCount += favorited ? 1 : -1
    }
    if (currentPost.value?.id === postId) {
      currentPost.value.isCollected = favorited
      currentPost.value.collectCount += favorited ? 1 : -1
    }
  }

  const fetchComments = async (postId: number) => {
    const response = await api.get<{ data: { list: BackendComment[] } }>(`/forum/post/${postId}/comments`, {
      params: { page: 1, size: 100 }
    })
    comments.value = response.data.data.list.map(c => mapBackendComment(c, postId))
  }

  const addComment = async (postId: number, content: string, parentId?: number) => {
    const response = await api.post<{ data: BackendComment }>(`/forum/post/${postId}/comment`, {
      content,
      parentId
    })
    const newComment = mapBackendComment(response.data.data, postId)
    
    if (parentId) {
      // 二级评论：找到对应的一级评论并添加到其 replies 中
      const parentComment = comments.value.find(c => c.id === parentId)
      if (parentComment) {
        if (!parentComment.replies) {
          parentComment.replies = []
        }
        parentComment.replies.push(newComment)
      }
    } else {
      // 一级评论：添加到列表开头
      comments.value.unshift(newComment)
    }
    
    return newComment
  }

  const updatePost = async (postId: number, data: { title?: string; content?: string; images?: string[] }) => {
    const requestData = {
      ...data,
      images: data.images && data.images.length > 0 ? data.images.join(',') : null
    }
    const response = await api.put<{ data: BackendPost }>(`/forum/post/${postId}`, requestData)
    return mapBackendPost(response.data.data)
  }

  const deletePost = async (postId: number) => {
    await api.delete(`/forum/post/${postId}`)
    posts.value = posts.value.filter(p => p.id !== postId)
    if (currentPost.value?.id === postId) {
      currentPost.value = null
    }
  }

  const deleteComment = async (commentId: number) => {
    await api.delete(`/forum/comment/${commentId}`)
    comments.value = comments.value.filter(c => c.id !== commentId)
  }

  const likeComment = async (commentId: number) => {
    const response = await api.post<{ data: { liked: boolean } }>(`/forum/comment/${commentId}/like`)
    const liked = response.data.data.liked

    // 递归查找并更新评论的点赞状态
    const updateCommentLike = (comments: Comment[], targetId: number): boolean => {
      for (const comment of comments) {
        if (comment.id === targetId) {
          comment.isLiked = liked
          comment.likeCount += liked ? 1 : -1
          return true
        }
        if (comment.replies && comment.replies.length > 0) {
          if (updateCommentLike(comment.replies, targetId)) {
            return true
          }
        }
      }
      return false
    }

    updateCommentLike(comments.value, commentId)
  }

  const getUserFavorites = async (page = 1, size = 20) => {
    const response = await api.get<{ data: { list: BackendPost[]; total: number } }>('/forum/user/favorites', {
      params: { page, size }
    })
    return response.data.data
  }

  const getUserPosts = async (userId: number, page = 1, size = 20) => {
    const response = await api.get<{ data: { list: BackendPost[]; total: number } }>('/forum/user/posts', {
      params: { userId, page, size }
    })
    return response.data.data
  }

  const setSortBy = (sort: 'latest' | 'hot' | 'relevance') => {
    if (sortBy.value === sort) return
    sortBy.value = sort

    // 如果在搜索模式，切换排序时重新执行搜索
    if (isSearching.value) {
      fetchSearchResults(true)
    } else {
      // 否则使用普通列表
      fetchPosts(true)
    }
  }

  const setSearchKeyword = (keyword: string) => {
    searchKeyword.value = keyword
    isSearching.value = keyword.trim().length > 0

    if (isSearching.value) {
      // 有搜索关键词时，默认使用相关性排序
      if (sortBy.value === 'latest' || sortBy.value === 'hot') {
        sortBy.value = 'relevance'
      }
      fetchSearchResults(true)
    } else {
      // 清空搜索时，恢复普通列表和默认排序
      sortBy.value = 'latest'
      fetchPosts(true)
    }
  }

  const setSelectedSection = (sectionId: number | null) => {
    selectedSectionId.value = sectionId
    // 切换版块时重置列表
    if (isSearching.value) {
      fetchSearchResults(true)
    } else {
      fetchPosts(true)
    }
  }

  const fetchSearchResults = async (reset = false) => {
    if (!searchKeyword.value.trim()) return
    if (loading.value || (!hasMore.value && !reset)) return

    loading.value = true
    if (reset) {
      page.value = 1
      posts.value = []
    }

    try {
      const response = await api.get<{ data: { list: BackendPost[]; total: number } }>('/forum/search', {
        params: {
          keyword: searchKeyword.value,
          sectionId: selectedSectionId.value,
          sortBy: sortBy.value,
          page: page.value,
          size: pageSize.value
        }
      })

      const mapped = response.data.data.list.map(mapBackendPost)

      if (reset) {
        posts.value = mapped
      } else {
        posts.value.push(...mapped)
      }

      hasMore.value = posts.value.length < response.data.data.total
      page.value++
    } finally {
      loading.value = false
    }
  }

  return {
    posts,
    currentPost,
    sections,
    comments,
    loading,
    hasMore,
    sortBy,
    searchKeyword,
    isSearching,
    selectedSectionId,
    fetchPosts,
    fetchSearchResults,
    setSearchKeyword,
    setSelectedSection,
    fetchPostById,
    fetchSections,
    createPost,
    updatePost,
    deletePost,
    likePost,
    collectPost,
    fetchComments,
    addComment,
    deleteComment,
    likeComment,
    getUserFavorites,
    getUserPosts,
    setSortBy
  }
})
