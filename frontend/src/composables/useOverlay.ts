import { onBeforeUnmount, watch, type Ref } from 'vue'

const stack: symbol[] = []

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  const top = stack[stack.length - 1]
  if (top) handlers.get(top)?.()
}

const handlers = new Map<symbol, () => void>()

export function useOverlay(open: Ref<boolean>, onClose: () => void) {
  const id = Symbol('overlay')

  function activate() {
    if (stack.includes(id)) return
    if (stack.length === 0) {
      document.documentElement.style.overflow = 'hidden'
      window.addEventListener('keydown', onKeydown)
    }
    stack.push(id)
    handlers.set(id, onClose)
  }

  function deactivate() {
    const index = stack.indexOf(id)
    if (index < 0) return
    stack.splice(index, 1)
    handlers.delete(id)
    if (stack.length === 0) {
      document.documentElement.style.overflow = ''
      window.removeEventListener('keydown', onKeydown)
    }
  }

  watch(open, (value) => (value ? activate() : deactivate()), { immediate: true })
  onBeforeUnmount(deactivate)
}
