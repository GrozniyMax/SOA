<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { IconArrowRight, IconQuote, IconRefresh, IconSortDescendingLetters } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import { ApiException } from '@/api/http'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import GenreTag from '@/components/ui/GenreTag.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import { useAsyncData } from '@/composables/useAsyncData'

const query = useAsyncData((signal) => moviesApi.getWithMaxTagline(signal))

onMounted(() => void query.reload())

const notFound = computed(() => query.error.value instanceof ApiException && query.error.value.isNotFound)
const movie = computed(() => query.data.value)
</script>

<template>
  <SectionCard
    title="Максимальный слоган"
    description="Фильм с максимальным значением tagline при сравнении строк"
    :icon="IconSortDescendingLetters"
  >
    <template #actions>
      <BaseButton size="sm" variant="ghost" icon-only :icon="IconRefresh" label="Обновить" :loading="query.loading.value" @click="query.reload" />
    </template>

    <EmptyState
      v-if="notFound"
      :icon="IconQuote"
      title="Слоганов пока нет"
      text="Ни у одного фильма в коллекции не заполнен слоган."
    />
    <ErrorState v-else-if="query.error.value" :error="query.error.value" compact @retry="query.reload" />
    <div v-else-if="!movie" class="loading">
      <SkeletonBlock height="56px" radius="12px" />
      <SkeletonBlock width="60%" />
    </div>
    <div v-else :key="movie.id" class="result rise-in">
      <blockquote class="quote">
        <IconQuote :size="22" stroke-width="1.5" class="quote-mark" />
        <p>{{ movie.tagline }}</p>
      </blockquote>
      <div class="meta">
        <GenreTag :genre="movie.genre" />
        <RouterLink :to="{ path: '/movies', query: { movie: String(movie.id) } }" class="open-link">
          Открыть карточку
          <IconArrowRight :size="15" stroke-width="2" />
        </RouterLink>
      </div>
    </div>
  </SectionCard>
</template>

<style scoped>
.loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quote {
  position: relative;
  margin: 0;
  padding: 16px 18px 16px 48px;
  border-radius: var(--radius);
  background: var(--accent-soft);
  color: var(--accent-text);
  font-size: 17px;
  font-style: italic;
  line-height: 1.5;
}

.quote-mark {
  position: absolute;
  top: 16px;
  left: 16px;
  opacity: 0.7;
}

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.open-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.open-link svg {
  transition: transform var(--dur) var(--ease);
}

.open-link:hover svg {
  transform: translateX(3px);
}
</style>
