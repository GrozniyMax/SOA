<script setup lang="ts">
const model = defineModel<boolean>({ default: false })

defineProps<{
  label: string
  hint?: string
}>()
</script>

<template>
  <label class="toggle">
    <input v-model="model" type="checkbox" role="switch" class="sr-only" />
    <span class="track" :class="{ on: model }" aria-hidden="true">
      <span class="thumb" />
    </span>
    <span class="text">
      <span class="label">{{ label }}</span>
      <span v-if="hint" class="hint">{{ hint }}</span>
    </span>
  </label>
</template>

<style scoped>
.toggle {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.track {
  position: relative;
  flex-shrink: 0;
  width: 34px;
  height: 20px;
  margin-top: 1px;
  border-radius: var(--radius-pill);
  background: var(--border-strong);
  transition: background var(--dur) var(--ease);
}

.track.on {
  background: var(--accent);
}

.thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  transition: transform var(--dur-slow) var(--ease-spring);
}

.track.on .thumb {
  transform: translateX(14px);
}

input:focus-visible + .track {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.text {
  display: flex;
  flex-direction: column;
}

.label {
  font-size: 13px;
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
