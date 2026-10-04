import type { EyeColor, HairColor, Movie, MovieGenre, MovieRequest } from '@/api/types'

export const TAGLINE_MAX = 181

export interface MovieFormModel {
  name: string
  genre: string
  oscarsCount: string
  totalBoxOffice: string
  tagline: string
  x: string
  y: string
  directorName: string
  directorBirthday: string
  directorEyeColor: string
  directorHairColor: string
}

export type MovieFormField = keyof MovieFormModel

export type MovieFormErrors = Partial<Record<MovieFormField, string>>

export function emptyForm(): MovieFormModel {
  return {
    name: '',
    genre: '',
    oscarsCount: '',
    totalBoxOffice: '',
    tagline: '',
    x: '',
    y: '',
    directorName: '',
    directorBirthday: '',
    directorEyeColor: '',
    directorHairColor: '',
  }
}

function str(value: number | string | null | undefined): string {
  return value === null || value === undefined ? '' : String(value)
}

export function formFromMovie(movie: Movie): MovieFormModel {
  return {
    name: movie.name,
    genre: movie.genre,
    oscarsCount: str(movie.oscarsCount),
    totalBoxOffice: str(movie.totalBoxOffice),
    tagline: str(movie.tagline),
    x: str(movie.coordinates.x),
    y: str(movie.coordinates.y),
    directorName: movie.director.name,
    directorBirthday: str(movie.director.birthday),
    directorEyeColor: str(movie.director.eyeColor),
    directorHairColor: str(movie.director.hairColor),
  }
}

const FLOAT_RE = /^-?\d+([.,]\d+)?$/

function looseNumber(raw: string): number | string | null {
  const value = raw.trim()
  if (value === '') return null
  const normalized = value.replace(',', '.')
  return FLOAT_RE.test(value) ? Number(normalized) : value
}

function optional(raw: string): string | null {
  return raw === '' ? null : raw
}

export function toMovieRequest(form: MovieFormModel): MovieRequest {
  return {
    name: form.name,
    coordinates: {
      x: looseNumber(form.x) as number,
      y: looseNumber(form.y) as number,
    },
    oscarsCount: looseNumber(form.oscarsCount) as number | null,
    totalBoxOffice: looseNumber(form.totalBoxOffice) as number | null,
    tagline: optional(form.tagline),
    genre: (optional(form.genre) ?? null) as MovieGenre,
    director: {
      name: form.directorName,
      birthday: optional(form.directorBirthday),
      eyeColor: optional(form.directorEyeColor) as EyeColor | null,
      hairColor: optional(form.directorHairColor) as HairColor | null,
    },
  }
}

const SERVER_FIELD_MAP: Record<string, MovieFormField> = {
  name: 'name',
  genre: 'genre',
  oscarsCount: 'oscarsCount',
  totalBoxOffice: 'totalBoxOffice',
  tagline: 'tagline',
  coordinates: 'x',
  'coordinates.x': 'x',
  'coordinates.y': 'y',
  director: 'directorName',
  'director.name': 'directorName',
  'director.birthday': 'directorBirthday',
  'director.eyeColor': 'directorEyeColor',
  'director.hairColor': 'directorHairColor',
}

export function mapServerErrors(message: string | null): MovieFormErrors {
  if (!message) return {}
  const errors: MovieFormErrors = {}
  for (const part of message.split(';')) {
    const trimmed = part.trim()
    const withColon = /^([\w.]+)\s*:\s*(.+)$/.exec(trimmed)
    const rawField = withColon ? withColon[1] : /^([\w.]+)\s/.exec(trimmed)?.[1]
    if (!rawField) continue
    const field = SERVER_FIELD_MAP[rawField]
    if (!field || errors[field]) continue
    errors[field] = withColon?.[2] ?? trimmed
  }
  return errors
}
