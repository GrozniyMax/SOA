import { reactive, readonly } from 'vue'
import { ApiException, isAbortError, networkHint } from '@/api/http'

export type ToastType = 'error' | 'success' | 'info'

export interface Toast {
  id: number
  type: ToastType
  title: string
  detail: string | null
  hint: string | null
  timeout: number
}

export interface ToastInput {
  type?: ToastType
  title: string
  detail?: string | null
  hint?: string | null
  timeout?: number
}

const state = reactive<{ items: Toast[] }>({ items: [] })
let nextId = 1

function push(input: ToastInput): number {
  const id = nextId++
  const type = input.type ?? 'info'
  state.items.push({
    id,
    type,
    title: input.title,
    detail: input.detail ?? null,
    hint: input.hint ?? null,
    timeout: input.timeout ?? (type === 'error' ? 8000 : 4000),
  })
  if (state.items.length > 4) state.items.shift()
  return id
}

function dismiss(id: number): void {
  const index = state.items.findIndex((t) => t.id === id)
  if (index >= 0) state.items.splice(index, 1)
}

function error(err: unknown, fallbackTitle = 'Что-то пошло не так'): void {
  if (isAbortError(err)) return
  if (err instanceof ApiException) {
    push({ type: 'error', title: err.message, detail: err.serverMessage, hint: networkHint(err) })
    return
  }
  push({ type: 'error', title: fallbackTitle, detail: err instanceof Error ? err.message : null })
}

export function useToast() {
  return {
    items: readonly(state).items,
    push,
    dismiss,
    error,
    success: (title: string, detail?: string) => push({ type: 'success', title, detail }),
    info: (title: string, detail?: string) => push({ type: 'info', title, detail }),
  }
}
