import { createContext, useContext } from 'react'

export type TripDestinationContext = {
  destinationId: number | null
  destinationName: string | null
  setDestination: (id: number | null, name?: string | null) => void
}

export const TripContext = createContext<TripDestinationContext | null>(null)

export function useTripContext(): TripDestinationContext {
  const ctx = useContext(TripContext)
  if (!ctx) throw new Error('useTripContext must be used within TripProvider')
  return ctx
}
