<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { IconX } from '@tabler/icons-vue'
import { useOverlay } from '@/composables/useOverlay'

const open = defineModel<boolean>('open', { default: false })

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    width?: string
  }>(),
  {
    subtitle: undefined,
    width: '520px',
  },
)

const panelRef = ref<HTMLElement | null>(null)

function close() {
  open.value = false
}

useOverlay(open, close)

watch(open, (value) => {
  if (value) void nextTick(() => panelRef.value?.focus())
})
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer">
      <div v-if="open" class="drawer-root">
        <div class="drawer-overlay" @click="close" />
        <aside
          ref="panelRef"
          class="drawer-panel"
          role="dialog"
          aria-modal="true"
          :aria-label="title"
          tabindex="-1"
          :style="{ width: `min(${width}, 100vw)` }"
        >
          <header class="drawer-header">
            <div class="drawer-titles">
              <h2>{{ title }}</h2>
              <p v-if="subtitle" class="secondary">{{ subtitle }}</p>
            </div>
            <button type="button" class="drawer-close" aria-label="Закрыть" @click="close">
              <IconX :size="18" stroke-width="2" />
            </button>
          </header>
          <div class="drawer-body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="drawer-footer">
            <slot name="footer" />
          </footer>
        </aside>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-root {
  position: fixed;
  inset: 0;
  z-index: 50;
}

.drawer-overlay {
  position: absolute;
  inset: 0;
  background: var(--overlay);
  backdrop-filter: blur(2px);
}

.drawer-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-left: 1px solid var(--border);
  box-shadow: var(--shadow-float);
  outline: none;
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border);
}

.drawer-titles h2 {
  font-size: 18px;
}

.drawer-titles p {
  margin-top: 2px;
  font-size: 13px;
}

.drawer-close {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background var(--dur) var(--ease),
    transform var(--dur) var(--ease),
    color var(--dur) var(--ease);
}

.drawer-close:hover {
  background: var(--surface-muted);
  color: var(--text);
  transform: rotate(90deg);
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.drawer-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 24px;
  border-top: 1px solid var(--border);
  background: var(--surface);
}

.drawer-enter-active,
.drawer-leave-active {
  transition: opacity var(--dur-slow) var(--ease);
}

.drawer-enter-active .drawer-panel,
.drawer-leave-active .drawer-panel {
  transition: transform var(--dur-slow) var(--ease);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .drawer-panel,
.drawer-leave-to .drawer-panel {
  transform: translateX(100%);
}
</style>
