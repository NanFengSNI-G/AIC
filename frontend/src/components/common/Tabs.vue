<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  tabs: { key: string; label: string }[]
  modelValue?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const activeTab = ref(props.modelValue || props.tabs[0]?.key)

watch(() => props.modelValue, (val) => {
  if (val) activeTab.value = val
})

watch(activeTab, (val) => {
  emit('update:modelValue', val)
})
</script>

<template>
  <div class="flex border-b border-stone-200">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      @click="activeTab = tab.key"
      :class="[
        'px-4 py-3 text-sm font-medium transition-colors duration-200 relative',
        activeTab === tab.key ? 'text-amber-600' : 'text-stone-500 hover:text-stone-700'
      ]"
    >
      {{ tab.label }}
      <span
        v-if="activeTab === tab.key"
        class="absolute bottom-0 left-0 right-0 h-0.5 bg-amber-500"
      />
    </button>
  </div>
</template>
