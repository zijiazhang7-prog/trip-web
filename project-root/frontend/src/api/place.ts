import { httpRequest } from './http'

export type PlaceVO = {
  id: number
  destinationId: number
  name: string
  placeType?: string
  description?: string
  lng?: number | string
  lat?: number | string
  floorInfo?: string
  openTimeRule?: string
  suggestedDurationMin?: number
  costLevel?: number
}

export async function fetchDestinationPlaces(
  destinationId: number,
  placeType?: string,
): Promise<PlaceVO[]> {
  const query = new URLSearchParams()
  if (placeType) query.set('placeType', placeType)
  const suffix = query.toString() ? `?${query}` : ''
  const raw = await httpRequest<PlaceVO[] | { list?: PlaceVO[] }>(
    `/api/v1/destinations/${destinationId}/places${suffix}`,
    { method: 'GET' },
  )
  if (Array.isArray(raw)) return raw
  return raw.list ?? []
}
