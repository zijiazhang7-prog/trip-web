import { AnimatePresence, motion } from 'framer-motion'
import { useEffect, useId, useRef } from 'react'
import { createPortal } from 'react-dom'
import type { AuthMode } from './authArt'
import { getAuthArt } from './authArt'
import { AuthForm } from './AuthForm'

export type AuthModalProps = {
  open: boolean
  mode: AuthMode
  onClose: () => void
  onModeChange: (mode: AuthMode) => void
  onAuthSuccess?: () => void
}

const panelTransition = { type: 'spring', stiffness: 380, damping: 32 } as const

export function AuthModal({ open, mode, onClose, onModeChange, onAuthSuccess }: AuthModalProps) {
  const titleId = useId()
  const firstFieldRef = useRef<HTMLInputElement>(null)
  const art = getAuthArt(mode)

  useEffect(() => {
    if (!open) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const t = window.setTimeout(() => firstFieldRef.current?.focus(), 160)
    return () => window.clearTimeout(t)
  }, [open, mode])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const tree = (
    <AnimatePresence>
      {open ? (
        <div
          key="auth-overlay"
          className="fixed inset-0 z-[260] flex items-center justify-center p-4 sm:p-8"
        >
          <motion.button
            type="button"
            aria-label="关闭"
            className="absolute inset-0 cursor-target bg-[color-mix(in_srgb,#2a3328_28%,transparent)] backdrop-blur-[4px]"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.28 }}
            onClick={onClose}
          />

          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            className="relative w-full max-w-[min(100%,440px)] overflow-hidden shadow-[0_32px_90px_-28px_rgba(52,58,48,0.22),0_2px_24px_-8px_rgba(93,112,82,0.12)]"
            style={{
              borderRadius: 'clamp(1.75rem, 5vw, 2.5rem) clamp(1.5rem, 4vw, 2.15rem) clamp(2rem, 5.5vw, 2.85rem) clamp(1.65rem, 4.5vw, 2.35rem)',
            }}
            initial={{ opacity: 0, scale: 0.94, y: 12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 8 }}
            transition={panelTransition}
          >
            <div className="absolute inset-0 scale-[1.025]">
              <AnimatePresence mode="wait">
                <motion.div
                  key={art.src}
                  className="absolute inset-0"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.42, ease: [0.22, 1, 0.36, 1] }}
                >
                  <img
                    src={art.src}
                    alt=""
                    className="h-full w-full object-cover"
                    style={{ objectPosition: art.objectPosition }}
                    decoding="async"
                  />
                  <div className="absolute inset-0" style={{ background: art.overlay }} aria-hidden />
                </motion.div>
              </AnimatePresence>
            </div>

            <div className="relative px-6 pb-8 pt-7 sm:px-8 sm:pb-9 sm:pt-8">
              <button
                type="button"
                onClick={onClose}
                className="cursor-target absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full border border-white/50 bg-white/38 text-[var(--ds-foreground)] shadow-sm backdrop-blur-md transition hover:bg-white/58"
                aria-label="关闭"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M18 6L6 18M6 6l12 12" strokeLinecap="round" />
                </svg>
              </button>

              <motion.div
                className="border border-white/50 bg-[color-mix(in_srgb,var(--ds-background)_84%,transparent)] px-5 py-6 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.75),0_1px_0_0_rgba(255,255,255,0.35)] backdrop-blur-[14px] backdrop-saturate-[1.05] sm:px-6 sm:py-7 sm:backdrop-blur-[18px]"
                style={{ borderRadius: 'clamp(1.25rem, 3vw, 1.75rem)' }}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ ...panelTransition, delay: 0.05 }}
              >
                <AuthForm
                  mode={mode}
                  onModeChange={onModeChange}
                  titleId={titleId}
                  firstFieldRef={firstFieldRef}
                  onAuthSuccess={onAuthSuccess ?? onClose}
                />
              </motion.div>
            </div>
          </motion.div>
        </div>
      ) : null}
    </AnimatePresence>
  )

  return createPortal(tree, document.body)
}
