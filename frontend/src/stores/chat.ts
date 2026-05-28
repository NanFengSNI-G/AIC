import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Friend, FriendRequest, Conversation, Message } from '@/types'
import api from '@/services/api'
import notification from '@/utils/notification'

interface BackendFriendResponse {
  id: number
  userId: number
  friendId: number
  friendUsername: string
  friendAvatar: string
  friendAge?: number
  friendBio?: string
  status: number
  message?: string
  createTime: string
}

interface BackendConversation {
  friendId: number
  friendUsername: string
  friendAvatar: string
  lastMsg?: string
  updateTime?: string
  unread?: number
}

interface BackendChatMessage {
  id: number
  fromUserId: number
  toUserId: number
  content: string
  msgType: number
  createTime: string
}

function mapBackendFriend(raw: BackendFriendResponse, currentUserId: number): Friend {
  const isInitiator = raw.userId === currentUserId

  return {
    id: raw.id,
    userId: isInitiator ? raw.userId : raw.friendId,
    friendId: isInitiator ? raw.friendId : raw.userId,
    user: {
      id: isInitiator ? raw.friendId : raw.userId,
      username: raw.friendUsername,
      nickname: raw.friendUsername,
      avatar: raw.friendAvatar || '',
      bio: raw.friendBio || '',
      createdAt: raw.createTime
    },
    status: raw.status === 0 ? 'pending' : raw.status === 1 ? 'accepted' : raw.status === 2 ? 'rejected' : 'deleted',
    createdAt: raw.createTime
  }
}

function mapBackendRequest(raw: BackendFriendResponse): FriendRequest {
  return {
    id: raw.id,
    userId: raw.userId,
    friendId: raw.friendId,
    fromUser: {
      id: raw.userId,
      username: raw.friendUsername,
      nickname: raw.friendUsername,
      avatar: raw.friendAvatar || '',
      bio: raw.friendBio || '',
      createdAt: raw.createTime
    },
    toUser: {
      id: raw.friendId,
      username: '',
      nickname: '',
      avatar: '',
      createdAt: ''
    },
    status: raw.status === 0 ? 'pending' : raw.status === 1 ? 'accepted' : 'rejected',
    message: raw.message,
    createdAt: raw.createTime
  }
}

function mapBackendConversation(raw: BackendConversation): Conversation {
  return {
    friend: {
      id: raw.friendId,
      username: raw.friendUsername,
      nickname: raw.friendUsername,
      avatar: raw.friendAvatar || '',
      createdAt: ''
    },
    lastMessage: raw.lastMsg ? {
      id: 0,
      conversationId: raw.friendId,
      senderId: 0,
      toUserId: raw.friendId,
      content: raw.lastMsg,
      type: 'text' as const,
      createdAt: raw.updateTime || ''
    } : undefined,
    unreadCount: raw.unread || 0,
    updatedAt: raw.updateTime || ''
  }
}

export const useChatStore = defineStore('chat', () => {
  const friends = ref<Friend[]>([])
  const requests = ref<FriendRequest[]>([])
  const conversations = ref<Conversation[]>([])
  const messages = ref<Message[]>([]) // 改用数组而非 Record
  const ws = ref<WebSocket | null>(null)
  const loading = ref(false)

  const onlineUserIds = ref<Set<number>>(new Set())

  let heartbeatTimer: number | null = null
  const HEARTBEAT_INTERVAL = 20000

  let onlineStatusTimer: number | null = null
  const ONLINE_STATUS_INTERVAL = 10000

  const totalUnread = computed(() => {
    return conversations.value.reduce((sum, c) => sum + c.unreadCount, 0)
  })

  const pendingRequestsCount = computed(() => {
    const currentUserId = getCurrentUserId()
    return requests.value.filter(r => r.status === 'pending' && r.toUser.id === currentUserId).length
  })

  // Get current user ID from localStorage (set by auth store)
  const getCurrentUserId = (): number => {
    const stored = localStorage.getItem('userId')
    if (stored) {
      return parseInt(stored, 10)
    }
    // Fallback: decode from token
    const token = localStorage.getItem('token')
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        return payload.userId || 0
      } catch {
        return 0
      }
    }
    return 0
  }

  const startHeartbeat = () => {
    stopHeartbeat()

    heartbeatTimer = window.setInterval(() => {
      if (ws.value?.readyState === WebSocket.OPEN) {
        ws.value.send('ping')
        console.log('[Heartbeat] ping sent')
      } else {
        console.warn('[Heartbeat] WebSocket not open, stopping heartbeat')
        stopHeartbeat()
      }
    }, HEARTBEAT_INTERVAL)

    console.log(`[Heartbeat] Started, interval: ${HEARTBEAT_INTERVAL}ms`)
  }

  const stopHeartbeat = () => {
    if (heartbeatTimer !== null) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
      console.log('[Heartbeat] Stopped')
    }
  }

  const startOnlineStatusPolling = () => {
    stopOnlineStatusPolling()

    fetchOnlineStatus()

    onlineStatusTimer = window.setInterval(() => {
      fetchOnlineStatus()
    }, ONLINE_STATUS_INTERVAL)

    console.log(`[Online Status] Polling started, interval: ${ONLINE_STATUS_INTERVAL}ms`)
  }

  const stopOnlineStatusPolling = () => {
    if (onlineStatusTimer !== null) {
      clearInterval(onlineStatusTimer)
      onlineStatusTimer = null
      console.log('[Online Status] Polling stopped')
    }
  }

  const connectWebSocket = (authToken: string) => {
    if (ws.value?.readyState === WebSocket.OPEN) return

    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    ws.value = new WebSocket(`${protocol}//${location.host}/ws/chat?token=${authToken}`)

    ws.value.onopen = () => {
      console.log('WebSocket connected')
      startHeartbeat()
      startOnlineStatusPolling()
    }

    ws.value.onmessage = (e) => {
      try {
        const data = e.data

        if (data === 'pong') {
          console.log('[Heartbeat] pong received')
          return
        }

        const msg: BackendChatMessage = JSON.parse(e.data)
        console.log('WebSocket message:', msg)

        const userId = getCurrentUserId()
        const conversationId = msg.fromUserId === userId ? msg.toUserId : msg.fromUserId

        // Add to messages array
        messages.value.push({
          id: msg.id,
          conversationId,
          senderId: msg.fromUserId,
          toUserId: msg.toUserId,
          content: msg.content || '',
          type: msg.msgType === 2 ? 'image' as const : 'text' as const,
          createdAt: msg.createTime || ''
        })
      } catch (err) {
        console.error('Failed to parse message:', err)
      }
    }

    ws.value.onclose = () => {
      console.log('WebSocket disconnected')
      stopHeartbeat()
      stopOnlineStatusPolling()
    }

    ws.value.onerror = (err) => {
      console.error('WebSocket error:', err)
      stopHeartbeat()
      stopOnlineStatusPolling()
    }
  }

  const disconnectWebSocket = () => {
    stopHeartbeat()
    stopOnlineStatusPolling()
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
  }

  const sendMessage = (friendId: number, content: string) => {
    const userId = getCurrentUserId()
    console.log('sendMessage:', { friendId, content, userId, wsState: ws.value?.readyState })

    if (ws.value?.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected')
      return
    }

    ws.value.send(JSON.stringify({ toUserId: friendId, content, msgType: 1 }))

    // Optimistically add message
    messages.value.push({
      id: Date.now(),
      conversationId: friendId,
      senderId: userId,
      toUserId: friendId,
      content,
      type: 'text' as const,
      createdAt: new Date().toISOString()
    })
  }

  const fetchOnlineStatus = async () => {
    try {
      const response = await api.get<{ data: number[] }>('/friend/online-status')
      onlineUserIds.value = new Set(response.data.data)
      console.log('[Online Status] Updated:', Array.from(onlineUserIds.value))
    } catch (error) {
      console.error('Failed to fetch online status:', error)
    }
  }

  const isUserOnline = (userId: number): boolean => {
    return onlineUserIds.value.has(userId)
  }

  const fetchFriends = async () => {
    loading.value = true
    try {
      const currentUserId = getCurrentUserId()
      const response = await api.post<{ data: { list: BackendFriendResponse[] } }>('/friend/list-friend', {
        page: 1,
        size: 100,
        sortField: null
      })
      friends.value = response.data.data.list.map(f => mapBackendFriend(f, currentUserId))
    } finally {
      loading.value = false
    }
  }

  const fetchRequests = async () => {
    const response = await api.post<{ data: { list: BackendFriendResponse[] } }>('/friend/list-friend-requests', {
      page: 1,
      size: 100,
      sortField: null
    })
    requests.value = response.data.data.list.map(mapBackendRequest)
  }

  const acceptRequest = async (requestId: number) => {
    await api.post('/friend/handle-friend', { id: requestId, accept: true })
    const req = requests.value.find(r => r.id === requestId)
    if (req) {
      requests.value = requests.value.filter(r => r.id !== requestId)
      const currentUserId = getCurrentUserId()
      friends.value.push({
        id: req.id,
        userId: currentUserId,
        friendId: req.fromUser.id,
        user: req.fromUser,
        status: 'accepted',
        createdAt: new Date().toISOString()
      })
    }
    notification.success('好友申请已接受', { title: '操作成功' })
  }

  const rejectRequest = async (requestId: number) => {
    await api.post('/friend/handle-friend', { id: requestId, accept: false })
    requests.value = requests.value.filter(r => r.id !== requestId)
    notification.info('好友申请已拒绝', { title: '操作成功' })
  }

  const fetchConversations = async () => {
    const response = await api.get<{ data: BackendConversation[] }>('/chat/conversations')
    conversations.value = response.data.data.map(mapBackendConversation)
  }

  const fetchMessages = async (friendId: number) => {
    const userId = getCurrentUserId()
    console.log('fetchMessages:', { friendId, userId })

    const response = await api.get<{ data: BackendChatMessage[] }>('/chat/history', {
      params: { friendId, page: 1, size: 50 }
    })

    console.log('Messages from backend:', response.data.data)

    // Clear previous messages and map new ones
    // Backend returns newest first (ORDER BY create_time DESC), reverse to show oldest first
    messages.value = response.data.data.map(m => {
      const msg: Message = {
        id: m.id,
        conversationId: friendId,
        senderId: m.fromUserId,
        toUserId: m.toUserId,
        content: m.content || '',
        type: m.msgType === 2 ? 'image' as const : 'text' as const,
        createdAt: m.createTime || ''
      }
      console.log('Mapped message:', msg, 'isOwn:', m.fromUserId === userId)
      return msg
    }).reverse()
  }

  const markAsRead = async (friendId: number) => {
    await api.put('/chat/read', null, { params: { friendId } })
    const conv = conversations.value.find(c => c.friend.id === friendId)
    if (conv) {
      conv.unreadCount = 0
    }
  }

  const getUnreadTotal = async (): Promise<number> => {
    const response = await api.get<{ data: number }>('/chat/unread/total')
    return response.data.data
  }

  const deleteFriend = async (relationId: number) => {
    await api.delete('/friend/delete-relation', { params: { relationId } })
    friends.value = friends.value.filter(f => f.id !== relationId)
    notification.success('好友已删除', { title: '删除成功' })
  }

  const sendFriendRequest = async (targetUserId: number, message?: string) => {
    await api.post('/friend/send-friend', { targetUserId, message })
    notification.success('好友申请已发送', { title: '发送成功' })
  }

  return {
    friends,
    requests,
    conversations,
    messages,
    totalUnread,
    pendingRequestsCount,
    loading,
    onlineUserIds,
    connectWebSocket,
    disconnectWebSocket,
    sendMessage,
    fetchFriends,
    fetchRequests,
    acceptRequest,
    rejectRequest,
    fetchConversations,
    fetchMessages,
    markAsRead,
    getUnreadTotal,
    deleteFriend,
    sendFriendRequest,
    fetchOnlineStatus,
    isUserOnline
  }
})
