import { lazy, Suspense, useCallback, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { CinematicHero } from '../components/home/CinematicHero'
import { ScrollCue } from '../components/home/ScrollCue'

const RecommendSection = lazy(() =>
  import('./RecommendSection').then((m) => ({ default: m.RecommendSection })),
)

function RecommendFallback() {
  return (
    <div className="animate-pulse columns-1 gap-x-6 md:columns-2 xl:columns-3">
      {Array.from({ length: 6 }).map((_, i) => (
        <div
          key={i}
          className="mb-6 break-inside-avoid overflow-hidden rounded-[28px] border border-[var(--ds-primary)]/10 bg-white/60"
        >
          <div className="h-60 bg-[var(--ds-muted)]" />
          <div className="space-y-3 p-6">
            <div className="h-6 w-2/3 rounded-lg bg-[var(--ds-muted)]" />
            <div className="h-4 w-full rounded-lg bg-[var(--ds-muted)]" />
            <div className="h-4 w-5/6 rounded-lg bg-[var(--ds-muted)]" />
          </div>
        </div>
      ))}
    </div>
  )
}

export function HomePage() {
  const location = useLocation()
  const navigate = useNavigate()

  const scrollToRecommend = useCallback(() => {
    navigate({ pathname: '/', hash: 'recommend-prefs' })
    window.setTimeout(() => {
      document.getElementById('recommend')?.scrollIntoView({ behavior: 'smooth' })
    }, 60)
  }, [navigate])

  const openPreferences = location.hash === '#recommend-prefs'

  useEffect(() => {
    if (location.hash !== '#recommend' && location.hash !== '#recommend-prefs') return
    const t = window.setTimeout(() => {
      document.getElementById('recommend')?.scrollIntoView({ behavior: 'smooth' })
    }, 100)
    return () => window.clearTimeout(t)
  }, [location.hash, location.pathname])

  return (
    <>
      <section id="hero" className="relative scroll-mt-28">
        <CinematicHero onStartStory={scrollToRecommend} />
        <ScrollCue onEnter={scrollToRecommend} />
      </section>

      {/* Organic "fold" — overlaps hero so video reads into rice-paper section */}
      <section id="recommend" className="relative z-10 -mt-24 scroll-mt-28 bg-[var(--ds-cream)] md:-mt-32">
        <div
          className="relative border border-[color-mix(in_srgb,var(--ds-border)_42%,transparent)] border-b-0 bg-[var(--ds-cream)] px-4 pb-6 pt-20 shadow-[0_-28px_80px_-48px_rgba(18,55,42,0.18)] sm:px-6 md:px-10 md:pt-24"
          style={{
            borderTopLeftRadius: 'var(--ds-organic-tl)',
            borderTopRightRadius: 'var(--ds-organic-tr)',
            boxShadow: '0 -24px 90px -50px rgba(18, 55, 42, 0.12), inset 0 1px 0 0 rgba(255,255,255,0.65)',
          }}
        >
          <div
            className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-gradient-to-b from-[var(--ds-cream)] to-transparent"
            aria-hidden
          />
          <Suspense fallback={<RecommendFallback />}>
            <RecommendSection openPreferences={openPreferences} />
          </Suspense>
        </div>
      </section>
    </>
  )
}
