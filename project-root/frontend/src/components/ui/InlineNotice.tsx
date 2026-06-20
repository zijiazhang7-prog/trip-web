type InlineNoticeVariant = 'info' | 'success' | 'error' | 'offline'

type InlineNoticeProps = {
  variant?: InlineNoticeVariant
  children: React.ReactNode
  onRetry?: () => void
}

const styles: Record<InlineNoticeVariant, string> = {
  info: 'border-[color-mix(in_srgb,var(--ds-primary)_20%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_6%,white)] text-[var(--ds-accent-foreground)]',
  success:
    'border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_10%,white)] text-[var(--ds-primary)]',
  error: 'border-[color-mix(in_srgb,var(--ds-destructive)_30%,transparent)] bg-[color-mix(in_srgb,var(--ds-destructive)_8%,white)] text-[var(--ds-destructive)]',
  offline:
    'border-amber-200/80 bg-amber-50/90 text-amber-900',
}

export function InlineNotice({ variant = 'info', children, onRetry }: InlineNoticeProps) {
  return (
    <div
      className={`mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border px-4 py-3 font-body text-sm ${styles[variant]}`}
      role="status"
    >
      <span>{children}</span>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="cursor-target shrink-0 rounded-full border border-current/20 px-3 py-1 text-xs font-semibold hover:bg-white/40"
        >
          重试
        </button>
      ) : null}
    </div>
  )
}
