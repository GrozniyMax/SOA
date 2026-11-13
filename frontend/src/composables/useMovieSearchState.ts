import { computed } from 'vue'
import { useRoute, useRouter, type LocationQuery, type LocationQueryRaw } from 'vue-router'
import {
  COMPARATORS,
  PAGE_SIZE_DEFAULT,
  PAGE_SIZE_MAX,
  type Comparator,
  type MovieFieldPath,
  type MovieFilter,
  type MovieSearchRequest,
  type SortDirection,
} from '@/api/types'
import { FIELDS, isFieldPath } from '@/domain/fields'

export interface SortOrder {
  key: MovieFieldPath
  dir: SortDirection
}

export interface MovieSearchState {
  q: string
  filters: MovieFilter[]
  sort: SortOrder[]
  page: number
  size: number
}

function first(value: LocationQuery[string] | undefined): string | null {
  if (Array.isArray(value)) return value[0] ?? null
  return value ?? null
}

function all(value: LocationQuery[string] | undefined): string[] {
  if (Array.isArray(value)) return value.filter((v): v is string => typeof v === 'string')
  return typeof value === 'string' ? [value] : []
}

function isComparator(value: string): value is Comparator {
  return (COMPARATORS as readonly string[]).includes(value)
}

export function parseFilter(raw: string): MovieFilter | null {
  const firstColon = raw.indexOf(':')
  const secondColon = raw.indexOf(':', firstColon + 1)
  if (firstColon < 0 || secondColon < 0) return null
  const path = raw.slice(0, firstColon)
  const comparator = raw.slice(firstColon + 1, secondColon)
  const value = raw.slice(secondColon + 1)
  if (!isFieldPath(path) || !isComparator(comparator)) return null
  if (!FIELDS[path].comparators.includes(comparator)) return null
  return { path, comparator, value }
}

export function serializeFilter(filter: MovieFilter): string {
  return `${filter.path}:${filter.comparator ?? 'eq'}:${filter.value}`
}

export function parseSort(raw: string | null): SortOrder[] {
  if (!raw) return []
  const seen = new Set<string>()
  const result: SortOrder[] = []
  for (const part of raw.split(',')) {
    const [key = '', dir = 'asc'] = part.trim().split(':')
    if (!isFieldPath(key) || seen.has(key)) continue
    seen.add(key)
    result.push({ key, dir: dir === 'desc' ? 'desc' : 'asc' })
  }
  return result
}

export function serializeSort(sort: readonly SortOrder[]): string {
  return sort.map((s) => `${s.key}:${s.dir}`).join(',')
}

function parseIntInRange(raw: string | null, fallback: number, min: number, max: number): number {
  if (raw === null || !/^\d+$/.test(raw)) return fallback
  const value = Number(raw)
  return value >= min && value <= max ? value : fallback
}

function toQuery(state: MovieSearchState): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  if (state.q) query.q = state.q
  if (state.filters.length) query.f = state.filters.map(serializeFilter)
  if (state.sort.length) query.sort = serializeSort(state.sort)
  if (state.page > 0) query.page = String(state.page)
  if (state.size !== PAGE_SIZE_DEFAULT) query.size = String(state.size)
  return query
}

export function toSearchRequest(state: MovieSearchState): MovieSearchRequest {
  const filters = [...state.filters]
  const q = state.q.trim()
  if (q) filters.push({ path: 'name', comparator: 'substring', value: q })
  return {
    filters,
    sort: state.sort.length ? serializeSort(state.sort) : undefined,
    page: state.page,
    size: state.size,
  }
}

export function useMovieSearchState() {
  const route = useRoute()
  const router = useRouter()

  const state = computed<MovieSearchState>(() => ({
    q: first(route.query.q) ?? '',
    filters: all(route.query.f)
      .map(parseFilter)
      .filter((f): f is MovieFilter => f !== null),
    sort: parseSort(first(route.query.sort)),
    page: parseIntInRange(first(route.query.page), 0, 0, Number.MAX_SAFE_INTEGER),
    size: parseIntInRange(first(route.query.size), PAGE_SIZE_DEFAULT, 1, PAGE_SIZE_MAX),
  }))

  function update(patch: Partial<MovieSearchState>, keepPage = false) {
    const next: MovieSearchState = { ...state.value, ...patch }
    if (!keepPage && patch.page === undefined) next.page = 0
    void router.replace({ query: toQuery(next) })
  }

  function reset() {
    void router.replace({ query: {} })
  }

  return { state, update, reset }
}
