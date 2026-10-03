<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { IconArrowsSort, IconMovie, IconSearch, IconZoomQuestion } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import type { MovieFilter } from '@/api/types'
import PageHeader from '@/components/layout/PageHeader.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import Chip from '@/components/ui/Chip.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import PopoverPanel from '@/components/ui/PopoverPanel.vue'
import FilterBar from '@/components/movies/FilterBar.vue'
import MovieTable from '@/components/movies/MovieTable.vue'
import SortEditor from '@/components/movies/SortEditor.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { toSearchRequest, useMovieSearchState, type SortOrder } from '@/composables/useMovieSearchState'
import { isFieldPath } from '@/domain/fields'
import { formatInteger, pluralize } from '@/domain/format'

const { state, update, reset } = useMovieSearchState()

const search = ref(state.value.q)
const sortOpen = ref(false)
let searchTimer: number | undefined

const request = computed(() => toSearchRequest(state.value))
const query = useAsyncData((signal) => moviesApi.search(request.value, signal))

watch(request, () => void query.reload(), { immediate: true, deep: true })

watch(
  () => state.value.q,
  (q) => {
    if (q !== search.value.trim()) search.value = q
  },
)

watch(search, (value) => {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    const q = value.trim()
    if (q !== state.value.q) update({ q })
  }, 300)
})

watch(
  () => query.data.value,
  (page) => {
    if (!page || page.items.length > 0 || page.total === 0 || state.value.page === 0) return
    const last = Math.max(0, Math.ceil(page.total / state.value.size) - 1)
    if (last !== state.value.page) update({ page: last })
  },
)

const movies = computed(() => query.data.value?.items ?? [])
const total = computed(() => query.data.value?.total ?? 0)
const hasConditions = computed(() => state.value.filters.length > 0 || state.value.q !== '')
const hasAnything = computed(() => hasConditions.value || state.value.sort.length > 0)

const subtitle = computed(() => {
  if (query.error.value) return 'Не удалось загрузить коллекцию'
  if (query.data.value === null) return 'Загрузка коллекции…'
  const count = total.value
  const word = pluralize(count, 'фильм', 'фильма', 'фильмов')
  return hasConditions.value ? `Найдено ${formatInteger(count)} ${word}` : `${formatInteger(count)} ${word} в коллекции`
})

function setFilters(filters: MovieFilter[]) {
  update({ filters })
}

function setSort(sort: SortOrder[]) {
  update({ sort })
}

function onHeaderSort(key: string, additive: boolean) {
  if (!isFieldPath(key)) return
  const current = state.value.sort
  const existing = current.find((s) => s.key === key)
  const nextDir = (s: SortOrder | undefined): SortOrder['dir'] | null =>
    s === undefined ? 'asc' : s.dir === 'asc' ? 'desc' : null

  const dir = nextDir(existing)
  if (!additive) {
    setSort(dir ? [{ key, dir }] : [])
    return
  }
  if (!existing) setSort([...current, { key, dir: 'asc' }])
  else if (dir) setSort(current.map((s) => (s.key === key ? { ...s, dir } : s)))
  else setSort(current.filter((s) => s.key !== key))
}

function resetAll() {
  search.value = ''
  reset()
}
</script>

<template>
  <section class="movies">
    <PageHeader title="Фильмы" :subtitle="subtitle" />

    <div class="toolbar">
      <BaseInput v-model="search" type="search" placeholder="Поиск по названию" :icon="IconSearch" class="search" />
      <FilterBar :filters="state.filters" @update:filters="setFilters" />
      <div class="toolbar-end">
        <PopoverPanel v-model:open="sortOpen" align="end" width="420px">
          <template #trigger>
            <Chip :active="sortOpen || state.sort.length > 0" :icon="IconArrowsSort" @click="sortOpen = !sortOpen">
              Сортировка<span v-if="state.sort.length" class="count tabular">{{ state.sort.length }}</span>
            </Chip>
          </template>
          <SortEditor :model-value="state.sort" @update:model-value="setSort" />
        </PopoverPanel>
        <Transition name="fade">
          <BaseButton v-if="hasAnything" size="sm" variant="ghost" @click="resetAll">Сбросить</BaseButton>
        </Transition>
      </div>
    </div>

    <ErrorState v-if="query.error.value" :error="query.error.value" class="error" @retry="query.reload" />

    <MovieTable
      v-if="!query.error.value || movies.length"
      :movies="movies"
      :loading="query.loading.value"
      :sort="state.sort"
      :generation="query.generation.value"
      @sort="onHeaderSort"
    >
      <template #empty>
        <EmptyState
          v-if="hasConditions"
          :icon="IconZoomQuestion"
          title="Ничего не найдено"
          text="Под условия поиска не подходит ни один фильм."
        >
          <BaseButton size="sm" @click="resetAll">Сбросить условия</BaseButton>
        </EmptyState>
        <EmptyState v-else :icon="IconMovie" title="Коллекция пуста" text="Добавьте первый фильм, и он появится здесь." />
      </template>
    </MovieTable>

    <PaginationBar
      v-if="total > 0"
      :page="state.page"
      :size="state.size"
      :total="total"
      :disabled="query.loading.value"
      @update:page="(page) => update({ page })"
      @update:size="(size) => update({ size })"
    />
  </section>
</template>

<style scoped>
.movies {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.movies :deep(.page-header) {
  margin-bottom: 4px;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.search {
  width: 260px;
}

.toolbar-end {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-left: auto;
}

.count {
  display: inline-grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  margin-left: 2px;
  padding: 0 5px;
  border-radius: var(--radius-pill);
  background: var(--accent);
  color: var(--on-accent);
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 640px) {
  .search {
    width: 100%;
  }
}
</style>
