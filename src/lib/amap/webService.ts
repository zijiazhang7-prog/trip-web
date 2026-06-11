import type { NavStep } from '../../types/macroRoute'
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
  steps?: NavStep[]
}

function stepsFromPath(path: RawDirectionPath, mode: DirectionMode): NavStep[] {
  return (path.steps ?? []).map((step) => ({
    type: mode,
    instruction: step.instruction?.replace(/<[^>]+>/g, '') || '继续前行',
    distance: Number(step.distance) || undefined,
    duration: Number(step.duration) || undefined,
  }))
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

  const steps = stepsFromPath(path, mode)

  return {
    distance: Number(path.distance) || 0,
    duration: Number(path.duration) || 0,
    polyline,
    instruction: steps[0]?.instruction,
    steps,
  }
}

type RawTransitSegment = {
  walking?: {
    distance?: string | number
    duration?: string | number
    steps?: Array<{
      instruction?: string
      distance?: string | number
      duration?: string | number
      polyline?: string
    }>
  }
  bus?: {
    buslines?: Array<{
      name?: string
      departure_stop?: { name?: string }
      arrival_stop?: { name?: string }
      distance?: string | number
      duration?: string | number
    }>
  }
  railway?: {
    name?: string
    trip?: string
    distance?: string | number
    time?: string | number
    departure_stop?: { name?: string }
    arrival_stop?: { name?: string }
  }
}

type RawTransitResponse = {
  route?: {
    transits?: Array<{
      distance?: string | number
      duration?: string | number
      segments?: RawTransitSegment[]
    }>
  }
}

export async function fetchTransitLeg(
  origin: { lng: number; lat: number },
  destination: { lng: number; lat: number },
  city = '北京',
): Promise<LegResult> {
  const data = await amapGet<RawTransitResponse>('/v3/direction/transit/integrated', {
    origin: `${origin.lng},${origin.lat}`,
    destination: `${destination.lng},${destination.lat}`,
    city,
    cityd: city,
    strategy: '0',
    nightflag: '0',
  })

  const transit = data.route?.transits?.[0]
  if (!transit) throw new Error('未获取到公交地铁路线')

  const steps: NavStep[] = []
  const polyline: [number, number][] = []

  for (const seg of transit.segments ?? []) {
    if (seg.walking?.steps?.length) {
      for (const w of seg.walking.steps) {
        if (w.polyline) polyline.push(...decodeAmapPolyline(w.polyline))
        steps.push({
          type: 'walking',
          instruction: w.instruction?.replace(/<[^>]+>/g, '') || '步行',
          distance: Number(w.distance) || undefined,
          duration: Number(w.duration) || undefined,
        })
      }
    } else if (seg.walking) {
      steps.push({
        type: 'walking',
        instruction: `步行 ${Math.round(Number(seg.walking.distance) || 0)} 米`,
        distance: Number(seg.walking.distance) || undefined,
        duration: Number(seg.walking.duration) || undefined,
      })
    }

    const line = seg.bus?.buslines?.[0]
    if (line) {
      const isSubway = /地铁|轨道|号线/.test(line.name ?? '')
      steps.push({
        type: isSubway ? 'subway' : 'bus',
        instruction: `乘坐 ${line.name ?? '公交'}`,
        lineName: line.name,
        departure: line.departure_stop?.name,
        arrival: line.arrival_stop?.name,
        distance: Number(line.distance) || undefined,
        duration: Number(line.duration) || undefined,
      })
    }

    if (seg.railway) {
      steps.push({
        type: 'subway',
        instruction: `乘坐 ${seg.railway.name ?? seg.railway.trip ?? '地铁'}`,
        lineName: seg.railway.name ?? seg.railway.trip,
        departure: seg.railway.departure_stop?.name,
        arrival: seg.railway.arrival_stop?.name,
        distance: Number(seg.railway.distance) || undefined,
        duration: Number(seg.railway.time) || undefined,
      })
    }
  }

  if (polyline.length < 2) {
    polyline.push([origin.lng, origin.lat], [destination.lng, destination.lat])
  }

  if (polyline.length <= 2) {
    try {
      const walk = await fetchDirectionLeg('walking', origin, destination)
      if (walk.polyline.length >= 2) {
        polyline.length = 0
        polyline.push(...walk.polyline)
      }
    } catch {
      /* keep straight fallback */
    }
  }

  return {
    distance: Number(transit.distance) || 0,
    duration: Number(transit.duration) || 0,
    polyline,
    instruction: steps[0]?.instruction,
    steps,
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
