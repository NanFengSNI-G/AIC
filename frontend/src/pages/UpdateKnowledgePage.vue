<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import notification from '@/utils/notification'
import LoadingOverlay from '@/components/common/LoadingOverlay.vue'

const router = useRouter()
const isLoading = ref(true)
const isUpdating = ref(false)

const updateKnowledge = async () => {
  isUpdating.value = true
  try {
    await api.post('/interview/update')
    notification.success('知识库更新成功', { title: '更新完成' })
    router.push('/interview')
  } catch (e: any) {
    notification.error(e.message || '更新失败', { title: '更新失败' })
  } finally {
    isUpdating.value = false
  }
}

onMounted(() => {
  updateKnowledge()
})
</script>

<template>
  <div class="max-w-md mx-auto mt-20">
    <LoadingOverlay v-if="isUpdating" message="正在更新知识库..." />
    <div v-else class="text-center">
      <p class="text-stone-500">知识库更新中...</p>
    </div>
  </div>
</template>