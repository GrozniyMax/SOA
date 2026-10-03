<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { IconAlertTriangle, IconCircleCheck, IconInfoCircle, IconX } from '@tabler/icons-vue'
import { useToast, type Toast } from '@/composables/useToast'

const toast = useToast()
const expanded = ref<Set<number>>(new Set())
const timers = new Map<number, { remaining: number; started: number; handle: number | null }>()

const icons = {
  error: IconAlertTriangle,
  success: IconCircleCheck,
  info: IconInfoCircle,
}

function schedule(item: Toast) {
  const timer = timers.get(item.id) ?? { remaining: item.timeout, started: 0, handle: null }
  timer.started = Date.now()
  timer.handle = window.setTimeout(() => toast.dismiss(item.id), timer.remaining)
  timers.set(item.id, timer)
}

function pause(id: number) {
  const timer = timers.get(id)
  if (!timer || timer.handle === null) return
  window.clearTimeout(timer.handle)
  timer.handle = null
  timer.remaining = Math.max(1500, timer.remaining - (Date.now() - timer.started))
}

function resume(item: Toast) {
  if (expanded.value.has(item.id)) return
  const timer = timers.get(item.id)
  if (timer && timer.handle === null) schedule(item)
}

function toggle(item: Toast) {
  const next = new Set(expanded.value)
  if (next.has(item.id)) next.delete(item.id)
  else {
    next.add(item.id)
    pause(item.id)
  }
  expanded.value = next
}

watch(
  () => toast.items.map((t) => t.id),
  (ids) => {
    for (const item of toast.items) if (!timers.has(item.id)) schedule(item)
    for (const id of [...timers.keys()]) {
      if (!ids.includes(id)) {
        const handle = timers.get(id)?.handle
        if (handle) window.clearTimeout(handle)
        timers.delete(id)
      }
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  for (const timer of timers.values()) if (timer.handle) window.clearTimeout(timer.handle)
})
</script>

<template>
  <Teleport to="body">
    <TransitionGroup tag="div" name="toast" class="toaster" aria-live="polite">
      <div
        v-for="item in toast.items"
        :key="item.id"
        class="toast"
        :class="`toast-${item.type}`"
        role="status"
        @mouseenter="pause(item.id)"
        @mouseleave="resume(item)"
      >
        <component :is="icons[item.type]" class="toast-icon" :size="18" stroke-width="2" />
        <div class="toast-content">
          <p class="toast-title">{{ item.title }}</p>
          <p v-if="item.hint" class="toast-hint">{{ item.hint }}</p>
          <template v-if="item.detail">
            <button type="button" class="toast-toggle" @click="toggle(item)">
              {{ expanded.has(item.id) ? 'Скрыть ответ сервиса' : 'Ответ сервиса' }}
            </button>
            <Transition name="fade">
              <pre v-if="expanded.has(item.id)" class="toast-detail">{{ item.detail }}</pre>
            </Transition>
          </template>
        </div>
        <button type="button" class="toast-close" aria-label="Закрыть" @click="toast.dismiss(item.id)">
          <IconX :size="15" stroke-width="2" />
        </button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<style scoped>
.toaster {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 80;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
  width: min(380px, calc(100vw - 32px));
  pointer-events: none;
}

.toast {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 12px 12px 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-float);
  pointer-events: auto;
}

.toast-icon {
  flex-shrink: 0;
  margin-top: 1px;
}

.toast-error .toast-icon {
  color: var(--danger);
}

.toast-success .toast-icon {
  color: var(--success);
}

.toast-info .toast-icon {
  color: var(--accent);
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-weight: 500;
}

.toast-hint {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-secondary);
}

.toast-toggle {
  margin-top: 4px;
  padding: 0;
  border: none;
  background: none;
  color: var(--accent-text);
  font-size: 12px;
  cursor: pointer;
}

.toast-toggle:hover {
  text-decoration: underline;
}

.toast-detail {
  margin: 6px 0 0;
  padding: 8px 10px;
  max-height: 160px;
  overflow: auto;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  font-family: var(--font-mono);
  font-size: 11.5px;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-secondary);
}

.toast-close {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition:
    background var(--dur) var(--ease),
    color var(--dur) var(--ease);
}

.toast-close:hover {
  background: var(--surface-muted);
  color: var(--text);
}

.toast-move,
.toast-enter-active,
.toast-leave-active {
  transition:
    transform var(--dur-slow) var(--ease),
    opacity var(--dur-slow) var(--ease);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(40px) scale(0.98);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

.toast-leave-active {
  position: absolute;
  right: 0;
}
</style>
