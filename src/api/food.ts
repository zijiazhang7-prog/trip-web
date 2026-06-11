import { coerceSpringPage, type NormalizedPage } from './coercePage'
import { httpRequest } from './http'
import type { Food } from '../data/siteData'

export type FoodVO = {
  id: number
  destinationId: number
  facilityId?: number
  name: string
  foodType?: string
  shopName?: string
  description?: string
  heatScore?: number
  ratingScore?: number
  avgPrice?: number
  coverUrl?: string
}

function normalizeAssetUrl(url: string | undefined): string {
  if (!url) return '/images/auth-bg-login.png'
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

export function foodVOToFood(vo: FoodVO): Food {
  const rating =
    vo.ratingScore != null ? String(vo.ratingScore) : vo.heatScore != null ? String(vo.heatScore) : '—'
  const tags = vo.foodType ? [vo.foodType] : []
  return {
    id: vo.id,
    destinationId: vo.destinationId,
    name: vo.shopName?.trim() || vo.name,
    dish: vo.name,
    price: vo.avgPrice != null ? `¥${vo.avgPrice}` : '—',
    distance: vo.heatScore != null ? `${Math.round(vo.heatScore)} 热度` : '—',
    rating,
    image: normalizeAssetUrl(vo.coverUrl),
    tags,
    description: vo.description?.trim(),
  }
}

/** 在多个目的地下并行搜索美食并去重（后端单接口必须带 destinationId 时的折中方案） */
export async function searchFoodsAcrossDestinations(
  destinationIds: number[],
  keyword: string,
  opts: { sortBy?: string; pageSize?: number } = {},
): Promise<FoodVO[]> {
  const pageSize = opts.pageSize ?? 24
  const concurrency = 8
  const seen = new Set<string>()
  const out: FoodVO[] = []
  const rowKey = (row: FoodVO) =>
    row.id != null ? `i:${row.id}` : `n:${row.destinationId}:${row.name}:${row.shopName ?? ''}`
  for (let i = 0; i < destinationIds.length; i += concurrency) {
    const slice = destinationIds.slice(i, i + concurrency)
    const batches = await Promise.all(
      slice.map((destinationId) =>
        searchFoods(destinationId, keyword, { pageNum: 1, pageSize, sortBy: opts.sortBy }),
      ),
    )
    for (const list of batches) {
      for (const row of list) {
        const k = rowKey(row)
        if (seen.has(k)) continue
        seen.add(k)
        out.push(row)
      }
    }
  }
  out.sort((a, b) => (b.heatScore ?? 0) - (a.heatScore ?? 0))
  return out
}

export type FetchRecommendedFoodsParams = {
  facilityId?: number
  foodType?: string
  sortBy?: string
  pageNum?: number
  pageSize?: number
  topK?: number
}

export async function fetchRecommendedFoodsPage(
  destinationId: number,
  params: FetchRecommendedFoodsParams = {},
): Promise<NormalizedPage<FoodVO>> {
  const query = new URLSearchParams({
    destinationId: String(destinationId),
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 50),
  })
  if (params.facilityId != null) query.set('facilityId', String(params.facilityId))
  if (params.foodType) query.set('foodType', params.foodType)
  query.set('sortBy', params.sortBy ?? 'heat')
  if (typeof params.topK === 'number') query.set('topK', String(params.topK))
  const raw = await httpRequest<unknown>(`/api/v1/foods/recommend?${query}`, { method: 'GET' })
  return coerceSpringPage<FoodVO>(raw)
}

export async function fetchRecommendedFoods(
  destinationId: number,
  params: FetchRecommendedFoodsParams = {},
): Promise<FoodVO[]> {
  const n = await fetchRecommendedFoodsPage(destinationId, params)
  return n.list
}

export type SearchFoodsParams = {
  sortBy?: string
  pageNum?: number
  pageSize?: number
}

export async function searchFoods(
  destinationId: number,
  keyword: string,
  params: SearchFoodsParams = {},
): Promise<FoodVO[]> {
  const query = new URLSearchParams({
    destinationId: String(destinationId),
    keyword: keyword.trim(),
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 50),
  })
  if (params.sortBy) query.set('sortBy', params.sortBy)
  const raw = await httpRequest<unknown>(`/api/v1/foods/search?${query}`, { method: 'GET' })
  const n = coerceSpringPage<FoodVO>(raw)
  return n.list
}
