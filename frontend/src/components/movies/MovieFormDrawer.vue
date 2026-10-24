<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { moviesApi } from '@/api/movies'
import { ApiException } from '@/api/http'
import { EYE_COLORS, HAIR_COLORS, MOVIE_GENRES, type Movie } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseSelect from '@/components/ui/BaseSelect.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import FormField from '@/components/ui/FormField.vue'
import SideDrawer from '@/components/ui/SideDrawer.vue'
import { useToast } from '@/composables/useToast'
import { EYE_COLOR_LABELS, GENRE_LABELS, HAIR_COLOR_LABELS } from '@/domain/labels'
import {
  TAGLINE_MAX,
  emptyForm,
  formFromMovie,
  mapServerErrors,
  toMovieRequest,
  type MovieFormErrors,
  type MovieFormModel,
} from '@/domain/validation'

const open = defineModel<boolean>('open', { default: false })

const props = defineProps<{
  movie: Movie | null
}>()

const emit = defineEmits<{
  saved: [movie: Movie, created: boolean]
}>()

const toast = useToast()
const form = ref<MovieFormModel>(emptyForm())
const errors = ref<MovieFormErrors>({})
const serverError = ref<unknown>(null)
const saving = ref(false)

const isEdit = computed(() => props.movie !== null)
const title = computed(() => (isEdit.value ? 'Редактирование' : 'Новый фильм'))
const subtitle = computed(() =>
  isEdit.value && props.movie ? `«${props.movie.name}», ID ${props.movie.id}` : 'Поля со звёздочкой обязательны',
)

const genreOptions = MOVIE_GENRES.map((g) => ({ value: g, label: GENRE_LABELS[g] }))
const eyeOptions = EYE_COLORS.map((c) => ({ value: c, label: EYE_COLOR_LABELS[c] }))
const hairOptions = HAIR_COLORS.map((c) => ({ value: c, label: HAIR_COLOR_LABELS[c] }))

const taglineHint = computed(() => `${form.value.tagline.length} / ${TAGLINE_MAX}`)

watch(open, (value) => {
  if (!value) return
  form.value = props.movie ? formFromMovie(props.movie) : emptyForm()
  errors.value = {}
  serverError.value = null
})

function clearError(field: keyof MovieFormModel) {
  if (errors.value[field]) {
    const next = { ...errors.value }
    delete next[field]
    errors.value = next
  }
}

async function submit() {
  serverError.value = null
  errors.value = {}
  saving.value = true
  try {
    const body = toMovieRequest(form.value)
    const saved = props.movie ? await moviesApi.update(props.movie.id, body) : await moviesApi.create(body)
    toast.success(props.movie ? 'Изменения сохранены' : 'Фильм добавлен', `«${saved.name}», ID ${saved.id}`)
    emit('saved', saved, !props.movie)
    open.value = false
  } catch (err) {
    if (err instanceof ApiException && err.isValidation) {
      const mapped = mapServerErrors(err.serverMessage)
      errors.value = mapped
      serverError.value = err
    } else {
      serverError.value = err
      toast.error(err)
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <SideDrawer v-model:open="open" :title="title" :subtitle="subtitle" width="560px">
    <form id="movie-form" class="movie-form" novalidate @submit.prevent="submit">
      <Transition name="fade">
        <ErrorState v-if="serverError" :error="serverError" :retryable="false" compact />
      </Transition>

      <section class="group">
        <h3>Фильм</h3>
        <FormField label="Название" for-id="mf-name" :error="errors.name" required>
          <BaseInput
            id="mf-name"
            v-model="form.name"
            placeholder="Интерстеллар"
            :invalid="!!errors.name"
            @update:model-value="clearError('name')"
          />
        </FormField>
        <div class="grid">
          <FormField label="Жанр" for-id="mf-genre" :error="errors.genre" required>
            <BaseSelect
              id="mf-genre"
              v-model="form.genre"
              :options="genreOptions"
              placeholder="Выберите жанр"
              :invalid="!!errors.genre"
              @update:model-value="clearError('genre')"
            />
          </FormField>
          <FormField label="Оскары" for-id="mf-oscars" :error="errors.oscarsCount" hint="Целое число больше 0">
            <BaseInput
              id="mf-oscars"
              v-model="form.oscarsCount"
              placeholder="Не указано"
              :invalid="!!errors.oscarsCount"
              @update:model-value="clearError('oscarsCount')"
            />
          </FormField>
        </div>
        <FormField label="Кассовые сборы" for-id="mf-box" :error="errors.totalBoxOffice" hint="Число больше 0">
          <BaseInput
            id="mf-box"
            v-model="form.totalBoxOffice"
            placeholder="Не указано"
            :decimals="2"
            :invalid="!!errors.totalBoxOffice"
            @update:model-value="clearError('totalBoxOffice')"
          />
        </FormField>
        <FormField label="Слоган" for-id="mf-tagline" :error="errors.tagline" :hint="taglineHint">
          <BaseInput
            id="mf-tagline"
            v-model="form.tagline"
            placeholder="Не указан"
            :invalid="!!errors.tagline"
            @update:model-value="clearError('tagline')"
          />
        </FormField>
      </section>

      <section class="group">
        <h3>Координаты</h3>
        <div class="grid">
          <FormField label="X" for-id="mf-x" :error="errors.x" hint="Целое число" required>
            <BaseInput id="mf-x" v-model="form.x" placeholder="12" :invalid="!!errors.x" @update:model-value="clearError('x')" />
          </FormField>
          <FormField label="Y" for-id="mf-y" :error="errors.y" hint="Дробное число" required>
            <BaseInput
              id="mf-y"
              v-model="form.y"
              placeholder="3.5"
              :decimals="2"
              :invalid="!!errors.y"
              @update:model-value="clearError('y')"
            />
          </FormField>
        </div>
      </section>

      <section class="group">
        <h3>Режиссёр</h3>
        <FormField label="Имя" for-id="mf-dname" :error="errors.directorName" required>
          <BaseInput
            id="mf-dname"
            v-model="form.directorName"
            placeholder="Christopher Nolan"
            :invalid="!!errors.directorName"
            @update:model-value="clearError('directorName')"
          />
        </FormField>
        <FormField label="Дата рождения" for-id="mf-dbirth" :error="errors.directorBirthday">
          <BaseInput id="mf-dbirth" v-model="form.directorBirthday" type="date" :invalid="!!errors.directorBirthday" />
        </FormField>
        <div class="grid">
          <FormField label="Цвет глаз" for-id="mf-eye" :error="errors.directorEyeColor">
            <BaseSelect id="mf-eye" v-model="form.directorEyeColor" :options="eyeOptions" placeholder="Не указан" />
          </FormField>
          <FormField label="Цвет волос" for-id="mf-hair" :error="errors.directorHairColor">
            <BaseSelect id="mf-hair" v-model="form.directorHairColor" :options="hairOptions" placeholder="Не указан" />
          </FormField>
        </div>
      </section>
    </form>

    <template #footer>
      <BaseButton variant="ghost" :disabled="saving" @click="open = false">Отмена</BaseButton>
      <BaseButton variant="primary" type="submit" form="movie-form" :loading="saving">
        {{ isEdit ? 'Сохранить' : 'Добавить' }}
      </BaseButton>
    </template>
  </SideDrawer>
</template>

<style scoped>
.movie-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.group h3 {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: start;
}

@media (max-width: 520px) {
  .grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
