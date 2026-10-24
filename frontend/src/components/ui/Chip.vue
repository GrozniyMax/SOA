<script setup lang="ts">
import { IconX } from '@tabler/icons-vue'
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    removable?: boolean
    active?: boolean
    dashed?: boolean
    icon?: Component
  }>(),
  {
    removable: false,
    active: false,
    dashed: false,
    icon: undefined,
  },
)

const emit = defineEmits<{
  click: []
  remove: []
}>()
</script>

<template>
  <span class="chip" :class="{ active, dashed }">
    <button type="button" class="chip-body" @click="emit('click')">
      <component :is="icon" v-if="icon" :size="14" stroke-width="2" />
      <slot />
    </button>
    <button v-if="removable" type="button" class="chip-remove" aria-label="Убрать" @click.stop="emit('remove')">
      <IconX :size="13" stroke-width="2.25" />
    </button>
  </span>
</template>

<style scoped>
.chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  border-radius: var(--radius-pill);
  background: var(--surface-muted);
  color: var(--text-secondary);
  font-size: 13px;
  transition:
    background var(--dur) var(--ease),
    color var(--dur) var(--ease),
    transform var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.chip:hover {
  background: var(--accent-soft);
  color: var(--accent-text);
  transform: translateY(-1px);
}

.chip.active {
  background: var(--accent-soft);
  color: var(--accent-text);
}

.chip.dashed {
  background: transparent;
  box-shadow: inset 0 0 0 1px var(--border-strong);
}

.chip.dashed:hover {
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 50%, transparent);
}

.chip-body {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 100%;
  padding: 0 12px;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.chip:has(.chip-remove) .chip-body {
  padding-right: 4px;
}

.chip-remove {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  margin-right: 4px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: inherit;
  cursor: pointer;
  transition:
    transform var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.chip-remove:hover {
  background: color-mix(in srgb, currentColor 14%, transparent);
  transform: rotate(90deg);
}
</style>
