export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  bio?: string
  age?: number
  createdAt: string
}

export interface AuthTokens {
  token: string
  refreshToken: string
}

export interface Post {
  id: number
  author: User
  title: string
  content: string
  images: string[]
  sectionId: number
  sectionName: string
  likeCount: number
  commentCount: number
  collectCount: number
  viewCount: number
  isLiked: boolean
  isCollected: boolean
  isSticky?: boolean
  status?: number
  createdAt: string
  updateTime?: string
}

export interface Section {
  id: number
  name: string
  description: string
  icon?: string
  postCount?: number
}

export interface Comment {
  id: number
  postId: number
  author: User
  content: string
  parentId?: number
  parentUsername?: string
  replyTo?: User
  likeCount: number
  isLiked: boolean
  status?: number
  createdAt: string
  replies?: Comment[]
}

export interface Friend {
  id: number
  userId: number
  friendId: number
  user: User
  status: 'accepted' | 'pending' | 'rejected' | 'deleted'
  message?: string
  createdAt: string
}

export interface FriendRequest {
  id: number
  userId: number
  friendId: number
  fromUser: User
  toUser: User
  status: 'pending' | 'accepted' | 'rejected'
  message?: string
  createdAt: string
}

export interface Conversation {
  friend: User
  lastMessage?: Message
  unreadCount: number
  updatedAt: string
}

export interface Message {
  id: number
  conversationId: number
  senderId: number
  toUserId: number
  content: string
  type: 'text' | 'image' | 'file' | 'voice'
  createdAt: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// Request DTOs
export interface SendCodeRequest {
  email: string
  type: 'register' | 'login' | 'reset-password'
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  code: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginByEmailRequest {
  email: string
  code: string
}

export interface ForgotPasswordRequest {
  email: string
  code: string
  newPassword: string
}

export interface UpdateProfileRequest {
  username?: string
  avatar?: string
  age?: number
  bio?: string
}

export interface CreatePostRequest {
  title: string
  content: string
  images?: string[]
  sectionId: number
}

export interface UpdatePostRequest {
  title?: string
  content?: string
  images?: string[]
}

export interface CreateCommentRequest {
  content: string
  parentId?: number
}

export interface SendFriendRequest {
  targetUserId: number
  message?: string
}

export interface HandleFriendRequest {
  id: number
  accept: boolean
}

export interface ConversationSummary {
  sessionId: string
  title: string
  createdAt: number
  updatedAt: number
}

export interface ConversationMessage {
  role: 'user' | 'assistant' | 'system'
  content: string
}
