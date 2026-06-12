import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AmapMapView } from '../components/route/AmapMapView'
import { DestinationPickerScroll } from '../components/route/DestinationPickerScroll'
import { RouteSequenceSidebar } from '../components/route/RouteSequenceSidebar'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import { useRoutePlan } from '../context/routePlanContext'
import { planMacroRoute } from '../lib/amap/planMacroRoute'
import { enrichWaypointCoords } from '../lib/geo/resolveCoords'
import { hasAmapJsKey, hasAmapWebKey, hasFullAmapSetup, getAmapSecurityCode } from '../lib/amap/config'
import { TRANSPORT_OPTIONS, type RouteWaypoint, type TransportMode } from '../types/macroRoute'

export function RoutePlanningPage() {
  const navigate = useNavigate()
  const { macroPlan, setMacroPlan, setActiveWaypoint } = useRoutePlan()
  const [selected, setSelected] = useState<RouteWaypoint[]>([])
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [localPlan, setLocalPlan] = useState(macroPlan)
  const [transportMode, setTransportMode] = useState<TransportMode>('transit')

  const selectedIds = useMemo(() => new Set(selected.map((s) => s.id)), [selected])
  const displayPlan = localPlan ?? macroPlan

  const addWaypoint = (wp: RouteWaypoint) => {
    setSelected((prev) => (prev.some((p) => p.id === wp.id) ? prev : [...prev, wp]))
    setLocalPlan(null)
    void enrichWaypointCoords(wp).then((enriched) => {
      setSelected((prev) => prev.map((p) => (p.id === enriched.id ? enriched : p)))
    })
  }

  const removeWaypoint = (id: string | number) => {
    setSelected((prev) => prev.filter((p) => p.id !== id))
    setLocalPlan(null)
  }

  const handlePlan = async () => {
    setPlanning(true)
    setError(null)
    try {
      const result = await planMacroRoute(selected, transportMode)
      setLocalPlan(result)
      setMacroPlan(result)
      setActiveWaypoint(result.waypoints[0] ?? null)
      navigate('/navigate', { replace: false })
    } catch (err) {
      setError(err instanceof Error ? err.message : '路线规划失败')
      setLocalPlan(null)
    } finally {
      setPlanning(false)
    }
  }

  const handleClear = () => {
    setSelected([])
    setLocalPlan(null)
    setMacroPlan(null)
    setActiveWaypoint(null)
    setError(null)
  }

  return (
    <div className="mx-auto max-w-[1320px] animate-fade-rise px-5 py-8 md:px-10">
      <PageHeader
        eyebrow="Beijing Route"
        title="北京市内 · 多景点路径规划"
        description="横向滑动选点加入路线；点击左侧站点可打开大屏内部导航（景区路线 + 室内地图）。"
      />

      {!hasFullAmapSetup() ? (
        <InlineNotice variant="info">
          高德配置不完整：{!hasAmapJsKey() ? '缺 JS Key（地图瓦片）' : ''}
          {!hasAmapWebKey() ? '缺 Web 服务 Key（路径规划）' : ''}
          {!getAmapSecurityCode() ? '缺 securityJsCode（地图可能空白）' : ''}
          。请在 .env 填写 VITE_AMAP_JS_KEY、VITE_AMAP_WEB_KEY、VITE_AMAP_SECURITY_CODE。
        </InlineNotice>
      ) : !getAmapSecurityCode() ? (
        <InlineNotice variant="offline">
          未配置 VITE_AMAP_SECURITY_CODE，地图区域可能只显示网格而无街景。这是高德 JS API 要求，与后端无关。
        </InlineNotice>
      ) : null}

      {error ? (
        <InlineNotice variant="error" onRetry={() => void handlePlan()}>
          {error}
        </InlineNotice>
      ) : null}

      <section className="mb-4 rounded-[2rem] border border-[var(--ds-border)]/50 bg-white/80 p-4 shadow-sm">
        <DestinationPickerScroll
          selectedIds={selectedIds}
          onAdd={addWaypoint}
          onRemove={removeWaypoint}
        />
      </section>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <p className="font-body text-sm text-[var(--ds-muted-foreground)]">
          已选 {selected.length} 个景点
        </p>
        <div className="flex flex-wrap gap-2">
          {TRANSPORT_OPTIONS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTransportMode(t.value)}
              className={`rounded-full px-4 py-1.5 font-body text-xs font-semibold transition ${
                transportMode === t.value
                  ? 'bg-[var(--ds-primary)] text-white'
                  : 'border border-[var(--ds-primary)]/20 bg-white text-[var(--ds-primary)]'
              }`}
            >
              {t.icon} {t.label}
            </button>
          ))}
        </div>
      </div>

      <section className="mb-6 grid min-h-[440px] gap-5 lg:grid-cols-[minmax(280px,32%)_1fr]">
        <RouteSequenceSidebar
          plan={displayPlan}
          selected={displayPlan ? undefined : selected}
          planning={planning}
          onRemoveSelected={removeWaypoint}
        />
        <AmapMapView
          key="route-plan-map"
          className="min-h-[440px]"
          waypoints={displayPlan?.waypoints ?? selected}
          polyline={displayPlan?.polyline}
          activeId={displayPlan?.waypoints[0]?.id ?? selected[0]?.id}
        />
      </section>

      <div className="flex flex-wrap gap-3">
        <PrimaryButton
          fullWidth
          className="max-w-md py-4 text-base"
          disabled={planning || selected.length < 2}
          onClick={() => void handlePlan()}
        >
          {planning ? '正在规划并进入导航…' : '生成城市路线并导航'}
        </PrimaryButton>
        {selected.length > 0 || displayPlan ? (
          <button
            type="button"
            onClick={handleClear}
            className="rounded-full border border-[color-mix(in_srgb,var(--ds-destructive)_35%,transparent)] bg-white px-6 py-4 font-body text-sm font-semibold text-[var(--ds-destructive)] transition hover:bg-[color-mix(in_srgb,var(--ds-destructive)_8%,white)]"
          >
            清空路线
          </button>
        ) : null}
      </div>
    </div>
  )
}
