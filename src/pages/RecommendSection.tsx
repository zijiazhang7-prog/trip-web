import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  destinationVOToDestination,
  fetchRecommendedDestinations,
  fetchRecommendedDestinationsPage,
  searchDestinationsPage,
} from '../api/destination'
import { fetchDiariesByDestination, type CommunityFeedItem } from '../api/community'
import { CommentSection } from '../components/ui/CommentSection'
import { DetailOverlay } from '../components/ui/DetailOverlay'
import { RatingPanel } from '../components/ui/RatingPanel'
import { Top10Strip } from '../components/ui/Top10Strip'
import { inferTotalPages, shouldStopRecommendPagination } from '../api/pagination'
import { useTripContext } from '../context/tripContext'
import { TravelPreferences } from '../components/travel/TravelPreferences'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import {
  explainRecommendation,
  hasDeepSeekKey,
  parseTravelIntent,
  rankDestinationsByPreference,
} from '../api/llm'
import {
  emptyTagSelection,
  isTagSelectionEmpty,
  mergeTagSelections,
  sortDestinationsByTagMatch,
  type UserTagSelection,
} from '../lib/taxonomy'
import type { PreferenceSavedPayload } from '../components/travel/TravelPreferences'
import {
  destTypes,
  destinations as destinationsFallback,
  interestTags,
  type Destination,
} from '../data/siteData'
import { isDemoDestination } from '../lib/destination/isDemoDestination'
import { fetchAllDestinationsCatalog } from '../lib/catalog/fetchFullCatalog'
import { displayHeatScore, recordClientView } from '../lib/heat/viewHeat'

const glass =
  'ds-glass-panel rounded-[2rem] transition duration-500 hover:-translate-y-0.5 hover:shadow-[var(--ds-shadow-lift)]'

const waterfallCard =
  'ds-card-lift ds-glass-panel mb-6 break-inside-avoid rounded-[28px] hover:-translate-y-1'

const sidebarTitle =
  "mb-5 flex items-center gap-2.5 text-[13px] font-bold uppercase tracking-[0.12em] text-[var(--ds-accent-foreground)] before:block before:h-[18px] before:w-1 before:rounded-full before:bg-gradient-to-b before:from-[var(--ds-primary)] before:to-[color-mix(in_srgb,var(--ds-secondary)_55%,var(--ds-primary))] font-body"

const PAGE_SIZE = 32

function isRealDestination(d: Destination): boolean {
  return !isDemoDestination(d.name, d.reason)
}

function matchesDestinationKeyword(d: Destination, keyword: string): boolean {
  const q = keyword.trim().toLowerCase()
  if (!q) return true
  const hay = [
    d.name,
    d.type,
    d.badge,
    d.reason,
    d.taxonomy?.destType ?? '',
    d.taxonomy?.apiCategory ?? '',
    ...(d.taxonomy?.interestTags ?? []),
    ...(d.taxonomy?.apiTags ?? []),
  ]
    .join(' ')
    .toLowerCase()
  return hay.includes(q)
}

function scoreDestinationSearchRelevance(d: Destination, keyword: string): number {
  const q = keyword.trim().toLowerCase()
  if (!q) return 0
  const name = d.name.toLowerCase()
  if (name === q) return 100
  if (name.includes(q)) return 80
  if (d.type.toLowerCase().includes(q) || d.badge.toLowerCase().includes(q)) return 60
  if (d.reason.toLowerCase().includes(q)) return 40
  return matchesDestinationKeyword(d, q) ? 20 : 0
}

function mergeDestinationLists(prev: Destination[], chunk: Destination[]): Destination[] {
  const seen = new Set<number>()
  const out: Destination[] = []
  for (const d of prev) {
    if (!isRealDestination(d)) continue
    if (d.id != null) seen.add(d.id)
    out.push(d)
  }
  for (const d of chunk) {
    if (!isRealDestination(d)) continue
    if (d.id != null) {
      if (seen.has(d.id)) continue
      seen.add(d.id)
    }
    out.push(d)
  }
  return out
}

function DestCardSkeleton() {
  return (
    <article className={`animate-pulse overflow-hidden ${waterfallCard}`}>
      <div className="h-60 bg-[var(--ds-muted)]" />
      <div className="space-y-3 p-6">
        <div className="h-6 w-2/3 rounded-lg bg-[var(--ds-muted)]" />
        <div className="h-4 w-full rounded-lg bg-[var(--ds-muted)]" />
        <div className="h-4 w-5/6 rounded-lg bg-[var(--ds-muted)]" />
        <div className="h-5 w-1/3 rounded-lg bg-[var(--ds-muted)]" />
      </div>
    </article>
  )
}

function DestCard({ dest, onOpen }: { dest: Destination; onOpen: () => void }) {
  return (
    <article
      role="button"
      tabIndex={0}
      onClick={onOpen}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onOpen()
        }
      }}
      className={`group flex cursor-pointer flex-col overflow-hidden ${waterfallCard}`}
      style={{ borderRadius: '2rem 2.25rem 2.1rem 2.4rem / 2rem 2rem 2.2rem 2rem' }}
    >
      <div className="relative h-60 overflow-hidden bg-gradient-to-br from-[var(--ds-muted)] to-[var(--ds-accent)]">
        <img
          src={dest.image}
          alt=""
          loading="lazy"
          decoding="async"
          className="img-warm h-full w-full object-cover transition duration-700 group-hover:scale-[1.06] group-hover:rotate-[0.5deg]"
        />
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-[color-mix(in_srgb,var(--ds-foreground)_55%,transparent)] via-transparent to-transparent" />
        <div className="absolute left-[18px] top-[18px] z-[2] rounded-full border border-white/80 bg-[color-mix(in_srgb,white_92%,transparent)] px-3.5 py-1 text-xs font-semibold text-[var(--ds-accent-foreground)] shadow-sm backdrop-blur-sm">
          {dest.type}
        </div>
        <div className="absolute right-[18px] top-[18px] z-[2]">
          <span className="rounded-full border border-[color-mix(in_srgb,var(--ds-secondary)_35%,transparent)] bg-[color-mix(in_srgb,var(--ds-accent)_90%,white)] px-3.5 py-1 text-xs font-semibold text-[color-mix(in_srgb,var(--ds-secondary)_85%,#4a4a40)] backdrop-blur-sm">
            {dest.badge}
          </span>
        </div>
      </div>
      <div className="flex flex-1 flex-col p-6">
        <div className="mb-3 flex items-start justify-between gap-3">
          <h3 className="font-display text-[21px] font-semibold tracking-wide text-[var(--ds-foreground)]">
            {dest.name}
          </h3>
          <div className="flex shrink-0 items-center gap-1 rounded-full bg-[color-mix(in_srgb,var(--ds-primary)_10%,var(--ds-muted))] px-2.5 py-1 text-sm">
            <span className="text-[var(--ds-secondary)]">★</span>
            <span className="font-semibold text-[var(--ds-accent-foreground)]">{dest.rating}</span>
          </div>
        </div>
        <p className="mb-5 line-clamp-4 flex-1 font-body text-[14.5px] leading-relaxed text-[var(--ds-muted-foreground)]">
          {dest.reason}
        </p>
        <div className="flex items-center justify-between">
          <span className="font-display text-lg font-bold text-[var(--ds-primary)]">{dest.price}</span>
          {dest.value ? (
            <span className="rounded-full border border-[color-mix(in_srgb,var(--ds-primary)_22%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_10%,transparent)] px-3 py-1 text-xs font-semibold text-[var(--ds-primary)]">
              高性价比
            </span>
          ) : null}
        </div>
      </div>
    </article>
  )
}

function destKey(d: Destination) {
  return d.id != null ? `id:${d.id}` : `name:${d.name}`
}

type VenueKind = 'all' | 'scenic' | 'campus'

function matchesVenueKind(d: Destination, kind: VenueKind): boolean {
  if (kind === 'all') return true
  const backend = d.taxonomy?.backendType?.toLowerCase()
  if (kind === 'campus') {
    if (backend === 'campus') return true
    if (backend === 'scenic') return false
    return /校园|大学|学院|school|campus/.test(`${d.type} ${d.badge} ${d.reason} ${d.name}`)
  }
  if (backend === 'scenic') return true
  if (backend === 'campus') return false
  const hay = `${d.type} ${d.badge} ${d.reason} ${d.name}`
  return /景区|景点|公园|博物|遗产|风光|古迹|旅游/.test(hay) || !/校园|大学|学院/.test(hay)
}

type RecommendSectionProps = {
  openPreferences?: boolean
}

export function RecommendSection({ openPreferences = false }: RecommendSectionProps) {
  const navigate = useNavigate()
  const { setDestination } = useTripContext()
  const [preferThemes, setPreferThemes] = useState<string[]>([])
  const [aiSearchText, setAiSearchText] = useState('')
  const [selectedDestType, setSelectedDestType] = useState('')
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])
  const [aiRankedIds, setAiRankedIds] = useState<number[]>([])
  const [aiTagSelection, setAiTagSelection] = useState<UserTagSelection>(emptyTagSelection())
  const [rankingDestinations, setRankingDestinations] = useState(false)
  const pendingRankRef = useRef<{ customText: string; tags: string[] } | null>(null)
  const [items, setItems] = useState<Destination[]>([])
  const [listMode, setListMode] = useState<'recommend' | 'search'>('recommend')
  const [activeSearchKeyword, setActiveSearchKeyword] = useState('')
  const [pageNum, setPageNum] = useState(1)
  const [totalPages, setTotalPages] = useState(1)
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [usingFallback, setUsingFallback] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailIndex, setDetailIndex] = useState(0)
  const [aiReason, setAiReason] = useState<string | null>(null)
  const [aiReasonLoading, setAiReasonLoading] = useState(false)
  const loadMoreRef = useRef<HTMLDivElement | null>(null)
  const loadNextPageRef = useRef<() => Promise<void>>(async () => {})
  const itemsRef = useRef(items)
  const loadMoreInFlightRef = useRef(false)
  const canAutoLoadMoreRef = useRef(false)
  const [venueKind, setVenueKind] = useState<VenueKind>('all')
  const [heatTop10, setHeatTop10] = useState<Destination[]>([])
  const [loadingTop10, setLoadingTop10] = useState(false)
  const [relatedDiaries, setRelatedDiaries] = useState<CommunityFeedItem[]>([])
  const [loadingRelatedDiaries, setLoadingRelatedDiaries] = useState(false)
  const [relatedDiariesOpen, setRelatedDiariesOpen] = useState(false)
  const [catalogDestinations, setCatalogDestinations] = useState<Destination[] | null>(null)
  const [loadingCatalog, setLoadingCatalog] = useState(false)
  const [catalogHint, setCatalogHint] = useState('')

  useEffect(() => {
    itemsRef.current = items
  }, [items])

  useEffect(() => {
    if (loadingInitial) {
      canAutoLoadMoreRef.current = false
      return undefined
    }
    const timer = window.setTimeout(() => {
      canAutoLoadMoreRef.current = true
    }, 800)
    return () => window.clearTimeout(timer)
  }, [loadingInitial])

  useEffect(() => {
    let cancelled = false
    setLoadingTop10(true)
    void fetchRecommendedDestinations({ topK: 10, sortBy: 'heat', pageSize: 10 })
      .then((rows) => {
        if (!cancelled) {
          setHeatTop10(
            rows
              .map(destinationVOToDestination)
              .filter(isRealDestination),
          )
        }
      })
      .catch(() => {
        if (!cancelled) setHeatTop10([])
      })
      .finally(() => {
        if (!cancelled) setLoadingTop10(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const activeTagSelection = useMemo(
    () =>
      mergeTagSelections(
        {
          destTypes: selectedDestType ? [selectedDestType] : [],
          interestTags: selectedInterests,
          cuisineTags: [],
        },
        aiTagSelection,
      ),
    [selectedDestType, selectedInterests, aiTagSelection],
  )

  const searchActive = listMode === 'search' && !!activeSearchKeyword.trim()
  const tagFilterActive =
    !searchActive && (!isTagSelectionEmpty(activeTagSelection) || aiRankedIds.length > 0)

  useEffect(() => {
    if (!tagFilterActive) {
      setCatalogDestinations(null)
      setCatalogHint('')
      return undefined
    }
    let cancelled = false
    setLoadingCatalog(true)
    setCatalogHint('正在从全库加载目的地…')
    void fetchAllDestinationsCatalog((p) => {
      if (!cancelled) setCatalogHint(p.phase ?? `已加载 ${p.loaded} 条`)
    })
      .then((rows) => {
        if (!cancelled) {
          setCatalogDestinations(rows.filter(isRealDestination))
          setCatalogHint(`全库共 ${rows.length} 条，已按标签优先排序`)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCatalogDestinations(null)
          setCatalogHint('')
        }
      })
      .finally(() => {
        if (!cancelled) setLoadingCatalog(false)
      })
    return () => {
      cancelled = true
    }
  }, [tagFilterActive, selectedDestType, selectedInterests, aiTagSelection])

  const sortedDestinations = useMemo(() => {
    if (searchActive) {
      const q = activeSearchKeyword.trim()
      return items
        .filter((d) => matchesVenueKind(d, venueKind) && matchesDestinationKeyword(d, q))
        .sort((a, b) => scoreDestinationSearchRelevance(b, q) - scoreDestinationSearchRelevance(a, q))
    }
    const pool = tagFilterActive && catalogDestinations ? catalogDestinations : items
    const filtered = pool.filter((d) => matchesVenueKind(d, venueKind))
    if (!tagFilterActive) return filtered
    return sortDestinationsByTagMatch(filtered, activeTagSelection, aiRankedIds)
  }, [
    items,
    catalogDestinations,
    venueKind,
    activeTagSelection,
    aiRankedIds,
    tagFilterActive,
    searchActive,
    activeSearchKeyword,
  ])

  const runDestinationRanking = useCallback(async (customText: string, tags: string[]) => {
    const text = customText.trim()
    if (!text) {
      setAiTagSelection(emptyTagSelection())
      setAiRankedIds([])
      return
    }
    if (!hasDeepSeekKey()) {
      setAiTagSelection(emptyTagSelection())
      setAiRankedIds([])
      return
    }
    const candidates = itemsRef.current.filter((d) => d.id != null)
    if (!candidates.length) return
    setRankingDestinations(true)
    try {
      const intent = await parseTravelIntent(text)
      setAiTagSelection({
        destTypes: intent.destTypes,
        interestTags: [...new Set([...intent.interestTags, ...tags])],
        cuisineTags: intent.cuisineTags,
      })
      const rankedIds = await rankDestinationsByPreference({
        userText: text,
        selectedTags: [...intent.interestTags, ...tags],
        destinations: candidates.map((d) => ({
          id: d.id!,
          name: d.name,
          type: d.type,
          badge: d.badge,
          reason: d.reason,
        })),
      })
      setAiRankedIds(rankedIds)
    } catch {
      setAiTagSelection(emptyTagSelection())
      setAiRankedIds([])
    } finally {
      setRankingDestinations(false)
    }
  }, [])

  useEffect(() => {
    if (loadingInitial || !pendingRankRef.current) return
    const pending = pendingRankRef.current
    pendingRankRef.current = null
    void runDestinationRanking(pending.customText, pending.tags)
  }, [items, loadingInitial, runDestinationRanking])

  /** 宽召回分页：标签仅用于前端排序，不作为 API 过滤条件 */
  const buildFetchParams = useCallback(
    (page: number) => ({
      sortBy: 'heat' as const,
      pageNum: page,
      pageSize: PAGE_SIZE,
    }),
    [],
  )

  const resetRecommendFirstPage = useCallback(async () => {
    setLoadingInitial(true)
    setError(null)
    setListMode('recommend')
    setActiveSearchKeyword('')
    setPageNum(1)
    try {
      const res = await fetchRecommendedDestinationsPage(buildFetchParams(1))
      setItems(mergeDestinationLists([], res.list.map(destinationVOToDestination)))
      setTotalPages(inferTotalPages(res, PAGE_SIZE, 1))
      setUsingFallback(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : '目的地加载失败')
      setItems(destinationsFallback)
      setTotalPages(1)
      setUsingFallback(true)
    } finally {
      setLoadingInitial(false)
    }
  }, [buildFetchParams])

  useEffect(() => {
    let cancelled = false
    queueMicrotask(() => {
      if (!cancelled) void resetRecommendFirstPage()
    })
    return () => {
      cancelled = true
    }
    // 仅首屏拉取；兴趣/目的地类型变更只做本地排序，避免 API 过滤导致空列表
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!detailOpen) return
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setDetailOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [detailOpen])

  const loadNextPage = useCallback(async () => {
    if (usingFallback || loadingInitial) return
    if (!canAutoLoadMoreRef.current) return
    if (loadMoreInFlightRef.current) return
    if (pageNum >= totalPages) return

    loadMoreInFlightRef.current = true
    setLoadingMore(true)
    const next = pageNum + 1

    try {
      const res =
        listMode === 'search'
          ? await searchDestinationsPage(activeSearchKeyword, { pageNum: next, pageSize: PAGE_SIZE })
          : await fetchRecommendedDestinationsPage(buildFetchParams(next))

      const mapped = res.list.map(destinationVOToDestination)
      const prev = itemsRef.current
      const merged = mergeDestinationLists(prev, mapped)
      const added = merged.length - prev.length

      setItems(merged)

      if (shouldStopRecommendPagination(added, mapped.length)) {
        setTotalPages(pageNum)
        return
      }

      setPageNum(next)
      setTotalPages(inferTotalPages(res, PAGE_SIZE, next))
    } catch {
      /* keep已有列表 */
    } finally {
      loadMoreInFlightRef.current = false
      setLoadingMore(false)
    }
  }, [usingFallback, loadingInitial, pageNum, totalPages, listMode, activeSearchKeyword, buildFetchParams])

  useEffect(() => {
    loadNextPageRef.current = loadNextPage
  }, [loadNextPage])

  useEffect(() => {
    if (usingFallback || loadingInitial || pageNum >= totalPages) return

    let rafScroll = 0
    const nudgeIfNearViewport = () => {
      const node = loadMoreRef.current
      if (!node) return
      const rect = node.getBoundingClientRect()
      const vh = window.innerHeight || document.documentElement.clientHeight
      if (rect.top <= vh + 360) void loadNextPageRef.current()
    }

    const onScrollOrResize = () => {
      if (rafScroll) cancelAnimationFrame(rafScroll)
      rafScroll = requestAnimationFrame(() => {
        rafScroll = 0
        nudgeIfNearViewport()
      })
    }

    const node = loadMoreRef.current
    if (!node) return undefined

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          void loadNextPageRef.current()
        })
      },
      { root: null, rootMargin: '60px 0px 100px 0px', threshold: 0 },
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
    }
  }, [usingFallback, loadingInitial, pageNum, totalPages])

  const toggleInterest = (t: string) => {
    setSelectedInterests((prev) =>
      prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t],
    )
  }

  const handleDestinationSearch = async () => {
    const q = aiSearchText.trim()
    if (!q) {
      setListMode('recommend')
      setActiveSearchKeyword('')
      await resetRecommendFirstPage()
      return
    }
    setLoadingInitial(true)
    setError(null)
    setListMode('search')
    setActiveSearchKeyword(q)
    setPageNum(1)
    setCatalogDestinations(null)
    try {
      const [apiRes, catalog] = await Promise.all([
        searchDestinationsPage(q, { pageNum: 1, pageSize: PAGE_SIZE }),
        fetchAllDestinationsCatalog(),
      ])
      const apiMapped = mergeDestinationLists(
        [],
        (apiRes.list ?? []).map(destinationVOToDestination),
      )
      const clientMatches = catalog.filter(
        (d) => isRealDestination(d) && matchesDestinationKeyword(d, q),
      )
      const merged = mergeDestinationLists(apiMapped, clientMatches)
      if (merged.length) {
        setItems(merged)
        setTotalPages(inferTotalPages(apiRes, PAGE_SIZE, 1))
        setUsingFallback(false)
      } else {
        setItems(destinationsFallback.filter((d) => matchesDestinationKeyword(d, q)))
        setTotalPages(1)
        setUsingFallback(true)
        setError('未找到匹配目的地，已展示本地示例')
      }
    } catch (err) {
      try {
        const catalog = await fetchAllDestinationsCatalog()
        const clientMatches = catalog.filter(
          (d) => isRealDestination(d) && matchesDestinationKeyword(d, q),
        )
        if (clientMatches.length) {
          setItems(clientMatches)
          setTotalPages(1)
          setUsingFallback(false)
        } else {
          setError(err instanceof Error ? err.message : '搜索失败')
          setItems(destinationsFallback.filter((d) => matchesDestinationKeyword(d, q)))
          setTotalPages(1)
          setUsingFallback(true)
        }
      } catch {
        setError(err instanceof Error ? err.message : '搜索失败')
        setItems(destinationsFallback)
        setTotalPages(1)
        setUsingFallback(true)
      }
    } finally {
      setLoadingInitial(false)
    }
  }

  const openDetail = (dest: Destination) => {
    const idx = sortedDestinations.findIndex((d) => destKey(d) === destKey(dest))
    setDetailIndex(idx >= 0 ? idx : 0)
    setAiReason(null)
    setDetailOpen(true)
  }

  const stepDetail = (dir: -1 | 1) => {
    if (sortedDestinations.length === 0) return
    setAiReason(null)
    setDetailIndex((i) => Math.max(0, Math.min(sortedDestinations.length - 1, i + dir)))
  }

  const detailDest = detailOpen ? (sortedDestinations[detailIndex] ?? null) : null

  useEffect(() => {
    if (!detailOpen) return
    const dest = sortedDestinations[detailIndex]
    if (!dest?.id) return
    recordClientView('destination', dest.id)
    const heat = displayHeatScore(dest.heatScore, 'destination', dest.id)
    const price = `${heat} 热度`
    const patch = (d: Destination) => (d.id === dest.id ? { ...d, price } : d)
    setItems((prev) => prev.map(patch))
    setHeatTop10((prev) => prev.map(patch))
    if (catalogDestinations) setCatalogDestinations((prev) => prev?.map(patch) ?? null)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 仅在打开详情或切换条目时计浏览量
  }, [detailOpen, detailIndex])

  return (
    <div className="relative z-[1] mx-auto max-w-7xl px-2 pb-16 pt-2 md:px-4 md:pb-20">
      <div className="group relative mb-12 min-h-[380px] overflow-hidden rounded-[28px] shadow-[0_40px_100px_-44px_rgba(18,55,42,0.5)] ring-1 ring-[color-mix(in_srgb,var(--ds-forest)_14%,transparent)] sm:min-h-[440px] md:min-h-[500px]">
        <img
          src="/images/recommend-hero-new.png"
          alt=""
          className="img-warm absolute inset-0 h-full w-full object-cover object-[center_55%] transition duration-[1400ms] ease-[cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.04]"
          decoding="async"
        />
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'linear-gradient(180deg, color-mix(in srgb, var(--ds-forest) 30%, transparent) 0%, transparent 30%, transparent 45%, color-mix(in srgb, var(--ds-forest) 78%, transparent) 100%)',
          }}
          aria-hidden
        />
        <div
          className="pointer-events-none absolute inset-0"
          style={{
            background:
              'linear-gradient(90deg, color-mix(in srgb, var(--ds-forest) 55%, transparent) 0%, transparent 55%)',
          }}
          aria-hidden
        />

        <div className="relative z-[2] flex min-h-[inherit] flex-col justify-end p-8 sm:p-12 md:p-16">
          <div className="flex items-center gap-3">
            <span className="h-px w-10 bg-[color-mix(in_srgb,var(--ds-cream)_70%,transparent)]" aria-hidden />
            <p className="font-display text-[11px] font-semibold uppercase tracking-[0.4em] text-[color-mix(in_srgb,var(--ds-cream)_85%,var(--ds-sage))]">
              Discover · 旅游推荐
            </p>
          </div>
          <h2 className="mt-5 max-w-[16ch] font-display text-[2.4rem] font-semibold leading-[1.06] tracking-[-0.02em] text-white drop-shadow-[0_8px_36px_rgba(0,0,0,0.45)] sm:text-[3.2rem] md:text-[3.9rem]">
            向有光处，温柔抵达
          </h2>
          <p className="mt-6 max-w-[34rem] font-body text-[1rem] leading-[1.75] tracking-[0.01em] text-[color-mix(in_srgb,white_90%,var(--ds-sage))] sm:text-[1.0625rem] md:text-[1.125rem]">
            穿过叶隙的风与远村。为你拣选值得慢下来的目的地，把启程还给风景本身。
          </p>
        </div>
      </div>

      <div className={`relative mb-12 overflow-visible rounded-[28px] px-5 py-5 sm:px-7 sm:py-6 ${glass}`}>
        <div className="relative flex flex-col gap-4 overflow-visible lg:flex-row lg:items-start lg:gap-5">
          <TravelPreferences
            key={openPreferences ? 'prefs-open' : 'prefs-default'}
            className="w-full max-w-full shrink-0 lg:w-auto lg:max-w-[min(100%,380px)]"
            openPanel={openPreferences}
            onSaved={(payload: PreferenceSavedPayload) => {
              setPreferThemes(payload.themes)
              if (payload.tags.length) setSelectedInterests(payload.tags)
              if (payload.customText.trim() && hasDeepSeekKey()) {
                pendingRankRef.current = { customText: payload.customText, tags: payload.tags }
              } else {
                pendingRankRef.current = null
                setAiTagSelection(emptyTagSelection())
                setAiRankedIds([])
              }
              void resetRecommendFirstPage()
            }}
          />

          <div className="flex min-w-0 flex-1 flex-row items-center gap-2 rounded-2xl border border-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)] bg-[color-mix(in_srgb,white_82%,var(--ds-background))] px-3 py-2 sm:gap-3 sm:px-4">
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="shrink-0 text-[var(--ds-primary)]"
              aria-hidden
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              value={aiSearchText}
              onChange={(e) => setAiSearchText(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && void handleDestinationSearch()}
              placeholder="输入地名、类别或关键词搜索目的地"
              className="min-w-0 flex-1 border-0 bg-transparent font-body text-base text-[var(--ds-foreground)] outline-none placeholder:text-[color-mix(in_srgb,var(--ds-muted-foreground)_75%,transparent)] focus-visible:ring-0"
            />
            <button
              type="button"
              onClick={() => void handleDestinationSearch()}
              className="cursor-target shrink-0 rounded-full bg-[var(--ds-primary)] px-5 py-2.5 font-body text-sm font-bold text-white shadow-[0_4px_18px_-4px_rgba(42,107,78,0.3)] transition duration-300 hover:scale-[1.02] hover:bg-[color-mix(in srgb, var(--ds-primary) 88%, white)] active:scale-[0.98] sm:px-7 sm:py-3"
            >
              {aiSearchText.trim() ? '搜索' : '刷新推荐'}
            </button>
          </div>
        </div>
      </div>

      <div className="grid gap-10 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="flex flex-col gap-5 lg:sticky lg:top-24 lg:self-start">
          <div className={`p-7 ${glass}`} style={{ borderRadius: '2.25rem 2rem 2.5rem 2rem' }}>
            <h3 className={sidebarTitle}>景区 / 校园</h3>
            <div className="mb-5 flex flex-wrap gap-2">
              {(
                [
                  { value: 'all', label: '全部' },
                  { value: 'scenic', label: '景区' },
                  { value: 'campus', label: '校园' },
                ] as const
              ).map((tab) => (
                <button
                  key={tab.value}
                  type="button"
                  onClick={() => setVenueKind(tab.value)}
                  className={`rounded-full px-4 py-1.5 text-xs font-semibold transition ${
                    venueKind === tab.value
                      ? 'bg-[var(--ds-primary)] text-white'
                      : 'border border-[var(--ds-primary)]/15 bg-white/70 text-[var(--ds-primary)]'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <h3 className={sidebarTitle}>目的地类型</h3>
            <div className="max-h-[min(52vh,440px)] overflow-y-auto pr-1 [-ms-overflow-style:none] [scrollbar-width:thin]">
            <div className="flex flex-col gap-2">
              {destTypes.map((type) => (
                <label
                  key={type.value || 'all'}
                  className={`flex cursor-pointer items-center gap-3 rounded-[1.35rem] border px-4 py-3 font-body text-[14.5px] transition ${
                    selectedDestType === type.value
                      ? 'border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)] bg-white font-semibold text-[var(--ds-primary)] shadow-[var(--ds-shadow-soft)]'
                      : 'border-transparent text-[var(--ds-accent-foreground)] hover:bg-[color-mix(in_srgb,white_55%,transparent)]'
                  }`}
                >
                  <input
                    type="radio"
                    name="destType"
                    className="accent-[var(--ds-primary)]"
                    checked={selectedDestType === type.value}
                    onChange={() => setSelectedDestType(type.value)}
                  />
                  <span>{type.label}</span>
                </label>
              ))}
            </div>
            </div>
          </div>
        </aside>

        <div className="min-w-0">
          {usingFallback ? (
            <p className="mb-4 rounded-2xl border border-amber-200/80 bg-amber-50/90 px-4 py-3 font-body text-sm text-amber-900">
              后端暂时不可用，已展示本地示例目的地。{error ? `（${error}）` : ''}
            </p>
          ) : null}
          {rankingDestinations ? (
            <p className="mb-4 font-body text-sm text-[var(--ds-primary)]">DeepSeek 正在根据你的描述智能排序推荐…</p>
          ) : null}

          <Top10Strip
            title="热度 Top10"
            loading={loadingTop10}
            items={heatTop10.map((d) => ({
              id: d.id ?? d.name,
              name: d.name,
              meta: d.price,
              image: d.image,
              onClick: () => openDetail(d),
            }))}
          />

          <div className="mb-9 flex flex-wrap items-center gap-3">
            <span className="mr-2 font-body text-xs font-semibold uppercase tracking-[0.1em] text-[color-mix(in_srgb,var(--ds-muted-foreground)_85%,var(--ds-border))]">
              兴趣偏好
            </span>
            {interestTags.map((t) => {
              const on = selectedInterests.includes(t)
              return (
                <button
                  key={t}
                  type="button"
                  onClick={() => toggleInterest(t)}
                  className={`rounded-full border px-5 py-2 font-body text-sm font-semibold transition duration-300 ${
                    on
                      ? 'border-[color-mix(in_srgb,var(--ds-primary)_20%,white)] bg-[var(--ds-primary)] text-[var(--ds-primary-foreground)] shadow-[var(--ds-shadow-soft)] hover:scale-105 active:scale-95'
                      : 'border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-[color-mix(in_srgb,white_65%,transparent)] text-[var(--ds-accent-foreground)] hover:border-[color-mix(in_srgb,var(--ds-primary)_35%,transparent)] hover:bg-white'
                  }`}
                >
                  {t}
                </button>
              )
            })}
          </div>

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-3">
            {loadingInitial || (tagFilterActive && loadingCatalog)
              ? Array.from({ length: 6 }).map((_, i) => <DestCardSkeleton key={`sk-${i}`} />)
              : null}
            {searchActive && !loadingInitial ? (
              <p className="mb-4 font-body text-xs text-[var(--ds-muted-foreground)]">
                搜索「{activeSearchKeyword}」共 {sortedDestinations.length} 条（名称 / 类别 / 关键词）
              </p>
            ) : null}
            {!loadingInitial && !(tagFilterActive && loadingCatalog) && items.length === 0 && !catalogDestinations?.length ? (
              <p className="break-inside-avoid py-12 text-center font-body text-sm text-[var(--ds-muted-foreground)]">
                暂无目的地数据。
              </p>
            ) : null}
            {!loadingInitial && items.length > 0 && sortedDestinations.length === 0 ? (
              <p className="break-inside-avoid py-12 text-center font-body text-sm text-[var(--ds-muted-foreground)]">
                当前景区/校园筛选下没有结果，试试切换「全部」。
              </p>
            ) : null}
            {tagFilterActive ? (
              <p className="mb-4 font-body text-xs text-[var(--ds-muted-foreground)]">
                {loadingCatalog
                  ? catalogHint || '正在从全库筛选目的地…'
                  : catalogHint || '已按标签从全库优先排序：匹配项在前，其余在后（不隐藏）。'}
              </p>
            ) : null}
            {sortedDestinations.map((dest) => (
              <DestCard key={destKey(dest)} dest={dest} onOpen={() => openDetail(dest)} />
            ))}
          </div>

          {!tagFilterActive && !usingFallback && !loadingInitial && pageNum < totalPages ? (
            <div ref={loadMoreRef} className="mt-8 flex h-16 items-center justify-center">
              <span className="font-body text-xs text-[var(--ds-muted-foreground)]">
                {loadingMore ? '加载更多...' : '下滑自动加载更多'}
              </span>
            </div>
          ) : null}
          {!tagFilterActive && !usingFallback && !loadingInitial && pageNum >= totalPages && items.length > 0 ? (
            <p className="mt-6 text-center font-body text-xs text-[color-mix(in_srgb,var(--ds-muted-foreground)_85%,transparent)]">
              已加载全部目的地
            </p>
          ) : null}
        </div>
      </div>

      {detailOpen && detailDest ? (
        <DetailOverlay
          open={detailOpen}
          title="目的地详情"
          onClose={() => setDetailOpen(false)}
          onPrev={() => stepDetail(-1)}
          onNext={() => stepDetail(1)}
          indexLabel={`${detailIndex + 1} / ${sortedDestinations.length}`}
        >
          <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="overflow-hidden rounded-2xl bg-[#edf4ef]">
              <img
                src={detailDest.image}
                alt=""
                className="max-h-[min(52vh,480px)] w-full object-cover"
                loading="lazy"
                decoding="async"
              />
            </div>
            <div className="space-y-4">
              <div className="flex flex-wrap items-center gap-2 text-xs text-[#6B8076]">
                <span className="rounded-full bg-[#E8F3EE] px-2 py-0.5">{detailDest.type}</span>
                <span className="rounded-full bg-[#E8F3EE] px-2 py-0.5">{detailDest.badge}</span>
                <span>{detailDest.price}</span>
              </div>
              <p className="font-display text-2xl font-semibold text-[#2C3E36]">{detailDest.name}</p>
              {detailDest.id != null ? (
                <RatingPanel targetType="destination" targetId={detailDest.id} average={detailDest.rating} />
              ) : null}
              <p className="text-sm leading-relaxed text-[#4f655c]">{detailDest.reason}</p>
              {hasDeepSeekKey() ? (
                <div className="rounded-xl border border-[#d8ebe3] bg-[#f4faf7] px-3 py-2">
                  <button
                    type="button"
                    disabled={aiReasonLoading}
                    className="text-xs font-semibold text-[var(--ds-primary)] disabled:opacity-60"
                    onClick={async () => {
                      if (!detailDest) return
                      setAiReasonLoading(true)
                      try {
                        const reason = await explainRecommendation({
                          destinationName: detailDest.name,
                          destinationType: detailDest.type,
                          travelerThemes: [...preferThemes, ...selectedInterests],
                        })
                        setAiReason(reason)
                      } catch (err) {
                        setAiReason(err instanceof Error ? err.message : 'AI 推荐理由生成失败')
                      } finally {
                        setAiReasonLoading(false)
                      }
                    }}
                  >
                    {aiReasonLoading ? 'DeepSeek 分析中…' : 'AI：为什么适合你'}
                  </button>
                  {aiReason ? <p className="mt-2 text-sm leading-relaxed text-[#3d5c50]">{aiReason}</p> : null}
                </div>
              ) : null}
              {detailDest.id != null ? (
                <CommentSection
                  targetType="destination"
                  targetId={detailDest.id}
                  title="景点评论"
                />
              ) : null}
              <div className="flex flex-wrap gap-3 pt-2">
                <PrimaryButton
                  variant="secondary"
                  onClick={async () => {
                    if (detailDest.id == null) return
                    setRelatedDiariesOpen(true)
                    setLoadingRelatedDiaries(true)
                    try {
                      const rows = await fetchDiariesByDestination(detailDest.id, 'heat')
                      setRelatedDiaries(rows)
                    } catch {
                      setRelatedDiaries([])
                    } finally {
                      setLoadingRelatedDiaries(false)
                    }
                  }}
                >
                  相关手账
                </PrimaryButton>
                <PrimaryButton
                  onClick={() => {
                    if (detailDest.id != null) {
                      setDestination(detailDest.id, detailDest.name)
                      setDetailOpen(false)
                      navigate('/route')
                    }
                  }}
                >
                  规划路线
                </PrimaryButton>
                <PrimaryButton
                  variant="secondary"
                  onClick={() => {
                    if (detailDest.id != null) {
                      setDestination(detailDest.id, detailDest.name)
                      setDetailOpen(false)
                      navigate('/food')
                    }
                  }}
                >
                  查看美食
                </PrimaryButton>
                <PrimaryButton
                  variant="secondary"
                  onClick={() => {
                    if (detailDest.id != null) {
                      setDestination(detailDest.id, detailDest.name)
                      setDetailOpen(false)
                      navigate('/navigate')
                    }
                  }}
                >
                  周边设施
                </PrimaryButton>
              </div>
            </div>
          </div>
        </DetailOverlay>
      ) : null}

      {relatedDiariesOpen ? (
        <div
          className="fixed inset-0 z-[290] flex items-center justify-center bg-black/35 p-4"
          onClick={(e) => {
            if (e.target === e.currentTarget) setRelatedDiariesOpen(false)
          }}
        >
          <div className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-white/60 bg-white p-5 shadow-xl">
            <div className="mb-3 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-[#2C3E36]">
                {detailDest?.name ?? '目的地'} · 相关手账
              </h3>
              <button
                type="button"
                className="text-xs text-[var(--ds-primary)]"
                onClick={() => setRelatedDiariesOpen(false)}
              >
                关闭
              </button>
            </div>
            {loadingRelatedDiaries ? (
              <p className="text-sm text-[#6B8076]">加载中…</p>
            ) : relatedDiaries.length === 0 ? (
              <p className="text-sm text-[#6B8076]">暂无相关手账</p>
            ) : (
              <ul className="space-y-3">
                {relatedDiaries.map((d) => (
                  <li key={d.id} className="rounded-xl border border-[#d8ebe3] px-3 py-2">
                    <p className="text-sm font-semibold text-[#2C3E36]">{d.title ?? d.bookTitle}</p>
                    <p className="mt-1 line-clamp-2 text-xs text-[#6B8076]">{d.excerpt}</p>
                    <button
                      type="button"
                      className="mt-2 text-xs font-semibold text-[var(--ds-primary)]"
                      onClick={() => {
                        setRelatedDiariesOpen(false)
                        setDetailOpen(false)
                        navigate(`/community?destinationId=${detailDest?.id}`)
                      }}
                    >
                      在社群中查看
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}
