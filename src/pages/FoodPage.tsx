import { useCallback, useEffect, useMemo, useRef, useState, type MutableRefObject } from 'react'
import { fetchRecommendedDestinationsPage, type PageResult } from '../api/destination'
import { inferTotalPages } from '../api/pagination'
import type { FoodVO } from '../api/food'
import {
  fetchRecommendedFoods,
  fetchRecommendedFoodsPage,
  foodVOToFood,
  searchFoodsAcrossDestinations,
} from '../api/food'
import { foodTags, foods as foodsFallback, type Food } from '../data/siteData'
import { useTripContext } from '../context/tripContext'
import { CommentSection } from '../components/ui/CommentSection'
import { DetailOverlay } from '../components/ui/DetailOverlay'
import { RatingPanel } from '../components/ui/RatingPanel'
import { Top10Strip } from '../components/ui/Top10Strip'
import { BEIJING_ATTRACTIONS } from '../data/beijingDestinations'
import { formatDistanceMeters, haversineMeters } from '../lib/geo/haversine'
import { readSessionCache, writeSessionCache } from '../lib/sessionCache'

const glass =
  'rounded-[28px] border border-white/80 bg-white/65 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl'

const waterfallCard =
  'mb-6 break-inside-avoid rounded-[28px] border border-[var(--ds-primary)]/12 bg-white/75 shadow-[0_8px_32px_rgba(42,107,78,0.06)] backdrop-blur-md transition duration-500 hover:-translate-y-1 hover:shadow-[0_24px_70px_rgba(42,107,78,0.12)]'

const sidebarTitle =
  "mb-5 flex items-center gap-2.5 text-[13px] font-bold uppercase tracking-[0.12em] text-[var(--ds-accent-foreground)] before:block before:h-[18px] before:w-1 before:rounded-full before:bg-gradient-to-b before:from-[var(--ds-primary)] before:to-[#6B8076] font-body"

const DEST_BATCH = 40
/** 首屏轻量扫目的地时的并行度（下滑加载不再走此路径，避免数百次 foods/recommend） */
const DEST_PARALLEL_CHUNK = 6
const FOOD_PAGE_SIZE = 32
const MAX_FOOD_WAVE = 24
/** 仅首屏 runFoodFeed 内：浏览扫描最大步数 */
const BROWSE_PUMP_MAX_ITERATIONS = 24
/** 探测哪些 destinationId 上挂了美食（缩小范围减请求） */
const FOOD_ANCHOR_PROBE_MAX = 80
const FOOD_ANCHOR_CHUNK = 10
const PROBE_CACHE_KEY = 'trip_food_anchor_ids_v1'
const PROBE_CACHE_TTL_MS = 24 * 60 * 60 * 1000
/** 首屏只拉第 1 页，其余页后台预取 */
const INITIAL_ANCHOR_PREFETCH_PAGES = 1
/** 后台为锚点连续预取的上限页数 */
const BACKGROUND_ANCHOR_PREFETCH_PAGES = 4
/** 单次「加载更多」只为锚点目的地连续请求几页，填满瀑布流且不把接口打爆 */
const ANCHOR_PAGES_PER_LOAD_MORE = 4

async function probeFoodAnchoredDestinationIds(): Promise<number[]> {
  const found: number[] = []
  for (let start = 1; start <= FOOD_ANCHOR_PROBE_MAX; start += FOOD_ANCHOR_CHUNK) {
    const ids: number[] = []
    for (let i = 0; i < FOOD_ANCHOR_CHUNK && start + i <= FOOD_ANCHOR_PROBE_MAX; i++) ids.push(start + i)
    const rows = await Promise.all(
      ids.map((destinationId) =>
        fetchRecommendedFoods(destinationId, {
          pageNum: 1,
          pageSize: 1,
          sortBy: 'heat',
        }).catch(() => [] as FoodVO[]),
      ),
    )
    ids.forEach((id, idx) => {
      if (rows[idx].length > 0) found.push(id)
    })
  }
  return [...new Set(found)].sort((a, b) => a - b)
}

async function resolveFoodAnchorIds(): Promise<number[]> {
  const cached = readSessionCache<number[]>(PROBE_CACHE_KEY, PROBE_CACHE_TTL_MS)
  if (cached?.length) return cached
  const anchored = await probeFoodAnchoredDestinationIds()
  if (anchored.length) writeSessionCache(PROBE_CACHE_KEY, anchored)
  return anchored
}

async function prefetchAnchorFoodPages(
  anchored: number[],
  fromPage: number,
  toPage: number,
  appendFoodVOs: (vos: FoodVO[]) => number,
  anchorNextFoodPageRef: MutableRefObject<Map<number, number>>,
): Promise<void> {
  await Promise.all(
    anchored.map(async (id) => {
      for (let p = fromPage; p <= toPage; p++) {
        const res = await fetchRecommendedFoodsPage(id, {
          pageNum: p,
          pageSize: FOOD_PAGE_SIZE,
          sortBy: 'heat',
        }).catch(() => null)
        if (!res || res.list.length === 0) {
          anchorNextFoodPageRef.current.delete(id)
          break
        }
        appendFoodVOs(res.list)
        const cap = inferTotalPages(
          { list: res.list, pageNum: res.pageNum, pageSize: res.pageSize, total: res.total, pages: res.pages },
          res.pageSize || FOOD_PAGE_SIZE,
          p,
        )
        const advanced = p + 1
        if (advanced > cap) anchorNextFoodPageRef.current.delete(id)
        else anchorNextFoodPageRef.current.set(id, advanced)
      }
    }),
  )
}

function FoodCardSkeleton() {
  return (
    <article className={`animate-pulse overflow-hidden ${waterfallCard}`}>
      <div className="h-[200px] bg-gradient-to-br from-[var(--ds-muted)] to-[#F5ECD8]" />
      <div className="space-y-3 p-5">
        <div className="h-5 w-2/3 rounded-lg bg-[var(--ds-muted)]" />
        <div className="h-4 w-full rounded-lg bg-[var(--ds-muted)]" />
        <div className="h-4 w-4/5 rounded-lg bg-[var(--ds-muted)]" />
      </div>
    </article>
  )
}

function foodVODedupeKey(vo: FoodVO): string {
  const shop = (vo.shopName?.trim() || vo.name?.trim() || '').toLowerCase()
  if (shop) return `shop:${shop}`
  return vo.id != null
    ? `id:${vo.id}`
    : `k:${vo.destinationId}:${vo.foodType ?? ''}`
}

function foodKey(f: Food) {
  return f.id != null ? `id:${f.id}` : `${f.name}-${f.dish}`
}

/** columns + 后端重复 id 时，单列 key 可能冲突导致卡片渲染残缺 */
function foodReactKey(f: Food, idx: number): string {
  if (f.id != null && f.destinationId != null) return `d${f.destinationId}-i${f.id}`
  return `${foodKey(f)}-${idx}`
}

export function FoodPage() {
  const { destinationId: ctxDestinationId, destinationName: ctxDestinationName } = useTripContext()
  const [foods, setFoods] = useState<Food[]>([])
  const [scopeDestinationId, setScopeDestinationId] = useState<number | null>(ctxDestinationId)
  const [scopeAll, setScopeAll] = useState(!ctxDestinationId)
  const [foodTop10, setFoodTop10] = useState<Food[]>([])
  const [loadingFoodTop10, setLoadingFoodTop10] = useState(false)
  const [foodSearch, setFoodSearch] = useState('')
  const [selectedFoodTag, setSelectedFoodTag] = useState('')
  const [dynamicFoodTags, setDynamicFoodTags] = useState<string[]>([])
  const [listSort, setListSort] = useState<'heat' | 'rating' | 'distance'>('heat')
  const [mode, setMode] = useState<'browse' | 'search'>('browse')
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [usingFallback, setUsingFallback] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailIndex, setDetailIndex] = useState(0)
  const [moreAvailable, setMoreAvailable] = useState(true)

  const seenFoodKeysRef = useRef<Set<string>>(new Set())
  const seenDestinationIdsRef = useRef<Set<number>>(new Set())
  const destIdsRef = useRef<number[]>([])
  const nextDestPageRef = useRef(1)
  const destTotalPagesRef = useRef(Number.MAX_SAFE_INTEGER)
  const browseDestIdxRef = useRef(0)
  /** 对所有目的地统一尝试的第 N 页美食（景区目的地常无美食，要靠页码与并行扩大命中率） */
  const foodWavePageRef = useRef(1)
  /** 已通过 probe 发现挂了美食数据的目的地，分页由此单独承担，避免与浏览扫描逻辑打架 */
  const foodAnchorIdsRef = useRef<number[]>([])
  const foodAnchorIdSetRef = useRef<Set<number>>(new Set())
  /** destinationId -> 下一页 pageNum */
  const anchorNextFoodPageRef = useRef<Map<number, number>>(new Map())
  /** 浏览扫描因 wave 上限已终止；此时若锚点也无下一页才算真正耗尽 */
  const browseFeedTerminatedRef = useRef(false)

  const searchKeywordRef = useRef('')
  const searchDestPageRef = useRef(1)
  const searchDestTotalPagesRef = useRef(Number.MAX_SAFE_INTEGER)

  const loadMoreRef = useRef<HTMLDivElement | null>(null)
  const loadingLockRef = useRef(false)
  const lastBrowsePumpAtRef = useRef(0)
  const pumpMoreRef = useRef<() => Promise<void>>(async () => {})

  const appendFoodVOs = useCallback((vos: FoodVO[]): number => {
    const fresh: Food[] = []
    for (const vo of vos) {
      const key = foodVODedupeKey(vo)
      if (seenFoodKeysRef.current.has(key)) continue
      seenFoodKeysRef.current.add(key)
      fresh.push(foodVOToFood(vo))
    }
    if (fresh.length === 0) return 0
    setFoods((prev) => [...prev, ...fresh])
    return fresh.length
  }, [])

  const pumpNextAnchorFoodPage = useCallback(async (): Promise<boolean> => {
    let progressed = false
    outer: for (const id of foodAnchorIdsRef.current) {
      for (let slot = 0; slot < ANCHOR_PAGES_PER_LOAD_MORE; slot++) {
        const next = anchorNextFoodPageRef.current.get(id)
        if (next == null) continue outer

        const res = await fetchRecommendedFoodsPage(id, {
          pageNum: next,
          pageSize: FOOD_PAGE_SIZE,
          sortBy: 'heat',
        })

        const pageEnvelope: PageResult<FoodVO> = {
          list: res.list,
          pageNum: res.pageNum,
          pageSize: res.pageSize,
          total: res.total,
          pages: res.pages,
        }
        const cap = inferTotalPages(pageEnvelope, res.pageSize || FOOD_PAGE_SIZE, next)
        if (next > cap) {
          anchorNextFoodPageRef.current.delete(id)
          continue outer
        }

        if (res.list.length === 0) {
          anchorNextFoodPageRef.current.delete(id)
          continue outer
        }

        appendFoodVOs(res.list)
        progressed = true

        const advanced = next + 1
        if (advanced > cap) {
          anchorNextFoodPageRef.current.delete(id)
          continue outer
        }
        anchorNextFoodPageRef.current.set(id, advanced)
      }
    }
    return progressed
  }, [appendFoodVOs])

  const resetBrowseRefs = () => {
    destIdsRef.current = []
    seenDestinationIdsRef.current = new Set()
    nextDestPageRef.current = 1
    destTotalPagesRef.current = Number.MAX_SAFE_INTEGER
    browseDestIdxRef.current = 0
    foodWavePageRef.current = 1
    browseFeedTerminatedRef.current = false
    foodAnchorIdsRef.current = []
    foodAnchorIdSetRef.current.clear()
    anchorNextFoodPageRef.current.clear()
  }

  const fetchNextDestinationBatch = useCallback(async (): Promise<boolean> => {
    const pageNum = nextDestPageRef.current
    if (pageNum > destTotalPagesRef.current) return false
    const res = await fetchRecommendedDestinationsPage({
      pageNum,
      pageSize: DEST_BATCH,
      sortBy: 'recommend',
    })
    destTotalPagesRef.current = inferTotalPages(res, DEST_BATCH, pageNum)
    nextDestPageRef.current = pageNum + 1
    for (const d of res.list) {
      const id = d.id
      if (id == null || seenDestinationIdsRef.current.has(id)) continue
      if (foodAnchorIdSetRef.current.has(id)) {
        seenDestinationIdsRef.current.add(id)
        continue
      }
      seenDestinationIdsRef.current.add(id)
      destIdsRef.current.push(id)
    }
    return true
  }, [])

  const pumpBrowseBatch = useCallback(async (): Promise<boolean> => {
    let iterations = 0
    while (iterations++ < BROWSE_PUMP_MAX_ITERATIONS) {
      if (browseDestIdxRef.current >= destIdsRef.current.length) {
        const ok = await fetchNextDestinationBatch()
        if (!ok) {
          foodWavePageRef.current += 1
          browseDestIdxRef.current = 0
          if (foodWavePageRef.current > MAX_FOOD_WAVE) {
            browseFeedTerminatedRef.current = true
            return false
          }
          continue
        }
        if (browseDestIdxRef.current >= destIdsRef.current.length) continue
      }

      const start = browseDestIdxRef.current
      const end = Math.min(destIdsRef.current.length, start + DEST_PARALLEL_CHUNK)
      const ids = destIdsRef.current.slice(start, end)
      browseDestIdxRef.current = end

      const wavePage = foodWavePageRef.current
      const lists = await Promise.all(
        ids.map((destinationId) =>
          fetchRecommendedFoods(destinationId, {
            pageNum: wavePage,
            pageSize: FOOD_PAGE_SIZE,
            sortBy: 'heat',
          }).catch(() => [] as FoodVO[]),
        ),
      )
      const flat = lists.flat()
      if (flat.length > 0) {
        const added = appendFoodVOs(flat)
        if (added > 0) return true
        /** 有返回数据但全部被去重：当作未命中，继续扫后续目的地 */
      }
    }
    return false
  }, [appendFoodVOs, fetchNextDestinationBatch])

  const pumpSearchBatch = useCallback(async (): Promise<boolean> => {
    const kw = searchKeywordRef.current
    if (!kw.trim()) return false
    const pageNum = searchDestPageRef.current
    if (pageNum > searchDestTotalPagesRef.current) return false
    const res = await fetchRecommendedDestinationsPage({
      pageNum,
      pageSize: DEST_BATCH,
      sortBy: 'recommend',
    })
    searchDestTotalPagesRef.current = inferTotalPages(res, DEST_BATCH, pageNum)
    searchDestPageRef.current = pageNum + 1
    const newIds = res.list.map((d) => d.id)
    if (newIds.length === 0) return false
    const vos = await searchFoodsAcrossDestinations(newIds, kw, { pageSize: 28 })
    return appendFoodVOs(vos) > 0
  }, [appendFoodVOs])

  useEffect(() => {
    pumpMoreRef.current = async () => {
      if (loadingLockRef.current || loadingInitial || !moreAvailable) return
      if (mode === 'browse') {
        const now = Date.now()
        /** 浏览模式：防止 IO 与 scroll 连触发造成请求风暴 */
        if (now - lastBrowsePumpAtRef.current < 280) return
        lastBrowsePumpAtRef.current = now
      }

      loadingLockRef.current = true
      setLoadingMore(true)
      try {
        if (mode === 'browse') {
          /** 主路径：锚点目的地分页（同一 total 下的后续页），单次最多 ANCHOR_PAGES_PER_LOAD_MORE 个 foods 请求 */
          await pumpNextAnchorFoodPage()
          if (anchorNextFoodPageRef.current.size === 0) {
            if (foodAnchorIdsRef.current.length > 0) {
              setMoreAvailable(false)
            } else {
              /** 未发现锚点时：仅退回少量跨目的地扫描（与首屏同一上限），避免一页打出数百请求 */
              const progressed = await pumpBrowseBatch()
              if (!progressed) setMoreAvailable(false)
            }
          }
          return
        }
        const progressed = await pumpSearchBatch()
        if (!progressed) setMoreAvailable(false)
      } catch {
        /* 单次追加失败可忽略，用户可继续下滑重试 */
      } finally {
        loadingLockRef.current = false
        setLoadingMore(false)
      }
    }
  }, [
    mode,
    loadingInitial,
    moreAvailable,
    pumpBrowseBatch,
    pumpSearchBatch,
    pumpNextAnchorFoodPage,
  ])

  useEffect(() => {
    if (!detailOpen) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setDetailOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [detailOpen])

  useEffect(() => {
    if (usingFallback || loadingInitial || !moreAvailable) return

    let rafScroll = 0
    let scrollNudgeTimer: number | null = null
    const nudgeIfNearViewport = () => {
      const node = loadMoreRef.current
      if (!node) return
      const rect = node.getBoundingClientRect()
      const vh = window.innerHeight || document.documentElement.clientHeight
      if (rect.top <= vh + 520) void pumpMoreRef.current()
    }

    const onScrollOrResize = () => {
      if (rafScroll) cancelAnimationFrame(rafScroll)
      rafScroll = requestAnimationFrame(() => {
        rafScroll = 0
        if (scrollNudgeTimer != null) return
        scrollNudgeTimer = window.setTimeout(() => {
          scrollNudgeTimer = null
          nudgeIfNearViewport()
        }, 120)
      })
    }

    const node = loadMoreRef.current
    if (!node) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) void pumpMoreRef.current()
        })
      },
      /** root 默认视口；扩大 rootMargin，避免列表短时 IO 不重触发 */
      { root: null, rootMargin: '380px 0px 520px 0px', threshold: 0 },
    )
    observer.observe(node)

    window.addEventListener('scroll', onScrollOrResize, { passive: true })
    window.addEventListener('resize', onScrollOrResize, { passive: true })
    requestAnimationFrame(nudgeIfNearViewport)

    return () => {
      observer.disconnect()
      window.removeEventListener('scroll', onScrollOrResize)
      window.removeEventListener('resize', onScrollOrResize)
      if (rafScroll) cancelAnimationFrame(rafScroll)
      if (scrollNudgeTimer != null) window.clearTimeout(scrollNudgeTimer)
    }
    /** 不依赖 foods.length：避免因每条追加都断开 IO 导致「已在视区内却不回调」 */
  }, [usingFallback, loadingInitial, mode, moreAvailable])

  const activeFoodTags = useMemo(
    () => (dynamicFoodTags.length > 0 ? dynamicFoodTags : foodTags),
    [dynamicFoodTags],
  )

  const anchorCoords = useMemo(() => {
    if (!scopeDestinationId) return null
    const hit = BEIJING_ATTRACTIONS.find((a) => a.destinationId === scopeDestinationId)
    if (hit) return { lng: hit.lng, lat: hit.lat }
    return null
  }, [scopeDestinationId])

  const filteredFoods = useMemo(() => {
    const filtered = foods.filter((f) => {
      const matchSearch =
        !foodSearch.trim() ||
        f.name.includes(foodSearch) ||
        f.dish.includes(foodSearch) ||
        f.tags.some((t) => t.includes(foodSearch))
      const matchTag = !selectedFoodTag || f.tags.includes(selectedFoodTag)
      return matchSearch && matchTag
    })
    const sorted = [...filtered]
    if (listSort === 'rating') {
      sorted.sort((a, b) => (b.ratingScore ?? 0) - (a.ratingScore ?? 0))
    } else if (listSort === 'distance' && anchorCoords) {
      const dist = (f: Food) => {
        if (f.lng == null || f.lat == null) return Number.POSITIVE_INFINITY
        return haversineMeters(anchorCoords.lng, anchorCoords.lat, f.lng, f.lat)
      }
      sorted.sort((a, b) => dist(a) - dist(b))
    } else {
      sorted.sort((a, b) => (b.heatScore ?? 0) - (a.heatScore ?? 0))
    }
    return sorted.map((f) => {
      if (listSort !== 'distance' || !anchorCoords || f.lng == null || f.lat == null) return f
      const m = haversineMeters(anchorCoords.lng, anchorCoords.lat, f.lng, f.lat)
      return { ...f, distance: formatDistanceMeters(m) }
    })
  }, [foods, foodSearch, selectedFoodTag, listSort, anchorCoords])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const destId = scopeDestinationId ?? ctxDestinationId ?? 1
        const page = await fetchRecommendedFoodsPage(destId, { pageSize: 40, sortBy: 'heat' })
        if (cancelled) return
        const types = [...new Set((page.list ?? []).map((f) => f.foodType).filter(Boolean))] as string[]
        if (types.length) setDynamicFoodTags(types.slice(0, 12))
      } catch {
        /* 保留静态标签 */
      }
    })()
    return () => {
      cancelled = true
    }
  }, [scopeDestinationId, ctxDestinationId])

  useEffect(() => {
    if (ctxDestinationId) {
      setScopeDestinationId(ctxDestinationId)
      setScopeAll(false)
    }
  }, [ctxDestinationId])

  useEffect(() => {
    let cancelled = false
    const destId = scopeAll ? null : scopeDestinationId
    if (!destId) {
      setFoodTop10([])
      return
    }
    setLoadingFoodTop10(true)
    void fetchRecommendedFoodsPage(destId, { topK: 10, pageSize: 10, sortBy: 'heat' })
      .then((res) => {
        if (!cancelled) setFoodTop10(res.list.map(foodVOToFood))
      })
      .catch(() => {
        if (!cancelled) setFoodTop10([])
      })
      .finally(() => {
        if (!cancelled) setLoadingFoodTop10(false)
      })
    return () => {
      cancelled = true
    }
  }, [scopeAll, scopeDestinationId])

  const runFoodFeed = async (keyword: string) => {
    setError(null)
    setLoadingInitial(true)
    setMoreAvailable(true)
    seenFoodKeysRef.current = new Set()
    setFoods([])
    const kw = keyword.trim()
    if (!kw) {
      setMode('browse')
      resetBrowseRefs()
      try {
        if (!scopeAll && scopeDestinationId) {
          foodAnchorIdsRef.current = [scopeDestinationId]
          foodAnchorIdSetRef.current = new Set([scopeDestinationId])
          anchorNextFoodPageRef.current.clear()
          await prefetchAnchorFoodPages(
            [scopeDestinationId],
            1,
            INITIAL_ANCHOR_PREFETCH_PAGES,
            appendFoodVOs,
            anchorNextFoodPageRef,
          )
          setMoreAvailable(true)
          setUsingFallback(false)
          setLoadingInitial(false)
          void prefetchAnchorFoodPages(
            [scopeDestinationId],
            INITIAL_ANCHOR_PREFETCH_PAGES + 1,
            BACKGROUND_ANCHOR_PREFETCH_PAGES,
            appendFoodVOs,
            anchorNextFoodPageRef,
          )
          return
        }

        const anchored = await resolveFoodAnchorIds()
        foodAnchorIdsRef.current = anchored
        foodAnchorIdSetRef.current = new Set(anchored)
        anchorNextFoodPageRef.current.clear()

        if (anchored.length > 0) {
          await prefetchAnchorFoodPages(
            anchored,
            1,
            INITIAL_ANCHOR_PREFETCH_PAGES,
            appendFoodVOs,
            anchorNextFoodPageRef,
          )
        }

        setMoreAvailable(anchored.length > 0 || destIdsRef.current.length > 0)
        setUsingFallback(false)
        setLoadingInitial(false)

        void (async () => {
          if (anchored.length > 0 && BACKGROUND_ANCHOR_PREFETCH_PAGES > INITIAL_ANCHOR_PREFETCH_PAGES) {
            await prefetchAnchorFoodPages(
              anchored,
              INITIAL_ANCHOR_PREFETCH_PAGES + 1,
              BACKGROUND_ANCHOR_PREFETCH_PAGES,
              appendFoodVOs,
              anchorNextFoodPageRef,
            )
          }
          for (let attempt = 0; attempt < 2; attempt++) {
            const before = seenFoodKeysRef.current.size
            await pumpBrowseBatch()
            if (seenFoodKeysRef.current.size > before) break
          }
        })()
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载失败')
        setFoods(foodsFallback)
        setUsingFallback(true)
      } finally {
        setLoadingInitial(false)
      }
      return
    }
    setMode('search')
    searchKeywordRef.current = kw
    searchDestPageRef.current = 1
    searchDestTotalPagesRef.current = Number.MAX_SAFE_INTEGER
    try {
      const p1 = await pumpSearchBatch()
      const p2 = await pumpSearchBatch()
      setMoreAvailable(p1 || p2)
      setUsingFallback(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索失败')
      setFoods(foodsFallback)
      setUsingFallback(true)
    } finally {
      setLoadingInitial(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    queueMicrotask(() => {
      if (cancelled) return
      void runFoodFeed('')
    })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 仅首屏聚合加载
  }, [])

  const openDetail = (food: Food) => {
    const idx = filteredFoods.findIndex((f) => foodKey(f) === foodKey(food))
    setDetailIndex(idx >= 0 ? idx : 0)
    setDetailOpen(true)
  }

  const stepDetail = (dir: -1 | 1) => {
    if (filteredFoods.length === 0) return
    setDetailIndex((i) => Math.max(0, Math.min(filteredFoods.length - 1, i + dir)))
  }

  const detailFood = filteredFoods[detailIndex] ?? null

  const showLoadSentinel = !usingFallback && !loadingInitial && moreAvailable

  return (
    <div className="mx-auto max-w-[1400px] animate-fade-rise px-5 py-10 md:px-8">
      <header className="mb-12">
        <div className="mb-5 inline-flex items-center gap-2.5 rounded-full border border-[var(--ds-primary)]/15 bg-[var(--ds-muted)] px-4 py-2 font-display text-[11px] font-semibold uppercase italic tracking-[0.2em] text-[var(--ds-primary)]">
          Food Guide
        </div>
        <h1 className="font-display text-4xl font-semibold tracking-wide text-[#2C3E36] md:text-[36px]">
          美食推荐
        </h1>
        <p className="mt-3 font-body text-[15.5px] text-[#6B8076]">
          瀑布流懒加载：滑到底部自动向后台拉取更多数据；支持关键词检索。
        </p>
        {!scopeAll && scopeDestinationId ? (
          <div className="mt-4 flex flex-wrap items-center gap-2">
            <span className="rounded-full border border-[var(--ds-primary)]/20 bg-white/80 px-4 py-1.5 text-sm font-semibold text-[var(--ds-primary)]">
              当前：{ctxDestinationName ?? `目的地 #${scopeDestinationId}`}
            </span>
            <button
              type="button"
              onClick={() => {
                setScopeAll(true)
                void runFoodFeed('')
              }}
              className="rounded-full border border-[var(--ds-primary)]/15 px-4 py-1.5 text-sm text-[#6B8076] hover:text-[var(--ds-primary)]"
            >
              查看全城美食
            </button>
          </div>
        ) : null}
      </header>

      <div className="grid gap-10 lg:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="lg:sticky lg:top-24 lg:self-start">
          <div className={`p-7 ${glass}`}>
            <h3 className={sidebarTitle}>搜索美食</h3>
            <input
              value={foodSearch}
              onChange={(e) => setFoodSearch(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && void runFoodFeed(foodSearch)}
              placeholder="搜一家店或一道菜..."
              className="mb-3 w-full rounded-[28px] border border-[var(--ds-primary)]/10 bg-white/85 px-5 py-3.5 font-body text-[14.5px] outline-none focus:ring-4 focus:ring-[var(--ds-primary)]/10"
            />
            <button
              type="button"
              onClick={() => void runFoodFeed(foodSearch)}
              className="mb-4 w-full rounded-full bg-[var(--ds-primary)] px-4 py-2.5 font-body text-sm font-semibold text-white transition hover:brightness-110"
            >
              {foodSearch.trim() ? '关键词检索' : '刷新推荐'}
            </button>
            <p className="mb-2 font-body text-xs font-semibold text-[var(--ds-muted-foreground)]">排序</p>
            <div className="mb-7 flex flex-wrap gap-2">
              {(
                [
                  { value: 'heat', label: '热度' },
                  { value: 'rating', label: '评分' },
                  { value: 'distance', label: '距离' },
                ] as const
              ).map((s) => (
                <button
                  key={s.value}
                  type="button"
                  onClick={() => setListSort(s.value)}
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                    listSort === s.value
                      ? 'bg-[var(--ds-primary)] text-white'
                      : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
                  }`}
                >
                  {s.label}
                </button>
              ))}
            </div>
            {listSort === 'distance' && !anchorCoords ? (
              <p className="mb-4 font-body text-[11px] text-amber-800">
                选择带坐标的目的地后可按直线距离排序（后端 distance 接口上线后将自动对接）。
              </p>
            ) : null}
            {!scopeAll && scopeDestinationId ? (
              <Top10Strip
                title="本目的地美食 Top10"
                loading={loadingFoodTop10}
                items={foodTop10.map((f, fi) => ({
                  id: foodReactKey(f, fi),
                  name: f.name,
                  meta: f.price,
                  image: f.image,
                  onClick: () => openDetail(f),
                }))}
              />
            ) : null}
            <h3 className={sidebarTitle}>菜系标签</h3>
            <div className="flex flex-col gap-2.5">
              {activeFoodTags.map((t) => {
                const on = selectedFoodTag === t
                return (
                  <button
                    key={t}
                    type="button"
                    onClick={() => setSelectedFoodTag(on ? '' : t)}
                    className={`flex justify-center rounded-full border px-5 py-2 font-body text-sm font-medium transition ${
                      on
                        ? 'border-white/30 bg-gradient-to-br from-[var(--ds-primary)] to-[color-mix(in srgb, var(--ds-primary) 88%, white)] text-white shadow-md'
                        : 'border-[var(--ds-primary)]/10 bg-white/70 text-[var(--ds-accent-foreground)] hover:bg-white'
                    }`}
                  >
                    {t}
                  </button>
                )
              })}
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          {usingFallback ? (
            <p className="mb-4 rounded-2xl border border-amber-200/80 bg-amber-50/90 px-4 py-3 font-body text-sm text-amber-900">
              后端暂时不可用，已展示本地示例美食。{error ? `（${error}）` : ''}
            </p>
          ) : null}
          {loadingInitial ? (
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <FoodCardSkeleton key={`sk-${i}`} />
              ))}
            </div>
          ) : null}
          {!loadingInitial && !usingFallback && foods.length === 0 ? (
            <p className="mb-4 font-body text-sm text-[#6B8076]">暂无美食数据，请下滑加载或更换关键词。</p>
          ) : null}

          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
            {!loadingInitial && foods.length > 0 && filteredFoods.length === 0 ? (
              <p className="break-inside-avoid py-10 text-center font-body text-sm text-[#6B8076]">
                当前筛选下没有匹配结果。
              </p>
            ) : null}
            {filteredFoods.map((food, fi) => (
              <article
                key={foodReactKey(food, fi)}
                role="button"
                tabIndex={0}
                className={`group cursor-pointer overflow-hidden ${waterfallCard}`}
                onClick={() => openDetail(food)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    openDetail(food)
                  }
                }}
              >
                <div className="relative h-[200px] overflow-hidden bg-gradient-to-br from-[var(--ds-muted)] to-[#F5ECD8]">
                  <img
                    src={food.image}
                    alt=""
                    loading="lazy"
                    decoding="async"
                    className="img-warm h-full w-full object-cover transition duration-700 group-hover:scale-[1.06]"
                  />
                  <div className="absolute left-3.5 top-3.5 rounded-full border border-white/80 bg-white/90 px-3.5 py-1 text-xs font-semibold text-[var(--ds-accent-foreground)] shadow-sm backdrop-blur-sm">
                    {food.distance}
                  </div>
                </div>
                <div className="p-5">
                  <h3 className="font-display text-[19px] font-bold tracking-wide text-[#2C3E36]">
                    {food.name}
                  </h3>
                  <p className="mt-1.5 line-clamp-2 font-body text-sm text-[#6B8076]">招牌：{food.dish}</p>
                  <div className="mb-4 mt-4 flex items-center justify-between">
                    <span className="font-body text-sm text-[var(--ds-accent-foreground)]">
                      人均{' '}
                      <strong className="font-display text-lg text-[#D4B896]">{food.price}</strong>
                    </span>
                    <div className="flex items-center gap-1">
                      <span className="text-[13px] text-[#D4B896]">★★★★</span>
                      <span className="text-[13px] font-semibold text-[#A3B5AD]">{food.rating}</span>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {food.tags.map((tag) => (
                      <span
                        key={tag}
                        className="rounded-full border border-[var(--ds-primary)]/12 bg-[var(--ds-primary)]/8 px-3 py-1 text-xs font-semibold text-[var(--ds-primary)]"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              </article>
            ))}
          </div>

          {showLoadSentinel ? (
            <div ref={loadMoreRef} className="mt-8 flex h-16 items-center justify-center">
              <span className="font-body text-xs text-[#6B8076]">
                {loadingMore ? '加载更多...' : '下滑加载更多'}
              </span>
            </div>
          ) : !usingFallback && !loadingInitial && foods.length > 0 ? (
            <p className="mt-6 text-center font-body text-xs text-[#8ca49a]">已加载全部（或可尝试关键词检索）</p>
          ) : null}
        </div>
      </div>

      {detailOpen && detailFood ? (
        <DetailOverlay
          open={detailOpen}
          title="美食详情"
          onClose={() => setDetailOpen(false)}
          onPrev={() => stepDetail(-1)}
          onNext={() => stepDetail(1)}
          indexLabel={`${detailIndex + 1} / ${filteredFoods.length}`}
        >
          <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="overflow-hidden rounded-2xl bg-[#edf4ef]">
              <img
                src={detailFood.image}
                alt=""
                className="max-h-[min(52vh,480px)] w-full object-cover"
                loading="lazy"
                decoding="async"
              />
            </div>
            <div className="space-y-4">
              <p className="font-display text-2xl font-semibold text-[#2C3E36]">{detailFood.name}</p>
              <p className="text-sm text-[#6B8076]">招牌：{detailFood.dish}</p>
              <div className="flex flex-wrap gap-3 text-sm text-[var(--ds-accent-foreground)]">
                <span>人均 {detailFood.price}</span>
                <span>{detailFood.distance}</span>
              </div>
              {detailFood.id != null ? (
                <RatingPanel
                  targetType="food"
                  targetId={detailFood.id}
                  average={
                    detailFood.ratingScore ??
                    (Number.isFinite(Number(detailFood.rating)) ? Number(detailFood.rating) : null)
                  }
                />
              ) : null}
              {detailFood.description ? (
                <p className="text-sm leading-relaxed text-[#4f655c]">{detailFood.description}</p>
              ) : (
                <p className="text-sm text-[#8ca49a]">暂无更多文案描述。</p>
              )}
              {detailFood.id != null ? (
                <CommentSection targetType="food" targetId={detailFood.id} title="美食评论" />
              ) : null}
            </div>
          </div>
        </DetailOverlay>
      ) : null}
    </div>
  )
}
