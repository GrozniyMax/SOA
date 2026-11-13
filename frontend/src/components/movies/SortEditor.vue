<script setup lang="ts">
import { computed } from 'vue'
import { IconArrowDown, IconArrowUp, IconChevronUp, IconPlus, IconX } from '@tabler/icons-vue'
import type { MovieFieldPath } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import type { SortOrder } from '@/composables/useMovieSearchState'
import { FIELDS, FIELD_LIST } from '@/domain/fields'

const props = defineProps<{
  modelValue: readonly SortOrder[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SortOrder[]]
}>()

const available = computed(() => FIELD_LIST.filter((f) => !props.modelValue.some((s) => s.key === f.path)))

function optionsFor(current: MovieFieldPath) {
  return FIELD_LIST.filter((f) => f.path === current || !props.modelValue.some((s) => s.key === f.path)).map(
    (f) => ({ value: f.path, label: f.label }),
  )
}

function commit(next: SortOrder[]) {
  emit('update:modelValue', next)
}

function setKey(index: number, key: string) {
  if (!(key in FIELDS)) return
  commit(props.modelValue.map((s, i) => (i === index ? { ...s, key: key as MovieFieldPath } : s)))
}

function toggleDir(index: number) {
  commit(props.modelValue.map((s, i) => (i === index ? { ...s, dir: s.dir === 'asc' ? 'desc' : 'asc' } : s)))
}

function remove(index: number) {
  commit(props.modelValue.filter((_, i) => i !== index))
}

function moveUp(index: number) {
  if (index === 0) return
  const next = [...props.modelValue]
  const [item] = next.splice(index, 1)
  if (item) next.splice(index - 1, 0, item)
  commit(next)
}

function add() {
  const field = available.value[0]
  if (field) commit([...props.modelValue, { key: field.path, dir: 'asc' }])
}
</script>

<template>
  <div class="sort-editor">
    <p v-if="modelValue.length === 0" class="hint">Без сортировки. Добавьте поле или нажмите на заголовок колонки.</p>
    <TransitionGroup tag="ol" name="sort-row" class="list">
      <li v-for="(order, index) in modelValue" :key="order.key" class="sort-row">
        <span class="order tabular">{{ index + 1 }}</span>
        <BaseSelect
          :model-value="order.key"
          :options="optionsFor(order.key)"
          size="sm"
          class="field"
          @update:model-value="setKey(index, $event)"
        />
        <BaseButton
          size="sm"
          variant="secondary"
          :icon="order.dir === 'asc' ? IconArrowUp : IconArrowDown"
          class="dir"
          @click="toggleDir(index)"
        >
          {{ order.dir === 'asc' ? 'Возр.' : 'Убыв.' }}
        </BaseButton>
        <BaseButton
          size="sm"
          variant="ghost"
          icon-only
          :icon="IconChevronUp"
          label="Выше по приоритету"
          :disabled="index === 0"
          @click="moveUp(index)"
        />
        <BaseButton size="sm" variant="ghost" icon-only :icon="IconX" label="Убрать" @click="remove(index)" />
      </li>
    </TransitionGroup>
    <BaseButton v-if="available.length" size="sm" variant="ghost" :icon="IconPlus" class="add" @click="add">
      Добавить поле
    </BaseButton>
  </div>
</template>

<style scoped>
.sort-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hint {
  font-size: 13px;
  color: var(--text-secondary);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.sort-row {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 6px;
}

.order {
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}

.dir {
  min-width: 78px;
}

.add {
  align-self: flex-start;
}

.sort-row-move,
.sort-row-enter-active,
.sort-row-leave-active {
  transition:
    transform var(--dur-slow) var(--ease),
    opacity var(--dur) var(--ease);
}

.sort-row-enter-from,
.sort-row-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.sort-row-leave-active {
  position: absolute;
}
</style>
