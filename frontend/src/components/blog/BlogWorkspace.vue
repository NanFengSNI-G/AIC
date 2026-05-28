<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

interface OutlineSection {
  heading: string
  keyPoints: string[]
}

interface AgentStatus {
  content: 'idle' | 'running' | 'done'
  code: 'idle' | 'running' | 'done'
  image: 'idle' | 'running' | 'done'
}

const props = defineProps<{
  currentPhase: string
  intent: string
  topic: string
  outlineTitle: string
  outlineSections: OutlineSection[]
  fullBlog: string
  sections: { id: number; name: string }[]
  collapsed: boolean
  agentStatus: AgentStatus
}>()

defineEmits<{
  'toggle-collapse': []
  'update:sectionId': [id: number]
  decision: [type: 'PUBLISH' | 'DRAFT']
}>()

const phases = [
  { key: 'intent', label: '意图', icon: '🎯' },
  { key: 'outline', label: '大纲', icon: '📋' },
  { key: 'writing', label: '正文', icon: '✍️' },
  { key: 'enhancing', label: '增强', icon: '⚡' },
  { key: 'review', label: '审核', icon: '🔍' },
  { key: 'done', label: '完成', icon: '✅' },
]

const currentPhaseIndex = computed(() => {
  const idx = phases.findIndex(p => p.key === props.currentPhase)
  return idx >= 0 ? idx : -1
})

const sectionId = defineModel<number>('sectionId', { default: 1 })
</script>

<template>
  <aside
    class="relative flex flex-col bg-white border-l border-stone-200 transition-all duration-300 overflow-hidden"
    :class="collapsed ? 'w-10' : 'w-80'"
  >
    <template v-if="!collapsed">
      <!-- 头部 -->
      <div class="px-4 py-3 border-b border-stone-100 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-stone-700">博客工作台</h3>
        <button
          @click="$emit('toggle-collapse')"
          class="w-6 h-6 flex items-center justify-center rounded text-stone-400 hover:text-stone-600 hover:bg-stone-100 text-xs transition-colors"
        >
          ▶
        </button>
      </div>

      <!-- 步骤指示器 -->
      <div class="px-4 py-3 border-b border-stone-50">
        <div class="flex items-center justify-between">
          <template v-for="(p, i) in phases" :key="p.key">
            <div class="flex flex-col items-center gap-1">
              <div
                class="w-8 h-8 rounded-full flex items-center justify-center text-sm transition-all"
                :class="
                  i < currentPhaseIndex ? 'bg-green-100 text-green-600' :
                  i === currentPhaseIndex ? 'bg-amber-100 text-amber-600 ring-2 ring-amber-300' :
                  'bg-stone-100 text-stone-400'
                "
              >
                {{ p.icon }}
              </div>
              <span
                class="text-xs"
                :class="
                  i <= currentPhaseIndex ? 'text-stone-700 font-medium' : 'text-stone-400'
                "
              >{{ p.label }}</span>
            </div>
            <div
              v-if="i < phases.length - 1"
              class="flex-1 h-0.5 mx-0.5 -mt-4"
              :class="i < currentPhaseIndex ? 'bg-green-300' : 'bg-stone-200'"
            />
          </template>
        </div>
      </div>

      <!-- 内容区：按阶段展示 -->
      <div class="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        <!-- 空状态 -->
        <div v-if="currentPhaseIndex < 0" class="text-center text-stone-400 mt-20">
          <p class="text-3xl mb-2">📝</p>
          <p class="text-sm">发送消息开始创作</p>
        </div>

        <!-- 意图阶段 -->
        <div v-if="currentPhase === 'intent' && intent" class="bg-blue-50 rounded-xl p-3">
          <p class="text-sm text-blue-500 font-medium mb-1">识别意图</p>
          <p class="text-sm text-blue-800">{{ intent === 'write_blog' ? '写博客' : intent === 'chat' ? '普通对话' : intent }}</p>
        </div>

        <!-- 大纲阶段：显示大纲 -->
        <div v-if="(currentPhase === 'outline' || currentPhase === 'writing' || currentPhase === 'enhancing') && outlineSections.length > 0">
          <p class="text-sm text-stone-500 font-medium mb-2">
            {{ outlineTitle || '博客大纲' }}
          </p>
          <div class="space-y-1.5">
            <div
              v-for="(s, i) in outlineSections"
              :key="i"
              class="bg-stone-50 rounded-xl p-2.5 border border-stone-100"
            >
              <p class="text-sm font-medium text-stone-800">{{ i + 1 }}. {{ s.heading }}</p>
              <p class="text-xs text-stone-500 mt-0.5">{{ s.keyPoints.join(' · ') }}</p>
            </div>
          </div>
        </div>

        <!-- 写作阶段：正文 Agent 单独写 -->
        <div v-if="currentPhase === 'writing'" class="space-y-2">
          <p class="text-sm text-stone-500 font-medium">撰写正文中...</p>

          <div class="rounded-xl p-2.5 border bg-amber-50 border-amber-200">
            <div class="flex items-center gap-2">
              <span class="animate-pulse text-amber-500 text-sm">✍️</span>
              <span class="text-sm font-medium text-amber-700">正文 Agent</span>
              <span class="ml-auto flex gap-1">
                <span class="w-1 h-1 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 0ms"/>
                <span class="w-1 h-1 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 150ms"/>
                <span class="w-1 h-1 bg-amber-400 rounded-full animate-bounce" style="animation-delay: 300ms"/>
              </span>
            </div>
            <p class="text-xs text-stone-400 mt-1 ml-6">撰写博客正文（所有章节）...</p>
          </div>
        </div>

        <!-- 增强阶段：代码 + 配图 Agent 并行 -->
        <div v-if="currentPhase === 'enhancing'" class="space-y-2">
          <p class="text-sm text-stone-500 font-medium">正文完成，并行增强中...</p>

          <!-- 代码 Agent -->
          <div
            class="rounded-xl p-2.5 border transition-colors"
            :class="
              agentStatus.code === 'done' ? 'bg-green-50 border-green-200' :
              agentStatus.code === 'running' ? 'bg-blue-50 border-blue-200' :
              'bg-stone-50 border-stone-100'
            "
          >
            <div class="flex items-center gap-2">
              <span v-if="agentStatus.code === 'done'" class="text-green-500 text-sm">✅</span>
              <span v-else-if="agentStatus.code === 'running'" class="animate-pulse text-blue-500 text-sm">💻</span>
              <span v-else class="text-stone-300 text-sm">&lt;/&gt;</span>
              <span class="text-xs font-medium"
                :class="
                  agentStatus.code === 'done' ? 'text-green-700' :
                  agentStatus.code === 'running' ? 'text-blue-700' :
                  'text-stone-400'
                "
              >代码 Agent</span>
              <span v-if="agentStatus.code === 'running'" class="ml-auto flex gap-1">
                <span class="w-1 h-1 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 0ms"/>
                <span class="w-1 h-1 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 150ms"/>
                <span class="w-1 h-1 bg-blue-400 rounded-full animate-bounce" style="animation-delay: 300ms"/>
              </span>
            </div>
            <p class="text-xs text-stone-400 mt-1 ml-6">
              {{ agentStatus.code === 'done' ? '代码生成完成' : agentStatus.code === 'running' ? '分析正文并生成代码...' : '等待正文完成' }}
            </p>
          </div>

          <!-- 配图 Agent -->
          <div
            class="rounded-xl p-2.5 border transition-colors"
            :class="
              agentStatus.image === 'done' ? 'bg-green-50 border-green-200' :
              agentStatus.image === 'running' ? 'bg-purple-50 border-purple-200' :
              'bg-stone-50 border-stone-100'
            "
          >
            <div class="flex items-center gap-2">
              <span v-if="agentStatus.image === 'done'" class="text-green-500 text-sm">✅</span>
              <span v-else-if="agentStatus.image === 'running'" class="animate-pulse text-purple-500 text-sm">🖼️</span>
              <span v-else class="text-stone-300 text-sm">🖼️</span>
              <span class="text-xs font-medium"
                :class="
                  agentStatus.image === 'done' ? 'text-green-700' :
                  agentStatus.image === 'running' ? 'text-purple-700' :
                  'text-stone-400'
                "
              >配图 Agent</span>
              <span v-if="agentStatus.image === 'running'" class="ml-auto flex gap-1">
                <span class="w-1 h-1 bg-purple-400 rounded-full animate-bounce" style="animation-delay: 0ms"/>
                <span class="w-1 h-1 bg-purple-400 rounded-full animate-bounce" style="animation-delay: 150ms"/>
                <span class="w-1 h-1 bg-purple-400 rounded-full animate-bounce" style="animation-delay: 300ms"/>
              </span>
            </div>
            <p class="text-xs text-stone-400 mt-1 ml-6">
              {{ agentStatus.image === 'done' ? '配图生成完成' : agentStatus.image === 'running' ? '分析正文并生成配图...' : '等待正文完成' }}
            </p>
          </div>
        </div>

        <!-- 审核阶段：博客预览 + 决策 -->
        <template v-if="currentPhase === 'review'">
          <div class="bg-indigo-50 rounded-xl p-3">
            <p class="text-sm text-indigo-500 font-medium mb-2">📄 博客预览</p>
            <div
              class="prose prose-sm max-w-none text-sm bg-white rounded-xl p-3 border border-indigo-100 max-h-96 overflow-y-auto"
              v-html="marked.parse(fullBlog || '')"
            />
          </div>

          <!-- 板块选择 -->
          <div>
            <label class="text-sm text-stone-500 font-medium mb-1.5 block">发布到板块</label>
            <select
              v-model="sectionId"
              class="w-full px-3 py-2 border border-stone-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
            >
              <option v-for="s in sections" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>

          <!-- 决策按钮 -->
          <div class="flex gap-2">
            <button
              @click="$emit('decision', 'PUBLISH')"
              class="flex-1 px-4 py-2.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-medium text-sm transition-colors"
            >
              🚀 发布到论坛
            </button>
            <button
              @click="$emit('decision', 'DRAFT')"
              class="px-4 py-2.5 border border-stone-200 hover:bg-stone-50 text-stone-700 rounded-xl font-medium text-sm transition-colors"
            >
              💾 存草稿
            </button>
          </div>
        </template>

        <!-- 完成阶段 -->
        <div v-if="currentPhase === 'done'" class="bg-green-50 rounded-xl p-4 text-center">
          <p class="text-2xl mb-1">🎉</p>
          <p class="text-sm text-green-700 font-medium">操作完成！</p>
          <p class="text-sm text-green-500 mt-1">可以继续对话或查看结果</p>
        </div>
      </div>
    </template>

    <!-- 折叠状态：竖向标签 -->
    <div v-if="collapsed" class="flex flex-col items-center py-3 gap-3">
      <button
        @click="$emit('toggle-collapse')"
        class="w-6 h-6 flex items-center justify-center rounded text-stone-400 hover:text-stone-600 text-xs"
        title="展开工作台"
      >
        ◀
      </button>
      <span class="text-xs text-stone-400" style="writing-mode: vertical-rl">工作台</span>
    </div>
  </aside>
</template>
