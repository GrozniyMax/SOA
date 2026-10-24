<script setup lang="ts">
import { toRef } from 'vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import { useOverlay } from '@/composables/useOverlay'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    message?: string
    confirmLabel?: string
    cancelLabel?: string
    danger?: boolean
    loading?: boolean
  }>(),
  {
    message: undefined,
    confirmLabel: 'Подтвердить',
    cancelLabel: 'Отмена',
    danger: false,
    loading: false,
  },
)

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

function cancel() {
  if (!props.loading) emit('cancel')
}

useOverlay(toRef(props, 'open'), cancel)
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="open" class="dialog-root">
        <div class="dialog-overlay" @click="cancel" />
        <div class="dialog" role="alertdialog" aria-modal="true" :aria-label="title">
          <h2>{{ title }}</h2>
          <p v-if="message" class="secondary">{{ message }}</p>
          <slot />
          <div class="dialog-actions">
            <BaseButton variant="ghost" :disabled="loading" @click="cancel">{{ cancelLabel }}</BaseButton>
            <BaseButton :variant="danger ? 'danger' : 'primary'" :loading="loading" @click="emit('confirm')">
              {{ confirmLabel }}
            </BaseButton>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-root {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 16px;
}

.dialog-overlay {
  position: absolute;
  inset: 0;
  background: var(--overlay);
  backdrop-filter: blur(2px);
}

.dialog {
  position: relative;
  width: min(420px, 100%);
  padding: 22px 22px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow-float);
}

.dialog h2 {
  font-size: 17px;
}

.dialog p {
  margin-top: 8px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity var(--dur) var(--ease);
}

.dialog-enter-active .dialog,
.dialog-leave-active .dialog {
  transition: transform var(--dur-slow) var(--ease-spring);
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}

.dialog-enter-from .dialog,
.dialog-leave-to .dialog {
  transform: scale(0.94) translateY(6px);
}
</style>
