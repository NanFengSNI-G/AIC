<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { marked } from 'marked'
import { useForumStore } from '@/stores/forum'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'
import BlogWorkspace from '@/components/blog/BlogWorkspace.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import type { ConversationSummary, ConversationMessage } from '@/types'

interface SSEData {
  type: string
  message: string
  data?: string
}

const authStore = useAuthStore()
const forumStore = useForumStore()

// ── 会话列表 ──
const conversations = ref<ConversationSummary[]>([])
const activeSessionId = ref<string | null>(null)
const conversationsLoading = ref(false)

// ── 消息历史 ──
const historyMessages = ref<ConversationMessage[]>([])

// ── 工作流 ──
const loading = ref(false)
const inputMessage = ref('')
const selectedSectionId = ref<number>(1)
const currentPhase = ref('')
const intent = ref('')
const topic = ref('')
const outlineTitle = ref('')
const outlineSections = ref<{ heading: string; keyPoints: string[] }[]>([])
const logMessages = ref<{ type: string; message: string; timestamp: number }[]>([])
const fullBlog = ref('')
const errorMessage = ref('')
const userDecision = ref<'PUBLISH' | 'DRAFT' | null>(null)

// ── 并行 Agent 状态 ──
const agentStatus = ref<{ content: 'idle' | 'running' | 'done'; code: 'idle' | 'running' | 'done'; image: 'idle' | 'running' | 'done' }>({
  content: 'idle',
  code: 'idle',
  image: 'idle'
})

// ── UI 状态 ──
const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const showDeleteConfirm = ref(false)
const conversationToDelete = ref<string | null>(null)

const chatContainer = ref<HTMLElement | null>(null)
let abortController: AbortController | null = null

const activeConversation = computed(() =>
  conversations.value.find(c => c.sessionId === activeSessionId.value)
)

// 只显示在聊天区的消息类型
const chatMessages = computed(() =>
  logMessages.value.filter(m =>
    ['user', 'assistant', 'system', 'error', 'search'].includes(m.type)
  )
)

onMounted(async () => {
  forumStore.fetchSections()
  await loadConversations()
})

onUnmounted(() => {
  if (abortController) abortController.abort()
})

// ═══════════════════════════════════════════════
// 会话管理
// ═══════════════════════════════════════════════
async function loadConversations() {
  conversationsLoading.value = true
  try {
    const { data } = await api.get<ConversationSummary[]>('/blog/conversations')
    conversations.value = data
  } catch {
    // 静默
  } finally {
    conversationsLoading.value = false
  }
}

async function createConversation() {
  try {
    const { data } = await api.post<ConversationSummary>('/blog/conversations')
    conversations.value.unshift(data)
    selectConversation(data.sessionId)
  } catch {
    // 静默
  }
}

async function selectConversation(sessionId: string) {
  activeSessionId.value = sessionId
  resetWorkflow()

  try {
    const { data } = await api.get<ConversationMessage[]>(`/blog/conversations/${sessionId}`)
    historyMessages.value = data
  } catch {
    historyMessages.value = []
  }

  logMessages.value = []
  for (const m of historyMessages.value) {
    logMessages.value.push({
      type: m.role === 'user' ? 'user' : 'assistant',
      message: m.content,
      timestamp: Date.now()
    })
  }
  autoScroll()
}

function promptDeleteConversation(sessionId: string) {
  conversationToDelete.value = sessionId
  showDeleteConfirm.value = true
}

async function handleConfirmDelete() {
  if (!conversationToDelete.value) return
  const sessionId = conversationToDelete.value
  try {
    await api.delete(`/blog/conversations/${sessionId}`)
    conversations.value = conversations.value.filter(c => c.sessionId !== sessionId)
    if (activeSessionId.value === sessionId) {
      activeSessionId.value = conversations.value[0]?.sessionId ?? null
      if (activeSessionId.value) {
        selectConversation(activeSessionId.value)
      } else {
        resetWorkflow()
      }
    }
  } catch {
    // 静默
  } finally {
    showDeleteConfirm.value = false
    conversationToDelete.value = null
  }
}

function handleCancelDelete() {
  showDeleteConfirm.value = false
  conversationToDelete.value = null
}

// ═══════════════════════════════════════════════
// 发送消息
// ═══════════════════════════════════════════════
async function handleSend() {
  const msg = inputMessage.value.trim()
  if (!msg || loading.value || !activeSessionId.value) return

  loading.value = true
  currentPhase.value = 'intent'
  errorMessage.value = ''
  userDecision.value = null
  outlineSections.value = []
  outlineTitle.value = ''
  topic.value = ''
  intent.value = ''
  agentStatus.value = { content: 'idle', code: 'idle', image: 'idle' }

  addLog('user', msg)
  inputMessage.value = ''

  // 自动展开工作台
  rightCollapsed.value = false

  abortController = new AbortController()

  try {
    const token = authStore.token
    const response = await fetch('/api/blog/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId: activeSessionId.value,
        message: msg,
        sectionId: selectedSectionId.value
      }),
      signal: abortController.signal
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          handleEvent(eventType || raw, raw)
          eventType = ''
        }
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      addLog('error', '连接失败: ' + e.message)
      errorMessage.value = e.message
    }
  } finally {
    loading.value = false
    abortController = null
    loadConversations()
  }
}

// ═══════════════════════════════════════════════
// SSE 事件处理
// ═══════════════════════════════════════════════
function handleEvent(_eventType: string, raw: string) {
  let data: SSEData
  try {
    data = JSON.parse(raw)
  } catch {
    return
  }

  switch (data.type) {
    case 'intent':
      intent.value = data.message
      if (data.data) {
        try {
          const parsed = JSON.parse(data.data)
          if (parsed.intent) intent.value = parsed.intent
        } catch {}
      }
      if (intent.value === 'write_blog') {
        currentPhase.value = 'outline'
      }
      break

    case 'outline':
      currentPhase.value = 'writing'
      agentStatus.value.content = 'running'
      agentStatus.value.code = 'idle'
      if (data.data) {
        try {
          const outline = JSON.parse(data.data)
          outlineTitle.value = outline.title || ''
          outlineSections.value = outline.sections || []
          topic.value = outline.title || ''
        } catch {
          // raw outline message
        }
      }
      addLog('assistant', data.message)
      break

    case 'content_progress':
      if (data.message.includes('完成') || data.message.includes('拼装')) {
        agentStatus.value.content = 'done'
        // 正文完成，转入增强阶段（代码和配图 Agent 并行运行）
        if (currentPhase.value === 'writing') {
          currentPhase.value = 'enhancing'
          agentStatus.value.code = 'running'
        }
      } else {
        agentStatus.value.content = 'running'
      }
      addLog('assistant', data.message)
      break

    case 'code_progress':
      if (data.message.includes('完成')) {
        agentStatus.value.code = 'done'
      } else {
        agentStatus.value.code = 'running'
      }
      addLog('assistant', data.message)
      break

    case 'image_progress':
      if (data.message.includes('完成')) {
        agentStatus.value.image = 'done'
      } else {
        agentStatus.value.image = 'running'
      }
      addLog('assistant', data.message)
      break

    case 'review':
      currentPhase.value = 'review'
      if (data.data) {
        try {
          const parsed = JSON.parse(data.data)
          fullBlog.value = parsed.blogContent || data.data
        } catch {
          fullBlog.value = data.data
        }
      }
      addLog('assistant', data.message)
      break

    case 'publish_result':
      addLog('system', data.message || '发布成功！')
      currentPhase.value = 'done'
      break

    case 'draft_result':
      addLog('system', data.message || '草稿已保存！')
      currentPhase.value = 'done'
      break

    case 'chat':
      addLog('assistant', data.data || data.message)
      break

    case 'search':
      addLog('assistant', '🔍 ' + (data.data || data.message))
      break

    case 'error':
      addLog('error', data.message)
      errorMessage.value = data.message
      break

    default:
      addLog('system', data.message || '')
  }
}

// ═══════════════════════════════════════════════
// 决策
// ═══════════════════════════════════════════════
async function handleDecision(decision: 'PUBLISH' | 'DRAFT') {
  if (!activeSessionId.value || loading.value) return

  userDecision.value = decision
  loading.value = true

  abortController = new AbortController()

  try {
    const token = authStore.token
    const response = await fetch('/api/blog/resume', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        sessionId: activeSessionId.value,
        decision: decision,
        sectionId: selectedSectionId.value
      }),
      signal: abortController.signal
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应流')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      let eventType = ''
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventType = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const raw = line.slice(5).trim()
          handleEvent(eventType || raw, raw)
          eventType = ''
        }
      }
    }
  } catch (e: any) {
    if (e.name !== 'AbortError') {
      addLog('error', '操作失败: ' + e.message)
    }
  } finally {
    loading.value = false
    abortController = null
  }
}

// ═══════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════
function resetWorkflow() {
  currentPhase.value = ''
  logMessages.value = []
  fullBlog.value = ''
  errorMessage.value = ''
  userDecision.value = null
  outlineSections.value = []
  outlineTitle.value = ''
  topic.value = ''
  intent.value = ''
  agentStatus.value = { content: 'idle', code: 'idle', image: 'idle' }
}

function addLog(type: string, message: string) {
  logMessages.value.push({ type, message, timestamp: Date.now() })
  autoScroll()
}

function autoScroll() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function bubbleClass(type: string): string {
  switch (type) {
    case 'user':
      return 'bg-amber-500 text-white self-end rounded-2xl rounded-br-md'
    case 'assistant':
    case 'search':
      return 'bg-stone-100 text-stone-800 self-start rounded-2xl rounded-bl-md'
    case 'error':
      return 'bg-red-50 text-red-700 self-start rounded-2xl border border-red-200'
    case 'system':
      return 'bg-transparent text-stone-400 self-center text-xs italic'
    default:
      return 'bg-stone-100 text-stone-700 self-start rounded-2xl rounded-bl-md'
  }
}
</script>

<template>
  <div class="flex h-[calc(100vh-96px)] rounded-2xl overflow-hidden border border-stone-200 shadow-card bg-white">
    <!-- ═══════════ 左侧：会话列表 ═══════════ -->
    <aside
      class="flex-shrink-0 bg-stone-50 border-r border-stone-200 flex flex-col transition-all duration-300 overflow-hidden"
      :class="leftCollapsed ? 'w-10' : 'w-60'"
    >
      <!-- 折叠态 -->
      <div v-if="leftCollapsed" class="flex flex-col items-center py-3 gap-3">
        <button
          @click="leftCollapsed = false"
          class="w-6 h-6 flex items-center justify-center rounded text-stone-400 hover:text-stone-600 text-xs"
          title="展开会话列表"
        >▶</button>
        <span class="text-[10px] text-stone-400" style="writing-mode: vertical-rl">会话</span>
      </div>

      <!-- 展开态 -->
      <template v-else>
        <div class="p-3 flex items-center gap-2">
          <button
            @click="createConversation"
            class="flex-1 px-3 py-2 bg-amber-500 hover:bg-amber-600 text-white rounded-xl text-sm font-medium transition-colors"
          >
            + 新对话
          </button>
          <button
            @click="leftCollapsed = true"
            class="w-6 h-6 flex items-center justify-center rounded text-stone-400 hover:text-stone-600 hover:bg-stone-200 text-xs flex-shrink-0 transition-colors"
            title="折叠"
          >◀</button>
        </div>

        <div class="flex-1 overflow-y-auto px-2 pb-2">
          <p v-if="conversationsLoading" class="text-xs text-stone-400 text-center mt-8">加载中...</p>
          <p v-else-if="conversations.length === 0" class="text-xs text-stone-400 text-center mt-8">
            暂无对话，点击上方按钮开始
          </p>

          <div
            v-for="conv in conversations"
            :key="conv.sessionId"
            @click="selectConversation(conv.sessionId)"
            class="group flex items-center gap-2 px-3 py-2.5 rounded-xl cursor-pointer transition-colors mb-0.5"
            :class="activeSessionId === conv.sessionId ? 'bg-white shadow-sm border border-stone-200' : 'hover:bg-stone-100'"
          >
            <span class="flex-1 truncate text-sm text-stone-700">{{ conv.title }}</span>
            <span class="text-xs text-stone-400 flex-shrink-0">{{ formatTime(conv.updatedAt) }}</span>
            <button
              @click.stop="promptDeleteConversation(conv.sessionId)"
              class="opacity-0 group-hover:opacity-100 flex-shrink-0 w-5 h-5 flex items-center justify-center rounded text-stone-400 hover:text-red-500 hover:bg-red-50 transition-all text-xs"
              title="删除"
            >×</button>
          </div>
        </div>
      </template>
    </aside>

    <!-- ═══════════ 中间：聊天区 ═══════════ -->
    <main class="flex-1 flex flex-col min-w-0 bg-white">
      <!-- 顶部栏 -->
      <header class="px-5 py-2.5 border-b border-stone-100 flex-shrink-0 flex items-center justify-between">
        <h2 class="text-sm font-medium text-stone-700 truncate">
          {{ activeConversation?.title || '博客助手' }}
        </h2>
        <span v-if="loading" class="flex items-center gap-1.5 text-xs text-amber-600">
          <span class="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
          处理中...
        </span>
      </header>

      <!-- 消息列表 -->
      <div
        ref="chatContainer"
        class="flex-1 overflow-y-auto px-5 py-4"
      >
        <!-- 空状态 -->
        <div v-if="!activeSessionId" class="flex flex-col items-center justify-center h-full text-stone-400">
          <p class="text-5xl mb-4">📝</p>
          <p class="text-base font-medium text-stone-500">创建或选择一个对话开始</p>
          <p class="text-sm mt-1">输入主题或关键词，AI 将为你生成技术博客</p>
        </div>

        <div v-else-if="logMessages.length === 0" class="flex flex-col items-center justify-center h-full text-stone-400">
          <p class="text-5xl mb-4">💬</p>
          <p class="text-base font-medium text-stone-500">发送第一条消息开始对话</p>
          <p class="text-sm mt-1">描述你想要的博客主题</p>
        </div>

        <!-- 消息气泡 -->
        <div v-else class="flex flex-col gap-3 max-w-3xl mx-auto">
          <div
            v-for="(msg, i) in chatMessages"
            :key="i"
            class="flex flex-col max-w-[80%]"
            :class="msg.type === 'user' ? 'self-end items-end' : 'self-start items-start'"
          >
            <!-- 系统消息：居中文字 -->
            <p
              v-if="msg.type === 'system' || msg.type === 'error'"
              :class="[
                'px-3 py-1.5 rounded-xl text-sm max-w-full',
                bubbleClass(msg.type)
              ]"
            >
              {{ msg.message }}
            </p>

            <!-- 对话消息：气泡 -->
            <template v-else>
              <div
                :class="[
                  'px-4 py-2.5 rounded-2xl text-sm leading-relaxed max-w-full',
                  bubbleClass(msg.type)
                ]"
              >
                <div
                  v-if="msg.type === 'assistant' || msg.type === 'search'"
                  class="prose prose-sm max-w-none prose-amber"
                  v-html="marked.parse(msg.message || '')"
                />
                <span v-else>{{ msg.message }}</span>
              </div>
              <span class="text-[10px] text-stone-400 mt-0.5 px-1">
                {{ formatTime(msg.timestamp) }}
              </span>
            </template>
          </div>

          <!-- 加载指示器 -->
          <div v-if="loading" class="self-start flex items-center gap-2 px-4 py-3">
            <span class="flex gap-1">
              <span class="w-2 h-2 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 0ms" />
              <span class="w-2 h-2 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 150ms" />
              <span class="w-2 h-2 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 300ms" />
            </span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="px-5 py-3 border-t border-stone-100">
        <div class="flex items-end gap-2 max-w-3xl mx-auto">
          <input
            v-model="inputMessage"
            @keydown.enter="handleSend"
            :disabled="loading || !activeSessionId"
            type="text"
            :placeholder="activeSessionId ? '输入消息描述你想要的博客...' : '请先选择或创建对话'"
            class="flex-1 px-4 py-2.5 border border-stone-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-amber-500 disabled:bg-stone-50 text-sm"
          />
          <button
            @click="handleSend"
            :disabled="!inputMessage.trim() || loading || !activeSessionId"
            class="px-5 py-2.5 bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white rounded-xl font-medium text-sm transition-colors flex-shrink-0"
          >
            {{ loading ? '···' : '发送' }}
          </button>
        </div>
      </div>
    </main>

    <!-- ═══════════ 右侧：博客工作台 ═══════════ -->
    <BlogWorkspace
      :current-phase="currentPhase"
      :intent="intent"
      :topic="topic"
      :outline-title="outlineTitle"
      :outline-sections="outlineSections"
      :full-blog="fullBlog"
      :sections="forumStore.sections"
      :collapsed="rightCollapsed"
      :agent-status="agentStatus"
      v-model:section-id="selectedSectionId"
      @toggle-collapse="rightCollapsed = !rightCollapsed"
      @decision="handleDecision"
    />

    <!-- 删除确认对话框 -->
    <ConfirmDialog
      v-model:show="showDeleteConfirm"
      title="删除对话"
      message="确定要删除此对话吗？此操作不可恢复。"
      confirm-text="删除"
      cancel-text="取消"
      type="danger"
      @confirm="handleConfirmDelete"
      @cancel="handleCancelDelete"
    />
  </div>
</template>
