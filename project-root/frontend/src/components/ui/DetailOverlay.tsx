import { useEffect, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

type DetailOverlayProps = {
  open: boolean
  title: string
  onClose: () => void
  onPrev?: () => void
  onNext?: () => void
  indexLabel?: string
  children: ReactNode
}

export function DetailOverlay({
  open,
  title,
  onClose,
  onPrev,
  onNext,
  indexLabel,
  children,
}: DetailOverlayProps) {
  useEffect(() => {
    if (!open) return undefined
    const prevOverflow = document.body.style.overflow
    const prevCursor = document.body.style.cursor
    document.body.style.overflow = 'hidden'
    document.body.style.cursor = 'auto'
    document.body.classList.add('overlay-cursor-auto')
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowLeft') onPrev?.()
      if (e.key === 'ArrowRight') onNext?.()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = prevOverflow
      document.body.style.cursor = prevCursor
      document.body.classList.remove('overlay-cursor-auto')
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [open, onClose, onPrev, onNext])

  if (!open) return null

  return createPortal(
    <div
      className="detail-overlay-root fixed inset-0 z-[320] flex items-center justify-center bg-black/55 p-2 sm:p-4"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        className="flex max-h-[96vh] w-full max-w-[min(96vw,1400px)] flex-col overflow-hidden rounded-[1.75rem] border border-white/70 bg-white shadow-[0_32px_80px_rgba(0,0,0,0.28)]"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="flex shrink-0 flex-wrap items-center justify-between gap-3 border-b border-[var(--ds-border)]/40 px-5 py-4 sm:px-7">
          <h2 className="font-display text-xl font-semibold text-[var(--ds-foreground)] sm:text-2xl">
            {title}
          </h2>
          <div className="flex flex-wrap items-center gap-2">
            {indexLabel ? (
              <span className="font-body text-xs text-[var(--ds-muted-foreground)]">{indexLabel}</span>
            ) : null}
            {onPrev ? (
              <button
                type="button"
                className="rounded-full border border-[var(--ds-primary)]/25 px-3 py-1.5 text-xs font-semibold text-[var(--ds-primary)] hover:bg-[var(--ds-primary)]/5"
                onClick={onPrev}
              >
                上一条
              </button>
            ) : null}
            {onNext ? (
              <button
                type="button"
                className="rounded-full border border-[var(--ds-primary)]/25 px-3 py-1.5 text-xs font-semibold text-[var(--ds-primary)] hover:bg-[var(--ds-primary)]/5"
                onClick={onNext}
              >
                下一条
              </button>
            ) : null}
            <button
              type="button"
              className="rounded-full border border-[var(--ds-primary)]/25 px-3 py-1.5 text-xs font-semibold text-[var(--ds-primary)] hover:bg-[var(--ds-primary)]/5"
              onClick={onClose}
            >
              关闭
            </button>
          </div>
        </header>
        <div className="detail-overlay-body min-h-0 flex-1 overflow-y-auto px-3 py-3 sm:px-5 sm:py-4">{children}</div>
      </div>
    </div>,
    document.body,
  )
}
