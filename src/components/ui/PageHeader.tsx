type PageHeaderProps = {
  eyebrow: string
  title: string
  description?: string
}

export function PageHeader({ eyebrow, title, description }: PageHeaderProps) {
  return (
    <header className="relative mb-14">
      <div className="flex items-center gap-3">
        <span className="h-px w-9 bg-[color-mix(in_srgb,var(--ds-primary)_55%,transparent)]" aria-hidden />
        <span className="font-display text-[11px] font-semibold uppercase italic tracking-[0.34em] text-[var(--ds-primary)]">
          {eyebrow}
        </span>
      </div>

      <h1 className="mt-5 font-display text-[clamp(2.1rem,4vw,3.4rem)] font-semibold leading-[1.04] tracking-[-0.015em] text-[var(--ds-foreground)]">
        {title}
      </h1>

      {description ? (
        <p className="mt-4 max-w-2xl font-body text-[15.5px] leading-relaxed text-[var(--ds-muted-foreground)]">
          {description}
        </p>
      ) : null}

      <div
        className="mt-7 h-px w-full bg-[linear-gradient(to_right,color-mix(in_srgb,var(--ds-border)_85%,transparent),transparent)]"
        aria-hidden
      />
    </header>
  )
}
