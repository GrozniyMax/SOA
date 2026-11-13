<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title: string
    text?: string
    icon?: Component
  }>(),
  {
    text: undefined,
    icon: undefined,
  },
)
</script>

<template>
  <div class="empty rise-in">
    <span v-if="icon" class="empty-icon">
      <component :is="icon" :size="22" stroke-width="1.75" />
    </span>
    <p class="empty-title">{{ title }}</p>
    <p v-if="text" class="empty-text">{{ text }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 44px 16px;
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin-bottom: 6px;
  border-radius: 14px;
  background: var(--accent-soft);
  color: var(--accent-text);
}

.empty-title {
  font-weight: 600;
}

.empty-text {
  max-width: 380px;
  color: var(--text-secondary);
}

.empty-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}
</style>
