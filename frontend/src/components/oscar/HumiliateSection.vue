<script setup lang="ts">
import { computed, ref } from 'vue'
import { IconAward, IconCheck, IconFlame } from '@tabler/icons-vue'
import { oscarApi } from '@/api/oscar'
import { MOVIE_GENRES, type HumiliationResult, type MovieGenre } from '@/api/types'
import BaseButton from '@/components/ui/BaseButton.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import { useCountUp } from '@/composables/useCountUp'
import { useToast } from '@/composables/useToast'
import { GENRE_LABELS } from '@/domain/labels'
import { formatInteger, pluralize } from '@/domain/format'

const emit = defineEmits<{
  done: [result: HumiliationResult]
}>()

const toast = useToast()
const genre = ref<MovieGenre | null>(null)
const confirmOpen = ref(false)
const running = ref(false)
const error = ref<unknown>(null)
const result = ref<HumiliationResult | null>(null)

const revoked = useCountUp(computed(() => result.value?.revokedOscars ?? null))
const directors = useCountUp(computed(() => result.value?.affectedDirectors ?? null))
const resultKey = ref(0)

const genreLabel = computed(() => (genre.value ? GENRE_LABELS[genre.value].toLowerCase() : ''))

const summary = computed(() => {
  const r = result.value
  if (!r) return ''
  const g = GENRE_LABELS[r.genre].toLowerCase()
  if (r.revokedOscars === 0) {
    return `У режиссёров, снимавших в жанре «${g}», не нашлось Оскаров, которые можно отобрать.`
  }
  return `Отобрано ${formatInteger(r.revokedOscars)} ${pluralize(r.revokedOscars, 'Оскар', 'Оскара', 'Оскаров')} у фильмов ${formatInteger(r.affectedDirectors)} ${pluralize(r.affectedDirectors, 'режиссёра', 'режиссёров', 'режиссёров')}, снимавших в жанре «${g}».`
})

function pick(value: MovieGenre) {
  genre.value = value
  error.value = null
}

async function run() {
  if (!genre.value) return
  running.value = true
  error.value = null
  try {
    const response = await oscarApi.humiliateByGenre(genre.value)
    result.value = response
    resultKey.value++
    confirmOpen.value = false
    toast.success('Оскары отобраны', `Жанр «${GENRE_LABELS[response.genre].toLowerCase()}»`)
    emit('done', response)
  } catch (err) {
    confirmOpen.value = false
    error.value = err
  } finally {
    running.value = false
  }
}

</script>

<template>
  <SectionCard
    title="Отобрать Оскары по жанру"
    description="Отбирает все Оскары у фильмов режиссёров, снявших хотя бы один фильм в выбранном жанре"
    :icon="IconFlame"
  >
    <div class="body">
      <div class="genres" role="radiogroup" aria-label="Жанр">
        <button
          v-for="g in MOVIE_GENRES"
          :key="g"
          type="button"
          role="radio"
          :aria-checked="genre === g"
          class="genre-option"
          :class="[`genre-${g.toLowerCase()}`, { selected: genre === g }]"
          @click="pick(g)"
        >
          <span class="dot" />
          <span>{{ GENRE_LABELS[g] }}</span>
          <IconCheck v-if="genre === g" :size="15" stroke-width="2.5" class="check" />
        </button>
      </div>

      <div class="actions">
        <BaseButton variant="danger" :icon="IconAward" :disabled="!genre" @click="confirmOpen = true">
          Отобрать Оскары
        </BaseButton>
        <span v-if="!genre" class="hint">Выберите жанр</span>
      </div>

      <ErrorState v-if="error" :error="error" compact @retry="run" />

      <div v-else-if="result" :key="resultKey" class="result rise-in">
        <div class="numbers">
          <div class="number">
            <span class="value tabular">{{ formatInteger(revoked) }}</span>
            <span class="label">{{ pluralize(result.revokedOscars, 'Оскар отобран', 'Оскара отобрано', 'Оскаров отобрано') }}</span>
          </div>
          <div class="number">
            <span class="value tabular">{{ formatInteger(directors) }}</span>
            <span class="label">{{ pluralize(result.affectedDirectors, 'режиссёр затронут', 'режиссёра затронуто', 'режиссёров затронуто') }}</span>
          </div>
        </div>
        <p class="summary">{{ summary }}</p>
      </div>
    </div>
  </SectionCard>

  <ConfirmDialog
    :open="confirmOpen"
    title="Отобрать Оскары?"
    :message="`Все Оскары будут отобраны у фильмов режиссёров, снимавших в жанре «${genreLabel}». Отменить это действие нельзя.`"
    confirm-label="Отобрать"
    danger
    :loading="running"
    @confirm="run"
    @cancel="confirmOpen = false"
  />
</template>

<style scoped>
.body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.genres {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.genre-option {
  --hue: var(--accent);
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  padding: 0 14px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--text);
  font-weight: 500;
  cursor: pointer;
  transition:
    border-color var(--dur) var(--ease),
    background var(--dur) var(--ease),
    transform var(--dur) var(--ease),
    box-shadow var(--dur) var(--ease);
}

.genre-option:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--hue) 50%, var(--border-strong));
}

.genre-option:active {
  transform: scale(0.98);
}

.genre-option.selected {
  border-color: var(--hue);
  background: color-mix(in srgb, var(--hue) 10%, var(--surface));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--hue) 16%, transparent);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--hue);
  transition: transform var(--dur-slow) var(--ease-spring);
}

.genre-option:hover .dot,
.genre-option.selected .dot {
  transform: scale(1.3);
}

.check {
  margin-left: auto;
  color: var(--hue);
  animation: pop var(--dur-slow) var(--ease-spring);
}

@keyframes pop {
  from {
    transform: scale(0);
  }
}

.genre-action {
  --hue: var(--genre-action);
}

.genre-drama {
  --hue: var(--genre-drama);
}

.genre-adventure {
  --hue: var(--genre-adventure);
}

.genre-fantasy {
  --hue: var(--genre-fantasy);
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint {
  font-size: 13px;
  color: var(--text-muted);
}

.result {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius);
  background: var(--surface-muted);
}

.numbers {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.number {
  display: flex;
  flex-direction: column;
}

.value {
  font-size: 32px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.label {
  font-size: 13px;
  color: var(--text-secondary);
}

.summary {
  color: var(--text-secondary);
}
</style>
