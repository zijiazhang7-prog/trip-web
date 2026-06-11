import { NavLink, useLocation, useNavigate } from 'react-router-dom'

const routes = [
  { to: '/route', label: '路径规划' },
  { to: '/navigate', label: '导航' },
  { to: '/food', label: '美食推荐' },
  { to: '/community', label: '社群分享' },
  { to: '/diary', label: '旅游日记' },
] as const

function navClass(active: boolean) {
  return [
    'cursor-target rounded-full px-4 py-2.5 text-sm font-semibold transition duration-300 ease-out',
    active
      ? 'bg-[color-mix(in_srgb,var(--ds-cream)_18%,transparent)] text-[var(--ds-cream)] shadow-[0_4px_16px_-6px_rgba(0,0,0,0.35)]'
      : 'text-[color-mix(in_srgb,var(--ds-cream)_72%,var(--ds-sage))] hover:bg-[color-mix(in_srgb,var(--ds-cream)_10%,transparent)] hover:text-[var(--ds-cream)]',
  ].join(' ')
}

export type NavbarProps = {
  onAccountClick?: () => void
  isAuthed?: boolean
  onLogout?: () => void
}

export function Navbar({ onAccountClick, isAuthed = false, onLogout }: NavbarProps) {
  const location = useLocation()
  const navigate = useNavigate()
  const hash = location.hash
  const recommendActive = location.pathname === '/' && hash === '#recommend'

  const goHero = () => {
    navigate({ pathname: '/', hash: '' })
    window.setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }, 0)
  }

  const goRecommend = () => {
    navigate({ pathname: '/', hash: 'recommend' })
    window.setTimeout(() => {
      document.getElementById('recommend')?.scrollIntoView({ behavior: 'smooth' })
    }, 60)
  }

  return (
    <div className="sticky top-4 z-[200] flex justify-center px-4 sm:px-6">
      <header className="ds-glass-dark flex w-full max-w-6xl items-center gap-2 rounded-full px-3 py-2 md:px-5">
        <button
          type="button"
          onClick={goHero}
          className="cursor-target flex shrink-0 items-center gap-2.5 rounded-full py-1 pl-1 pr-2 text-left transition hover:opacity-90 md:gap-3 md:pr-3"
          aria-label="回到顶部"
        >
          <span
            className="flex h-10 w-10 shrink-0 items-center justify-center bg-[linear-gradient(145deg,var(--ds-moss),var(--ds-sage))] text-[var(--ds-cream)] shadow-[0_4px_14px_-4px_rgba(0,0,0,0.4)]"
            style={{ borderRadius: '60% 40% 35% 65% / 55% 45% 55% 45%' }}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden>
              <path
                d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
                fill="currentColor"
              />
            </svg>
          </span>
          <span className="font-display text-lg font-semibold tracking-tight text-[var(--ds-cream)] md:text-xl">
            智游行
          </span>
        </button>

        <nav
          className="ml-1 flex min-w-0 flex-1 flex-wrap items-center justify-center gap-0.5 sm:gap-1 md:ml-3"
          aria-label="主导航"
        >
          <button type="button" onClick={goRecommend} className={navClass(recommendActive)}>
            旅游推荐
          </button>
          {routes.map(({ to, label }) => (
            <NavLink key={to} to={to} className={({ isActive }) => navClass(isActive)}>
              {label}
            </NavLink>
          ))}
        </nav>

        {isAuthed ? (
          <button
            type="button"
            className="cursor-target ml-auto rounded-full border border-[color-mix(in_srgb,var(--ds-cream)_35%,transparent)] bg-[color-mix(in_srgb,var(--ds-cream)_12%,transparent)] px-4 py-2 text-xs font-semibold text-[var(--ds-cream)] transition hover:scale-[1.02] hover:bg-[color-mix(in_srgb,var(--ds-cream)_20%,transparent)] active:scale-[0.98]"
            onClick={() => onLogout?.()}
          >
            退出登录
          </button>
        ) : (
          <button
            type="button"
            className="cursor-target ml-auto flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-[color-mix(in_srgb,var(--ds-cream)_35%,transparent)] bg-[color-mix(in_srgb,var(--ds-cream)_12%,transparent)] text-[var(--ds-cream)] transition hover:scale-105 hover:bg-[color-mix(in_srgb,var(--ds-cream)_22%,transparent)] active:scale-95"
            aria-label="登录"
            aria-haspopup="dialog"
            onClick={() => onAccountClick?.()}
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </button>
        )}
      </header>
    </div>
  )
}
