type LoadingBlockProps = {
  label?: string
  className?: string
}

export function LoadingBlock({ label = '加载中…', className = '' }: LoadingBlockProps) {
  return (
    <div
      className={`animate-pulse rounded-2xl border border-[color-mix(in_srgb,var(--ds-border)_40%,transparent)] bg-[color-mix(in_srgb,var(--ds-muted)_50%,white)] px-4 py-8 text-center font-body text-sm text-[var(--ds-muted-foreground)] ${className}`}
      role="status"
    >
      {label}
    </div>
  )
}
