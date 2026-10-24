<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { IconSearch, IconSortAscendingLetters, IconUserSearch, IconZoomQuestion } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import { PAGE_SIZE_DEFAULT, type Movie } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import FormField from '@/components/ui/FormField.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import MovieTable from '@/components/movies/MovieTable.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { formatInteger, pluralize } from '@/domain/format'

const router = useRouter()

const input = ref('')
const inputError = ref<string | null>(null)
const director = ref<string | null>(null)
const page = ref(0)
const size = ref(PAGE_SIZE_DEFAULT)

const query = useAsyncData((signal) =>
  moviesApi.getByDirectorGreaterThan(director.value ?? '', { page: page.value, size: size.value }, signal),
)

watch([director, page, size], () => {
  if (director.value !== null) void query.reload()
})

watch(input, () => {
  inputError.value = null
})

const movies = computed(() => query.data.value?.items ?? [])
const total = computed(() => query.data.value?.total ?? 0)

const summary = computed(() => {
  if (!query.data.value || director.value === null) return null
  const n = total.value
  return `${formatInteger(n)} ${pluralize(n, 'фильм', 'фильма', 'фильмов')}, у которых имя режиссёра по алфавиту больше «${director.value}».`
})

function submit() {
  const value = input.value
  if (!value.trim()) {
    inputError.value = 'Введите имя режиссёра для сравнения'
    return
  }
  page.value = 0
  if (director.value === value) void query.reload()
  else director.value = value
}

function open(movie: Movie) {
  void router.push({ path: '/movies', query: { movie: String(movie.id) } })
}
</script>

<template>
  <SectionCard
    title="Режиссёр больше заданного"
    description="Фильмы, у которых имя режиссёра больше введённого значения (сравнение строк по алфавиту)"
    :icon="IconSortAscendingLetters"
  >
    <form class="query-form" @submit.prevent="submit">
      <FormField label="Имя режиссёра" for-id="dg-director" :error="inputError" class="query-field">
        <BaseInput
          id="dg-director"
          v-model="input"
          placeholder="Greta Gerwig"
          :icon="IconUserSearch"
          :invalid="!!inputError"
        />
      </FormField>
      <BaseButton type="submit" variant="primary" :icon="IconSearch" :loading="query.loading.value && !query.data.value">
        Найти
      </BaseButton>
    </form>

    <template v-if="director !== null">
      <ErrorState v-if="query.error.value" :error="query.error.value" compact class="block" @retry="query.reload" />
      <template v-else>
        <Transition name="fade" mode="out-in">
          <p v-if="summary" :key="summary" class="summary block">{{ summary }}</p>
        </Transition>
        <MovieTable
          :movies="movies"
          :loading="query.loading.value"
          :generation="query.generation.value"
          :sortable="false"
          compact
          clickable
          class="block"
          @open="open"
        >
          <template #empty>
            <EmptyState
              :icon="IconZoomQuestion"
              title="Таких фильмов нет"
              text="Попробуйте ввести имя, которое раньше по алфавиту."
            />
          </template>
        </MovieTable>
        <PaginationBar
          v-if="total > 0"
          v-model:page="page"
          :size="size"
          :total="total"
          :disabled="query.loading.value"
          class="block"
          @update:size="(s) => ((size = s), (page = 0))"
        />
      </template>
    </template>
  </SectionCard>
</template>

<style scoped>
.query-form {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.query-field {
  width: min(360px, 100%);
}

.query-form > :deep(.btn) {
  margin-top: 25px;
}

.block {
  margin-top: 16px;
}

.summary {
  color: var(--text-secondary);
}
</style>
