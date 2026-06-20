export type ShellBackdropVariant = 'ambient' | 'plain' | 'white' | 'dark'

/**
 * Global page canvas behind content.
 * - `white`：首页 hero 下方过渡
 * - `plain`：旅游日记纸感
 * - `ambient`：其余页奶油底 + 森林渐变光斑
 * - `dark`：深林沉浸（可选）
 */
export function ShellBackdrop({ variant }: { variant: ShellBackdropVariant }) {
  if (variant === 'white') {
    return (
      <div
        className="pointer-events-none fixed inset-0 -z-20 bg-[linear-gradient(180deg,var(--ds-cream)_0%,#ffffff_42%,var(--ds-muted)_100%)]"
        aria-hidden
      />
    )
  }

  if (variant === 'dark') {
    return (
      <div
        className="pointer-events-none fixed inset-0 -z-20 bg-[linear-gradient(165deg,var(--ds-forest)_0%,#0a1f17_55%,var(--ds-moss)_100%)]"
        aria-hidden
      />
    )
  }

  if (variant === 'plain') {
    return (
      <div className="pointer-events-none fixed inset-0 -z-20 overflow-hidden bg-[var(--ds-cream)]">
        <div
          className="absolute -left-[8%] top-[-6%] h-[min(76vw,520px)] w-[min(76vw,520px)] bg-[color-mix(in_srgb,var(--ds-sage)_42%,transparent)] blur-[100px]"
          style={{ borderRadius: '60% 40% 30% 70% / 60% 30% 70% 40%' }}
          aria-hidden
        />
        <div
          className="absolute -right-[10%] bottom-[-10%] h-[min(68vw,460px)] w-[min(68vw,460px)] bg-[color-mix(in_srgb,var(--ds-moss)_22%,transparent)] blur-[110px]"
          style={{ borderRadius: '40% 60% 70% 30% / 45% 55% 45% 55%' }}
          aria-hidden
        />
        <div
          className="absolute left-1/2 top-1/2 h-[min(50vw,380px)] w-[min(50vw,380px)] -translate-x-1/2 -translate-y-1/2 bg-[color-mix(in_srgb,var(--ds-cream)_80%,white)] blur-[80px]"
          style={{ borderRadius: '55% 45% 45% 55% / 50% 50% 50% 50%' }}
          aria-hidden
        />
      </div>
    )
  }

  return (
    <div className="pointer-events-none fixed inset-0 -z-20 overflow-hidden bg-[var(--ds-cream)]">
      <div
        className="absolute inset-0 opacity-90"
        style={{
          background: `
            radial-gradient(120% 90% at 50% -10%, color-mix(in srgb, var(--ds-sage) 35%, transparent) 0%, transparent 55%),
            radial-gradient(90% 70% at 100% 80%, color-mix(in srgb, var(--ds-moss) 18%, transparent) 0%, transparent 50%),
            radial-gradient(80% 60% at 0% 90%, color-mix(in srgb, var(--ds-sage) 28%, transparent) 0%, transparent 45%),
            linear-gradient(180deg, var(--ds-cream) 0%, color-mix(in srgb, var(--ds-muted) 65%, var(--ds-cream)) 100%)
          `,
        }}
        aria-hidden
      />
      <div
        className="absolute -left-[12%] top-[-8%] h-[min(82vw,720px)] w-[min(82vw,720px)] bg-[color-mix(in_srgb,var(--ds-sage)_32%,transparent)] blur-[130px]"
        style={{ borderRadius: '50%' }}
        aria-hidden
      />
      <div
        className="absolute -right-[14%] top-[12%] h-[min(68vw,600px)] w-[min(68vw,600px)] bg-[color-mix(in_srgb,var(--ds-moss)_20%,transparent)] blur-[120px]"
        style={{ borderRadius: '50%' }}
        aria-hidden
      />
      <div
        className="absolute bottom-[-18%] left-[18%] h-[min(85vw,780px)] w-[min(85vw,780px)] bg-[color-mix(in_srgb,var(--ds-forest)_12%,transparent)] blur-[140px]"
        style={{ borderRadius: '50%' }}
        aria-hidden
      />
    </div>
  )
}
