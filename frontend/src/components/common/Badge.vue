<script setup lang="ts">
defineProps<{
  value?: number
  max?: number
  dot?: boolean
}>()

const displayValue = (val?: number, max?: number) => {
  if (!val) return ''
  if (max && val > max) return `${max}+`
  return val > 99 ? '99+' : String(val)
}
</script>

<template>
  <span
    v-if="!dot"
    class="inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 text-xs font-medium bg-red-500 text-white rounded-full"
  >
    {{ displayValue(value, max) }}
  </span>
  <span
    v-else
    class="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full"
  />
</template>
