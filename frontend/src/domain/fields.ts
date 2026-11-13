import {
  EYE_COLORS,
  HAIR_COLORS,
  MOVIE_FIELD_PATHS,
  MOVIE_GENRES,
  type Comparator,
  type MovieFieldPath,
} from '@/api/types'
import { EYE_COLOR_LABELS, FIELD_LABELS, GENRE_LABELS, HAIR_COLOR_LABELS } from '@/domain/labels'
import { formatDate, formatDecimal, formatInteger } from '@/domain/format'

export type FieldKind = 'integer' | 'float' | 'string' | 'date' | 'enum'

export interface FieldOption {
  value: string
  label: string
}

export interface FieldMeta {
  path: MovieFieldPath
  label: string
  kind: FieldKind
  comparators: readonly Comparator[]
  options?: readonly FieldOption[]
}

const ORDERED: readonly Comparator[] = ['eq', 'gt', 'lt']
const TEXT: readonly Comparator[] = ['substring', 'eq', 'gt', 'lt']
const EQUAL_ONLY: readonly Comparator[] = ['eq']

function options<T extends string>(values: readonly T[], labels: Record<T, string>): FieldOption[] {
  return values.map((value) => ({ value, label: labels[value] }))
}

const KINDS: Record<MovieFieldPath, FieldKind> = {
  id: 'integer',
  name: 'string',
  'coordinates.x': 'integer',
  'coordinates.y': 'float',
  creationDate: 'date',
  oscarsCount: 'integer',
  totalBoxOffice: 'float',
  tagline: 'string',
  genre: 'enum',
  'director.name': 'string',
  'director.birthday': 'date',
  'director.eyeColor': 'enum',
  'director.hairColor': 'enum',
}

const ENUM_OPTIONS: Partial<Record<MovieFieldPath, readonly FieldOption[]>> = {
  genre: options(MOVIE_GENRES, GENRE_LABELS),
  'director.eyeColor': options(EYE_COLORS, EYE_COLOR_LABELS),
  'director.hairColor': options(HAIR_COLORS, HAIR_COLOR_LABELS),
}

export const FIELDS: Record<MovieFieldPath, FieldMeta> = Object.fromEntries(
  MOVIE_FIELD_PATHS.map((path) => {
    const kind = KINDS[path]
    const comparators = kind === 'enum' ? EQUAL_ONLY : kind === 'string' ? TEXT : ORDERED
    return [path, { path, label: FIELD_LABELS[path], kind, comparators, options: ENUM_OPTIONS[path] }]
  }),
) as Record<MovieFieldPath, FieldMeta>

export const FIELD_LIST: readonly FieldMeta[] = MOVIE_FIELD_PATHS.map((p) => FIELDS[p])

export function isFieldPath(value: string): value is MovieFieldPath {
  return (MOVIE_FIELD_PATHS as readonly string[]).includes(value)
}

export function validateFilterValue(path: MovieFieldPath, raw: string): string | null {
  const value = raw.trim()
  const meta = FIELDS[path]
  if (value === '') return 'Введите значение'
  switch (meta.kind) {
    case 'integer':
      if (!/^-?\d+$/.test(value)) return 'Нужно целое число'
      if (Math.abs(Number(value)) > 2147483647) return 'Слишком большое число'
      return null
    case 'float':
      return /^-?\d+([.,]\d+)?$/.test(value) ? null : 'Нужно число, например 12.5'
    case 'date':
      return /^\d{4}-\d{2}-\d{2}$/.test(value) ? null : 'Выберите дату'
    case 'enum':
      return meta.options?.some((o) => o.value === value) ? null : 'Выберите значение из списка'
    default:
      return null
  }
}

export function normalizeFilterValue(path: MovieFieldPath, raw: string): string {
  const value = raw.trim()
  return FIELDS[path].kind === 'float' ? value.replace(',', '.') : value
}

export function formatFilterValue(path: MovieFieldPath, value: string): string {
  const meta = FIELDS[path]
  switch (meta.kind) {
    case 'enum':
      return meta.options?.find((o) => o.value === value)?.label ?? value
    case 'date':
      return formatDate(value)
    case 'integer':
      return Number.isFinite(Number(value)) ? formatInteger(Number(value)) : value
    case 'float':
      return Number.isFinite(Number(value)) ? formatDecimal(Number(value)) : value
    default:
      return `«${value}»`
  }
}
