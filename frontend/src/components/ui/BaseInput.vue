<script setup lang="ts">
import { ref, type Component } from 'vue'

const model = defineModel<string>({ default: '' })

withDefaults(
  defineProps<{
    type?: 'text' | 'number' | 'date' | 'search'
    placeholder?: string
    invalid?: boolean
    icon?: Component
    step?: string
    min?: string
    max?: string
    maxlength?: number
    id?: string
    size?: 'md' | 'sm'
  }>(),
  {
    type: 'text',
    placeholder: undefined,
    invalid: false,
    icon: undefined,
    step: undefined,
    min: undefined,
    max: undefined,
    maxlength: undefined,
    id: undefined,
    size: 'md',
  },
)

const inputRef = ref<HTMLInputElement | null>(null)

defineExpose({ focus: () => inputRef.value?.focus() })
</script>

<template>
  <label class="input" :class="[`input-${size}`, { invalid, 'has-icon': icon }]">
    <component :is="icon" v-if="icon" class="input-icon" :size="16" stroke-width="2" />
    <input
      :id="id"
      ref="inputRef"
      v-model="model"
      :type="type"
      :placeholder="placeholder"
      :step="step"
      :min="min"
      :max="max"
      :maxlength="maxlength"
      :aria-invalid="invalid || undefined"
      autocomplete="off"
    />
  </label>
</template>

<style scoped>
.input {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--h-control);
  padding: 0 12px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  cursor: text;
  transition:
    border-color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.input-sm {
  height: var(--h-control-sm);
  padding: 0 10px;
  font-size: 13px;
}

.input:hover {
  border-color: color-mix(in srgb, var(--accent) 40%, var(--border-strong));
}

.input:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.input.invalid {
  border-color: var(--danger);
}

.input.invalid:focus-within {
  box-shadow: 0 0 0 3px var(--danger-soft);
}

.input-icon {
  flex-shrink: 0;
  margin-right: 8px;
  color: var(--text-muted);
  transition: color var(--dur) var(--ease);
}

.input:focus-within .input-icon {
  color: var(--accent);
}

input {
  flex: 1;
  min-width: 0;
  height: 100%;
  border: none;
  outline: none;
  background: transparent;
  font-variant-numeric: tabular-nums;
}

input::placeholder {
  color: var(--text-muted);
}

input[type='date']::-webkit-calendar-picker-indicator {
  opacity: 0.55;
  cursor: pointer;
}

@media (prefers-color-scheme: dark) {
  input[type='date']::-webkit-calendar-picker-indicator {
    filter: invert(1);
  }
}
</style>
