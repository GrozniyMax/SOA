import type { Comparator, EyeColor, HairColor, MovieFieldPath, MovieGenre, SortDirection } from '@/api/types'

export const GENRE_LABELS: Record<MovieGenre, string> = {
  ACTION: 'Боевик',
  DRAMA: 'Драма',
  ADVENTURE: 'Приключения',
  FANTASY: 'Фэнтези',
}

export const EYE_COLOR_LABELS: Record<EyeColor, string> = {
  GREEN: 'Зелёные',
  BLACK: 'Чёрные',
  BLUE: 'Голубые',
}

export const HAIR_COLOR_LABELS: Record<HairColor, string> = {
  RED: 'Рыжие',
  BLACK: 'Чёрные',
  ORANGE: 'Оранжевые',
  BROWN: 'Каштановые',
}

export const FIELD_LABELS: Record<MovieFieldPath, string> = {
  id: 'ID',
  name: 'Название',
  'coordinates.x': 'Координата X',
  'coordinates.y': 'Координата Y',
  creationDate: 'Дата создания',
  oscarsCount: 'Оскары',
  totalBoxOffice: 'Сборы',
  tagline: 'Слоган',
  genre: 'Жанр',
  'director.name': 'Режиссёр',
  'director.birthday': 'День рождения режиссёра',
  'director.eyeColor': 'Цвет глаз режиссёра',
  'director.hairColor': 'Цвет волос режиссёра',
}

export const COMPARATOR_LABELS: Record<Comparator, string> = {
  eq: '=',
  gt: '>',
  lt: '<',
  substring: 'содержит',
}

export const SORT_DIRECTION_LABELS: Record<SortDirection, string> = {
  asc: 'по возрастанию',
  desc: 'по убыванию',
}
