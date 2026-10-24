<script setup lang="ts">
withDefaults(
  defineProps<{
    width?: string
    height?: string
    radius?: string
  }>(),
  {
    width: '100%',
    height: '14px',
    radius: '6px',
  },
)
</script>

<template>
  <span class="skeleton" :style="{ width, height, borderRadius: radius }" aria-hidden="true" />
</template>

<style scoped>
.skeleton {
  display: block;
  background: linear-gradient(
    90deg,
    var(--surface-muted) 0%,
    var(--surface-hover) 50%,
    var(--surface-muted) 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}
</style>
