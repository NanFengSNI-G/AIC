<script setup lang="ts">
import { ref, watch } from 'vue'
import Modal from './Modal.vue'

const props = withDefaults(defineProps<{
  show: boolean
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'danger' | 'warning' | 'info'
}>(), {
  title: '确认操作',
  confirmText: '确认',
  cancelText: '取消',
  type: 'danger'
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  confirm: []
  cancel: []
}>()

const loading = ref(false)

watch(() => props.show, (val) => {
  if (val) {
    loading.value = false
  }
})

const handleConfirm = async () => {
  loading.value = true
  emit('confirm')
}

const handleCancel = () => {
  emit('cancel')
  emit('update:show', false)
}

const getTypeStyles = () => {
  switch (props.type) {
    case 'danger':
      return {
        icon: '⚠️',
        iconBg: 'bg-red-50',
        iconColor: 'text-red-500',
        confirmBg: 'bg-red-500 hover:bg-red-600',
        borderColor: 'border-red-200'
      }
    case 'warning':
      return {
        icon: '⚡',
        iconBg: 'bg-amber-50',
        iconColor: 'text-amber-500',
        confirmBg: 'bg-amber-500 hover:bg-amber-600',
        borderColor: 'border-amber-200'
      }
    default:
      return {
        icon: 'ℹ️',
        iconBg: 'bg-blue-50',
        iconColor: 'text-blue-500',
        confirmBg: 'bg-accent-primary hover:bg-accent-hover',
        borderColor: 'border-blue-200'
      }
  }
}

const typeStyle = getTypeStyles()
</script>

<template>
  <Modal :show="show" @update:show="emit('update:show', $event)">
    <div class="p-2">
      <!-- Header with Icon and Title -->
      <div class="flex items-center gap-3 mb-4">
        <span class="text-xl">{{ typeStyle.icon }}</span>
        <h3 class="text-lg font-semibold text-text-primary">
          {{ title }}
        </h3>
      </div>

      <!-- Message -->
      <p class="text-sm text-text-secondary mb-6 leading-relaxed">
        {{ message }}
      </p>

      <!-- Actions -->
      <div class="flex gap-3 w-full">
        <button
          @click="handleCancel"
          :disabled="loading"
          class="flex-1 px-4 py-2.5 rounded-lg border border-bg-tertiary bg-bg-secondary text-text-secondary hover:bg-bg-tertiary transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium text-sm"
        >
          {{ cancelText }}
        </button>
        <button
          @click="handleConfirm"
          :disabled="loading"
          :class="[typeStyle.confirmBg, 'flex-1 px-4 py-2.5 rounded-lg text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed font-medium text-sm']"
        >
          <span v-if="!loading">{{ confirmText }}</span>
          <span v-else class="flex items-center justify-center gap-2">
            <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            处理中...
          </span>
        </button>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
/* Modal already has center positioning and transitions */
</style>
