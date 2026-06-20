import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { TripContext } from './tripContext'

export function TripProvider({ children }: { children: ReactNode }) {
  const [destinationId, setDestinationId] = useState<number | null>(null)
  const [destinationName, setDestinationName] = useState<string | null>(null)

  const setDestination = useCallback((id: number | null, name?: string | null) => {
    setDestinationId(id)
    setDestinationName(name ?? null)
  }, [])

  const value = useMemo(
    () => ({ destinationId, destinationName, setDestination }),
    [destinationId, destinationName, setDestination],
  )

  return <TripContext.Provider value={value}>{children}</TripContext.Provider>
}
