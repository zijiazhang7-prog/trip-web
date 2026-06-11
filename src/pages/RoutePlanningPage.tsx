import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AmapMapView } from '../components/route/AmapMapView'
import { DestinationSearchGrid } from '../components/route/DestinationSearchGrid'
import { RouteSequenceSidebar } from '../components/route/RouteSequenceSidebar'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import { useRoutePlan } from '../context/routePlanContext'
import { planMacroRoute } from '../lib/amap/planMacroRoute'
import { hasAmapJsKey, hasAmapWebKey, hasFullAmapSetup } from '../lib/amap/config'
import type { RouteWaypoint, TransportMode } from '../types/macroRoute'
import { TRANSPORT_OPTIONS } from '../types/macroRoute'

export function RoutePlanningPage() {
  const navigate = useNavigate()
  const { setMacroPlan, setActiveWaypoint } = useRoutePlan()
  const [selected, setSelected] = useState<RouteWaypoint[]>([])
  const [transportMode, setTransportMode] = useState<TransportMode>('transit')
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [localPlan, setLocalPlan] = useState<Awaited<ReturnType<typeof planMacroRoute>> | null>(null)

  const selectedIds = useMemo(() => new Set(selected.map((s) => s.id)), [selected])

  const toggleWaypoint = (wp: RouteWaypoint, checked: boolean) => {
    setSelected((prev) => {
      if (checked) {
        if (prev.some((p) => p.id === wp.id)) return prev
        return [...prev, wp]
      }
      return prev.filter((p) => p.id !== wp.id)
    })
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
    } catch (err) {
      setError(err instanceof Error ? err.message : '路线规划失败')
      setLocalPlan(null)
    } finally {
      setPlanning(false)
    }
  }

  const goNavigate = () => {
    if (!localPlan) return
    setMacroPlan(localPlan)
    navigate('/navigate', { replace: false })
  }

  return (
    <div className="mx-auto max-w-[1320px] animate-fade-rise px-5 py-8 md:px-10">
      <PageHeader
        eyebrow="Beijing Route"
        title="北京市内 · 多景点路径规划"
        description="搜索并勾选北京市内景点，系统将按优化顺序生成路线，并在地图上展示。"
      />

      {!hasFullAmapSetup() ? (
        <InlineNotice variant="info">
          高德配置不完整：{!hasAmapJsKey() ? '缺 JS Key（地图）' : ''}
          {!hasAmapWebKey() ? '缺 Web 服务 Key（路径规划）' : ''}。请在 .env 填写 VITE_AMAP_JS_KEY、VITE_AMAP_WEB_KEY、VITE_AMAP_SECURITY_CODE。
        </InlineNotice>
      ) : null}

      {error ? (
        <InlineNotice variant="error" onRetry={() => void handlePlan()}>
          {error}
        </InlineNotice>
      ) : null}

      <section className="mb-6 min-h-[300px] lg:min-h-[340px]">
        <DestinationSearchGrid selectedIds={selectedIds} onToggle={toggleWaypoint} />
      </section>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <span className="font-body text-xs font-semibold uppercase tracking-wider text-[var(--ds-muted-foreground)]">
          交通方式
        </span>
        {TRANSPORT_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => {
              setTransportMode(opt.value)
              setLocalPlan(null)
            }}
            className={`rounded-full border px-4 py-2 font-body text-sm font-semibold transition ${
              transportMode === opt.value
                ? 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-[var(--ds-primary-foreground)]'
                : 'border-[color-mix(in_srgb,var(--ds-border)_60%,transparent)] bg-white text-[var(--ds-foreground)]'
            }`}
          >
            {opt.icon} {opt.label}
          </button>
        ))}
        <span className="font-body text-xs text-[var(--ds-muted-foreground)]">
          已选 {selected.length} 个景点 · 顺序将自动优化
        </span>
      </div>

      <section className="grid min-h-[420px] gap-5 lg:grid-cols-[minmax(240px,28%)_1fr]">
        <RouteSequenceSidebar plan={localPlan} planning={planning} />
        <AmapMapView
          key="route-plan-map"
          className="min-h-[420px]"
          waypoints={localPlan?.waypoints ?? selected}
          polyline={localPlan?.polyline}
        />
      </section>

      <div className="mt-6 flex flex-wrap gap-3">
        <PrimaryButton
          fullWidth
          className="max-w-md py-4 text-base"
          disabled={planning || selected.length < 2}
          onClick={() => void handlePlan()}
        >
          {planning ? '正在生成路线…' : '生成路线'}
        </PrimaryButton>
        {localPlan ? (
          <PrimaryButton variant="secondary" className="py-4" onClick={goNavigate}>
            前往旅行导航
          </PrimaryButton>
        ) : null}
      </div>
    </div>
  )
}
