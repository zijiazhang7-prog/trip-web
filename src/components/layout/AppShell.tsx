import { useCallback, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { clearStoredToken, hasStoredToken } from '../../api/http'
import { AuthUiProvider } from '../../context/AuthUiProvider'
import { AuthModal } from '../auth/AuthModal'
import type { AuthMode } from '../auth/authArt'
import { TargetCursor } from '../effects/TargetCursor'
import { Navbar } from './Navbar'
import { ShellBackdrop, type ShellBackdropVariant } from './ShellBackdrop'

export function AppShell() {
  const { pathname } = useLocation()
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [isAuthed, setIsAuthed] = useState<boolean>(() => hasStoredToken())
  const backdropVariant: ShellBackdropVariant =
    pathname === '/' ? 'white' : pathname === '/diary' ? 'plain' : 'ambient'
  const rootWash =
    pathname === '/' ? 'bg-[var(--ds-forest)]' : pathname === '/diary' ? 'bg-[var(--ds-background)]' : 'bg-transparent'

  const requestLogin = useCallback(() => {
    setAuthMode('login')
    setAuthOpen(true)
  }, [])

  return (
    <div className={`relative min-h-screen text-[var(--ds-foreground)] ${rootWash}`}>
      <TargetCursor
        spinDuration={2.2}
        hideDefaultCursor
        hoverDuration={0.22}
        parallaxOn
      />
      <ShellBackdrop variant={backdropVariant} />
      <Navbar
        isAuthed={isAuthed}
        onLogout={() => {
          clearStoredToken()
          setIsAuthed(false)
        }}
        onAccountClick={() => {
          setAuthMode('login')
          setAuthOpen(true)
        }}
      />
      <AuthModal
        open={authOpen}
        mode={authMode}
        onClose={() => setAuthOpen(false)}
        onModeChange={setAuthMode}
        onAuthSuccess={() => {
          setIsAuthed(true)
          setAuthOpen(false)
        }}
      />
      <AuthUiProvider requestLogin={requestLogin}>
        <Outlet />
      </AuthUiProvider>
    </div>
  )
}
