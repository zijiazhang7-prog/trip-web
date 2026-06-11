export type TransportMode = 'driving' | 'walking' | 'bicycling' | 'transit'

export type NavStep = {
  type: 'walking' | 'driving' | 'bicycling' | 'bus' | 'subway' | 'railway'
  instruction: string
  distance?: number
  duration?: number
  lineName?: string
  departure?: string
  arrival?: string
}

export type RouteWaypoint = {
  id: number | string
  name: string
  lng: number
  lat: number
  city?: string
  image?: string
  destinationId?: number
}

export type RouteSegment = {
  fromName: string
  toName: string
  fromLng: number
  fromLat: number
  toLng: number
  toLat: number
  distance: number
  duration: number
  transportMode: TransportMode
  instruction?: string
  steps?: NavStep[]
}

export type MacroRoutePlan = {
  transportMode: TransportMode
  waypoints: RouteWaypoint[]
  segments: RouteSegment[]
  totalDistance: number
  totalDuration: number
  polyline: [number, number][]
  summary?: string
  createdAt: number
}

export const TRANSPORT_OPTIONS: { value: TransportMode; label: string; icon: string }[] = [
  { value: 'transit', label: '公交地铁', icon: '🚇' },
  { value: 'driving', label: '驾车', icon: '🚗' },
  { value: 'walking', label: '步行', icon: '🚶' },
  { value: 'bicycling', label: '骑行', icon: '🚴' },
]

export const BEIJING_CENTER: [number, number] = [116.397428, 39.90923]
