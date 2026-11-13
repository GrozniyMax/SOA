<script setup lang="ts">
import type { MovieGenre } from '@/api/types'
import { GENRE_LABELS } from '@/domain/labels'

defineProps<{ genre: MovieGenre }>()
</script>

<template>
  <span class="genre" :class="`genre-${genre.toLowerCase()}`">{{ GENRE_LABELS[genre] }}</span>
</template>

<style scoped>
.genre {
  --hue: var(--accent);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 9px 0 8px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--hue) 13%, transparent);
  color: color-mix(in srgb, var(--hue) 82%, var(--text));
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.genre::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hue);
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
</style>
