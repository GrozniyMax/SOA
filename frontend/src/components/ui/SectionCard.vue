<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    icon?: Component
    endpoint?: string
  }>(),
  {
    description: undefined,
    icon: undefined,
    endpoint: undefined,
  },
)
</script>

<template>
  <section class="section-card">
    <header class="section-head">
      <span v-if="icon" class="section-icon">
        <component :is="icon" :size="18" stroke-width="1.9" />
      </span>
      <div class="section-titles">
        <h2>{{ title }}</h2>
        <p v-if="description" class="secondary">{{ description }}</p>
        <code v-if="endpoint" class="endpoint">{{ endpoint }}</code>
      </div>
      <div v-if="$slots.actions" class="section-actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="section-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.section-card {
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  transition: border-color var(--dur) var(--ease);
}

.section-card:hover {
  border-color: var(--border-strong);
}

.section-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 20px 0;
  flex-wrap: wrap;
}

.section-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent-text);
  transition: transform var(--dur-slow) var(--ease-spring);
}

.section-card:hover .section-icon {
  transform: rotate(-8deg) scale(1.06);
}

.section-titles {
  flex: 1;
  min-width: 200px;
}

.section-titles h2 {
  font-size: 16px;
}

.section-titles p {
  margin-top: 2px;
  font-size: 13px;
}

.endpoint {
  display: inline-block;
  margin-top: 6px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  font-size: 11.5px;
}

.section-actions {
  display: flex;
  gap: 6px;
  align-self: center;
}

.section-body {
  padding: 16px 20px 20px;
}
</style>
