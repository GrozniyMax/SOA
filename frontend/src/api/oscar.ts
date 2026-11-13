import { request } from '@/api/http'
import type { HumiliationResult, MovieGenre, OperatorPage, PageParams } from '@/api/types'

export const oscarApi = {
  getLosers(params: PageParams = {}, signal?: AbortSignal): Promise<OperatorPage> {
    return request<OperatorPage>('oscar', '/operators/losers', {
      query: { page: params.page, size: params.size },
      signal,
    })
  },

  humiliateByGenre(genre: MovieGenre): Promise<HumiliationResult> {
    return request<HumiliationResult>('oscar', `/directors/by-genre/${encodeURIComponent(genre)}/humiliate`, {
      method: 'POST',
      timeoutMs: 30000,
    })
  },
}
