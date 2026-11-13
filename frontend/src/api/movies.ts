import { request } from '@/api/http'
import type {
  GenrePage,
  Movie,
  MoviePage,
  MovieRequest,
  MovieSearchRequest,
  PageParams,
} from '@/api/types'

export const moviesApi = {
  search(body: MovieSearchRequest, signal?: AbortSignal): Promise<MoviePage> {
    return request<MoviePage>('movie', '/movies/search', { method: 'POST', body, signal })
  },

  getById(id: number, signal?: AbortSignal): Promise<Movie> {
    return request<Movie>('movie', `/movies/${encodeURIComponent(id)}`, { signal })
  },

  create(body: MovieRequest): Promise<Movie> {
    return request<Movie>('movie', '/movies', { method: 'POST', body })
  },

  update(id: number, body: MovieRequest): Promise<Movie> {
    return request<Movie>('movie', `/movies/${encodeURIComponent(id)}`, { method: 'PUT', body })
  },

  remove(id: number): Promise<void> {
    return request<void>('movie', `/movies/${encodeURIComponent(id)}`, { method: 'DELETE' })
  },

  getWithMaxTagline(signal?: AbortSignal): Promise<Movie> {
    return request<Movie>('movie', '/movies/by-max-tagline', { signal })
  },

  getByDirectorGreaterThan(director: string, params: PageParams = {}, signal?: AbortSignal): Promise<MoviePage> {
    return request<MoviePage>('movie', '/movies/by-directors-greater-than', {
      query: { director, page: params.page, size: params.size },
      signal,
    })
  },

  getUniqueGenres(params: PageParams = {}, signal?: AbortSignal): Promise<GenrePage> {
    return request<GenrePage>('movie', '/movies/genres', {
      query: { page: params.page, size: params.size },
      signal,
    })
  },
}
