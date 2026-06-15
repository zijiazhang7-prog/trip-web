type Top10Item = {
  id: number | string
  name: string
  meta?: string
  image?: string
  onClick?: () => void
}

type Top10StripProps = {
  title: string
  items: Top10Item[]
  loading?: boolean
}

export function Top10Strip({ title, items, loading }: Top10StripProps) {
  if (loading) {
    return (
      <div className="mb-6 animate-pulse rounded-[1.5rem] border border-[var(--ds-primary)]/10 bg-white/60 p-4">
        <div className="mb-3 h-4 w-32 rounded bg-[var(--ds-muted)]" />
        <div className="flex gap-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="h-20 w-28 shrink-0 rounded-xl bg-[var(--ds-muted)]" />
          ))}
        </div>
      </div>
    )
  }

  if (!items.length) return null

  return (
    <div className="mb-6 rounded-[1.5rem] border border-[var(--ds-primary)]/12 bg-white/70 p-4 shadow-sm backdrop-blur-md">
      <h3 className="mb-3 font-display text-sm font-semibold tracking-wide text-[var(--ds-foreground)]">
        {title}
      </h3>
      <div className="flex gap-3 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {items.map((item, idx) => (
          <button
            key={String(item.id)}
            type="button"
            onClick={item.onClick}
            className="group flex w-28 shrink-0 flex-col overflow-hidden rounded-xl border border-[var(--ds-primary)]/10 bg-white text-left transition hover:-translate-y-0.5 hover:shadow-md"
          >
            <div className="relative h-16 overflow-hidden bg-[var(--ds-muted)]">
              {item.image ? (
                <img src={item.image} alt="" className="h-full w-full object-cover" loading="lazy" />
              ) : null}
              <span className="absolute left-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-[var(--ds-primary)] text-[10px] font-bold text-white">
                {idx + 1}
              </span>
            </div>
            <div className="p-2">
              <p className="line-clamp-2 font-body text-[11px] font-semibold text-[var(--ds-foreground)]">
                {item.name}
              </p>
              {item.meta ? (
                <p className="mt-0.5 font-body text-[10px] text-[var(--ds-muted-foreground)]">{item.meta}</p>
              ) : null}
            </div>
          </button>
        ))}
      </div>
    </div>
  )
}
