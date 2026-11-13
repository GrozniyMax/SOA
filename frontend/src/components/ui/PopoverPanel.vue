<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

const open = defineModel<boolean>('open', { default: false })

withDefaults(
  defineProps<{
    align?: 'start' | 'end'
    width?: string
  }>(),
  {
    align: 'start',
    width: '340px',
  },
)

const rootRef = ref<HTMLElement | null>(null)

function onPointerDown(event: PointerEvent) {
  const root = rootRef.value
  const target = event.target
  if (!root || !(target instanceof Node)) return
  if (root.contains(target)) return
  if (target instanceof Element && target.closest('[role="dialog"], [role="alertdialog"], .toaster')) return
  open.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.stopPropagation()
    open.value = false
  }
}

function attach() {
  document.addEventListener('pointerdown', onPointerDown, true)
  document.addEventListener('keydown', onKeydown, true)
}

function detach() {
  document.removeEventListener('pointerdown', onPointerDown, true)
  document.removeEventListener('keydown', onKeydown, true)
}

watch(open, (value) => (value ? attach() : detach()), { immediate: true })
onBeforeUnmount(detach)
</script>

<template>
  <div ref="rootRef" class="popover-root">
    <slot name="trigger" />
    <Transition name="popover">
      <div
        v-if="open"
        class="popover"
        :class="`align-${align}`"
        :style="{ width: `min(${width}, calc(100vw - 32px))` }"
        role="group"
      >
        <slot />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.popover-root {
  position: relative;
  display: inline-flex;
}

.popover {
  position: absolute;
  top: calc(100% + 8px);
  z-index: 30;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-float);
}

.align-start {
  left: 0;
  transform-origin: top left;
}

.align-end {
  right: 0;
  transform-origin: top right;
}

.popover-enter-active,
.popover-leave-active {
  transition:
    opacity var(--dur) var(--ease),
    transform var(--dur) var(--ease);
}

.popover-enter-from,
.popover-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.97);
}
</style>
