<script setup lang="ts">
import { ref, type Component } from 'vue'

const model = defineModel<string>({ default: '' })

const props = withDefaults(
  defineProps<{
    type?: 'text' | 'number' | 'date' | 'search'
    placeholder?: string
    invalid?: boolean
    icon?: Component
    step?: string
    min?: string
    max?: string
    maxlength?: number
    decimals?: number
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
    decimals: undefined,
    id: undefined,
    size: 'md',
  },
)

const inputRef = ref<HTMLInputElement | null>(null)

function fractionLength(value: string): number {
  return /[.,](\d*)/.exec(value)?.[1]?.length ?? 0
}

function trimFraction(value: string, digits: number): string {
  return value.replace(/[.,]\d*/, (part) => part.slice(0, digits + 1))
}

function onInput(event: Event) {
  const el = event.target as HTMLInputElement
  const next = el.value
  if (props.decimals !== undefined) {
    const length = fractionLength(next)
    const previous = fractionLength(model.value)
    if (length > props.decimals && length > previous) {
      const fixed =
        next.length - model.value.length > 1 ? trimFraction(next, Math.max(props.decimals, previous)) : model.value
      const caret = Math.min((el.selectionStart ?? next.length) - (next.length - fixed.length), fixed.length)
      el.value = fixed
      el.setSelectionRange(Math.max(caret, 0), Math.max(caret, 0))
      model.value = fixed
      return
    }
  }
  model.value = next
}

defineExpose({ focus: () => inputRef.value?.focus() })
</script>

<template>
  <label class="input" :class="[`input-${size}`, { invalid, 'has-icon': icon }]">
    <component :is="icon" v-if="icon" class="input-icon" :size="16" stroke-width="2" />
    <input
      :id="id"
      ref="inputRef"
      :value="model"
      :type="type"
      :placeholder="placeholder"
      :step="step"
      :min="min"
      :max="max"
      :maxlength="maxlength"
      :aria-invalid="invalid || undefined"
      autocomplete="off"
      @input="onInput"
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
