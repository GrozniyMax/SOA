<script setup lang="ts" generic="T">
import { computed } from 'vue'
import { IconArrowDown, IconArrowUp, IconArrowsSort, IconChevronRight } from '@tabler/icons-vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'

export interface TableColumn<Row> {
  key: string
  label: string
  align?: 'left' | 'right' | 'center'
  width?: string
  sortable?: boolean
  value?: (row: Row) => string
  muted?: boolean
}

export interface TableSort {
  key: string
  dir: 'asc' | 'desc'
}

const props = withDefaults(
  defineProps<{
    columns: TableColumn<T>[]
    rows: readonly T[]
    rowKey: (row: T) => string | number
    loading?: boolean
    sort?: readonly TableSort[]
    clickable?: boolean
    skeletonRows?: number
    generation?: number
    minWidth?: string
  }>(),
  {
    minWidth: '720px',
    loading: false,
    sort: () => [],
    clickable: false,
    skeletonRows: 6,
    generation: 0,
  },
)

const emit = defineEmits<{
  sort: [key: string]
  rowClick: [row: T]
}>()

defineSlots<
  {
    empty?: () => unknown
  } & Record<`cell-${string}`, (props: { row: T; index: number }) => unknown>
>()

const sortIndex = computed(() => {
  const map = new Map<string, { dir: 'asc' | 'desc'; order: number }>()
  props.sort.forEach((s, i) => map.set(s.key, { dir: s.dir, order: i + 1 }))
  return map
})

const showSkeleton = computed(() => props.loading && props.rows.length === 0)

function onHeaderClick(column: TableColumn<T>) {
  if (!column.sortable) return
  emit('sort', column.key)
}

function ariaSort(column: TableColumn<T>): 'ascending' | 'descending' | 'none' | undefined {
  if (!column.sortable) return undefined
  const s = sortIndex.value.get(column.key)
  if (!s) return 'none'
  return s.dir === 'asc' ? 'ascending' : 'descending'
}

function cellText(column: TableColumn<T>, row: T): string {
  if (column.value) return column.value(row)
  const raw = (row as Record<string, unknown>)[column.key]
  return raw === null || raw === undefined || raw === '' ? '—' : String(raw)
}
</script>

<template>
  <div class="table-card" :class="{ busy: loading && rows.length > 0 }">
    <div class="table-scroll">
      <table :style="{ minWidth }">
        <colgroup>
          <col v-for="column in columns" :key="column.key" :style="column.width ? { width: column.width } : undefined" />
          <col v-if="clickable" style="width: 36px" />
        </colgroup>
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :class="[`align-${column.align ?? 'left'}`, { sortable: column.sortable, sorted: sortIndex.has(column.key) }]"
              :aria-sort="ariaSort(column)"
              scope="col"
            >
              <button
                v-if="column.sortable"
                type="button"
                class="th-sort"
                :title="'Сортировать'"
                @click="onHeaderClick(column)"
              >
                <span>{{ column.label }}</span>
                <span class="sort-icon">
                  <IconArrowUp v-if="sortIndex.get(column.key)?.dir === 'asc'" :size="14" stroke-width="2.25" />
                  <IconArrowDown v-else-if="sortIndex.get(column.key)?.dir === 'desc'" :size="14" stroke-width="2.25" />
                  <IconArrowsSort v-else :size="14" stroke-width="2" class="sort-idle" />
                  <sup v-if="sort.length > 1 && sortIndex.get(column.key)" class="sort-order">
                    {{ sortIndex.get(column.key)?.order }}
                  </sup>
                </span>
              </button>
              <span v-else>{{ column.label }}</span>
            </th>
            <th v-if="clickable" aria-hidden="true" />
          </tr>
        </thead>
        <tbody v-if="showSkeleton">
          <tr v-for="n in skeletonRows" :key="`sk-${n}`" class="skeleton-row">
            <td v-for="column in columns" :key="column.key" :class="`align-${column.align ?? 'left'}`">
              <SkeletonBlock :width="column.align === 'right' ? '48px' : `${50 + ((n * 37) % 40)}%`" />
            </td>
            <td v-if="clickable" />
          </tr>
        </tbody>
        <tbody v-else :key="generation">
          <tr
            v-for="(row, index) in rows"
            :key="rowKey(row)"
            class="data-row"
            :class="{ clickable }"
            :style="{ animationDelay: `${Math.min(index, 14) * 22}ms` }"
            :tabindex="clickable ? 0 : undefined"
            @click="clickable && emit('rowClick', row)"
            @keydown.enter="clickable && emit('rowClick', row)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              :class="[`align-${column.align ?? 'left'}`, { muted: column.muted }]"
            >
              <slot :name="`cell-${column.key}`" :row="row" :index="index">{{ cellText(column, row) }}</slot>
            </td>
            <td v-if="clickable" class="row-go">
              <IconChevronRight :size="16" stroke-width="2" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="!loading && rows.length === 0" class="table-empty">
      <slot name="empty" />
    </div>
    <div class="busy-bar" aria-hidden="true" />
  </div>
</template>

<style scoped>
.table-card {
  position: relative;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  overflow: hidden;
}

.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

th {
  position: sticky;
  top: 0;
  height: 40px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  text-align: left;
  white-space: nowrap;
}

th.sorted {
  color: var(--accent-text);
}

.th-sort {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  height: 26px;
  margin: 0 -6px;
  padding: 0 6px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
  transition:
    background var(--dur) var(--ease),
    color var(--dur) var(--ease);
}

.th-sort:hover {
  background: var(--surface-muted);
  color: var(--text);
}

.th-sort > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
}

.sort-icon {
  display: inline-flex;
  align-items: center;
}

.sort-idle {
  opacity: 0;
  transition: opacity var(--dur) var(--ease);
}

.th-sort:hover .sort-idle {
  opacity: 0.7;
}

.sort-order {
  margin-left: 1px;
  font-size: 10px;
  font-weight: 600;
}

td {
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background var(--dur) var(--ease);
}

tbody tr:last-child td {
  border-bottom: none;
}

.align-right {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

th.align-right .th-sort {
  flex-direction: row-reverse;
}

.align-center {
  text-align: center;
}

td.muted {
  color: var(--text-muted);
}

.data-row {
  animation: rise-in var(--dur-slow) var(--ease) both;
}

.data-row.clickable {
  cursor: pointer;
  outline: none;
}

.data-row.clickable:hover td,
.data-row.clickable:focus-visible td {
  background: var(--surface-muted);
}

.data-row.clickable:focus-visible td:first-child {
  box-shadow: inset 3px 0 0 var(--accent);
}

.data-row td:first-child {
  transition:
    background var(--dur) var(--ease),
    padding var(--dur) var(--ease);
}

.data-row.clickable:hover td:first-child {
  padding-left: 18px;
}

.row-go {
  padding: 0 10px 0 0;
  color: var(--accent);
  text-align: right;
}

.row-go svg {
  opacity: 0;
  transform: translateX(-6px);
  transition:
    opacity var(--dur) var(--ease),
    transform var(--dur) var(--ease);
}

.data-row:hover .row-go svg,
.data-row:focus-visible .row-go svg {
  opacity: 1;
  transform: none;
}

.skeleton-row td {
  height: 48px;
}

.align-right :deep(.skeleton) {
  margin-left: auto;
}

.table-empty {
  border-top: 1px solid var(--border);
}

.busy-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--accent), transparent);
  background-size: 40% 100%;
  background-repeat: no-repeat;
  opacity: 0;
  transition: opacity var(--dur) var(--ease);
}

.busy .busy-bar {
  opacity: 1;
  animation: busy 1.1s var(--ease) infinite;
}

.busy tbody {
  opacity: 0.55;
  transition: opacity var(--dur) var(--ease);
}

@keyframes busy {
  from {
    background-position: -40% 0;
  }
  to {
    background-position: 140% 0;
  }
}
</style>
