<script setup lang="ts">
import { IconChevronDown } from '@tabler/icons-vue'

export interface SelectOption {
  value: string
  label: string
}

const model = defineModel<string>({ default: '' })

withDefaults(
  defineProps<{
    options: readonly SelectOption[]
    placeholder?: string
    invalid?: boolean
    id?: string
    size?: 'md' | 'sm'
  }>(),
  {
    placeholder: undefined,
    invalid: false,
    id: undefined,
    size: 'md',
  },
)
</script>

<template>
  <div class="select" :class="[`select-${size}`, { invalid, empty: model === '' }]">
    <select :id="id" v-model="model" :aria-invalid="invalid || undefined">
      <option v-if="placeholder !== undefined" value="">{{ placeholder }}</option>
      <option v-for="option in options" :key="option.value" :value="option.value">
        {{ option.label }}
      </option>
    </select>
    <IconChevronDown class="chevron" :size="16" stroke-width="2" />
  </div>
</template>

<style scoped>
.select {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--h-control);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  transition:
    border-color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.select-sm {
  height: var(--h-control-sm);
  font-size: 13px;
}

.select:hover {
  border-color: color-mix(in srgb, var(--accent) 40%, var(--border-strong));
}

.select:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-soft);
}

.select.invalid {
  border-color: var(--danger);
}

.select.empty select {
  color: var(--text-muted);
}

select {
  appearance: none;
  width: 100%;
  height: 100%;
  padding: 0 34px 0 12px;
  border: none;
  outline: none;
  background: transparent;
  cursor: pointer;
}

.select-sm select {
  padding: 0 30px 0 10px;
}

select option {
  color: var(--text);
  background: var(--surface);
}

.chevron {
  position: absolute;
  right: 10px;
  color: var(--text-muted);
  pointer-events: none;
  transition: transform var(--dur) var(--ease);
}

.select:focus-within .chevron {
  transform: rotate(180deg);
  color: var(--accent);
}
</style>
