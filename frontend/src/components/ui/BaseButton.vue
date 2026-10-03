<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-ghost'
    size?: 'md' | 'sm'
    type?: 'button' | 'submit' | 'reset'
    icon?: Component
    iconRight?: Component
    loading?: boolean
    disabled?: boolean
    block?: boolean
    iconOnly?: boolean
    label?: string
  }>(),
  {
    variant: 'secondary',
    size: 'md',
    type: 'button',
    icon: undefined,
    iconRight: undefined,
    loading: false,
    disabled: false,
    block: false,
    iconOnly: false,
    label: undefined,
  },
)
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn-${variant}`, `btn-${size}`, { block, 'icon-only': iconOnly, loading }]"
    :disabled="disabled || loading"
    :aria-label="label"
    :aria-busy="loading || undefined"
  >
    <span v-if="loading" class="spinner" aria-hidden="true" />
    <component :is="icon" v-else-if="icon" class="btn-icon" :size="size === 'sm' ? 15 : 17" stroke-width="2" />
    <span v-if="$slots.default" class="btn-label"><slot /></span>
    <component :is="iconRight" v-if="iconRight" class="btn-icon-right" :size="size === 'sm' ? 15 : 17" stroke-width="2" />
  </button>
</template>

<style scoped>
.btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: var(--h-control);
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: var(--radius);
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  transition:
    transform var(--dur-fast) var(--ease),
    background var(--dur) var(--ease),
    border-color var(--dur) var(--ease),
    color var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.btn:not(:disabled):hover {
  transform: translateY(-1px);
}

.btn:not(:disabled):active {
  transform: translateY(0) scale(0.97);
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.btn.loading {
  cursor: progress;
  opacity: 0.85;
}

.btn-sm {
  height: var(--h-control-sm);
  padding: 0 10px;
  font-size: 13px;
  gap: 6px;
}

.icon-only {
  width: var(--h-control);
  padding: 0;
}

.btn-sm.icon-only {
  width: var(--h-control-sm);
}

.block {
  width: 100%;
}

.btn-primary {
  background: var(--accent);
  color: var(--on-accent);
}

.btn-primary:not(:disabled):hover {
  background: var(--accent-hover);
  box-shadow: 0 6px 16px color-mix(in srgb, var(--accent) 28%, transparent);
}

.btn-secondary {
  background: var(--surface);
  border-color: var(--border-strong);
  color: var(--text);
}

.btn-secondary:not(:disabled):hover {
  background: var(--surface-muted);
}

.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
}

.btn-ghost:not(:disabled):hover {
  background: var(--surface-muted);
  color: var(--text);
}

.btn-danger {
  background: var(--danger);
  color: #fff;
}

.btn-danger:not(:disabled):hover {
  background: color-mix(in srgb, var(--danger) 88%, #000);
  box-shadow: 0 6px 16px color-mix(in srgb, var(--danger) 28%, transparent);
}

.btn-danger-ghost {
  background: transparent;
  color: var(--danger-text);
}

.btn-danger-ghost:not(:disabled):hover {
  background: var(--danger-soft);
}

.btn-icon,
.btn-icon-right {
  flex-shrink: 0;
  transition: transform var(--dur) var(--ease);
}

.btn:not(:disabled):hover .btn-icon-right {
  transform: translateX(2px);
}

.spinner {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 2px solid currentColor;
  border-right-color: transparent;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
