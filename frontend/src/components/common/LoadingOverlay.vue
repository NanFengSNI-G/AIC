<script setup lang="ts">
defineProps<{
  message?: string
}>()
</script>

<template>
  <Teleport to="body">
    <div class="loading-overlay">
      <div class="wave-text">
        <span
          v-for="(char, i) in (message || '加载中...')"
          :key="i"
          class="char"
          :style="{ animationDelay: `${i * 0.08}s` }"
        >{{ char }}</span>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.wave-text {
  display: flex;
  gap: 2px;
  font-size: 18px;
  font-weight: 600;
  color: #F59E0B;
  letter-spacing: 2px;
}

.char {
  display: inline-block;
  animation: wave 1.4s ease-in-out infinite;
  transform-origin: bottom center;
}

@keyframes wave {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
</style>