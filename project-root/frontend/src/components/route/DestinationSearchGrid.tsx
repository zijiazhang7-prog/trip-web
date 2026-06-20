import { useEffect, useState } from 'react'
import { searchDestinationsPage } from '../../api/destination'
import { BEIJING_ATTRACTIONS, filterBeijingAttractions, type BeijingAttraction } from '../../data/beijingDestinations'
import type { RouteWaypoint } from '../../types/macroRoute'
import { InlineNotice } from '../ui/InlineNotice'

function toWaypoint(a: BeijingAttraction): RouteWaypoint {
  return {
    id: a.id,
    name: a.name,
    lng: a.lng,
    lat: a.lat,
    city: a.city,
    image: a.image,
    destinationId: typeof a.id === 'number' ? a.id : undefined,
  }
}

function isBeijingRow(city?: string): boolean {
  return !city || city.includes('北京')
}

function resolveBeijingCoords(
  row: Omit<BeijingAttraction, 'lng' | 'lat'> & { lng?: number; lat?: number },
): BeijingAttraction {
  const fb = BEIJING_ATTRACTIONS.find((a) => a.name === row.name)
  return {
    ...row,
    lng: row.lng || fb?.lng || 0,
    lat: row.lat || fb?.lat || 0,
    reason: row.reason,
    rating: row.rating,
    badge: row.badge,
    type: row.type,
    image: row.image,
    city: row.city,
    name: row.name,
    id: row.id,
  }
}

type DestinationSearchGridProps = {
  selectedIds: Set<string | number>
  onToggle: (wp: RouteWaypoint, checked: boolean) => void
}

export function DestinationSearchGrid({ selectedIds, onToggle }: DestinationSearchGridProps) {
  const [keyword, setKeyword] = useState('')
  const [items, setItems] = useState<BeijingAttraction[]>(BEIJING_ATTRACTIONS)
  const [loading, setLoading] = useState(false)
  const [usingFallback, setUsingFallback] = useState(true)

  useEffect(() => {
    let cancelled = false
    const debounceMs = keyword.trim() ? 280 : 0
    const timer = window.setTimeout(() => {
      ;(async () => {
        if (cancelled) return
        setLoading(true)
        try {
          const page = await searchDestinationsPage(keyword || '北京', {
            pageNum: 1,
            pageSize: 24,
          })
          if (cancelled) return
          const apiRows = (page.list ?? [])
            .filter((d) => isBeijingRow(d.city))
            .map((d, i) => {
              const fb = BEIJING_ATTRACTIONS.find((a) => a.name === d.name)
              return resolveBeijingCoords({
                id: d.id ?? `api-${i}`,
                name: d.name,
                city: d.city ?? '北京',
                lng: fb?.lng,
                lat: fb?.lat,
                reason: d.description?.trim() || d.tags?.join('、') || '北京市内推荐景点',
                rating: d.ratingScore ?? 4.5,
                badge: d.category || d.tags?.[0] || '推荐',
                type: d.category || d.type || '景点',
                image: d.coverUrl?.startsWith('http')
                  ? d.coverUrl
                  : fb?.image ?? '/images/recommend-hero-new.png',
                destinationId: d.id,
              })
            })

          if (apiRows.length) {
            if (!cancelled) {
              setItems(apiRows)
              setUsingFallback(false)
            }
          } else if (!cancelled) {
            setItems(filterBeijingAttractions(keyword))
            setUsingFallback(true)
          }
        } catch {
          if (!cancelled) {
            setItems(filterBeijingAttractions(keyword))
            setUsingFallback(true)
          }
        } finally {
          if (!cancelled) setLoading(false)
        }
      })()
    }, debounceMs)

    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [keyword])

  return (
    <div className="flex h-full max-h-[min(52vh,520px)] min-h-[280px] flex-col overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-[color-mix(in_srgb,white_88%,var(--ds-background))] p-5 shadow-[var(--ds-shadow-soft)] backdrop-blur-xl">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="font-display text-xl font-semibold text-[var(--ds-foreground)]">目的地搜索</h2>
          <p className="font-body mt-1 text-xs text-[var(--ds-muted-foreground)]">
            北京市内景点 · 勾选后加入路线规划（顺序将由系统优化）
          </p>
        </div>
        <div className="flex min-w-[200px] flex-1 items-center gap-2">
          <input
            type="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索故宫、天坛、颐和园…"
            className="w-full rounded-full border border-[color-mix(in_srgb,var(--ds-border)_70%,transparent)] bg-white/90 px-5 py-2.5 font-body text-sm outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary)]"
          />
          {loading ? (
            <span className="shrink-0 font-body text-xs text-[var(--ds-muted-foreground)]">更新中…</span>
          ) : null}
        </div>
      </div>

      {usingFallback ? (
        <InlineNotice variant="offline">
          离线演示数据 · 北京市内经典景点
        </InlineNotice>
      ) : null}

      <div
        className={`grid min-h-0 flex-1 grid-cols-2 gap-4 overflow-y-auto sm:grid-cols-3 lg:grid-cols-4 ${
          loading ? 'pointer-events-none opacity-70' : ''
        }`}
      >
        {items.map((item) => {
          const wp = toWaypoint(item)
          const checked = selectedIds.has(item.id)
          return (
            <article
              key={String(item.id)}
              className={`flex flex-col overflow-hidden rounded-2xl border transition ${
                checked
                  ? 'border-[color-mix(in_srgb,var(--ds-primary)_35%,transparent)] bg-white shadow-md'
                  : 'border-[color-mix(in_srgb,var(--ds-border)_40%,transparent)] bg-white/80'
              }`}
            >
              <div className="aspect-[4/3] overflow-hidden bg-[var(--ds-muted)]">
                <img src={item.image} alt="" className="h-full w-full object-cover" loading="lazy" />
              </div>
              <div className="flex flex-1 flex-col p-3">
                <h3 className="font-display text-sm font-semibold text-[var(--ds-foreground)]">{item.name}</h3>
                <p className="font-body mt-1 line-clamp-2 flex-1 text-[11px] text-[var(--ds-muted-foreground)]">
                  {item.reason}
                </p>
                <label className="mt-3 flex cursor-pointer items-center justify-center gap-2">
                  <input
                    type="checkbox"
                    className="h-4 w-4 accent-[var(--ds-primary)]"
                    checked={checked}
                    onChange={(e) => onToggle(wp, e.target.checked)}
                  />
                  <span className="font-body text-xs font-semibold text-[var(--ds-primary)]">
                    {checked ? '已加入路线' : '加入规划'}
                  </span>
                </label>
              </div>
            </article>
          )
        })}
      </div>
    </div>
  )
}
