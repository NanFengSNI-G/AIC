<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  columnCount?: number
  gap?: number
}>()

const containerRef = ref<HTMLElement | null>(null)
const columns = ref(3)

const updateColumns = () => {
  const width = window.innerWidth
  if (width < 640) {
    columns.value = 1
  } else if (width < 1024) {
    columns.value = 2
  } else {
    columns.value = props.columnCount || 3
  }
}

onMounted(() => {
  updateColumns()
  window.addEventListener('resize', updateColumns)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateColumns)
})
</script>

<template>
  <div ref="containerRef" class="w-full">
    <div :style="{ columnCount: columns, columnGap: `${gap || 16}px` }">
      <slot />
    </div>
  </div>
</template>

<style scoped>
div > * {
  break-inside: avoid;
  margin-bottom: v-bind("`${gap || 16}px`");
}
</style>
