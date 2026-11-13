<script setup lang="ts">
import { computed, ref } from 'vue'
import { IconFilter, IconPlus } from '@tabler/icons-vue'
import type { MovieFilter } from '@/api/types'
import Chip from '@/components/ui/Chip.vue'
import PopoverPanel from '@/components/ui/PopoverPanel.vue'
import FilterEditor from '@/components/movies/FilterEditor.vue'
import { FIELDS, formatFilterValue } from '@/domain/fields'
import { COMPARATOR_LABELS } from '@/domain/labels'

const props = defineProps<{
  filters: readonly MovieFilter[]
}>()

const emit = defineEmits<{
  'update:filters': [filters: MovieFilter[]]
}>()

const open = ref(false)
const editingIndex = ref<number | null>(null)
const editorKey = ref(0)

const chipKeys = computed(() => {
  const seen = new Map<string, number>()
  return props.filters.map((f) => {
    const base = `${f.path}|${f.comparator ?? 'eq'}|${f.value}`
    const n = seen.get(base) ?? 0
    seen.set(base, n + 1)
    return `${base}|${n}`
  })
})

function describe(filter: MovieFilter) {
  const cmp = COMPARATOR_LABELS[filter.comparator ?? 'eq']
  return { field: FIELDS[filter.path].label, rest: `${cmp} ${formatFilterValue(filter.path, filter.value)}` }
}

function openAdd() {
  editingIndex.value = null
  editorKey.value++
  open.value = true
}

function openEdit(index: number) {
  editingIndex.value = index
  editorKey.value++
  open.value = true
}

function apply(filter: MovieFilter) {
  const next = [...props.filters]
  if (editingIndex.value === null) next.push(filter)
  else next.splice(editingIndex.value, 1, filter)
  emit('update:filters', next)
  open.value = false
}

function remove(index: number) {
  emit(
    'update:filters',
    props.filters.filter((_, i) => i !== index),
  )
}
</script>

<template>
  <div class="filter-bar">
    <TransitionGroup name="chip">
      <Chip
        v-for="(filter, index) in filters"
        :key="chipKeys[index]"
        :active="open && editingIndex === index"
        removable
        @click="openEdit(index)"
        @remove="remove(index)"
      >
        <span class="chip-field">{{ describe(filter).field }}</span>
        <span class="chip-value">{{ describe(filter).rest }}</span>
      </Chip>
    </TransitionGroup>
    <PopoverPanel v-model:open="open">
      <template #trigger>
        <Chip dashed :active="open && editingIndex === null" :icon="filters.length ? IconPlus : IconFilter" @click="openAdd">
          {{ filters.length ? 'Ещё фильтр' : 'Фильтр' }}
        </Chip>
      </template>
      <FilterEditor
        :key="editorKey"
        :initial="editingIndex === null ? null : filters[editingIndex]"
        @apply="apply"
        @cancel="open = false"
      />
    </PopoverPanel>
  </div>
</template>

<style scoped>
.filter-bar {
  position: relative;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.chip-field {
  color: var(--text);
  font-weight: 500;
}

.chip-value {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
