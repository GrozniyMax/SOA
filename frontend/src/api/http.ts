import type { ApiErrorBody } from '@/api/types'

export type ServiceName = 'movie' | 'oscar'

export type ApiErrorKind = 'http' | 'network' | 'timeout'

const SERVICE_TITLES: Record<ServiceName, string> = {
  movie: 'Сервис фильмов',
  oscar: 'Сервис оскаров',
}

const BASE_URLS: Record<ServiceName, string> = {
  movie: import.meta.env.VITE_MOVIE_API.replace(/\/+$/, ''),
  oscar: import.meta.env.VITE_OSCAR_API.replace(/\/+$/, ''),
}

const DEFAULT_TIMEOUT_MS = 15000

export class ApiException extends Error {
  readonly service: ServiceName
  readonly kind: ApiErrorKind
  readonly status: number | null
  readonly serverMessage: string | null

  constructor(params: {
    service: ServiceName
    kind: ApiErrorKind
    status?: number | null
    serverMessage?: string | null
    cause?: unknown
  }) {
    super(describeStatus(params.service, params.kind, params.status ?? null), { cause: params.cause })
    this.name = 'ApiException'
    this.service = params.service
    this.kind = params.kind
    this.status = params.status ?? null
    this.serverMessage = params.serverMessage ?? null
  }

  get isValidation(): boolean {
    return this.status === 400 || this.status === 422
  }

  get isNotFound(): boolean {
    return this.status === 404
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function describeStatus(service: ServiceName, kind: ApiErrorKind, status: number | null): string {
  const title = SERVICE_TITLES[service]
  if (kind === 'network') return `${title} недоступен`
  if (kind === 'timeout') return `${title} не ответил вовремя`
  switch (status) {
    case 400:
      return 'Некорректный запрос'
    case 404:
      return 'Объект не найден'
    case 405:
      return 'Метод не поддерживается сервисом'
    case 406:
      return 'Сервис не может вернуть данные в формате JSON'
    case 415:
      return 'Сервис не принимает такой формат данных'
    case 422:
      return 'Данные не прошли проверку'
    case 500:
      return `${title}: внутренняя ошибка`
    case 502:
      return 'Сервис фильмов вернул некорректный ответ'
    case 503:
      return 'Сервис фильмов временно недоступен'
    case 504:
      return 'Сервис фильмов не ответил вовремя'
    default:
      return `${title} вернул ошибку ${status ?? ''}`.trim()
  }
}

export function networkHint(error: ApiException): string | null {
  if (error.kind !== 'network') return null
  const url = BASE_URLS[error.service]
  if (/^https:/i.test(url)) {
    return `Проверьте, что сервис запущен, и откройте ${url} в браузере, чтобы принять самоподписанный сертификат.`
  }
  return 'Проверьте, что сервис запущен.'
}

type QueryValue = string | number | boolean | null | undefined

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  query?: Record<string, QueryValue>
  body?: unknown
  signal?: AbortSignal
  timeoutMs?: number
}

function buildUrl(service: ServiceName, path: string, query?: Record<string, QueryValue>): string {
  const url = `${BASE_URLS[service]}${path}`
  if (!query) return url
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') continue
    params.append(key, String(value))
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== 'object' || value === null) return false
  const record = value as Record<string, unknown>
  return typeof record.status === 'number' && typeof record.message === 'string'
}

async function readErrorBody(response: Response): Promise<{ message: string | null; empty: boolean }> {
  const text = await response.text().catch(() => '')
  if (!text.trim()) return { message: null, empty: true }
  try {
    const parsed: unknown = JSON.parse(text)
    return { message: isApiErrorBody(parsed) ? parsed.message : null, empty: false }
  } catch {
    return { message: null, empty: false }
  }
}

export async function request<T>(service: ServiceName, path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', query, body, signal, timeoutMs = DEFAULT_TIMEOUT_MS } = options
  const timeoutSignal = AbortSignal.timeout(timeoutMs)
  const combined = signal ? AbortSignal.any([signal, timeoutSignal]) : timeoutSignal

  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(buildUrl(service, path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: combined,
    })
  } catch (error) {
    if (signal?.aborted) throw error
    if (timeoutSignal.aborted) throw new ApiException({ service, kind: 'timeout', cause: error })
    throw new ApiException({ service, kind: 'network', cause: error })
  }

  if (!response.ok) {
    const { message, empty } = await readErrorBody(response)
    if (empty && response.status >= 500) {
      throw new ApiException({ service, kind: 'network', status: response.status })
    }
    throw new ApiException({ service, kind: 'http', status: response.status, serverMessage: message })
  }

  if (response.status === 204) return undefined as T
  const text = await response.text()
  if (!text) return undefined as T
  try {
    return JSON.parse(text) as T
  } catch (error) {
    throw new ApiException({ service, kind: 'http', status: 502, cause: error })
  }
}
