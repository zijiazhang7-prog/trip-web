import type { MacroRoutePlan } from '../../types/macroRoute'
import { TRANSPORT_OPTIONS } from '../../types/macroRoute'

type RouteSequenceSidebarProps = {
  plan: MacroRoutePlan | null
  planning?: boolean
}

export function RouteSequenceSidebar({ plan, planning }: RouteSequenceSidebarProps) {
  const modeLabel = TRANSPORT_OPTIONS.find((t) => t.value === plan?.transportMode)?.label ?? '路线'

  return (
    <div className="flex h-full max-h-[min(58vh,560px)] flex-col overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-[color-mix(in_srgb,white_90%,var(--ds-background))] p-5 shadow-[var(--ds-shadow-soft)]">
      <div className="mb-4 inline-flex w-fit items-center rounded-full border border-[color-mix(in_srgb,var(--ds-primary)_20%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_10%,white)] px-4 py-1.5 font-display text-sm font-semibold text-[var(--ds-primary)]">
        路线 A
      </div>
      <p className="font-body mb-4 text-xs text-[var(--ds-muted-foreground)]">
        {plan ? `${modeLabel} · 优化顺序` : '勾选景点后点击「生成路线」'}
      </p>

      {planning ? (
        <p className="py-6 text-center font-body text-sm text-[var(--ds-muted-foreground)]">正在优化顺序并规划…</p>
      ) : null}

      {!planning && !plan ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 py-8 text-center">
          <div className="h-16 w-px border-l-2 border-dashed border-[color-mix(in_srgb,var(--ds-primary)_30%,transparent)]" />
          <p className="font-body text-sm text-[var(--ds-muted-foreground)]">暂无站点</p>
        </div>
      ) : null}

      {plan ? (
        <ol className="flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto py-2 pr-1">
          {plan.waypoints.map((wp, i) => {
            const seg = plan.segments?.[i - 1]
            return (
              <li key={String(wp.id)} className="flex w-full flex-col">
                {seg ? (
                  <div className="mb-2 rounded-xl border border-dashed border-[color-mix(in_srgb,var(--ds-primary)_22%,transparent)] bg-[color-mix(in_srgb,var(--ds-muted)_40%,white)] px-3 py-2">
                    <p className="font-body text-[11px] font-semibold text-[var(--ds-foreground)]">
                      {seg.fromName} → {seg.toName}
                    </p>
                    {seg.steps?.length ? (
                      <ul className="mt-1.5 space-y-1 border-l border-[color-mix(in_srgb,var(--ds-primary)_18%,transparent)] pl-2">
                        {seg.steps.slice(0, 4).map((step, idx) => (
                          <li key={`${step.instruction}-${idx}`} className="font-body text-[10px] text-[var(--ds-muted-foreground)]">
                            {step.instruction}
                          </li>
                        ))}
                        {(seg.steps?.length ?? 0) > 4 ? (
                          <li className="font-body text-[10px] text-[var(--ds-primary)]">…共 {seg.steps?.length} 段</li>
                        ) : null}
                      </ul>
                    ) : null}
                  </div>
                ) : null}
                <div className="w-full rounded-2xl border border-[color-mix(in_srgb,var(--ds-primary)_18%,transparent)] bg-white px-4 py-3 text-center shadow-sm">
                  <span className="font-body text-[10px] font-bold uppercase tracking-wider text-[var(--ds-muted-foreground)]">
                    第 {i + 1} 站
                  </span>
                  <p className="font-display mt-1 text-sm font-semibold text-[var(--ds-foreground)]">{wp.name}</p>
                </div>
              </li>
            )
          })}
        </ol>
      ) : null}
    </div>
  )
}
