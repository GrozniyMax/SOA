<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { IconEdit, IconTrash } from '@tabler/icons-vue'
import { moviesApi } from '@/api/movies'
import { ApiException } from '@/api/http'
import type { Movie } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import SideDrawer from '@/components/ui/SideDrawer.vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import MovieSummary from '@/components/movies/MovieSummary.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { useToast } from '@/composables/useToast'

const open = defineModel<boolean>('open', { default: false })

const props = defineProps<{
  movieId: number | null
}>()

const emit = defineEmits<{
  edit: [movie: Movie]
  deleted: [id: number]
}>()

const toast = useToast()
const confirmOpen = ref(false)
const deleting = ref(false)

const query = useAsyncData((signal) => moviesApi.getById(props.movieId ?? 0, signal))

watch(
  () => [open.value, props.movieId] as const,
  ([isOpen, id]) => {
    if (isOpen && id !== null) {
      query.data.value = query.data.value?.id === id ? query.data.value : null
      void query.reload()
    }
  },
  { immediate: true },
)

const movie = computed(() => (query.data.value?.id === props.movieId ? query.data.value : null))
const title = computed(() => movie.value?.name ?? (query.error.value ? 'Фильм' : 'Загрузка…'))
const subtitle = computed(() => (props.movieId !== null ? `ID ${props.movieId}` : undefined))

async function remove() {
  const current = movie.value
  if (!current) return
  deleting.value = true
  try {
    await moviesApi.remove(current.id)
    toast.success('Фильм удалён', `«${current.name}», ID ${current.id}`)
    confirmOpen.value = false
    open.value = false
    emit('deleted', current.id)
  } catch (err) {
    confirmOpen.value = false
    toast.error(err)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <SideDrawer v-model:open="open" :title="title" :subtitle="subtitle" width="540px">
    <ErrorState
      v-if="query.error.value"
      :error="query.error.value"
      :retryable="!(query.error.value instanceof ApiException && query.error.value.isNotFound)"
      @retry="query.reload"
    />
    <div v-else-if="!movie" class="loading">
      <SkeletonBlock width="40%" height="22px" />
      <SkeletonBlock height="64px" radius="12px" />
      <SkeletonBlock height="180px" radius="12px" />
    </div>
    <MovieSummary v-else :movie="movie" />

    <template v-if="movie" #footer>
      <BaseButton variant="danger-ghost" :icon="IconTrash" class="delete" @click="confirmOpen = true">Удалить</BaseButton>
      <BaseButton variant="primary" :icon="IconEdit" @click="emit('edit', movie)">Редактировать</BaseButton>
    </template>
  </SideDrawer>

  <ConfirmDialog
    :open="confirmOpen"
    title="Удалить фильм?"
    :message="movie ? `«${movie.name}» будет удалён из коллекции без возможности восстановления.` : undefined"
    confirm-label="Удалить"
    danger
    :loading="deleting"
    @confirm="remove"
    @cancel="confirmOpen = false"
  />
</template>

<style scoped>
.loading {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.delete {
  margin-right: auto;
}
</style>
