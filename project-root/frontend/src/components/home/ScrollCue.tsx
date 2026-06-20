type ScrollCueProps = {
  onEnter: () => void
}

export function ScrollCue({ onEnter }: ScrollCueProps) {
  return (
    <div className="pointer-events-auto absolute inset-x-0 bottom-0 z-20 flex flex-col items-center pb-8 pt-4 md:pb-12">
      <button
        type="button"
        onClick={onEnter}
        className="cursor-target font-body group flex flex-col items-center gap-2.5 text-sm font-semibold text-[color-mix(in_srgb,var(--ds-cream)_82%,var(--ds-sage))] transition duration-300 hover:text-white"
        aria-label="向下滑动进入旅游推荐"
      >
        <span className="tracking-wide">向下滑动 · 探索推荐</span>
        <span
          className="flex h-11 w-7 items-start justify-center border border-[color-mix(in_srgb,var(--ds-border)_70%,var(--ds-primary))] bg-[color-mix(in_srgb,var(--ds-background)_55%,transparent)] px-1.5 pt-2.5 shadow-[var(--ds-shadow-soft)] backdrop-blur-md transition duration-300 group-hover:border-[var(--ds-primary)] group-hover:shadow-[var(--ds-shadow-float)]"
          style={{ borderRadius: '999px 999px 60% 60% / 70% 70% 38% 38%' }}
        >
          <span
            className="block h-2 w-2 animate-bounce shadow-sm"
            style={{
              borderRadius: '55% 45% 50% 50% / 50% 50% 50% 50%',
              background: 'linear-gradient(145deg, var(--ds-primary), color-mix(in srgb, var(--ds-secondary) 55%, var(--ds-primary)))',
            }}
          />
        </span>
      </button>
    </div>
  )
}
