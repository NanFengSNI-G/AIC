<script setup lang="ts">
defineProps<{
  variant?: 'text' | 'circular' | 'rectangular'
  width?: string
  height?: string
  lines?: number
}>()
</script>

<template>
  <div v-if="variant === 'circular'" class="rounded-full bg-bg-tertiary animate-pulse" :style="{ width: width || '40px', height: height || width || '40px' }" />
  <div v-else-if="variant === 'rectangular'" class="rounded-lg bg-bg-tertiary animate-pulse" :style="{ width: width || '100%', height: height || '100px' }" />
  <div v-else class="space-y-2">
    <div v-for="i in (lines || 1)" :key="i" class="h-4 bg-bg-tertiary rounded animate-pulse" :style="{ width: i === (lines || 1) ? '60%' : '100%' }" />
  </div>
</template>
