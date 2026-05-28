import { ref, onMounted, onUnmounted } from 'vue'

export function useInfiniteScroll(
  callback: () => void,
  options?: { threshold?: number; rootMargin?: string }
) {
  const sentinel = ref<HTMLElement | null>(null)
  let observer: IntersectionObserver | null = null

  const handleIntersect: IntersectionObserverCallback = (entries) => {
    if (entries[0].isIntersecting) {
      callback()
    }
  }

  onMounted(() => {
    observer = new IntersectionObserver(handleIntersect, {
      threshold: options?.threshold || 0.1,
      rootMargin: options?.rootMargin || '0px'
    })

    if (sentinel.value) {
      observer.observe(sentinel.value)
    }
  })

  onUnmounted(() => {
    observer?.disconnect()
  })

  return { sentinel }
}
