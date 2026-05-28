<script setup lang="ts">
defineProps<{
  icon: string
  size?: 'sm' | 'md' | 'lg'
  variant?: 'default' | 'primary'
}>()

defineEmits<{
  click: [event: MouseEvent]
}>()
</script>

<template>
  <button
    @click="$emit('click', $event)"
    :class="[
      'flex items-center justify-center rounded-xl transition-all duration-200',
      'hover:scale-105 active:scale-95',
      'disabled:opacity-50 disabled:cursor-not-allowed',
      size === 'sm' ? 'w-9 h-9' : size === 'lg' ? 'w-12 h-12' : 'w-10 h-10',
      variant === 'primary' ? 'bg-amber-500 hover:bg-amber-600 text-white' : 'bg-stone-100 hover:bg-stone-200 text-stone-700'
    ]"
  >
    <span :class="size === 'sm' ? 'text-base' : size === 'lg' ? 'text-xl' : 'text-lg'">{{ icon }}</span>
  </button>
</template>
