import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchRecommendedDestinationsPage,
  searchDestinationsPage,
  type DestinationVO,
} from '../../api/destination'
import { inferTotalPages, shouldStopRecommendPagination } from '../../api/pagination'
import { fetchRecommendedFoodsPage } from '../../api/food'
import { BEIJING_ATTRACTIONS, type BeijingAttraction } from '../../data/beijingDestinations'
import {
  isDemoDestination,
  sanitizeDestinationDisplayName,
} from '../../lib/destination/isDemoDestination'
import { fetchAllDestinationsCatalog } from '../../lib/catalog/fetchFullCatalog'
import { resolveCoordsFromLocal, resolveDestinationCoords } from '../../lib/geo/resolveCoords'
import type { RouteWaypoint } from '../../types/macroRoute'
import { InlineNotice } from '../ui/InlineNotice'

export type PickerItem = BeijingAttraction & { destinationId: number }

const PAGE_SIZE = 32

function isBeijingRow(city?: string, name?: string): boolean {
  const hay = `${city ?? ''}${name ?? ''}`
  return hay.includes('北京') || BEIJING_ATTRACTIONS.some((a) => a.name === name)
}

function resolveCoords(vo: DestinationVO): { lng: number; lat: number } | null {
  return resolveCoordsFromLocal(vo.name)
}

function voToPickerItem(vo: DestinationVO): PickerItem | null {
  const id = vo.id
  if (typeof id !== 'number' || id <= 0) return null
  if (isDemoDestination(vo.name, vo.description)) return null
  const displayName = sanitizeDestinationDisplayName(vo.name)
  if (!isBeijingRow(vo.city, vo.name) && !isBeijingRow(vo.city, displayName)) return null
  const coords = resolveCoords({ ...vo, name: vo.name })
  const fb = BEIJING_ATTRACTIONS.find((a) => a.name === vo.name || a.name === displayName)
  return {
    id,
    destinationId: id,
    name: displayName,
    city: vo.city ?? '北京',
    lng: coords?.lng ?? 0,
    lat: coords?.lat ?? 0,
    reason: vo.description?.trim() || vo.tags?.join('、') || '北京市内目的地',
    rating: vo.ratingScore ?? 4.5,
    badge: vo.category || vo.tags?.[0] || '推荐',
    type: vo.category || vo.type || '景点',
    image: vo.coverUrl?.startsWith('http')
      ? vo.coverUrl
      : fb?.image ?? '/images/recommend-hero-new.png',
  }
}

function mergePickerItems(prev: PickerItem[], next: PickerItem[]): { merged: PickerItem[]; added: number } {
  const seen = new Set(prev.map((p) => p.destinationId))
  const merged = [...prev]
  let added = 0
  for (const row of next) {
    if (seen.has(row.destinationId)) continue
    seen.add(row.destinationId)
    merged.push(row)
    added += 1
  }
  return { merged, added }
}

export function toRouteWaypoint(item: PickerItem): RouteWaypoint {
  return {
    id: item.destinationId,
    name: item.name,
    lng: item.lng,
    lat: item.lat,
    city: item.city,
    image: item.image,
    destinationId: item.destinationId,
  }
}

type DestinationPickerScrollProps = {
  selectedIds: Set<string | number>
  onAdd: (wp: RouteWaypoint) => void
  onRemove: (id: string | number) => void
}

export function DestinationPickerScroll({
  selectedIds,
  onAdd,
  onRemove,
}: DestinationPickerScrollProps) {
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [items, setItems] = useState<PickerItem[]>([])
  const [pageNum, setPageNum] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [foodTypes, setFoodTypes] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [usingFallback, setUsingFallback] = useState(false)
  const [catalogItems, setCatalogItems] = useState<PickerItem[] | null>(null)
  const [loadingCatalog, setLoadingCatalog] = useState(false)
  const geocodeAttemptedRef = useRef<Set<number>>(new Set())
  const scrollRef = useRef<HTMLDivElement>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)
  const loadMoreInFlightRef = useRef(false)
  const itemsRef = useRef<PickerItem[]>([])

  useEffect(() => {
    itemsRef.current = items
  }, [items])

  const fetchPage = useCallback(async (page: number, kw: string) => {
    const res = kw.trim()
      ? await searchDestinationsPage(kw.trim(), { pageNum: page, pageSize: PAGE_SIZE })
      : await fetchRecommendedDestinationsPage({ sortBy: 'heat', pageNum: page, pageSize: PAGE_SIZE })
    const rows = (res.list ?? [])
      .map((vo) => voToPickerItem(vo))
      .filter((x): x is PickerItem => x != null)
    return { rows, res }
  }, [])

  useEffect(() => {
    let cancelled = false
    const timer = window.setTimeout(() => {
      void (async () => {
        setLoading(true)
        setPageNum(1)
        setTotalPages(1)
        try {
          const { rows, res } = await fetchPage(1, keyword)
          if (cancelled) return
          if (rows.length) {
            setItems(rows)
            setUsingFallback(false)
            setTotalPages(inferTotalPages(res, PAGE_SIZE, 1))
          } else {
            const fb = BEIJING_ATTRACTIONS.map((a, i) => ({
              ...a,
              id: i + 1,
              destinationId: i + 1,
            }))
            setItems(fb)
            setUsingFallback(true)
            setTotalPages(1)
          }
        } catch {
          if (!cancelled) {
            const fb = BEIJING_ATTRACTIONS.map((a, i) => ({
              ...a,
              id: i + 1,
              destinationId: i + 1,
            }))
            setItems(fb)
            setUsingFallback(true)
            setTotalPages(1)
          }
        } finally {
          if (!cancelled) setLoading(false)
        }
      })()
    }, keyword.trim() ? 280 : 0)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [keyword, fetchPage])

  const loadMore = useCallback(async () => {
    if (usingFallback || loading || loadingMore || loadMoreInFlightRef.current) return
    if (pageNum >= totalPages) return

    loadMoreInFlightRef.current = true
    setLoadingMore(true)
    const next = pageNum + 1
    try {
      const { rows, res } = await fetchPage(next, keyword)
      const { merged, added } = mergePickerItems(itemsRef.current, rows)
      setItems(merged)
      if (shouldStopRecommendPagination(added, rows.length)) {
        setTotalPages(pageNum)
        return
      }
      setPageNum(next)
      setTotalPages(inferTotalPages(res, PAGE_SIZE, next))
    } catch {
      /* 保留已加载 */
    } finally {
      loadMoreInFlightRef.current = false
      setLoadingMore(false)
    }
  }, [usingFallback, loading, loadingMore, pageNum, totalPages, keyword, fetchPage])

  useEffect(() => {
    if (usingFallback || loading || pageNum >= totalPages) return undefined
    const root = scrollRef.current
    const sentinel = sentinelRef.current
    if (!root || !sentinel) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) void loadMore()
      },
      { root, rootMargin: '80px', threshold: 0 },
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [usingFallback, loading, pageNum, totalPages, loadMore, items.length])

  useEffect(() => {
    if (!items.length) return undefined
    let cancelled = false
    const pending = items.filter((it) => {
      if (geocodeAttemptedRef.current.has(it.destinationId)) return false
      if (resolveCoordsFromLocal(it.name)) return false
      return it.lng === 0 || it.lat === 0
    })
    if (!pending.length) return undefined

    void (async () => {
      for (const item of pending) {
        geocodeAttemptedRef.current.add(item.destinationId)
        if (cancelled) return
        const coords = await resolveDestinationCoords(item.name, item.city)
        if (!coords) continue
        setItems((prev) =>
          prev.map((row) =>
            row.destinationId === item.destinationId ? { ...row, lng: coords.lng, lat: coords.lat } : row,
          ),
        )
        await new Promise((r) => window.setTimeout(r, 150))
      }
    })()

    return () => {
      cancelled = true
    }
  }, [items])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      try {
        const page = await fetchRecommendedFoodsPage(1, { pageSize: 30, sortBy: 'heat' })
        if (cancelled) return
        const types = [...new Set((page.list ?? []).map((f) => f.foodType).filter(Boolean))] as string[]
        if (types.length) setFoodTypes(types.slice(0, 6))
      } catch {
        /* 美食标签可选 */
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const categories = useMemo(() => {
    const cats = [...new Set(items.map((r) => r.type).filter(Boolean))]
    return cats.filter((c) => c !== '联调测试').slice(0, 12)
  }, [items])

  useEffect(() => {
    if (!category) {
      setCatalogItems(null)
      return undefined
    }
    let cancelled = false
    setLoadingCatalog(true)
    void fetchAllDestinationsCatalog()
      .then((rows) => {
        if (cancelled) return
        const mapped: PickerItem[] = []
        for (const d of rows) {
          const id = d.id
          if (typeof id !== 'number' || id <= 0) continue
          const fb = BEIJING_ATTRACTIONS.find((a) => a.name === d.name)
          const coords = resolveCoordsFromLocal(d.name)
          mapped.push({
            id,
            destinationId: id,
            name: d.name,
            city: '北京',
            lng: coords?.lng ?? 0,
            lat: coords?.lat ?? 0,
            reason: d.reason,
            rating: d.rating,
            badge: d.badge,
            type: d.type,
            image: d.image || fb?.image || '/images/recommend-hero-new.png',
          })
        }
        setCatalogItems(mapped)
      })
      .catch(() => {
        if (!cancelled) setCatalogItems(null)
      })
      .finally(() => {
        if (!cancelled) setLoadingCatalog(false)
      })
    return () => {
      cancelled = true
    }
  }, [category])

  const filtered = useMemo(() => {
    const pool = category && catalogItems ? catalogItems : items
    if (!category) return pool
    return pool.filter((it) => it.type === category || it.badge === category)
  }, [items, catalogItems, category])

  const allTags = useMemo(() => {
    const merged = [...categories, ...foodTypes.filter((t) => !categories.includes(t))]
    return merged.slice(0, 10)
  }, [categories, foodTypes])

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-display text-lg font-semibold text-[var(--ds-foreground)]">目的地搜索</h2>
          <p className="font-body text-xs text-[var(--ds-muted-foreground)]">
            横向滑动浏览 · 滑到右侧自动加载更多 · 点击卡片加入路线
          </p>
        </div>
        <input
          type="search"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="搜索故宫、天坛、奥林匹克森林公园…"
          className="min-w-[200px] flex-1 rounded-full border border-[var(--ds-border)] bg-white px-4 py-2 font-body text-sm"
        />
      </div>

      {usingFallback ? (
        <InlineNotice variant="offline">后端未返回北京目的地，使用离线经典景点（含坐标）</InlineNotice>
      ) : null}

      {category && loadingCatalog ? (
        <p className="font-body text-[10px] text-[var(--ds-muted-foreground)]">正在从全库筛选「{category}」…</p>
      ) : null}
      {!usingFallback && items.length > 0 ? (
        <p className="font-body text-[10px] text-[var(--ds-muted-foreground)]">
          {category && catalogItems
            ? `全库筛选「${category}」共 ${filtered.length} 个`
            : `已加载 ${items.length} 个目的地${pageNum < totalPages ? ' · 继续右滑加载更多' : ' · 已全部加载'}`}
          {loadingMore ? ' · 加载中…' : ''}
        </p>
      ) : null}

      {allTags.length > 0 ? (
        <div className="flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          <button
            type="button"
            onClick={() => setCategory('')}
            className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${
              !category ? 'bg-[var(--ds-primary)] text-white' : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
            }`}
          >
            全部
          </button>
          {allTags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => setCategory(category === tag ? '' : tag)}
              className={`shrink-0 rounded-full px-3 py-1 text-xs font-semibold ${
                category === tag
                  ? 'bg-[var(--ds-primary)] text-white'
                  : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
              }`}
            >
              {tag}
            </button>
          ))}
        </div>
      ) : null}

      <div
        ref={scrollRef}
        className={`flex snap-x snap-mandatory gap-3 overflow-x-auto pb-2 [-ms-overflow-style:none] [scrollbar-width:thin] ${
          loading ? 'opacity-60' : ''
        }`}
      >
        {filtered.map((item) => {
          const selected = selectedIds.has(item.destinationId)
          return (
            <button
              key={item.destinationId}
              type="button"
              onClick={() => {
                if (selected) onRemove(item.destinationId)
                else onAdd(toRouteWaypoint(item))
              }}
              className={`w-[148px] shrink-0 snap-start overflow-hidden rounded-2xl border text-left transition ${
                selected
                  ? 'border-[var(--ds-primary)] bg-white shadow-md ring-2 ring-[var(--ds-primary)]/25'
                  : 'border-[var(--ds-border)]/60 bg-white/90 hover:border-[var(--ds-primary)]/30'
              }`}
            >
              <div className="aspect-[4/3] overflow-hidden bg-[var(--ds-muted)]">
                <img src={item.image} alt="" className="h-full w-full object-cover" loading="lazy" />
              </div>
              <div className="p-2.5">
                <p className="line-clamp-1 font-display text-xs font-semibold text-[var(--ds-foreground)]">
                  {item.name}
                </p>
                <p className="mt-0.5 line-clamp-1 font-body text-[10px] text-[var(--ds-muted-foreground)]">
                  {item.badge} · #{item.destinationId}
                </p>
                <p className="mt-1 font-body text-[10px] font-semibold text-[var(--ds-primary)]">
                  {selected ? '✓ 已加入' : '+ 加入路线'}
                </p>
              </div>
            </button>
          )
        })}
        {!category && !usingFallback && pageNum < totalPages ? (
          <div
            ref={sentinelRef}
            className="flex w-16 shrink-0 items-center justify-center font-body text-[10px] text-[var(--ds-muted-foreground)]"
          >
            {loadingMore ? '…' : '更多'}
          </div>
        ) : (
          <div ref={sentinelRef} className="h-px w-px shrink-0" aria-hidden />
        )}
      </div>
    </div>
  )
}
