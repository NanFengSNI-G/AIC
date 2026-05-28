<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 px-4 text-center">
    <span class="text-5xl mb-4 opacity-50">{{ icon || '📭' }}</span>
    <h3 class="text-lg font-medium text-stone-900 mb-2">{{ title }}</h3>
    <p v-if="description" class="text-sm text-stone-500">{{ description }}</p>
    <slot />
  </div>
</template>
