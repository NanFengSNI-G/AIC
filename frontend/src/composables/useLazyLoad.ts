import { ref, onMounted, onUnmounted, type Ref } from 'vue'

export function useLazyLoad(
  el: Ref<HTMLElement | null>,
  callback: () => void,
  options?: IntersectionObserverInit
) {
  let observer: IntersectionObserver | null = null

  const observerCallback: IntersectionObserverCallback = (entries) => {
    if (entries[0].isIntersecting) {
      callback()
      observer?.disconnect()
    }
  }

  onMounted(() => {
    observer = new IntersectionObserver(observerCallback, {
      threshold: 0.1,
      ...options
    })

    if (el.value) {
      observer.observe(el.value)
    }
  })

  onUnmounted(() => {
    observer?.disconnect()
  })

  return {
    disconnect: () => observer?.disconnect()
  }
}
