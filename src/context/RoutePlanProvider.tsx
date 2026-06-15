import { useCallback, useMemo, useState, type ReactNode } from 'react'
import type { MacroRoutePlan, RouteWaypoint } from '../types/macroRoute'
import { RoutePlanContext } from './routePlanContext'

const STORAGE_KEY = 'trip_macro_route_plan'

function readStoredPlan(): MacroRoutePlan | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as MacroRoutePlan
    if (!parsed?.waypoints?.length) return null
    return parsed
  } catch {
    return null
  }
}

function writeStoredPlan(plan: MacroRoutePlan | null) {
  try {
    if (!plan) {
      sessionStorage.removeItem(STORAGE_KEY)
      return
    }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(plan))
  } catch {
    /* quota / private mode */
  }
}

export function RoutePlanProvider({ children }: { children: ReactNode }) {
  const [macroPlan, setMacroPlanState] = useState<MacroRoutePlan | null>(() => readStoredPlan())
  const [activeWaypoint, setActiveWaypoint] = useState<RouteWaypoint | null>(null)

  const setMacroPlan = useCallback((plan: MacroRoutePlan | null) => {
    setMacroPlanState(plan)
    writeStoredPlan(plan)
    if (plan?.waypoints[0]) {
      setActiveWaypoint(plan.waypoints[0])
    }
  }, [])

  const value = useMemo(
    () => ({ macroPlan, setMacroPlan, activeWaypoint, setActiveWaypoint }),
    [macroPlan, setMacroPlan, activeWaypoint],
  )

  return <RoutePlanContext.Provider value={value}>{children}</RoutePlanContext.Provider>
}
