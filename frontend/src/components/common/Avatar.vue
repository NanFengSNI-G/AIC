<script setup lang="ts">
defineProps<{
  src?: string
  alt?: string
  size?: 'sm' | 'md' | 'lg'
  showStatus?: boolean
  online?: boolean
}>()
</script>

<template>
  <div class="relative inline-block">
    <img
      :src="src || '/default-avatar.png'"
      :alt="alt || 'avatar'"
      :class="[
        'rounded-full object-cover bg-stone-200',
        size === 'sm' ? 'w-8 h-8' : size === 'lg' ? 'w-14 h-14' : 'w-10 h-10'
      ]"
    />
    <span
      v-if="showStatus"
      :class="[
        'absolute bottom-0 right-0 rounded-full border-2 border-white',
        online ? 'bg-green-500' : 'bg-stone-400',
        size === 'sm' ? 'w-2 h-2' : 'w-3 h-3'
      ]"
    />
  </div>
</template>
