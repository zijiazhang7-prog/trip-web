import { coerceSpringPage } from './coercePage'
import { httpRequest } from './http'
import type { Destination } from '../data/siteData'

export type PageResult<T> = {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
  pages: number
}

export type DestinationVO = {
  id: number
  name: string
  type?: string
  category?: string
  city?: string
  description?: string
  heatScore?: number
  ratingScore?: number
  coverUrl?: string
  tags?: string[]
}

function normalizeAssetUrl(url: string | undefined): string {
  if (!url) return '/images/recommend-hero-new.png'
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

export function destinationVOToDestination(vo: DestinationVO): Destination {
  const rating = typeof vo.ratingScore === 'number' ? vo.ratingScore : 4.5
  const badge = vo.category || (vo.tags && vo.tags[0]) || '推荐'
  const heat = vo.heatScore != null ? Math.round(vo.heatScore) : null
  return {
    id: vo.id,
    name: vo.name,
    reason: vo.description?.trim() || (vo.tags?.length ? vo.tags.join('、') : '点击查看目的地详情与玩法'),
    rating,
    price: heat != null ? `${heat} 热度` : '—',
    badge,
    type: vo.category || vo.type || '目的地',
    image: normalizeAssetUrl(vo.coverUrl),
    value: rating >= 4.5,
  }
}

export type FetchRecommendedDestinationsParams = {
  type?: string
  theme?: string
  sortBy?: string
  pageNum?: number
  pageSize?: number
  topK?: number
}

export async function fetchRecommendedDestinationsPage(
  params: FetchRecommendedDestinationsParams = {},
): Promise<PageResult<DestinationVO>> {
  const query = new URLSearchParams()
  if (params.type) query.set('type', params.type)
  if (params.theme) query.set('theme', params.theme)
  query.set('sortBy', params.sortBy ?? 'recommend')
  query.set('pageNum', String(params.pageNum ?? 1))
  query.set('pageSize', String(params.pageSize ?? 50))
  if (typeof params.topK === 'number') query.set('topK', String(params.topK))
  const raw = await httpRequest<unknown>(`/api/v1/destinations/recommend?${query}`, {
    method: 'GET',
  })
  const n = coerceSpringPage<DestinationVO>(raw)
  return {
    list: n.list,
    pageNum: n.pageNum,
    pageSize: n.pageSize,
    total: n.total,
    pages: n.pages,
  }
}

export async function fetchRecommendedDestinations(
  params: FetchRecommendedDestinationsParams = {},
): Promise<DestinationVO[]> {
  const page = await fetchRecommendedDestinationsPage(params)
  return page.list ?? []
}

export type SearchDestinationsParams = {
  type?: string
  sortBy?: string
  pageNum?: number
  pageSize?: number
}

export async function searchDestinationsPage(
  keyword: string,
  params: SearchDestinationsParams = {},
): Promise<PageResult<DestinationVO>> {
  const query = new URLSearchParams({
    keyword: keyword.trim(),
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 50),
  })
  if (params.type) query.set('type', params.type)
  if (params.sortBy) query.set('sortBy', params.sortBy)
  const raw = await httpRequest<unknown>(`/api/v1/destinations/search?${query}`, {
    method: 'GET',
  })
  const n = coerceSpringPage<DestinationVO>(raw)
  return {
    list: n.list,
    pageNum: n.pageNum,
    pageSize: n.pageSize,
    total: n.total,
    pages: n.pages,
  }
}

export async function searchDestinations(
  keyword: string,
  params: SearchDestinationsParams = {},
): Promise<DestinationVO[]> {
  const page = await searchDestinationsPage(keyword, params)
  return page.list ?? []
}

type DiaryListItem = {
  id: number
  title?: string
  username?: string
  destinationName?: string
  contentText?: string
  heatScore?: number
}

export async function fetchDestinationDiariesPage(
  destinationId: number,
  params: { sortBy?: string; pageNum?: number; pageSize?: number } = {},
): Promise<PageResult<DiaryListItem>> {
  const query = new URLSearchParams({
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 20),
  })
  if (params.sortBy) query.set('sortBy', params.sortBy)
  const raw = await httpRequest<unknown>(
    `/api/v1/destinations/${destinationId}/diaries?${query}`,
    { method: 'GET' },
  )
  const n = coerceSpringPage<DiaryListItem>(raw)
  return {
    list: n.list,
    pageNum: n.pageNum,
    pageSize: n.pageSize,
    total: n.total,
    pages: n.pages,
  }
}
