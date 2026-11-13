export const MOVIE_GENRES = ['ACTION', 'DRAMA', 'ADVENTURE', 'FANTASY'] as const
export type MovieGenre = (typeof MOVIE_GENRES)[number]

export const EYE_COLORS = ['GREEN', 'BLACK', 'BLUE'] as const
export type EyeColor = (typeof EYE_COLORS)[number]

export const HAIR_COLORS = ['RED', 'BLACK', 'ORANGE', 'BROWN'] as const
export type HairColor = (typeof HAIR_COLORS)[number]

export const COMPARATORS = ['eq', 'gt', 'lt', 'substring'] as const
export type Comparator = (typeof COMPARATORS)[number]

export const MOVIE_FIELD_PATHS = [
  'id',
  'name',
  'coordinates.x',
  'coordinates.y',
  'creationDate',
  'oscarsCount',
  'totalBoxOffice',
  'tagline',
  'genre',
  'director.name',
  'director.birthday',
  'director.eyeColor',
  'director.hairColor',
] as const
export type MovieFieldPath = (typeof MOVIE_FIELD_PATHS)[number]

export const SORT_DIRECTIONS = ['asc', 'desc'] as const
export type SortDirection = (typeof SORT_DIRECTIONS)[number]

export interface Coordinates {
  x: number
  y: number
}

export interface Person {
  name: string
  birthday?: string | null
  eyeColor?: EyeColor | null
  hairColor?: HairColor | null
}

export interface Movie {
  id: number
  name: string
  coordinates: Coordinates
  creationDate: string
  oscarsCount?: number | null
  totalBoxOffice?: number | null
  tagline?: string | null
  genre: MovieGenre
  director: Person
}

export interface MovieRequest {
  name: string
  coordinates: Coordinates
  oscarsCount?: number | null
  totalBoxOffice?: number | null
  tagline?: string | null
  genre: MovieGenre
  director: Person
}

export interface MovieFilter {
  path: MovieFieldPath
  value: string
  comparator?: Comparator
}

export interface MovieSearchRequest {
  filters?: MovieFilter[]
  sort?: string
  page?: number
  size?: number
}

export interface Page<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export type MoviePage = Page<Movie>
export type GenrePage = Page<MovieGenre>

export interface Operator {
  id: number
  name: string
}

export type OperatorPage = Page<Operator>

export interface HumiliationResult {
  genre: MovieGenre
  affectedDirectors: number
  revokedOscars: number
}

export interface ApiErrorBody {
  status: number
  message: string
}

export interface PageParams {
  page?: number
  size?: number
}

export const PAGE_SIZE_DEFAULT = 20
export const PAGE_SIZE_MAX = 100
