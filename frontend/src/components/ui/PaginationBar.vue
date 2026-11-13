<script setup lang="ts">
import { computed } from 'vue'
import { IconChevronLeft, IconChevronRight } from '@tabler/icons-vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import { formatInteger } from '@/domain/format'

const props = withDefaults(
  defineProps<{
    page: number
    size: number
    total: number
    sizeOptions?: readonly number[]
    disabled?: boolean
  }>(),
  {
    sizeOptions: () => [10, 20, 50, 100],
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:page': [page: number]
  'update:size': [size: number]
}>()

const lastPage = computed(() => Math.max(0, Math.ceil(props.total / props.size) - 1))

const range = computed(() => {
  if (props.total === 0) return null
  const from = props.page * props.size + 1
  const to = Math.min(props.total, (props.page + 1) * props.size)
  return { from, to }
})

type PageItem = { kind: 'page'; index: number } | { kind: 'gap'; key: string }

const items = computed<PageItem[]>(() => {
  const last = lastPage.value
  const current = props.page
  const pages = new Set<number>([0, last, current - 1, current, current + 1])
  if (current <= 2) [1, 2, 3].forEach((p) => pages.add(p))
  if (current >= last - 2) [last - 1, last - 2, last - 3].forEach((p) => pages.add(p))
  const sorted = [...pages].filter((p) => p >= 0 && p <= last).sort((a, b) => a - b)
  const result: PageItem[] = []
  sorted.forEach((p, i) => {
    const prev = sorted[i - 1]
    if (prev !== undefined && p - prev > 1) result.push({ kind: 'gap', key: `gap-${prev}` })
    result.push({ kind: 'page', index: p })
  })
  return result
})

const sizeSelectOptions = computed(() =>
  props.sizeOptions.map((s) => ({ value: String(s), label: `${s} на странице` })),
)

const sizeModel = computed({
  get: () => String(props.size),
  set: (value: string) => emit('update:size', Number(value)),
})

function go(index: number) {
  if (props.disabled) return
  const target = Math.min(Math.max(index, 0), lastPage.value)
  if (target !== props.page) emit('update:page', target)
}
</script>

<template>
  <div class="pagination">
    <p class="pagination-range secondary tabular">
      <template v-if="range">{{ formatInteger(range.from) }}–{{ formatInteger(range.to) }} из {{ formatInteger(total) }}</template>
      <template v-else>Нет записей</template>
    </p>
    <div class="pagination-controls">
      <BaseSelect v-model="sizeModel" :options="sizeSelectOptions" size="sm" class="size-select" />
      <nav class="pages" aria-label="Страницы">
        <button type="button" class="page-btn" :disabled="disabled || page <= 0" aria-label="Предыдущая страница" @click="go(page - 1)">
          <IconChevronLeft :size="16" stroke-width="2" />
        </button>
        <template v-for="item in items" :key="item.kind === 'page' ? item.index : item.key">
          <span v-if="item.kind === 'gap'" class="page-gap">…</span>
          <button
            v-else
            type="button"
            class="page-btn tabular"
            :class="{ current: item.index === page }"
            :aria-current="item.index === page ? 'page' : undefined"
            :disabled="disabled"
            @click="go(item.index)"
          >
            {{ item.index + 1 }}
          </button>
        </template>
        <button type="button" class="page-btn" :disabled="disabled || page >= lastPage" aria-label="Следующая страница" @click="go(page + 1)">
          <IconChevronRight :size="16" stroke-width="2" />
        </button>
      </nav>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pagination-range {
  font-size: 13px;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.size-select {
  width: 160px;
}

.pages {
  display: flex;
  align-items: center;
  gap: 2px;
}

.page-btn {
  display: grid;
  place-items: center;
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition:
    background var(--dur) var(--ease),
    color var(--dur) var(--ease),
    transform var(--dur-fast) var(--ease);
}

.page-btn:not(:disabled):hover {
  background: var(--surface-muted);
  color: var(--text);
}

.page-btn:not(:disabled):active {
  transform: scale(0.92);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-btn.current {
  background: var(--accent);
  color: var(--on-accent);
  font-weight: 600;
}

.page-btn.current:hover {
  background: var(--accent-hover);
  color: var(--on-accent);
}

.page-gap {
  min-width: 20px;
  text-align: center;
  color: var(--text-muted);
}
</style>
