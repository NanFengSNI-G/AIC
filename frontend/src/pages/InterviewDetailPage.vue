<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getQARecords, getInterviewRecords } from '@/services/api'
import type { InterviewQA, InterviewRecord } from '@/types/interview'
import { marked } from 'marked'
import {
  parseQAEvaluation,
  parseFinalEvaluation,
  getWeaknessColor,
  type FinalEvaluation
} from '@/composables/useInterviewEvaluation'

const route = useRoute()
const router = useRouter()

const qaList = ref<InterviewQA[]>([])
const record = ref<InterviewRecord | null>(null)
const loading = ref(false)

const finalEvaluation = computed<FinalEvaluation | null>(() => {
  return parseFinalEvaluation(record.value?.evaluation || '')
})

const renderMarkdown = (text: string) => marked.parse(text || '') as string

onMounted(async () => {
  const recordId = Number(route.params.id)
  loading.value = true
  try {
    const [qaRes, recordsRes] = await Promise.all([
      getQARecords(recordId),
      getInterviewRecords()
    ])
    qaList.value = qaRes.data.data || []
    record.value = (recordsRes.data.data || []).find((r: InterviewRecord) => r.id === recordId) || null
  } catch {
    router.push('/interview')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="min-h-screen bg-stone-50">
    <!-- Header -->
    <div class="bg-white border-b border-stone-100 sticky top-0 z-10">
      <div class="max-w-7xl mx-auto px-6 py-4 flex items-center gap-4">
        <button
          @click="router.push('/interview')"
          class="p-2 hover:bg-stone-100 rounded-lg transition-colors"
        >
          <svg class="w-5 h-5 text-stone-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 class="text-xl font-semibold text-stone-900">{{ record?.recordName || '面试详情' }}</h1>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="text-stone-400">加载中...</div>
    </div>

    <!-- Content -->
    <div v-else class="max-w-7xl mx-auto px-6 py-8">
      <!-- Final Evaluation (总评) -->
      <div v-if="finalEvaluation" class="bg-white rounded-2xl shadow-sm border border-stone-100 p-6 mb-8">
        <h2 class="text-lg font-semibold text-stone-900 mb-4">面试总评</h2>
        <!-- 待提升知识点列表 -->
        <div v-if="finalEvaluation.待提升知识点列表?.length" class="mb-4">
          <div class="text-sm text-stone-500 mb-2">待提升知识点</div>
          <div class="flex flex-wrap gap-2">
            <span
              v-for="(kp, idx) in finalEvaluation.待提升知识点列表"
              :key="idx"
              :class="getWeaknessColor(kp.薄弱程度)"
              class="px-3 py-1 rounded-full text-sm font-medium"
            >
              {{ kp.知识点名称 }} ({{ kp.薄弱程度 }})
            </span>
          </div>
        </div>
        <!-- 总评内容 -->
        <div>
          <div class="text-sm text-stone-500 mb-2">总体评价</div>
          <div class="prose prose-sm max-w-none text-stone-800" v-html="renderMarkdown(finalEvaluation.总评)" />
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- Left: Q&A Timeline -->
        <div class="lg:col-span-2 space-y-6">
          <div v-if="qaList.length === 0" class="bg-white rounded-2xl p-8 text-center text-stone-400">
            暂无问答记录
          </div>
          <div
            v-for="(qa, index) in qaList"
            :key="qa.id"
            class="bg-white rounded-2xl shadow-sm border border-stone-100 overflow-hidden"
          >
            <!-- Question -->
            <div class="bg-amber-50 px-6 py-4 border-b border-amber-100">
              <div class="flex items-center gap-2 mb-2">
                <span class="text-xs font-medium text-amber-600 bg-amber-100 px-2 py-1 rounded">第 {{ index + 1 }} 轮</span>
                <span class="text-xs text-amber-500">面试官</span>
              </div>
              <div class="prose prose-sm max-w-none text-stone-800" v-html="renderMarkdown(qa.question)" />
            </div>
            <!-- Answer -->
            <div class="px-6 py-4 border-b border-stone-100">
              <div class="flex items-center gap-2 mb-2">
                <span class="text-xs font-medium text-stone-500 bg-stone-100 px-2 py-1 rounded">候选人</span>
              </div>
              <div class="prose prose-sm max-w-none text-stone-700" v-html="renderMarkdown(qa.answer)" />
            </div>
          </div>
        </div>

        <!-- Right: Evaluation Panel -->
        <div class="space-y-6">
          <h2 class="text-lg font-semibold text-stone-900">评价详情</h2>
          <div v-if="qaList.length === 0" class="bg-white rounded-2xl p-8 text-center text-stone-400">
            暂无评价
          </div>
          <div
            v-for="(qa, index) in qaList"
            :key="qa.id"
            class="bg-white rounded-2xl shadow-sm border border-stone-100 p-5"
          >
            <div class="flex items-center justify-between mb-3">
              <span class="text-sm font-medium text-stone-700">第 {{ index + 1 }} 轮</span>
            </div>
            <div class="space-y-3">
              <div v-if="parseQAEvaluation(qa.evaluation)">
                <div class="text-xs text-stone-500 mb-1">考查知识点</div>
                <div class="text-sm text-stone-800 font-medium">
                  {{ parseQAEvaluation(qa.evaluation)?.考查知识点 || '未知' }}
                </div>
              </div>
              <div>
                <div class="text-xs text-stone-500 mb-1">评估</div>
                <div class="prose prose-sm max-w-none text-stone-900" v-html="renderMarkdown(parseQAEvaluation(qa.evaluation)?.评估 || qa.evaluation || '暂无评价')" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>