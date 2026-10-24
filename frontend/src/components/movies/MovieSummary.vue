<script setup lang="ts">
import { computed } from 'vue'
import { IconQuote } from '@tabler/icons-vue'
import type { Movie } from '@/api/types'
import GenreTag from '@/components/ui/GenreTag.vue'
import { EYE_COLOR_LABELS, GENRE_LABELS, HAIR_COLOR_LABELS } from '@/domain/labels'
import {
  EMPTY,
  formatDecimal,
  formatInteger,
  formatLongDate,
  formatMoney,
  pluralize,
} from '@/domain/format'

const props = defineProps<{
  movie: Movie
}>()

const paragraph = computed(() => {
  const m = props.movie
  const parts: string[] = []
  parts.push(`«${m.name}» — фильм в жанре «${GENRE_LABELS[m.genre].toLowerCase()}», режиссёр — ${m.director.name}.`)
  if (m.oscarsCount) {
    parts.push(`Получил ${formatInteger(m.oscarsCount)} ${pluralize(m.oscarsCount, 'Оскар', 'Оскара', 'Оскаров')}.`)
  } else {
    parts.push('Оскаров не получал.')
  }
  if (m.totalBoxOffice != null) parts.push(`Кассовые сборы составили ${formatMoney(m.totalBoxOffice)}.`)
  parts.push(`Запись добавлена в коллекцию ${formatLongDate(m.creationDate)}`)
  return parts.join(' ').replace(/([^.])$/, '$1.')
})

const rows = computed(() => {
  const m = props.movie
  return {
    movie: [
      { label: 'ID', value: String(m.id) },
      { label: 'Оскары', value: formatInteger(m.oscarsCount) },
      { label: 'Сборы', value: formatMoney(m.totalBoxOffice) },
      { label: 'Добавлен', value: formatLongDate(m.creationDate) },
    ],
    coordinates: [
      { label: 'X', value: formatInteger(m.coordinates.x) },
      { label: 'Y', value: formatDecimal(m.coordinates.y) },
    ],
    director: [
      { label: 'Имя', value: m.director.name },
      { label: 'Дата рождения', value: formatLongDate(m.director.birthday) },
      { label: 'Цвет глаз', value: m.director.eyeColor ? EYE_COLOR_LABELS[m.director.eyeColor] : EMPTY },
      { label: 'Цвет волос', value: m.director.hairColor ? HAIR_COLOR_LABELS[m.director.hairColor] : EMPTY },
    ],
  }
})
</script>

<template>
  <article class="summary rise-in">
    <div class="head">
      <GenreTag :genre="movie.genre" />
    </div>

    <p class="paragraph">{{ paragraph }}</p>

    <blockquote v-if="movie.tagline" class="tagline">
      <IconQuote :size="18" stroke-width="1.75" class="quote-icon" />
      <span>{{ movie.tagline }}</span>
    </blockquote>

    <section class="block">
      <h3>Фильм</h3>
      <dl>
        <div v-for="row in rows.movie" :key="row.label" class="row">
          <dt>{{ row.label }}</dt>
          <dd :class="{ empty: row.value === EMPTY }" class="tabular">{{ row.value }}</dd>
        </div>
      </dl>
    </section>

    <section class="block">
      <h3>Координаты</h3>
      <dl>
        <div v-for="row in rows.coordinates" :key="row.label" class="row">
          <dt>{{ row.label }}</dt>
          <dd class="tabular">{{ row.value }}</dd>
        </div>
      </dl>
    </section>

    <section class="block">
      <h3>Режиссёр</h3>
      <dl>
        <div v-for="row in rows.director" :key="row.label" class="row">
          <dt>{{ row.label }}</dt>
          <dd :class="{ empty: row.value === EMPTY }">{{ row.value }}</dd>
        </div>
      </dl>
    </section>
  </article>
</template>

<style scoped>
.summary {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.paragraph {
  font-size: 15px;
  line-height: 1.6;
}

.tagline {
  display: flex;
  gap: 10px;
  margin: 0;
  padding: 12px 14px;
  border-radius: var(--radius);
  background: var(--accent-soft);
  color: var(--accent-text);
  font-style: italic;
}

.quote-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.block h3 {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}

dl {
  margin: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 12px;
  padding: 9px 14px;
  transition: background var(--dur) var(--ease);
}

.row + .row {
  border-top: 1px solid var(--border);
}

.row:hover {
  background: var(--surface-muted);
}

dt {
  color: var(--text-secondary);
}

dd {
  margin: 0;
  overflow-wrap: anywhere;
}

dd.empty {
  color: var(--text-muted);
}
</style>
