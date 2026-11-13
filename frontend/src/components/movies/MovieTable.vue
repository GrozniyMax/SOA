<script setup lang="ts">
import type { Movie } from '@/api/types'
import DataTable, { type TableColumn, type TableSort } from '@/components/ui/DataTable.vue'
import GenreTag from '@/components/ui/GenreTag.vue'
import { formatDate, formatInteger, formatMoney } from '@/domain/format'

withDefaults(
  defineProps<{
    movies: readonly Movie[]
    loading?: boolean
    sort?: readonly TableSort[]
    generation?: number
    clickable?: boolean
    sortable?: boolean
    compact?: boolean
  }>(),
  {
    loading: false,
    sort: () => [],
    generation: 0,
    clickable: false,
    sortable: true,
    compact: false,
  },
)

const emit = defineEmits<{
  sort: [key: string]
  open: [movie: Movie]
}>()

defineSlots<{ empty?: () => unknown }>()

function columns(sortable: boolean, compact: boolean): TableColumn<Movie>[] {
  const all: TableColumn<Movie>[] = [
    { key: 'id', label: 'ID', width: '72px', align: 'right', sortable, muted: true },
    { key: 'name', label: 'Название', sortable },
    { key: 'genre', label: 'Жанр', width: '150px', sortable },
    { key: 'director.name', label: 'Режиссёр', width: '200px', sortable, value: (m) => m.director.name },
    { key: 'oscarsCount', label: 'Оскары', width: '112px', align: 'right', sortable, value: (m) => formatInteger(m.oscarsCount) },
    { key: 'totalBoxOffice', label: 'Сборы', width: '150px', align: 'right', sortable, value: (m) => formatMoney(m.totalBoxOffice) },
    { key: 'creationDate', label: 'Создан', width: '120px', align: 'right', sortable, value: (m) => formatDate(m.creationDate) },
  ]
  return compact ? all.filter((c) => c.key !== 'creationDate') : all
}
</script>

<template>
  <DataTable
    :columns="columns(sortable, compact)"
    :rows="movies"
    :row-key="(m) => m.id"
    :loading="loading"
    :sort="sort"
    :generation="generation"
    :clickable="clickable"
    :min-width="compact ? '880px' : '1040px'"
    @sort="(key) => emit('sort', key)"
    @row-click="(m) => emit('open', m)"
  >
    <template #cell-name="{ row }">
      <span class="name" :title="row.name">{{ row.name }}</span>
    </template>
    <template #cell-genre="{ row }">
      <GenreTag :genre="row.genre" />
    </template>
    <template #cell-oscarsCount="{ row }">
      <span :class="{ empty: !row.oscarsCount }">{{ formatInteger(row.oscarsCount) }}</span>
    </template>
    <template #cell-totalBoxOffice="{ row }">
      <span :class="{ empty: row.totalBoxOffice == null }">{{ formatMoney(row.totalBoxOffice) }}</span>
    </template>
    <template #empty>
      <slot name="empty" />
    </template>
  </DataTable>
</template>

<style scoped>
.name {
  font-weight: 500;
}

.empty {
  color: var(--text-muted);
}
</style>
