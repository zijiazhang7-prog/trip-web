import { useState } from 'react'
import type { MacroRoutePlan, RouteWaypoint } from '../../types/macroRoute'
import { TRANSPORT_OPTIONS } from '../../types/macroRoute'
import { InternalNavModal } from './InternalNavModal'

type RouteSequenceSidebarProps = {
  plan: MacroRoutePlan | null
  selected?: RouteWaypoint[]
  planning?: boolean
  onRemoveSelected?: (id: string | number) => void
}

export function RouteSequenceSidebar({
  plan,
  selected = [],
  planning,
  onRemoveSelected,
}: RouteSequenceSidebarProps) {
  const [navWaypoint, setNavWaypoint] = useState<RouteWaypoint | null>(null)
  const modeLabel = TRANSPORT_OPTIONS.find((t) => t.value === plan?.transportMode)?.label ?? '路线'
  const list = plan?.waypoints ?? selected

  const openInternalNav = (wp: RouteWaypoint) => {
    setNavWaypoint(wp)
  }

  return (
    <>
      <div className="flex h-full min-h-[360px] max-h-[min(62vh,600px)] flex-col overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-[color-mix(in_srgb,white_90%,var(--ds-background))] shadow-[var(--ds-shadow-soft)]">
        <div className="shrink-0 border-b border-[var(--ds-border)]/40 p-4">
          <div className="inline-flex w-fit items-center rounded-full border border-[color-mix(in_srgb,var(--ds-primary)_20%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_10%,white)] px-4 py-1.5 font-display text-sm font-semibold text-[var(--ds-primary)]">
            我的路线
          </div>
          <p className="font-body mt-2 text-xs text-[var(--ds-muted-foreground)]">
            {plan ? `${modeLabel} · 已生成 ${list.length} 站` : `已选 ${selected.length} 站 · 点击站点打开内部导航`}
          </p>

          {selected.length > 0 ? (
            <div className="mt-3 flex snap-x snap-mandatory gap-2 overflow-x-auto pb-1">
              {selected.map((wp, i) => (
                <button
                  key={String(wp.id)}
                  type="button"
                  onClick={() => openInternalNav(wp)}
                  className={`shrink-0 snap-start rounded-full border px-3 py-1 text-xs font-semibold transition ${
                    navWaypoint && String(navWaypoint.id) === String(wp.id)
                      ? 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white'
                      : 'border-[var(--ds-primary)]/25 bg-white text-[var(--ds-primary)]'
                  }`}
                >
                  {i + 1}. {wp.name.length > 8 ? `${wp.name.slice(0, 8)}…` : wp.name}
                </button>
              ))}
            </div>
          ) : null}
        </div>

        {planning ? (
          <p className="py-6 text-center font-body text-sm text-[var(--ds-muted-foreground)]">正在优化顺序并规划…</p>
        ) : null}

        {!planning && list.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-2 py-8 text-center">
            <p className="font-body text-sm text-[var(--ds-muted-foreground)]">在上方滑动选择目的地后加入此处</p>
          </div>
        ) : null}

        {!planning && list.length > 0 ? (
          <ol className="flex min-h-0 flex-1 flex-col gap-2 overflow-y-auto p-4 pt-2">
            {list.map((wp, i) => {
              const seg = plan?.segments?.[i - 1]
              return (
                <li key={String(wp.id)} className="flex w-full flex-col">
                  {seg ? (
                    <div className="mb-2 rounded-xl border border-dashed border-[color-mix(in_srgb,var(--ds-primary)_22%,transparent)] bg-[color-mix(in_srgb,var(--ds-muted)_40%,white)] px-3 py-2">
                      <p className="font-body text-[11px] font-semibold text-[var(--ds-foreground)]">
                        {seg.fromName} → {seg.toName}
                      </p>
                    </div>
                  ) : null}
                  <div className="w-full rounded-2xl border border-[color-mix(in_srgb,var(--ds-primary)_18%,transparent)] bg-white shadow-sm">
                    <button
                      type="button"
                      onClick={() => openInternalNav(wp)}
                      className="flex w-full items-center justify-between gap-2 px-4 py-3 text-left"
                    >
                      <div>
                        <span className="font-body text-[10px] font-bold uppercase tracking-wider text-[var(--ds-muted-foreground)]">
                          第 {i + 1} 站
                        </span>
                        <p className="font-display text-sm font-semibold text-[var(--ds-foreground)]">{wp.name}</p>
                      </div>
                      <span className="text-xs text-[var(--ds-primary)]">内部导航 →</span>
                    </button>
                    {!plan && onRemoveSelected ? (
                      <div className="border-t border-[var(--ds-border)]/30 px-4 py-2">
                        <button
                          type="button"
                          onClick={() => onRemoveSelected(wp.id)}
                          className="font-body text-[10px] text-[var(--ds-destructive)]"
                        >
                          移出路线
                        </button>
                      </div>
                    ) : null}
                  </div>
                </li>
              )
            })}
          </ol>
        ) : null}
      </div>

      <InternalNavModal
        open={navWaypoint != null}
        waypoint={navWaypoint}
        onClose={() => setNavWaypoint(null)}
      />
    </>
  )
}
