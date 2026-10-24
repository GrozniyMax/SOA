<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<template>
  <header class="page-header">
    <div class="titles">
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="secondary">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.actions" class="actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}

h1 {
  font-size: 24px;
  line-height: 1.2;
}

.titles p {
  margin-top: 4px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
