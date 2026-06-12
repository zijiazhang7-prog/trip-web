import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AmapMapView } from '../components/route/AmapMapView'
import { DestinationSearchGrid } from '../components/route/DestinationSearchGrid'
import { InternalRoutePlanModal } from '../components/route/InternalRoutePlanModal'
import { RouteSequenceSidebar } from '../components/route/RouteSequenceSidebar'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import { useRoutePlan } from '../context/routePlanContext'
import { planMacroRoute } from '../lib/amap/planMacroRoute'
import { hasAmapJsKey, hasAmapWebKey, hasFullAmapSetup, getAmapSecurityCode } from '../lib/amap/config'
import type { RouteWaypoint } from '../types/macroRoute'

export function RoutePlanningPage() {
  const navigate = useNavigate()
  const { macroPlan, setMacroPlan, setActiveWaypoint } = useRoutePlan()
  const [selected, setSelected] = useState<RouteWaypoint[]>([])
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [localPlan, setLocalPlan] = useState(macroPlan)
  const [internalModalOpen, setInternalModalOpen] = useState(false)

  const selectedIds = useMemo(() => new Set(selected.map((s) => s.id)), [selected])
  const displayPlan = localPlan ?? macroPlan

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
      const result = await planMacroRoute(selected, 'transit')
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
        description="搜索并勾选北京市内景点，系统将按优化顺序生成路线，并在地图上展示。"
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

      <section className="mb-6 max-h-[min(52vh,520px)] min-h-[280px]">
        <DestinationSearchGrid selectedIds={selectedIds} onToggle={toggleWaypoint} />
      </section>

      <p className="mb-4 font-body text-sm text-[var(--ds-muted-foreground)]">
        已选 {selected.length} 个景点 · 默认公交地铁出行 · 顺序将自动优化
      </p>

      <section className="mb-6 grid min-h-[420px] gap-5 lg:grid-cols-[minmax(240px,28%)_1fr]">
        <RouteSequenceSidebar plan={displayPlan} planning={planning} />
        <AmapMapView
          key="route-plan-map"
          className="min-h-[420px]"
          waypoints={displayPlan?.waypoints ?? selected}
          polyline={displayPlan?.polyline}
          activeId={displayPlan?.waypoints[0]?.id}
        />
      </section>

      <div className="flex flex-wrap gap-3">
        <PrimaryButton
          fullWidth
          className="max-w-md py-4 text-base"
          disabled={planning || selected.length < 2}
          onClick={() => void handlePlan()}
        >
          {planning ? '正在规划并进入导航…' : '生成路线并导航'}
        </PrimaryButton>
        <button
          type="button"
          onClick={() => setInternalModalOpen(true)}
          className="rounded-full border border-[var(--ds-primary)]/25 bg-white px-6 py-4 font-body text-sm font-semibold text-[var(--ds-primary)] transition hover:bg-[var(--ds-primary)]/5"
        >
          景区内部路线
        </button>
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

      <InternalRoutePlanModal
        open={internalModalOpen}
        onClose={() => setInternalModalOpen(false)}
        selectedWaypoints={selected}
        onApplyPlan={(plan) => {
          setLocalPlan(plan)
          setMacroPlan(plan)
          setActiveWaypoint(plan.waypoints[0] ?? null)
        }}
      />
    </div>
  )
}
