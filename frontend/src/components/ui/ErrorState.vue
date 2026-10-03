<script setup lang="ts">
import { computed, ref } from 'vue'
import { IconAlertTriangle, IconRefresh } from '@tabler/icons-vue'
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

const showDetail = ref(false)

const view = computed(() => {
  const err = props.error
  if (err instanceof ApiException) {
    return {
      title: err.message,
      detail: err.serverMessage,
      hint: networkHint(err),
      status: err.kind === 'http' ? err.status : null,
    }
  }
  return {
    title: 'Что-то пошло не так',
    detail: err instanceof Error ? err.message : null,
    hint: null,
    status: null,
  }
})
</script>

<template>
  <div class="error-state rise-in" :class="{ compact }" role="alert">
    <span class="error-icon">
      <IconAlertTriangle :size="compact ? 18 : 20" stroke-width="2" />
    </span>
    <div class="error-body">
      <p class="error-title">
        {{ view.title }}
        <span v-if="view.status" class="error-status tabular">{{ view.status }}</span>
      </p>
      <p v-if="view.hint" class="error-hint">{{ view.hint }}</p>
      <template v-if="view.detail">
        <button type="button" class="error-toggle" @click="showDetail = !showDetail">
          {{ showDetail ? 'Скрыть ответ сервиса' : 'Ответ сервиса' }}
        </button>
        <Transition name="fade">
          <pre v-if="showDetail" class="error-detail">{{ view.detail }}</pre>
        </Transition>
      </template>
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

.error-icon {
  flex-shrink: 0;
  color: var(--danger);
  margin-top: 1px;
}

.error-body {
  flex: 1;
  min-width: 0;
}

.error-title {
  font-weight: 600;
  color: var(--danger-text);
}

.error-status {
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 6px;
  background: color-mix(in srgb, var(--danger) 16%, transparent);
  font-size: 11px;
  font-weight: 600;
}

.error-hint {
  margin-top: 2px;
  color: var(--text-secondary);
}

.error-toggle {
  margin-top: 4px;
  padding: 0;
  border: none;
  background: none;
  color: var(--danger-text);
  font-size: 12px;
  cursor: pointer;
  opacity: 0.85;
}

.error-toggle:hover {
  opacity: 1;
  text-decoration: underline;
}

.error-detail {
  margin: 6px 0 0;
  padding: 8px 10px;
  max-height: 180px;
  overflow: auto;
  border-radius: var(--radius-sm);
  background: var(--surface);
  font-family: var(--font-mono);
  font-size: 11.5px;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--text-secondary);
}
</style>
