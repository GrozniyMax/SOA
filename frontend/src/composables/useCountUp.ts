import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

export function useCountUp(source: Ref<number | null | undefined>, duration = 700): Ref<number | null> {
  const display = ref<number | null>(source.value ?? null)
  let frame = 0

  const reduceMotion =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

  function animate(from: number, to: number) {
    cancelAnimationFrame(frame)
    if (reduceMotion || from === to) {
      display.value = to
      return
    }
    display.value = from
    const start = performance.now()
    const step = (now: number) => {
      const t = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - t, 3)
      display.value = Math.round(from + (to - from) * eased)
      if (t < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
  }

  watch(
    source,
    (value) => {
      if (value === null || value === undefined) {
        cancelAnimationFrame(frame)
        display.value = null
        return
      }
      animate(display.value ?? 0, value)
    },
    { immediate: true },
  )

  onBeforeUnmount(() => cancelAnimationFrame(frame))

  return display
}
