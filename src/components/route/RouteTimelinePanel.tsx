import type { MacroRoutePlan, NavStep } from '../../types/macroRoute'
import { TRANSPORT_OPTIONS } from '../../types/macroRoute'

type RouteTimelinePanelProps = {
  plan: MacroRoutePlan | null
  activeId?: string | number | null
  onSelectWaypoint?: (index: number) => void
}

function formatDuration(seconds: number): string {
  const m = Math.max(1, Math.round(seconds / 60))
  if (m < 60) return `${m} 分钟`
  const h = Math.floor(m / 60)
  const rm = m % 60
  return rm ? `${h} 小时 ${rm} 分` : `${h} 小时`
}

function formatDistance(meters: number): string {
  if (meters >= 1000) return `${(meters / 1000).toFixed(1)} 公里`
  return `${Math.round(meters)} 米`
}

function etaFromNow(seconds: number): string {
  const d = new Date(Date.now() + seconds * 1000)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')} 到达`
}

function stepIcon(step: NavStep): string {
  if (step.type === 'subway' || step.type === 'railway') return '🚇'
  if (step.type === 'bus') return '🚌'
  if (step.type === 'walking') return '🚶'
  if (step.type === 'driving') return '🚗'
  if (step.type === 'bicycling') return '🚴'
  return '→'
}

function StepList({ steps }: { steps: NavStep[] }) {
  return (
    <ul className="mt-2 space-y-1.5 border-l border-[color-mix(in_srgb,var(--ds-primary)_20%,transparent)] pl-3">
      {steps.map((step, idx) => (
        <li key={`${step.instruction}-${idx}`} className="font-body text-xs text-[var(--ds-foreground)]">
          <span className="mr-1">{stepIcon(step)}</span>
          {step.instruction}
          {step.departure && step.arrival ? (
            <span className="mt-0.5 block text-[10px] text-[var(--ds-muted-foreground)]">
              {step.departure} → {step.arrival}
              {step.lineName ? ` · ${step.lineName}` : ''}
            </span>
          ) : null}
        </li>
      ))}
    </ul>
  )
}

export function RouteTimelinePanel({ plan, activeId, onSelectWaypoint }: RouteTimelinePanelProps) {
  if (!plan) {
    return (
      <div className="flex h-full min-h-[240px] flex-col items-center justify-center rounded-[2rem] border border-dashed border-[color-mix(in_srgb,var(--ds-border)_60%,transparent)] bg-white/70 p-8 text-center">
        <p className="font-display text-lg font-semibold text-[var(--ds-foreground)]">具体路线</p>
        <p className="font-body mt-2 text-sm text-[var(--ds-muted-foreground)]">
          请先在路径规划页生成北京市内路线，再进入旅行导航
        </p>
      </div>
    )
  }

  const mode = TRANSPORT_OPTIONS.find((t) => t.value === plan.transportMode)
  const modeLabel = mode?.label ?? '出行'

  return (
    <div className="flex h-full max-h-[min(58vh,560px)] flex-col overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white shadow-[var(--ds-shadow-soft)]">
      <div className="border-b border-[color-mix(in_srgb,var(--ds-border)_35%,transparent)] bg-[color-mix(in_srgb,var(--ds-muted)_40%,white)] px-5 py-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="font-display text-3xl font-bold text-[var(--ds-foreground)]">
              {formatDuration(plan.totalDuration)}
            </p>
            <p className="font-body mt-1 text-sm text-[var(--ds-muted-foreground)]">
              {etaFromNow(plan.totalDuration)} · 全程 {formatDistance(plan.totalDistance)}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="rounded-lg bg-[color-mix(in_srgb,var(--ds-primary)_12%,white)] px-2.5 py-1 text-xs font-semibold text-[var(--ds-primary)]">
              {mode?.icon} {modeLabel}
            </span>
            <span className="rounded-lg bg-[color-mix(in_srgb,var(--ds-secondary)_15%,white)] px-2.5 py-1 text-xs font-semibold text-[var(--ds-secondary)]">
              北京市
            </span>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4">
        <div className="relative ml-3 border-l-2 border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)] pl-6">
          <button type="button" className="relative mb-6 w-full text-left" onClick={() => onSelectWaypoint?.(0)}>
            <span className="absolute -left-[1.65rem] top-1 flex h-5 w-5 items-center justify-center rounded-full bg-[var(--ds-primary)] text-[10px] font-bold text-white">
              起
            </span>
            <p className="font-body text-xs text-[var(--ds-muted-foreground)]">出发点</p>
            <p className="font-display font-semibold text-[var(--ds-foreground)]">{plan.waypoints[0]?.name}</p>
          </button>

          {(plan.segments ?? []).map((seg, i) => {
            const toWp = plan.waypoints[i + 1]
            const isActive = toWp && String(activeId) === String(toWp.id)
            const steps = seg.steps?.length
              ? seg.steps
              : seg.instruction
                ? [{ type: 'walking' as const, instruction: seg.instruction }]
                : []

            return (
              <div key={`${seg.fromName}-${seg.toName}`} className="mb-6">
                <div className="mb-3 rounded-xl border border-dashed border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)] bg-[color-mix(in_srgb,var(--ds-muted)_35%,white)] px-3 py-2">
                  <p className="font-body text-sm font-semibold text-[var(--ds-foreground)]">
                    {seg.fromName} → {seg.toName}
                  </p>
                  <p className="font-body mt-1 text-xs text-[var(--ds-muted-foreground)]">
                    {modeLabel} · {formatDistance(seg.distance)}（{formatDuration(seg.duration)}）
                  </p>
                  {steps.length ? <StepList steps={steps} /> : null}
                </div>
                <button
                  type="button"
                  className={`relative w-full rounded-xl border px-3 py-2 text-left transition ${
                    isActive
                      ? 'border-[var(--ds-primary)] bg-[color-mix(in_srgb,var(--ds-primary)_8%,white)]'
                      : 'border-transparent bg-white hover:bg-[var(--ds-muted)]'
                  }`}
                  onClick={() => onSelectWaypoint?.(i + 1)}
                >
                  <span
                    className={`absolute -left-[1.65rem] top-2 h-4 w-4 rounded-full border-2 ${
                      isActive ? 'border-[var(--ds-primary)] bg-[var(--ds-primary)]' : 'border-[var(--ds-primary)] bg-white'
                    }`}
                  />
                  <p className="font-display font-semibold text-[var(--ds-foreground)]">{seg.toName}</p>
                  <p className="font-body text-xs text-[var(--ds-muted-foreground)]">第 {i + 2} 站 · 地图同步此段导航</p>
                </button>
              </div>
            )
          })}
        </div>
      </div>

      <div className="flex items-center justify-between gap-2 border-t border-[color-mix(in_srgb,var(--ds-border)_35%,transparent)] px-5 py-3">
        <span className="font-body text-xs text-[var(--ds-muted-foreground)]">高德导航 · 含步行/地铁/公交分段</span>
        <span className="rounded-full bg-[color-mix(in_srgb,var(--ds-primary)_90%,#12372a)] px-4 py-1.5 text-xs font-semibold text-white">
          导航中
        </span>
      </div>
    </div>
  )
}
