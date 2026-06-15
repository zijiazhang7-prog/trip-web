import type { ReactNode } from 'react'
import { AuthUiContext } from './authUi'

export function AuthUiProvider({
  children,
  requestLogin,
}: {
  children: ReactNode
  requestLogin: () => void
}) {
  return <AuthUiContext.Provider value={{ requestLogin }}>{children}</AuthUiContext.Provider>
}
