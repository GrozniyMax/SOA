import { onBeforeUnmount, ref, shallowRef, type Ref, type ShallowRef } from 'vue'
import { isAbortError } from '@/api/http'

export interface AsyncData<T> {
  data: ShallowRef<T | null>
  error: Ref<unknown>
  loading: Ref<boolean>
  generation: Ref<number>
  reload: () => Promise<void>
}

export function useAsyncData<T>(loader: (signal: AbortSignal) => Promise<T>): AsyncData<T> {
  const data = shallowRef<T | null>(null)
  const error = ref<unknown>(null)
  const loading = ref(false)
  const generation = ref(0)
  let controller: AbortController | null = null

  async function reload() {
    controller?.abort()
    const current = new AbortController()
    controller = current
    loading.value = true
    error.value = null
    try {
      const result = await loader(current.signal)
      if (current.signal.aborted) return
      data.value = result
      generation.value++
    } catch (err) {
      if (current.signal.aborted || isAbortError(err)) return
      data.value = null
      error.value = err
    } finally {
      if (controller === current) loading.value = false
    }
  }

  onBeforeUnmount(() => controller?.abort())

  return { data, error, loading, generation, reload }
}
