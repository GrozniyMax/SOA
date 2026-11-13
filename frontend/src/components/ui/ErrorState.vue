<script setup lang="ts">
import { computed } from 'vue'
import { IconRefresh } from '@tabler/icons-vue'
import { ApiException, networkHint } from '@/api/http'
import BaseButton from '@/components/ui/BaseButton.vue'

const props = withDefaults(
  defineProps<{
    error: unknown
    retryable?: boolean
    compact?: boolean
  }>(),
  {
    retryable: true,
    compact: false,
  },
)

const emit = defineEmits<{ retry: [] }>()

const view = computed(() => {
  const err = props.error
  if (err instanceof ApiException) {
    return {
      title: err.message,
      detail: err.serverMessage,
      hint: networkHint(err),
    }
  }
  return {
    title: 'Что-то пошло не так',
    detail: err instanceof Error ? err.message : null,
    hint: null,
  }
})
</script>

<template>
  <div class="error-state rise-in" :class="{ compact }" role="alert">
    <div class="error-body">
      <p class="error-title">{{ view.title }}</p>
      <p v-if="view.detail" class="error-hint">{{ view.detail }}</p>
      <p v-if="view.hint" class="error-hint">{{ view.hint }}</p>
    </div>
    <BaseButton v-if="retryable" size="sm" variant="secondary" :icon="IconRefresh" @click="emit('retry')">
      Повторить
    </BaseButton>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 1px solid color-mix(in srgb, var(--danger) 25%, transparent);
  border-radius: var(--radius-lg);
  background: var(--danger-soft);
}

.error-state.compact {
  padding: 12px 14px;
}

.error-body {
  flex: 1;
  min-width: 0;
}

.error-title {
  font-weight: 600;
  color: var(--danger-text);
}

.error-hint {
  margin-top: 2px;
  color: var(--text-secondary);
}
</style>
