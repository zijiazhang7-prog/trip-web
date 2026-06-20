export type MapViewMode = 'route' | 'road-graph'

type MapViewModeToggleProps = {
  mode: MapViewMode
  onChange: (mode: MapViewMode) => void
  className?: string
}

export function MapViewModeToggle({ mode, onChange, className = '' }: MapViewModeToggleProps) {
  return (
    <div className={`flex rounded-full border border-[var(--ds-border)]/50 bg-white/90 p-0.5 text-[10px] ${className}`}>
      <button
        type="button"
        className={`rounded-full px-3 py-1 font-semibold transition ${
          mode === 'route' ? 'bg-[var(--ds-primary)] text-white' : 'text-[var(--ds-muted-foreground)]'
        }`}
        onClick={() => onChange('route')}
      >
        路线图
      </button>
      <button
        type="button"
        className={`rounded-full px-3 py-1 font-semibold transition ${
          mode === 'road-graph' ? 'bg-[var(--ds-primary)] text-white' : 'text-[var(--ds-muted-foreground)]'
        }`}
        onClick={() => onChange('road-graph')}
      >
        道路图
      </button>
    </div>
  )
}
