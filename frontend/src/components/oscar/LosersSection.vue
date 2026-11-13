<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { IconMoodSad, IconRefresh, IconTrophyOff } from '@tabler/icons-vue'
import { oscarApi } from '@/api/oscar'
import { PAGE_SIZE_DEFAULT, type Operator } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import DataTable, { type TableColumn } from '@/components/ui/DataTable.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { formatInteger, pluralize } from '@/domain/format'

const page = ref(0)
const size = ref(PAGE_SIZE_DEFAULT)

const query = useAsyncData((signal) => oscarApi.getLosers({ page: page.value, size: size.value }, signal))

onMounted(() => void query.reload())
watch([page, size], () => void query.reload())

const operators = computed(() => query.data.value?.items ?? [])
const total = computed(() => query.data.value?.total ?? 0)

const columns: TableColumn<Operator>[] = [
  { key: 'id', label: 'ID', width: '80px', align: 'right', muted: true },
  { key: 'name', label: 'Оператор' },
]

const summary = computed(() => {
  if (!query.data.value) return null
  const n = total.value
  if (n === 0) return null
  return `${pluralize(n, 'Найден', 'Найдено', 'Найдено')} ${formatInteger(n)} ${pluralize(n, 'оператор', 'оператора', 'операторов')}, у фильмов ${pluralize(n, 'которого', 'которых', 'которых')} нет ни одного Оскара.`
})

defineExpose({ reload: query.reload })
</script>

<template>
  <SectionCard
    title="Операторы без Оскаров"
    description="Операторы, ни один фильм которых не получил ни одного Оскара"
    :icon="IconTrophyOff"
  >
    <template #actions>
      <BaseButton size="sm" variant="ghost" icon-only :icon="IconRefresh" label="Обновить" :loading="query.loading.value" @click="query.reload" />
    </template>

    <ErrorState v-if="query.error.value" :error="query.error.value" compact @retry="query.reload" />
    <div v-else class="body">
      <Transition name="fade" mode="out-in">
        <p v-if="summary" :key="summary" class="summary">{{ summary }}</p>
      </Transition>
      <DataTable
        :columns="columns"
        :rows="operators"
        :row-key="(o) => o.id"
        :loading="query.loading.value"
        :generation="query.generation.value"
        :skeleton-rows="3"
        min-width="320px"
      >
        <template #cell-name="{ row }">
          <span class="operator">
            <span class="avatar" aria-hidden="true">{{ row.name.slice(0, 1).toUpperCase() }}</span>
            {{ row.name }}
          </span>
        </template>
        <template #empty>
          <EmptyState
            :icon="IconMoodSad"
            title="Таких операторов нет"
            text="У каждого оператора есть хотя бы один фильм с Оскаром."
          />
        </template>
      </DataTable>
      <PaginationBar
        v-if="total > 0"
        v-model:page="page"
        :size="size"
        :total="total"
        :disabled="query.loading.value"
        @update:size="(s) => ((size = s), (page = 0))"
      />
    </div>
  </SectionCard>
</template>

<style scoped>
.body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary {
  color: var(--text-secondary);
}

.operator {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
}

.avatar {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--surface-muted);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}
</style>
