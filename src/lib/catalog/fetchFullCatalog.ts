import {
  destinationVOToDestination,
  fetchRecommendedDestinationsPage,
  type DestinationVO,
} from '../../api/destination'
import { fetchNonDemoDestinationIds } from '../../api/destinationAnchors'
import { inferTotalPages } from '../../api/pagination'
import { fetchRecommendedFoodsPage, type FoodVO } from '../../api/food'
import type { Destination } from '../../data/siteData'
import { isDemoDestination } from '../destination/isDemoDestination'
import { readSessionCache, writeSessionCache } from '../sessionCache'

const DEST_CATALOG_CACHE = 'trip_full_dest_catalog_v1'
const FOOD_CATALOG_CACHE = 'trip_full_food_catalog_v1'
const CATALOG_TTL_MS = 10 * 60 * 1000

const MAX_DEST_PAGES = 40
const DEST_PAGE_SIZE = 50
const MAX_FOOD_DEST_IDS = 200
const FOOD_PAGE_SIZE = 50
const FOOD_DEST_PARALLEL = 6
const MAX_FOOD_PAGES_PER_DEST = 8

export type CatalogProgress = {
  loaded: number
  phase?: string
}

function mergeUniqueDestinations(rows: DestinationVO[]): DestinationVO[] {
  const seen = new Set<number>()
  const out: DestinationVO[] = []
  for (const row of rows) {
    if (row.id == null || seen.has(row.id)) continue
    if (isDemoDestination(row.name, row.description)) continue
    seen.add(row.id)
    out.push(row)
  }
  return out
}

/** 分页拉取推荐接口中的全部目的地（用于标签全库筛选） */
export async function fetchAllDestinationsCatalog(
  onProgress?: (p: CatalogProgress) => void,
): Promise<Destination[]> {
  const cached = readSessionCache<Destination[]>(DEST_CATALOG_CACHE, CATALOG_TTL_MS)
  if (cached?.length) return cached

  const vos: DestinationVO[] = []
  let pageNum = 1
  let totalPages = 1

  while (pageNum <= totalPages && pageNum <= MAX_DEST_PAGES) {
    const res = await fetchRecommendedDestinationsPage({
      pageNum,
      pageSize: DEST_PAGE_SIZE,
      sortBy: 'heat',
    })
    totalPages = inferTotalPages(res, DEST_PAGE_SIZE, pageNum)
    vos.push(...mergeUniqueDestinations(res.list ?? []))
    onProgress?.({ loaded: vos.length, phase: `目的地第 ${pageNum}/${totalPages} 页` })
    pageNum += 1
  }

  const mapped = vos.map(destinationVOToDestination)
  writeSessionCache(DEST_CATALOG_CACHE, mapped)
  return mapped
}

function mergeUniqueFoodVOs(rows: FoodVO[], seen: Set<string>): FoodVO[] {
  const out: FoodVO[] = []
  for (const row of rows) {
    const key = row.id != null ? `id:${row.id}` : `row:${row.shopName ?? row.name}-${row.name}`
    if (seen.has(key)) continue
    seen.add(key)
    out.push(row)
  }
  return out
}

async function fetchFoodsForDestination(destinationId: number): Promise<FoodVO[]> {
  const out: FoodVO[] = []
  let pageNum = 1
  let totalPages = 1

  while (pageNum <= totalPages && pageNum <= MAX_FOOD_PAGES_PER_DEST) {
    const res = await fetchRecommendedFoodsPage(destinationId, {
      pageNum,
      pageSize: FOOD_PAGE_SIZE,
      sortBy: 'heat',
    }).catch(() => null)
    if (!res?.list?.length) break
    out.push(...res.list)
    totalPages = inferTotalPages(res, FOOD_PAGE_SIZE, pageNum)
    pageNum += 1
  }
  return out
}

/** 扫描多目的地美食分页，汇总全库美食（用于菜系标签全库筛选） */
export async function fetchAllFoodsCatalog(
  onProgress?: (p: CatalogProgress) => void,
): Promise<FoodVO[]> {
  const cached = readSessionCache<FoodVO[]>(FOOD_CATALOG_CACHE, CATALOG_TTL_MS)
  if (cached?.length) return cached

  const destIds = await fetchNonDemoDestinationIds(24, 50)
  const ids = (destIds.length ? destIds : await fetchNonDemoDestinationIds(40, 50)).slice(
    0,
    MAX_FOOD_DEST_IDS,
  )

  const seen = new Set<string>()
  const all: FoodVO[] = []

  for (let start = 0; start < ids.length; start += FOOD_DEST_PARALLEL) {
    const chunk = ids.slice(start, start + FOOD_DEST_PARALLEL)
    const batches = await Promise.all(chunk.map((id) => fetchFoodsForDestination(id)))
    for (const batch of batches) {
      all.push(...mergeUniqueFoodVOs(batch, seen))
    }
    onProgress?.({
      loaded: all.length,
      phase: `美食扫描 ${Math.min(start + FOOD_DEST_PARALLEL, ids.length)}/${ids.length} 个目的地`,
    })
  }

  writeSessionCache(FOOD_CATALOG_CACHE, all)
  return all
}

export function invalidateDestinationCatalogCache(): void {
  writeSessionCache(DEST_CATALOG_CACHE, [])
}

export function invalidateFoodCatalogCache(): void {
  writeSessionCache(FOOD_CATALOG_CACHE, [])
}
