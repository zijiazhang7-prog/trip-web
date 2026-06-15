import { createContext, useContext } from 'react'
import type { MacroRoutePlan, RouteWaypoint } from '../types/macroRoute'

export type RoutePlanContextValue = {
  macroPlan: MacroRoutePlan | null
  setMacroPlan: (plan: MacroRoutePlan | null) => void
  activeWaypoint: RouteWaypoint | null
  setActiveWaypoint: (wp: RouteWaypoint | null) => void
}

export const RoutePlanContext = createContext<RoutePlanContextValue | null>(null)

export function useRoutePlan(): RoutePlanContextValue {
  const ctx = useContext(RoutePlanContext)
  if (!ctx) throw new Error('useRoutePlan must be used within RoutePlanProvider')
  return ctx
}
