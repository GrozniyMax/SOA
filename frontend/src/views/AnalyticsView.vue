<script setup lang="ts">
import PageHeader from '@/components/layout/PageHeader.vue'
import MaxTaglineSection from '@/components/analytics/MaxTaglineSection.vue'
import DirectorsGreaterSection from '@/components/analytics/DirectorsGreaterSection.vue'
import GenresSection from '@/components/analytics/GenresSection.vue'
</script>

<template>
  <section class="analytics">
    <PageHeader title="Аналитика" subtitle="Дополнительные запросы к коллекции фильмов" />
    <div class="grid">
      <MaxTaglineSection />
      <GenresSection />
    </div>
    <DirectorsGreaterSection />
  </section>
</template>

<style scoped>
.analytics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analytics :deep(.page-header) {
  margin-bottom: 4px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
  align-items: start;
}

@media (max-width: 520px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
