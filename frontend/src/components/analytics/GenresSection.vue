<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { IconCategory, IconRefresh } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import { MOVIE_GENRES, type MovieGenre } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import GenreTag from '@/components/ui/GenreTag.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { useCountUp } from '@/composables/useCountUp'
import { GENRE_LABELS } from '@/domain/labels'
import { pluralize } from '@/domain/format'

const page = ref(0)
const size = ref(20)

const query = useAsyncData((signal) => moviesApi.getUniqueGenres({ page: page.value, size: size.value }, signal))

onMounted(() => void query.reload())
watch([page, size], () => void query.reload())

const genres = computed<readonly MovieGenre[]>(() => query.data.value?.items ?? [])
const total = computed(() => query.data.value?.total ?? null)
const animatedTotal = useCountUp(total)

const missing = computed(() => {
  if (!query.data.value || query.data.value.total > query.data.value.items.length) return []
  return MOVIE_GENRES.filter((g) => !genres.value.includes(g))
})

const summary = computed(() => {
  const n = total.value
  if (n === null) return ''
  if (n === 0) return 'В коллекции пока нет фильмов, поэтому жанров тоже нет.'
  const word = pluralize(n, 'жанр', 'жанра', 'жанров')
  const base = `В коллекции представлено ${n} ${word} из ${MOVIE_GENRES.length} возможных.`
  if (missing.value.length === 0) return base
  return `${base} Нет фильмов в жанре ${missing.value.map((g) => `«${GENRE_LABELS[g].toLowerCase()}»`).join(', ')}.`
})
</script>

<template>
  <SectionCard
    title="Жанры коллекции"
    description="Уникальные значения поля genre по всем фильмам"
    :icon="IconCategory"
    endpoint="GET /movies/genres"
  >
    <template #actions>
      <BaseButton size="sm" variant="ghost" icon-only :icon="IconRefresh" label="Обновить" :loading="query.loading.value" @click="query.reload" />
    </template>

    <ErrorState v-if="query.error.value" :error="query.error.value" compact @retry="query.reload" />
    <div v-else-if="!query.data.value" class="loading">
      <SkeletonBlock width="48px" height="34px" radius="8px" />
      <SkeletonBlock width="70%" />
    </div>
    <EmptyState v-else-if="total === 0" :icon="IconCategory" title="Жанров нет" text="Добавьте фильмы, и здесь появятся их жанры." />
    <div v-else class="genres">
      <div class="headline">
        <span class="big tabular">{{ animatedTotal }}</span>
        <p class="summary">{{ summary }}</p>
      </div>
      <TransitionGroup tag="ul" name="chip" class="list" :key="query.generation.value">
        <li v-for="(genre, index) in genres" :key="genre" class="item" :style="{ animationDelay: `${index * 60}ms` }">
          <GenreTag :genre="genre" />
        </li>
      </TransitionGroup>
      <PaginationBar
        v-model:page="page"
        :size="size"
        :total="total ?? 0"
        :size-options="[1, 2, 5, 20]"
        :disabled="query.loading.value"
        @update:size="(s) => ((size = s), (page = 0))"
      />
    </div>
  </SectionCard>
</template>

<style scoped>
.loading {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.genres {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.headline {
  display: flex;
  align-items: center;
  gap: 14px;
}

.big {
  font-size: 34px;
  font-weight: 600;
  letter-spacing: -0.03em;
  line-height: 1;
  color: var(--accent-text);
}

.summary {
  color: var(--text-secondary);
  max-width: 560px;
}

.list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  animation: rise-in var(--dur-slow) var(--ease) both;
}

.item :deep(.genre) {
  height: 30px;
  padding: 0 14px 0 12px;
  font-size: 13px;
  transition: transform var(--dur) var(--ease);
}

.item:hover :deep(.genre) {
  transform: translateY(-2px) scale(1.04);
}
</style>
