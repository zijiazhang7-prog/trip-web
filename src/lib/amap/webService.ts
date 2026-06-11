import { getAmapWebKey } from './config'
import { decodeAmapPolyline } from './polyline'

type DirectionMode = 'driving' | 'walking' | 'bicycling'

type RawDirectionPath = {
  distance: string | number
  duration: string | number
  steps?: Array<{
    instruction?: string
    distance?: string | number
    duration?: string | number
    polyline?: string
  }>
}

type RawDirectionResponse = {
  status: string
  info: string
  route?: {
    paths?: RawDirectionPath[]
  }
}

type GeocodeResponse = {
  status: string
  info: string
  geocodes?: Array<{ location?: string }>
}

type AroundPoi = {
  id: string
  name: string
  type: string
  address: string
  distance: string
  location: string
}

type AroundResponse = {
  status: string
  info: string
  pois?: AroundPoi[]
}

async function amapGet<T>(path: string, params: Record<string, string>): Promise<T> {
  const key = getAmapWebKey()
  if (!key) throw new Error('未配置 VITE_AMAP_WEB_KEY（Web 服务 Key）')

  const query = new URLSearchParams({ ...params, key })
  const res = await fetch(`/amap-api${path}?${query}`)
  if (!res.ok) throw new Error(`高德请求失败 (${res.status})`)
  const json = (await res.json()) as T & { status?: string; info?: string }
  if (json.status !== '1') {
    throw new Error(json.info || '高德接口返回错误')
  }
  return json
}

export async function geocodeBeijingSpot(name: string): Promise<{ lng: number; lat: number } | null> {
  try {
    const data = await amapGet<GeocodeResponse>('/v3/geocode/geo', {
      address: `北京市${name}`,
      city: '北京',
    })
    const loc = data.geocodes?.[0]?.location
    if (!loc) return null
    const [lng, lat] = loc.split(',').map(Number)
    if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
    return { lng, lat }
  } catch {
    return null
  }
}

export type LegResult = {
  distance: number
  duration: number
  polyline: [number, number][]
  instruction?: string
}

export async function fetchDirectionLeg(
  mode: DirectionMode,
  origin: { lng: number; lat: number },
  destination: { lng: number; lat: number },
): Promise<LegResult> {
  const data = await amapGet<RawDirectionResponse>(`/v3/direction/${mode}`, {
    origin: `${origin.lng},${origin.lat}`,
    destination: `${destination.lng},${destination.lat}`,
    extensions: 'all',
  })

  const path = data.route?.paths?.[0]
  if (!path) throw new Error('未获取到路线')

  const polyline: [number, number][] = []
  for (const step of path.steps ?? []) {
    if (step.polyline) polyline.push(...decodeAmapPolyline(step.polyline))
  }

  return {
    distance: Number(path.distance) || 0,
    duration: Number(path.duration) || 0,
    polyline,
    instruction: path.steps?.[0]?.instruction,
  }
}

export type NearbyPoiResult = {
  id: string
  name: string
  type: string
  address: string
  distance: number
  lng: number
  lat: number
}

const POI_TYPE_FILTER: Record<string, string> = {
  toilet: '110000',
  canteen: '050000',
  shop: '060000',
  '': '',
}

export async function fetchAroundPois(
  lng: number,
  lat: number,
  facilityType: string,
  radius = 3000,
): Promise<NearbyPoiResult[]> {
  const types = POI_TYPE_FILTER[facilityType] ?? ''
  const params: Record<string, string> = {
    location: `${lng},${lat}`,
    radius: String(radius),
    offset: '20',
    page: '1',
    extensions: 'base',
  }
  if (types) params.types = types
  if (facilityType === 'toilet') params.keywords = '厕所'
  if (facilityType === 'canteen') params.keywords = '餐厅'
  if (facilityType === 'shop') params.keywords = '商店'

  const data = await amapGet<AroundResponse>('/v3/place/around', params)
  return (data.pois ?? []).map((p) => {
    const [plng, plat] = p.location.split(',').map(Number)
    return {
      id: p.id,
      name: p.name,
      type: p.type,
      address: p.address,
      distance: Number(p.distance) || 0,
      lng: plng,
      lat: plat,
    }
  })
}
