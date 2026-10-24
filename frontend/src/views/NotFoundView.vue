<script setup lang="ts">
import { useRouter } from 'vue-router'
import { IconMapQuestion, IconMovie } from '@tabler/icons-vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <section class="not-found">
    <EmptyState :icon="IconMapQuestion" title="Страница не найдена" text="Такого раздела нет. Вернитесь к списку фильмов.">
      <BaseButton variant="primary" :icon="IconMovie" @click="router.push('/movies')">К фильмам</BaseButton>
    </EmptyState>
  </section>
</template>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
  min-height: 60vh;
}
</style>
