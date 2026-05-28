<script setup lang="ts">
import { ref, onUnmounted, nextTick } from 'vue'
import { marked } from 'marked'
import { useAuthStore } from '@/stores/auth'
import api from '@/services/api'
import notification from '@/utils/notification'
import LoadingOverlay from '@/components/common/LoadingOverlay.vue'
import InterviewHistoryPanel from '@/components/interview/InterviewHistoryPanel.vue'

// ============ Types ============
interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

// ============ Markdown Renderer ============
const renderMarkdown = (text: string) => marked.parse(text) as string

// ============ Auth ============
const authStore = useAuthStore()

// ============ Setup Phase ============
const resumeFile = ref<File | null>(null)
const resumeFileName = ref('')
const jdText = ref('')
const uploadLoading = ref(false)
const isSetupComplete = ref(false)

// File input ref
const fileInputRef = ref<HTMLInputElement | null>(null)

// ============ History Panel ============
const showHistoryPanel = ref(false)

// ============ Interview Phase ============
const messages = ref<ChatMessage[]>([])
const lastAssistantMessage = ref('')  // 记录上一条 AI 消息，作为当前问题的 question
const userInput = ref('')
const isStreaming = ref(false)
const isSending = ref(false) // 追踪发送状态，等待后端响应
const currentStreamingContent = ref('')
const isInterviewActive = ref(false)
const isEnded = ref(false)
const isWaitingFirstMessage = ref(false)
const remainingTime = ref(30 * 60) // 30 minutes in seconds
let timerInterval: ReturnType<typeof setInterval> | null = null

// WebSocket
let ws: WebSocket | null = null
let messageIdCounter = 0

// Audio playback
let audioElement: HTMLAudioElement | null = null
let audioQueue: string[] = []  // 音频数据队列（base64 PCM s16 24000Hz）
let pcmBuffer: Uint8Array[] = []
let pcmBytes = 0
const PCM_CHUNK_BYTES = 24000 * 2 * 12  // 12000ms PCM
let isPlayingAudio = false

// ============ Voice Input (Web Audio API + Opus) ============
const isRecording = ref(false)
let audioContext: AudioContext | null = null
let mediaStream: MediaStream | null = null
let audioProcessor: ScriptProcessorNode | null = null
let opusEncoder: AudioEncoder | null = null
let opusChunks: Uint8Array[] = []  // Opus 编码后的帧列表

// ============ Computed ============
const acceptedFileTypes = ['.pdf', '.doc', '.docx']

// ============ File Handling ============
const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files.length > 0) {
    const file = input.files[0]
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()

    if (!acceptedFileTypes.includes(ext)) {
      notification.error('只支持 PDF、DOC、DOCX 格式的简历文件', { title: '文件格式错误' })
      return
    }

    resumeFile.value = file
    resumeFileName.value = file.name
  }
}

const triggerFileSelect = () => {
  fileInputRef.value?.click()
}

const clearFile = () => {
  resumeFile.value = null
  resumeFileName.value = ''
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

// ============ Voice Input (Opus) ============
const sendOpusChunks = () => {
  if (!ws || ws.readyState !== WebSocket.OPEN || opusChunks.length === 0) return
  let totalLen = 0
  for (const c of opusChunks) totalLen += c.length
  const merged = new Uint8Array(totalLen)
  let offset = 0
  for (const c of opusChunks) {
    merged.set(c, offset)
    offset += c.length
  }
  ws.send(merged.buffer)
  opusChunks = []
}

const stopRecording = () => {
  if (!isRecording.value) return

  if (audioProcessor) { audioProcessor.disconnect(); audioProcessor = null }
  if (audioContext) { audioContext.close(); audioContext = null }
  if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }

  if (opusEncoder && opusEncoder.state === 'configured') {
    try { opusEncoder.flush() } catch (_) {}
  }
  // 等 encoder 异步冲刷完再发送
  setTimeout(() => {
    sendOpusChunks()
    if (opusEncoder) { opusEncoder.close(); opusEncoder = null }
  }, 100)

  isRecording.value = false
  isSending.value = true
}

const startRecording = async () => {
  if (isRecording.value) return

  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
    })

    audioContext = new AudioContext({ sampleRate: 16000 })
    audioProcessor = audioContext.createScriptProcessor(4096, 1, 1)
    opusChunks = []

    opusEncoder = new AudioEncoder({
      output: (chunk) => {
        const data = new Uint8Array(chunk.byteLength)
        chunk.copyTo(data)
        opusChunks.push(data)
      },
      error: (e) => console.error('Opus 编码错误:', e)
    })
    opusEncoder.configure({ codec: 'opus', sampleRate: 16000, numberOfChannels: 1, bitrate: 24000 })

    const source = audioContext.createMediaStreamSource(mediaStream)
    source.connect(audioProcessor)
    audioProcessor.connect(audioContext.destination)

    audioProcessor.onaudioprocess = (event) => {
      if (!isRecording.value) return

      const floatData = event.inputBuffer.getChannelData(0)
      const pcm = float32ToPcm(floatData)

      try {
        const audioData = new AudioData({
          format: 's16',
          sampleRate: 16000,
          numberOfChannels: 1,
          numberOfFrames: pcm.length,
          data: new Uint8Array(pcm.buffer)
        })
        opusEncoder!.encode(audioData)
        audioData.close()
      } catch (_) {}

      if (opusChunks.length >= 5) sendOpusChunks()
    }

    isRecording.value = true
  } catch (error) {
    console.error('无法访问麦克风:', error)
    notification.error('无法访问麦克风，请检查权限设置', { title: '语音输入错误' })
  }
}

// Float32Array (-1.0 ~ 1.0) 转 Int16Array PCM
const float32ToPcm = (float32Array: Float32Array): Int16Array => {
  const pcm = new Int16Array(float32Array.length)
  for (let i = 0; i < float32Array.length; i++) {
    const s = Math.max(-1, Math.min(1, float32Array[i]))
    pcm[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
  }
  return pcm
}

// ============ Upload & Start ============
const handleUploadAndStart = async () => {
  if (!resumeFile.value) {
    notification.error('请上传简历文件', { title: '提示' })
    return
  }

  if (!jdText.value.trim()) {
    notification.error('请输入期望岗位 JD', { title: '提示' })
    return
  }

  uploadLoading.value = true

  try {
    // Upload resume and JD
    const formData = new FormData()
    formData.append('file', resumeFile.value)
    formData.append('jdText', jdText.value)

    await api.post('/interview/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    // Connect WebSocket and start interview
    connectWebSocket()

    // Wait for WebSocket connection to establish
    await new Promise<void>((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('WebSocket 连接超时'))
      }, 5000)

      if (ws && ws.readyState === WebSocket.OPEN) {
        clearTimeout(timeout)
        resolve()
      } else if (ws) {
        ws.onopen = () => {
          clearTimeout(timeout)
          resolve()
        }
        ws.onerror = () => {
          clearTimeout(timeout)
          reject(new Error('WebSocket 连接失败'))
        }
      }
    })

    // Send START message via WebSocket to trigger opening statement
    ws!.send(JSON.stringify({ type: 'START' }))

    // Start 30-minute countdown timer
    startTimer()

    isSetupComplete.value = true
    isInterviewActive.value = true
    isWaitingFirstMessage.value = true

  } catch (e: any) {
    notification.error(e.message || '上传失败', { title: '上传失败' })
  } finally {
    uploadLoading.value = false
  }
}

// ============ WebSocket ============
const getWebSocketUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname
  const port = 8080
  const token = authStore.token
  return `${protocol}//${host}:${port}/ws/interview?token=${token}`
}

const connectWebSocket = () => {
  ws = new WebSocket(getWebSocketUrl())

  ws.onopen = () => {
    console.log('Interview WebSocket connected')
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      handleWebSocketMessage(data)
    } catch (e) {
      console.error('Failed to parse WebSocket message:', e)
    }
  }

  ws.onclose = () => {
    console.log('Interview WebSocket closed')
    ws = null
    // 停止录音
    if (isRecording.value) {
      stopRecording()
    }
  }

  ws.onerror = (error) => {
    console.error('Interview WebSocket error:', error)
    notification.error('连接中断，正在重连...', { title: '错误' })
  }
}

const handleWebSocketMessage = (data: any) => {
  switch (data.type) {
    case 'STREAM':
      isStreaming.value = true
      isSending.value = false // 收到响应，停止发送状态
      if (isWaitingFirstMessage.value) isWaitingFirstMessage.value = false
      currentStreamingContent.value += data.content || ''
      break

    case 'STREAM_END':
      if (currentStreamingContent.value) {
        lastAssistantMessage.value = currentStreamingContent.value  // 保存 AI 问题
        const msg: ChatMessage = {
          id: `msg-${++messageIdCounter}`,
          role: 'assistant',
          content: currentStreamingContent.value,
          timestamp: new Date()
        }
        messages.value.push(msg)
        currentStreamingContent.value = ''
      }
      isStreaming.value = false
      scrollToBottom()
      break

    case 'AUDIO_STREAM':
      // TTS PCM chunk → 解码后累积，攒够指定时长就推一个播放块
      if (data.audioBase64 && typeof data.audioBase64 === 'string' && data.audioBase64.length > 0) {
        try {
          const chars = atob(data.audioBase64)
          const bytes = new Uint8Array(chars.length)
          for (let i = 0; i < chars.length; i++) bytes[i] = chars.charCodeAt(i)
          pcmBuffer.push(bytes)
          pcmBytes += bytes.length
          console.log(`[AUDIO_STREAM] 收到音频块, 大小: ${bytes.length} bytes, 当前缓冲: ${pcmBytes} bytes, 阈值: ${PCM_CHUNK_BYTES}`)
          if (pcmBytes >= PCM_CHUNK_BYTES) {
            console.log('[AUDIO_STREAM] 达到阈值，触发 flush')
            flushPcm()
          }
        } catch (_) { /* 跳过无效数据 */ }
      }
      break

    case 'AUDIO_END':
      console.log(`[AUDIO_END] 收到结束信号, 剩余缓冲: ${pcmBytes} bytes, 队列长度: ${audioQueue.length}, 正在播放: ${isPlayingAudio}`)
      // 立即 flush，后端已确保所有音频都已发送完毕
      flushPcm()
      console.log(`[AUDIO_END] flush 完成, 队列长度: ${audioQueue.length}`)
      break

    case 'AGENT_END':
      isInterviewActive.value = false
      isEnded.value = true
      notification.info('面试已结束', { title: '面试结束' })
      break

    case 'END':
      isInterviewActive.value = false
      isEnded.value = true
      notification.info('面试已结束', { title: '面试结束' })
      break

    case 'ERROR':
      notification.error(data.message || '发生错误', { title: '错误' })
      isStreaming.value = false
      break
  }
}

const sendMessage = () => {
  if (!userInput.value.trim() || !ws || ws.readyState !== WebSocket.OPEN) return

  const content = userInput.value.trim()
  const question = lastAssistantMessage.value  // 获取上一条 AI 问题

  // Add user message immediately
  const msg: ChatMessage = {
    id: `msg-${++messageIdCounter}`,
    role: 'user',
    content,
    timestamp: new Date()
  }
  messages.value.push(msg)

  // Mark as sending (waiting for response)
  isSending.value = true

  // Send via WebSocket with question field
  ws.send(JSON.stringify({
    type: 'MESSAGE',
    content,
    question
  }))

  userInput.value = ''
  scrollToBottom()
}

const handleEndInterview = () => {
  stopTimer()
  if (isRecording.value) {
    stopRecording()
  }
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'END' }))
  }
  isInterviewActive.value = false
  isEnded.value = true
}

// ============ Timer ============
const startTimer = () => {
  remainingTime.value = 30 * 60
  timerInterval = setInterval(() => {
    remainingTime.value--
    if (remainingTime.value <= 0) {
      handleEndInterview()
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

// ============ Utilities ============
const scrollToBottom = () => {
  nextTick(() => {
    const container = document.getElementById('chat-container')
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

// ============ Audio Playback (Opus → WAV) ============
const pcmToWav = (pcmData: Uint8Array): Blob => {
  const dataSize = pcmData.length
  const wavBuf = new ArrayBuffer(44 + dataSize)
  const view = new DataView(wavBuf)
  const writeStr = (off: number, s: string) => {
    for (let i = 0; i < s.length; i++) view.setUint8(off + i, s.charCodeAt(i))
  }
  writeStr(0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true)
  writeStr(8, 'WAVE')
  writeStr(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, 1, true)
  view.setUint32(24, 24000, true)
  view.setUint32(28, 24000 * 2, true)
  view.setUint16(32, 2, true)
  view.setUint16(34, 16, true)
  writeStr(36, 'data')
  view.setUint32(40, dataSize, true)
  new Uint8Array(wavBuf, 44).set(new Uint8Array(pcmData))
  return new Blob([wavBuf], { type: 'audio/wav' })
}

const flushPcm = () => {
  if (pcmBuffer.length === 0) return
  const merged = new Uint8Array(pcmBytes)
  let offset = 0
  for (const c of pcmBuffer) {
    merged.set(c, offset)
    offset += c.length
  }
  pcmBuffer = []
  pcmBytes = 0
  let binary = ''
  for (let i = 0; i < merged.length; i++) binary += String.fromCharCode(merged[i])
  audioQueue.push(btoa(binary))
  playNextAudio()
}

const playNextAudio = () => {
  if (isPlayingAudio || audioQueue.length === 0) return

  const base64Audio = audioQueue.shift()
  if (!base64Audio || typeof base64Audio !== 'string' || base64Audio.trim().length === 0) {
    isPlayingAudio = false
    playNextAudio()
    return
  }

  isPlayingAudio = true

  try {
    const byteCharacters = atob(base64Audio)
    const byteNumbers = new Uint8Array(byteCharacters.length)
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i)
    }

    const wavBlob = pcmToWav(byteNumbers)
    const audioUrl = URL.createObjectURL(wavBlob)
    audioElement = new Audio(audioUrl)
    audioElement.src = audioUrl

    audioElement.onended = () => {
      URL.revokeObjectURL(audioUrl)
      isPlayingAudio = false
      playNextAudio()
    }
    audioElement.onerror = () => {
      URL.revokeObjectURL(audioUrl)
      isPlayingAudio = false
      playNextAudio()
    }
    audioElement.play().catch(() => {
      URL.revokeObjectURL(audioUrl)
      isPlayingAudio = false
      playNextAudio()
    })
  } catch (e) {
    console.error('音频播放失败，跳过:', e)
    isPlayingAudio = false
    playNextAudio()
  }
}

const resetInterview = () => {
  stopTimer()
  if (isRecording.value) {
    stopRecording()
  }
  isSetupComplete.value = false
  isInterviewActive.value = false
  isEnded.value = false
  messages.value = []
  userInput.value = ''
  currentStreamingContent.value = ''
  isStreaming.value = false
  resumeFile.value = null
  resumeFileName.value = ''
  jdText.value = ''

  // 清理音频状态
  audioQueue = []
  pcmBuffer = []
  pcmBytes = 0
  if (audioElement) {
    audioElement.pause()
    audioElement = null
  }
  isPlayingAudio = false

  if (ws) {
    ws.close()
    ws = null
  }
}

// ============ Lifecycle ============
onUnmounted(() => {
  stopTimer()
  if (isRecording.value) {
    stopRecording()
  }
  if (ws) {
    ws.close()
    ws = null
  }
})
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <!-- Header -->
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-stone-900">AI 模拟面试</h1>
        <p class="text-stone-500 mt-1">上传简历，输入 JD，开启智能面试体验</p>
      </div>
      <button
        @click="showHistoryPanel = true"
        class="px-4 py-2 bg-white border border-stone-200 text-stone-600 rounded-xl hover:bg-stone-50 hover:text-stone-900 transition-colors flex items-center gap-2"
      >
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        历史记录
      </button>
    </div>

    <!-- Setup Phase -->
    <div v-if="!isSetupComplete" class="space-y-6">
      <!-- Resume Upload -->
      <div class="bg-white rounded-2xl p-6 shadow-sm border border-stone-100">
        <h2 class="text-lg font-semibold text-stone-900 mb-4">上传简历</h2>

        <input
          ref="fileInputRef"
          type="file"
          :accept="acceptedFileTypes.join(',')"
          class="hidden"
          @change="handleFileSelect"
        />

        <div
          v-if="!resumeFileName"
          @click="triggerFileSelect"
          class="border-2 border-dashed border-stone-200 rounded-xl p-8 text-center cursor-pointer hover:border-amber-400 hover:bg-amber-50/50 transition-colors"
        >
          <div class="text-4xl mb-3">📄</div>
          <div class="text-stone-600">点击上传简历</div>
          <div class="text-stone-400 text-sm mt-1">支持 PDF、DOC、DOCX 格式</div>
        </div>

        <div
          v-else
          class="flex items-center justify-between p-4 bg-stone-50 rounded-xl"
        >
          <div class="flex items-center gap-3">
            <div class="text-2xl">📄</div>
            <div>
              <div class="font-medium text-stone-900">{{ resumeFileName }}</div>
              <div class="text-sm text-stone-500">简历已选择</div>
            </div>
          </div>
          <button
            @click="clearFile"
            class="p-2 text-stone-400 hover:text-red-500 transition-colors"
          >
            ✕
          </button>
        </div>
      </div>

      <!-- JD Input -->
      <div class="bg-white rounded-2xl p-6 shadow-sm border border-stone-100">
        <h2 class="text-lg font-semibold text-stone-900 mb-4">期望岗位 JD</h2>

        <textarea
          v-model="jdText"
          rows="6"
          placeholder="请输入期望岗位的职位描述（JD），例如：&#10;• 岗位：Java 后端开发&#10;• 要求：熟悉 Spring Boot、MySQL、Redis&#10;• 经验：3 年以上工作经验..."
          class="w-full px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent resize-none transition-all"
        ></textarea>
      </div>

      <!-- Start Button -->
      <button
        @click="handleUploadAndStart"
        :disabled="uploadLoading"
        class="w-full py-4 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-colors shadow-sm text-lg"
      >
        {{ uploadLoading ? '准备中...' : '开始面试' }}
      </button>
    </div>

    <!-- Interview Phase -->
    <div v-else class="space-y-4">
      <!-- Loading Overlay -->
      <LoadingOverlay v-if="isWaitingFirstMessage" message="面试官正在查看你的简历..." />
      <!-- Status Bar -->
      <div class="bg-white rounded-2xl p-4 shadow-sm border border-stone-100 flex items-center justify-between">
        <div class="flex items-center gap-2">
          <div
            :class="isEnded ? 'bg-stone-400' : 'bg-green-500'"
            class="w-2 h-2 rounded-full animate-pulse"
          ></div>
          <span class="text-sm text-stone-500">{{ isEnded ? '已结束' : '进行中' }}</span>
        </div>
        <div v-if="!isEnded" class="text-sm font-mono" :class="remainingTime < 60 ? 'text-red-500' : 'text-stone-500'">
          {{ Math.floor(remainingTime / 60) }}:{{ String(remainingTime % 60).padStart(2, '0') }}
        </div>
      </div>

      <!-- Chat Messages -->
      <div
        id="chat-container"
        class="bg-white rounded-2xl shadow-sm border border-stone-100 p-4 h-96 overflow-y-auto space-y-4"
      >
        <!-- Empty State -->
        <div v-if="messages.length === 0 && !isStreaming" class="h-full flex items-center justify-center">
          <div class="text-center text-stone-400">
            <div class="text-4xl mb-2">🤖</div>
          </div>
        </div>

        <!-- Messages -->
        <template v-for="msg in messages" :key="msg.id">
          <!-- User Message -->
          <div v-if="msg.role === 'user'" class="flex justify-end">
            <div class="max-w-[80%] px-4 py-3 bg-amber-500 text-white rounded-2xl rounded-br-md">
              {{ msg.content }}
            </div>
          </div>

          <!-- Assistant Message -->
          <div v-else class="flex justify-start">
            <div class="max-w-[80%] px-4 py-3 bg-stone-100 text-stone-900 rounded-2xl rounded-bl-md prose prose-sm" v-html="renderMarkdown(msg.content)">
            </div>
          </div>
        </template>

        <!-- Streaming Indicator -->
        <div v-if="isStreaming" class="flex justify-start">
          <div class="max-w-[80%] px-4 py-3 bg-stone-100 text-stone-900 rounded-2xl rounded-bl-md prose prose-sm" v-html="renderMarkdown(currentStreamingContent)"></div>
        </div>
      </div>

      <!-- Input Area -->
      <div v-if="!isEnded" class="bg-white rounded-2xl p-4 shadow-sm border border-stone-100">
        <div class="flex gap-3">
          <input
            v-model="userInput"
            type="text"
            placeholder="输入你的回答，或点击麦克风语音输入..."
            class="flex-1 px-4 py-3 bg-stone-50 border border-stone-200 rounded-xl text-stone-900 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition-all"
            @keyup.enter="sendMessage"
          />

          <!-- 语音输入按钮 -->
          <button
            @click="isRecording ? stopRecording() : startRecording()"
            :disabled="isSending || isStreaming"
            :class="isRecording ? 'bg-red-500 hover:bg-red-600' : 'bg-stone-100 hover:bg-stone-200'"
            class="px-4 py-3 text-stone-600 rounded-xl transition-colors flex items-center justify-center min-w-14"
            :title="isRecording ? '停止录音' : '开始语音输入'"
          >
            <svg v-if="!isRecording" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
            </svg>
            <svg v-else class="w-5 h-5 animate-pulse text-white" fill="currentColor" viewBox="0 0 24 24">
              <rect x="6" y="6" width="12" height="12" rx="2" />
            </svg>
          </button>

          <button
            @click="sendMessage"
            :disabled="!userInput.trim() || isStreaming || isSending"
            class="px-6 py-3 bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-colors flex items-center justify-center min-w-20"
          >
            <span v-if="isSending" class="inline-block w-5 h-5">
                <svg viewBox="0 0 24 24" class="animate-spin">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-dasharray="31.4 62.8" />
                </svg>
              </span>
              <span v-else>发送</span>
          </button>
        </div>
      </div>

      <!-- End Interview Button -->
      <div class="flex justify-center">
        <button
          v-if="!isEnded"
          @click="handleEndInterview"
          class="px-6 py-2 text-stone-500 hover:text-red-500 font-medium transition-colors"
        >
          结束面试
        </button>
        <button
          v-else
          @click="resetInterview"
          class="px-6 py-2 bg-stone-100 hover:bg-stone-200 text-stone-700 font-medium rounded-xl transition-colors"
        >
          重新开始
        </button>
      </div>
    </div>

    <!-- History Panel -->
    <InterviewHistoryPanel
      :visible="showHistoryPanel"
      @close="showHistoryPanel = false"
    />
  </div>
</template>
