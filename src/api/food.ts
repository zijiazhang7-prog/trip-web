import { coerceSpringPage, type NormalizedPage } from './coercePage'
import { httpRequest } from './http'
import type { Food } from '../data/siteData'
import { foodTypesForCuisines, resolveFoodCuisineTags } from '../lib/taxonomy'

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
  lng?: number | string
  lat?: number | string
  cuisineTag?: string
  cuisineTags?: string[]
}

function normalizeAssetUrl(url: string | undefined): string {
  if (!url) return '/images/auth-bg-login.png'
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function toCoord(v: number | string | undefined): number | undefined {
  const n = typeof v === 'string' ? Number(v) : v
  return Number.isFinite(n) ? (n as number) : undefined
}

/** 店铺名规范化：跨目的地、跨 foodId 时仍能识别为同一家店 */
export function normalizeFoodShopLabel(label: string): string {
  return label
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/[（）]/g, (c) => (c === '（' ? '(' : ')'))
}

/** 全局去重键：优先设施/店名，避免同一店铺挂多个 destinationId 时重复展示 */
export function foodVODedupeKey(vo: FoodVO): string {
  if (vo.facilityId != null && vo.facilityId > 0) return `facility:${vo.facilityId}`
  const shop = normalizeFoodShopLabel(vo.shopName || vo.name || '')
  if (shop.length >= 2) return `shop:${shop}`
  const cover = (vo.coverUrl || '').trim().toLowerCase()
  if (cover) return `cover:${cover}`
  if (vo.id != null) return `id:${vo.id}`
  return `row:${vo.destinationId}:${vo.foodType ?? ''}:${vo.name ?? ''}`
}

export function foodDedupeKey(f: Food): string {
  const shop = normalizeFoodShopLabel(f.name || '')
  if (shop.length >= 2) return `shop:${shop}`
  const cover = (f.image || '').trim().toLowerCase()
  if (cover && !cover.includes('auth-bg-login')) return `cover:${cover}`
  if (f.id != null) return `id:${f.id}`
  return `row:${f.name}-${f.dish}`
}

export function foodVOToFood(vo: FoodVO): Food {
  const rating =
    vo.ratingScore != null ? String(vo.ratingScore) : vo.heatScore != null ? String(vo.heatScore) : '—'
  const cuisineTags = resolveFoodCuisineTags({
    cuisineTags: vo.cuisineTags,
    cuisineTag: vo.cuisineTag,
    foodType: vo.foodType,
    name: vo.shopName ?? vo.name,
    dish: vo.name,
    tags: vo.foodType ? [vo.foodType] : [],
  })
  const cuisineTag = cuisineTags[0] ?? vo.cuisineTag ?? null
  const tags = cuisineTags.length
    ? cuisineTags
    : vo.foodType
      ? [vo.foodType]
      : []
  const lng = toCoord(vo.lng)
  const lat = toCoord(vo.lat)
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
    cuisineTag,
    cuisineTags,
    foodType: vo.foodType ?? null,
    description: vo.description?.trim(),
    lng,
    lat,
    heatScore: vo.heatScore,
    ratingScore: vo.ratingScore,
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
  for (let i = 0; i < destinationIds.length; i += concurrency) {
    const slice = destinationIds.slice(i, i + concurrency)
    const batches = await Promise.all(
      slice.map((destinationId) =>
        searchFoods(destinationId, keyword, { pageNum: 1, pageSize, sortBy: opts.sortBy }),
      ),
    )
    for (const list of batches) {
      for (const row of list) {
        const k = foodVODedupeKey(row)
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
  /** 标准菜系标签，后端上线后生效；未上线时前端本地排序 */
  cuisineTags?: string[]
  sortBy?: string
  pageNum?: number
  pageSize?: number
  topK?: number
}

const FOOD_RECOMMEND_MAX_PAGE_SIZE = 100

function foodRecommendPageSize(params: FetchRecommendedFoodsParams): number {
  const raw = params.pageSize ?? 50
  return Math.min(Math.max(Math.floor(raw), 1), FOOD_RECOMMEND_MAX_PAGE_SIZE)
}

export async function fetchRecommendedFoodsPage(
  destinationId: number,
  params: FetchRecommendedFoodsParams = {},
): Promise<NormalizedPage<FoodVO>> {
  const pageNum = Math.max(1, Math.floor(params.pageNum ?? 1))
  const pageSize = foodRecommendPageSize(params)
  const query = new URLSearchParams({
    destinationId: String(destinationId),
    pageNum: String(pageNum),
    pageSize: String(pageSize),
  })
  if (params.facilityId != null) query.set('facilityId', String(params.facilityId))
  const cuisineDbTypes =
    params.cuisineTags?.length ? foodTypesForCuisines(params.cuisineTags) : []
  if (cuisineDbTypes.length === 1) query.set('foodType', cuisineDbTypes[0])
  else if (params.foodType) query.set('foodType', params.foodType)
  if (params.cuisineTags?.length) {
    for (const tag of params.cuisineTags) query.append('cuisineTags', tag)
  }
  query.set('sortBy', params.sortBy ?? 'heat')

  // 仅显式 topK（如 Top10）；常规列表走 pageNum + pageSize，与后端 recommend 分页对齐
  if (typeof params.topK === 'number') {
    query.set('topK', String(Math.min(Math.max(params.topK, 1), FOOD_RECOMMEND_MAX_PAGE_SIZE)))
  }

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
