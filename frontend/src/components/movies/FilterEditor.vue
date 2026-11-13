<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import type { Comparator, MovieFieldPath, MovieFilter } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import FormField from '@/components/ui/FormField.vue'
import { FIELDS, FIELD_LIST, normalizeFilterValue, validateFilterValue } from '@/domain/fields'
import { COMPARATOR_LABELS } from '@/domain/labels'

const props = defineProps<{
  initial?: MovieFilter | null
}>()

const emit = defineEmits<{
  apply: [filter: MovieFilter]
  cancel: []
}>()

const path = ref<MovieFieldPath>(props.initial?.path ?? 'genre')
const comparator = ref<Comparator>(props.initial?.comparator ?? FIELDS[path.value].comparators[0] ?? 'eq')
const value = ref(props.initial?.value ?? '')
const error = ref<string | null>(null)
const inputRef = ref<InstanceType<typeof BaseInput> | null>(null)

const meta = computed(() => FIELDS[path.value])

const fieldOptions = FIELD_LIST.map((f) => ({ value: f.path, label: f.label }))

const comparatorOptions = computed(() =>
  meta.value.comparators.map((c) => ({ value: c, label: COMPARATOR_LABELS[c] })),
)

const pathModel = computed({
  get: () => path.value,
  set: (v: string) => {
    if (v in FIELDS) path.value = v as MovieFieldPath
  },
})

const comparatorModel = computed({
  get: () => comparator.value,
  set: (v: string) => {
    comparator.value = v as Comparator
  },
})

const inputType = computed(() => (meta.value.kind === 'date' ? 'date' : 'text'))

const placeholder = computed(() => {
  switch (meta.value.kind) {
    case 'integer':
      return '3'
    case 'float':
      return '12.5'
    default:
      return 'Значение'
  }
})

watch(path, (next, previous) => {
  if (next === previous) return
  const allowed = FIELDS[next].comparators
  if (!allowed.includes(comparator.value)) comparator.value = allowed[0] ?? 'eq'
  if (FIELDS[next].kind !== FIELDS[previous].kind || FIELDS[next].kind === 'enum') value.value = ''
  error.value = null
})

watch(value, () => {
  error.value = null
})

function submit() {
  const message = validateFilterValue(path.value, value.value)
  if (message) {
    error.value = message
    return
  }
  emit('apply', {
    path: path.value,
    comparator: comparator.value,
    value: normalizeFilterValue(path.value, value.value),
  })
}

onMounted(() => {
  void nextTick(() => inputRef.value?.focus())
})
</script>

<template>
  <form class="filter-editor" @submit.prevent="submit">
    <FormField label="Поле" for-id="filter-path">
      <BaseSelect id="filter-path" v-model="pathModel" :options="fieldOptions" size="sm" />
    </FormField>
    <div class="row">
      <FormField label="Условие" for-id="filter-cmp" class="cmp">
        <BaseSelect id="filter-cmp" v-model="comparatorModel" :options="comparatorOptions" size="sm" />
      </FormField>
      <FormField label="Значение" for-id="filter-value" :error="error" class="val">
        <BaseSelect
          v-if="meta.kind === 'enum'"
          id="filter-value"
          v-model="value"
          :options="meta.options ?? []"
          placeholder="Выберите"
          size="sm"
          :invalid="!!error"
        />
        <BaseInput
          v-else
          id="filter-value"
          ref="inputRef"
          v-model="value"
          :type="inputType"
          :placeholder="placeholder"
          :decimals="meta.kind === 'float' ? 2 : undefined"
          size="sm"
          :invalid="!!error"
        />
      </FormField>
    </div>
    <div class="actions">
      <BaseButton size="sm" variant="ghost" @click="emit('cancel')">Отмена</BaseButton>
      <BaseButton size="sm" variant="primary" type="submit">{{ initial ? 'Сохранить' : 'Добавить' }}</BaseButton>
    </div>
  </form>
</template>

<style scoped>
.filter-editor {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding-top: 2px;
}
</style>
