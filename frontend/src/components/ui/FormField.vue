<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    forId?: string
    error?: string | null
    hint?: string
    required?: boolean
  }>(),
  {
    forId: undefined,
    error: null,
    hint: undefined,
    required: false,
  },
)

const shakeKey = ref(0)

watch(
  () => props.error,
  (value, previous) => {
    if (value && value !== previous) shakeKey.value++
  },
)
</script>

<template>
  <div class="field" :class="{ 'has-error': error }">
    <label :for="forId" class="field-label">
      {{ label }}<span v-if="required" class="required" aria-hidden="true">*</span>
    </label>
    <div :key="shakeKey" :class="{ shake: shakeKey > 0 && error }">
      <slot />
    </div>
    <Transition name="msg" mode="out-in">
      <p v-if="error" :key="error" class="field-error" role="alert">{{ error }}</p>
      <p v-else-if="hint" class="field-hint">{{ hint }}</p>
    </Transition>
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.required {
  margin-left: 2px;
  color: var(--danger);
}

.field-error,
.field-hint {
  font-size: 12px;
  line-height: 1.4;
}

.field-error {
  color: var(--danger-text);
}

.field-hint {
  color: var(--text-muted);
}

.msg-enter-active,
.msg-leave-active {
  transition:
    opacity var(--dur) var(--ease),
    transform var(--dur) var(--ease);
}

.msg-enter-from,
.msg-leave-to {
  opacity: 0;
  transform: translateY(-3px);
}
</style>
