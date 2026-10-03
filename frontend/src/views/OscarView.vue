<script setup lang="ts">
import { ref } from 'vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import HumiliateSection from '@/components/oscar/HumiliateSection.vue'
import LosersSection from '@/components/oscar/LosersSection.vue'

const losers = ref<InstanceType<typeof LosersSection> | null>(null)

function onHumiliated() {
  void losers.value?.reload()
}
</script>

<template>
  <section class="oscar">
    <PageHeader title="Оскары" subtitle="Операции сервиса оскаров поверх коллекции фильмов" />
    <div class="grid">
      <HumiliateSection @done="onHumiliated" />
      <LosersSection ref="losers" />
    </div>
  </section>
</template>

<style scoped>
.oscar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.oscar :deep(.page-header) {
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
