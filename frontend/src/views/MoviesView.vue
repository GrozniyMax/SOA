<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { IconArrowsSort, IconMovie, IconPlus, IconSearch, IconZoomQuestion } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import type { Movie, MovieFilter } from '@/api/types'
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
import MovieDetailsDrawer from '@/components/movies/MovieDetailsDrawer.vue'
import MovieFormDrawer from '@/components/movies/MovieFormDrawer.vue'
import OpenByIdPopover from '@/components/movies/OpenByIdPopover.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { toSearchRequest, useMovieSearchState, type SortOrder } from '@/composables/useMovieSearchState'
import { isFieldPath } from '@/domain/fields'

const { state, update, reset } = useMovieSearchState()
const route = useRoute()
const router = useRouter()

const search = ref(state.value.q)
const sortOpen = ref(false)
let searchTimer: number | undefined

const request = computed(() => toSearchRequest(state.value))
const requestKey = computed(() => JSON.stringify(request.value))
const query = useAsyncData((signal) => moviesApi.search(request.value, signal))

watch(requestKey, () => void query.reload(), { immediate: true })

const detailsId = ref<number | null>(null)
const formOpen = ref(false)
const editing = ref<Movie | null>(null)

const routeMovieId = computed(() => {
  const raw = route.query.movie
  const value = Array.isArray(raw) ? raw[0] : raw
  return typeof value === 'string' && /^\d+$/.test(value) ? Number(value) : null
})

watch(
  routeMovieId,
  (id) => {
    if (id !== null) detailsId.value = id
  },
  { immediate: true },
)

const detailsOpen = computed({
  get: () => routeMovieId.value !== null,
  set: (value: boolean) => {
    if (!value) setMovieParam(null)
  },
})

function setMovieParam(id: number | null) {
  const next = { ...route.query }
  if (id === null) delete next.movie
  else next.movie = String(id)
  void router.replace({ query: next })
}

function openDetails(id: number) {
  setMovieParam(id)
}

function openCreate() {
  editing.value = null
  formOpen.value = true
}

function openEdit(movie: Movie) {
  editing.value = movie
  setMovieParam(null)
  formOpen.value = true
}

function onSaved(movie: Movie) {
  void query.reload()
  setMovieParam(movie.id)
}

function onDeleted() {
  void query.reload()
}

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

function setFilters(filters: MovieFilter[]) {
  update({ filters })
}

function setSort(sort: SortOrder[]) {
  update({ sort })
}

function onHeaderSort(key: string) {
  if (!isFieldPath(key)) return
  const current = state.value.sort
  const existing = current.find((s) => s.key === key)
  const dir = existing === undefined ? 'asc' : existing.dir === 'asc' ? 'desc' : null

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
    <PageHeader title="Фильмы">
      <template #actions>
        <OpenByIdPopover @open="openDetails" />
        <BaseButton variant="primary" :icon="IconPlus" @click="openCreate">Добавить</BaseButton>
      </template>
    </PageHeader>

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
      clickable
      @sort="onHeaderSort"
      @open="(movie) => openDetails(movie.id)"
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
        <EmptyState v-else :icon="IconMovie" title="Коллекция пуста" text="Добавьте первый фильм, и он появится здесь.">
          <BaseButton size="sm" variant="primary" :icon="IconPlus" @click="openCreate">Добавить фильм</BaseButton>
        </EmptyState>
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

    <MovieDetailsDrawer v-model:open="detailsOpen" :movie-id="detailsId" @edit="openEdit" @deleted="onDeleted" />
    <MovieFormDrawer v-model:open="formOpen" :movie="editing" @saved="onSaved" />
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
