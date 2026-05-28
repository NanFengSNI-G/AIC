<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getInterviewRecords, updateRecordName, deleteInterviewRecord } from '@/services/api'
import type { InterviewRecord } from '@/types/interview'
import notification from '@/utils/notification'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const router = useRouter()
const records = ref<InterviewRecord[]>([])
const loading = ref(false)
const editingId = ref<number | null>(null)
const editingName = ref('')
const deleteConfirmId = ref<number | null>(null)

// 当面板打开时获取数据
watch(() => props.visible, (newVal) => {
  if (newVal) {
    fetchRecords()
  }
})

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getInterviewRecords()
    records.value = res.data.data || []
  } catch {
    notification.error('获取历史记录失败')
  } finally {
    loading.value = false
  }
}

const handleEdit = (record: InterviewRecord) => {
  editingId.value = record.id
  editingName.value = record.recordName
}

const handleSaveEdit = async () => {
  if (!editingId.value) return
  try {
    await updateRecordName({ recordId: editingId.value, name: editingName.value })
    const record = records.value.find(r => r.id === editingId.value)
    if (record) record.recordName = editingName.value
    editingId.value = null
    notification.success('修改成功')
  } catch {
    notification.error('修改失败')
  }
}

const handleCancelEdit = () => {
  editingId.value = null
  editingName.value = ''
}

const handleDelete = async (id: number) => {
  deleteConfirmId.value = id
}

const confirmDelete = async () => {
  if (!deleteConfirmId.value) return
  try {
    await deleteInterviewRecord(deleteConfirmId.value)
    records.value = records.value.filter(r => r.id !== deleteConfirmId.value)
    notification.success('删除成功')
  } catch {
    notification.error('删除失败')
  } finally {
    deleteConfirmId.value = null
  }
}

const cancelDelete = () => {
  deleteConfirmId.value = null
}

const handleViewDetail = (record: InterviewRecord) => {
  router.push(`/interview/${record.id}`)
}

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

</script>

<template>
  <Transition name="slide">
    <div v-if="visible" class="fixed inset-y-0 right-0 w-full max-w-md bg-white shadow-xl z-50 flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-stone-100">
        <h2 class="text-lg font-semibold text-stone-900">历史记录</h2>
        <button
          @click="emit('close')"
          class="p-2 text-stone-400 hover:text-stone-600 hover:bg-stone-100 rounded-lg transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto p-6">
        <div v-if="loading" class="text-center text-stone-400 py-8">加载中...</div>
        <div v-else-if="records.length === 0" class="text-center text-stone-400 py-8">暂无历史记录</div>
        <div v-else class="space-y-4">
          <div
            v-for="record in records"
            :key="record.id"
            class="bg-stone-50 rounded-2xl p-4 hover:bg-stone-100 transition-colors group"
          >
            <!-- 编辑模式 -->
            <div v-if="editingId === record.id" class="space-y-3">
              <input
                v-model="editingName"
                class="w-full px-3 py-2 bg-white border border-stone-200 rounded-xl text-stone-900 focus:outline-none focus:ring-2 focus:ring-amber-500"
                @keyup.enter="handleSaveEdit"
              />
              <div class="flex gap-2">
                <button
                  @click="handleSaveEdit"
                  class="px-4 py-1.5 bg-amber-500 text-white text-sm font-medium rounded-lg hover:bg-amber-600 transition-colors"
                >
                  保存
                </button>
                <button
                  @click="handleCancelEdit"
                  class="px-4 py-1.5 bg-stone-200 text-stone-600 text-sm font-medium rounded-lg hover:bg-stone-300 transition-colors"
                >
                  取消
                </button>
              </div>
            </div>

            <!-- 显示模式 -->
            <div v-else>
              <div class="flex items-start justify-between">
                <div
                  class="flex-1 cursor-pointer"
                  @click="handleViewDetail(record)"
                >
                  <h3 class="font-medium text-stone-900">{{ record.recordName }}</h3>
                  <p class="text-sm text-stone-500 mt-1">{{ formatDate(record.createTime) }}</p>
                  <p class="text-sm text-stone-400 mt-1">{{ record.questionCount }} 轮问答</p>
                </div>
                <div class="flex gap-2 ml-4 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    @click.stop="handleEdit(record)"
                    class="p-2 text-stone-400 hover:text-amber-500 hover:bg-amber-50 rounded-lg transition-colors"
                    title="改名"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                    </svg>
                  </button>
                  <button
                    @click.stop="handleDelete(record.id)"
                    class="p-2 text-stone-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    title="删除"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                  <button
                    @click.stop="handleViewDetail(record)"
                    class="p-2 text-stone-400 hover:text-amber-500 hover:bg-amber-50 rounded-lg transition-colors"
                    title="查看详情"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Backdrop -->
  <Transition name="fade">
    <div v-if="visible" class="fixed inset-0 bg-black/20 z-40" @click="emit('close')" />
  </Transition>

  <!-- Delete Confirmation Dialog -->
  <ConfirmDialog
    :show="deleteConfirmId !== null"
    title="删除确认"
    message="确定要删除这条面试记录吗？此操作无法撤销。"
    confirm-text="删除"
    cancel-text="取消"
    type="danger"
    @update:show="deleteConfirmId = null"
    @confirm="confirmDelete"
    @cancel="cancelDelete"
  />
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>