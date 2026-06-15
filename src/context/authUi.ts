import { createContext, useContext } from 'react'

export type AuthUiValue = {
  requestLogin: () => void
}

export const AuthUiContext = createContext<AuthUiValue | null>(null)

export function useAuthUi(): AuthUiValue {
  const ctx = useContext(AuthUiContext)
  return ctx ?? { requestLogin: () => {} }
}
