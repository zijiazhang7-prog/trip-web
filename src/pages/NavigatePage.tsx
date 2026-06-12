import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { fetchNearbyFacilities, type NearbyFacilityVO } from '../api/facility'
import { AmapNavigateMap } from '../components/route/AmapNavigateMap'
import { RouteTimelinePanel } from '../components/route/RouteTimelinePanel'
import { GlassPanel } from '../components/ui/GlassPanel'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { RouteErrorBoundary } from '../components/ui/RouteErrorBoundary'
import { useRoutePlan } from '../context/routePlanContext'
import { resolveSourceNodeId } from '../lib/route/resolveNode'
import { fetchAroundPois, type NearbyPoiResult } from '../lib/amap/webService'
import { hasAmapWebKey } from '../lib/amap/config'

const FACILITY_TYPES = [
  { value: '', label: '全部' },
  { value: 'toilet', label: '卫生间' },
  { value: 'canteen', label: '食堂' },
  { value: 'shop', label: '商店' },
] as const

type FacilityRow = {
  id: string
  name: string
  type: string
  distance: number
  distanceLabel: string
}

function mapGraphFacility(f: NearbyFacilityVO): FacilityRow {
  const dist = Number(f.reachableDistance) || 0
  return {
    id: String(f.id),
    name: f.name,
    type: f.facilityType || '设施',
    distance: dist,
    distanceLabel: `道路距离约 ${dist} 米`,
  }
}

function mapAmapFacility(f: NearbyPoiResult): FacilityRow {
  return {
    id: f.id,
    name: f.name,
    type: f.type,
    distance: f.distance,
    distanceLabel: `直线约 ${f.distance} 米`,
  }
}

export function NavigatePage() {
  return (
    <RouteErrorBoundary>
      <NavigatePageContent />
    </RouteErrorBoundary>
  )
}

function NavigatePageContent() {
  const navigate = useNavigate()
  const { macroPlan, activeWaypoint, setActiveWaypoint, setMacroPlan } = useRoutePlan()
  const [facilityType, setFacilityType] = useState('')
  const [facilitySource, setFacilitySource] = useState<'graph' | 'amap'>('graph')
  const [facilities, setFacilities] = useState<FacilityRow[]>([])
  const [loadingFac, setLoadingFac] = useState(false)
  const [facError, setFacError] = useState<string | null>(null)

  const focusWaypoint = activeWaypoint ?? macroPlan?.waypoints?.[0] ?? null
  const activeLegIndex = Math.max(
    0,
    (macroPlan?.waypoints?.findIndex((wp) => String(wp.id) === String(focusWaypoint?.id)) ?? 1) - 1,
  )

  const loadFacilities = useCallback(
    async (wp: NonNullable<typeof focusWaypoint>) => {
      setLoadingFac(true)
      setFacError(null)
      try {
        if (facilitySource === 'graph' && typeof wp.destinationId === 'number') {
          const sourceNodeId = await resolveSourceNodeId(wp.destinationId, wp.name)
          if (sourceNodeId != null) {
            const page = await fetchNearbyFacilities({
              destinationId: wp.destinationId,
              sourceNodeId,
              facilityType: facilityType || undefined,
              sortBy: 'distance',
              pageSize: 20,
            })
            const rows = (page.list ?? []).map(mapGraphFacility)
            setFacilities(rows)
            if (!rows.length) setFacError('图距离范围内暂无匹配设施')
            return
          }
        }

        if (hasAmapWebKey()) {
          const list = await fetchAroundPois(wp.lng, wp.lat, facilityType)
          const rows = list.map(mapAmapFacility)
          setFacilities(rows)
          if (!rows.length) setFacError('附近暂无匹配设施')
        } else {
          setFacilities([
            mapAmapFacility({
              id: 'demo-1',
              name: `${wp.name}游客服务中心`,
              type: '生活服务',
              address: '北京市',
              distance: 120,
              lng: wp.lng,
              lat: wp.lat,
            }),
          ])
        }
      } catch (err) {
        setFacError(err instanceof Error ? err.message : '设施查询失败')
        setFacilities([])
      } finally {
        setLoadingFac(false)
      }
    },
    [facilityType, facilitySource],
  )

  useEffect(() => {
    let cancelled = false
    if (!focusWaypoint) return
    ;(async () => {
      await Promise.resolve()
      if (cancelled) return
      await loadFacilities(focusWaypoint)
    })()
    return () => {
      cancelled = true
    }
  }, [focusWaypoint, loadFacilities])

  const selectWaypointByIndex = useCallback(
    (index: number) => {
      const wp = macroPlan?.waypoints?.[index]
      if (wp) setActiveWaypoint(wp)
    },
    [macroPlan, setActiveWaypoint],
  )

  const handleClearRoute = () => {
    setMacroPlan(null)
    setActiveWaypoint(null)
    setFacilities([])
    navigate('/route')
  }

  return (
    <div className="mx-auto max-w-[1320px] animate-fade-rise px-5 py-8 md:px-10">
      <div className="mb-10 flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <PageHeader
            eyebrow="Travel Navigate"
            title="旅行导航 · 北京市内"
            description="高德导航时间轴含步行/地铁分段；地图根据当前站点与定位显示路线。"
          />
        </div>
        {macroPlan ? (
          <button
            type="button"
            onClick={handleClearRoute}
            className="shrink-0 rounded-full border border-[color-mix(in_srgb,var(--ds-destructive)_35%,transparent)] bg-white px-5 py-2.5 font-body text-sm font-semibold text-[var(--ds-destructive)] transition hover:bg-[color-mix(in_srgb,var(--ds-destructive)_8%,white)]"
          >
            清空路线
          </button>
        ) : null}
      </div>

      {!macroPlan ? (
        <InlineNotice variant="info">
          尚未带入路线。请先到{' '}
          <Link to="/route" className="font-semibold text-[var(--ds-primary)] underline">
            路径规划
          </Link>{' '}
          生成路线后再进入本页。
        </InlineNotice>
      ) : null}

      <section className="mb-6">
        <RouteTimelinePanel
          plan={macroPlan}
          activeId={focusWaypoint?.id}
          onSelectWaypoint={selectWaypointByIndex}
        />
      </section>

      <section className="grid min-h-[360px] gap-5 lg:grid-cols-[minmax(260px,34%)_1fr]">
        <GlassPanel className="flex max-h-[min(58vh,560px)] min-h-[360px] flex-col overflow-hidden p-5">
          <h2 className="font-display text-lg font-semibold text-[var(--ds-foreground)]">附近设施</h2>
          <p className="font-body mt-1 text-xs text-[var(--ds-muted-foreground)]">
            {focusWaypoint
              ? `当前站点：${focusWaypoint.name} · 点击时间轴或地图可切换`
              : '请点击时间轴上的目的地'}
          </p>

          <div className="mt-3 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setFacilitySource('graph')}
              className={`rounded-full px-3 py-1 text-xs font-semibold ${
                facilitySource === 'graph'
                  ? 'bg-[var(--ds-primary)] text-white'
                  : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
              }`}
            >
              道路距离
            </button>
            <button
              type="button"
              onClick={() => setFacilitySource('amap')}
              className={`rounded-full px-3 py-1 text-xs font-semibold ${
                facilitySource === 'amap'
                  ? 'bg-[var(--ds-primary)] text-white'
                  : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
              }`}
            >
              高德周边
            </button>
          </div>

          <select
            value={facilityType}
            onChange={(e) => setFacilityType(e.target.value)}
            className="mt-3 rounded-full border border-[color-mix(in_srgb,var(--ds-border)_70%,transparent)] bg-white px-4 py-2 font-body text-sm"
          >
            {FACILITY_TYPES.map((t) => (
              <option key={t.value || 'all'} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>

          {facError && !loadingFac ? (
            <p className="mt-3 font-body text-xs text-[var(--ds-destructive)]">{facError}</p>
          ) : null}

          <ul className="mt-4 min-h-0 flex-1 space-y-3 overflow-y-auto pr-1">
            {loadingFac ? (
              <li className="py-6 text-center font-body text-sm text-[var(--ds-muted-foreground)]">查询中…</li>
            ) : null}
            {!loadingFac &&
              facilities.map((f) => (
                <li
                  key={f.id}
                  className="rounded-2xl border border-[color-mix(in_srgb,var(--ds-border)_40%,transparent)] bg-white/90 px-4 py-3"
                >
                  <p className="font-display text-sm font-semibold text-[var(--ds-foreground)]">{f.name}</p>
                  <p className="font-body mt-1 text-xs text-[var(--ds-muted-foreground)]">{f.type}</p>
                  <p className="font-body mt-1 text-xs text-[var(--ds-primary)]">{f.distanceLabel}</p>
                </li>
              ))}
          </ul>
        </GlassPanel>

        {macroPlan ? (
          <AmapNavigateMap
            key={`nav-${activeLegIndex}-${focusWaypoint?.id}`}
            className="min-h-[360px]"
            plan={macroPlan}
            activeWaypoint={focusWaypoint}
            activeLegIndex={activeLegIndex}
          />
        ) : (
          <div className="flex min-h-[360px] items-center justify-center rounded-[2rem] border border-dashed border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/60 font-body text-sm text-[var(--ds-muted-foreground)]">
            地图将在生成路线后显示
          </div>
        )}
      </section>
    </div>
  )
}
