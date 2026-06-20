import { geocodeBeijingSpot } from '../amap/webService'
import { BEIJING_ATTRACTIONS } from '../../data/beijingDestinations'
import { readSessionCache, writeSessionCache } from '../sessionCache'
import type { RouteWaypoint } from '../../types/macroRoute'

const GEO_CACHE_KEY = 'trip_beijing_geo_v1'
const GEO_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000

type GeoCache = Record<string, { lng: number; lat: number }>

function readGeoCache(): GeoCache {
  return readSessionCache<GeoCache>(GEO_CACHE_KEY, GEO_CACHE_TTL_MS) ?? {}
}

function writeGeoEntry(name: string, coords: { lng: number; lat: number }) {
  const cache = readGeoCache()
  cache[name.trim()] = coords
  writeSessionCache(GEO_CACHE_KEY, cache)
}

function fuzzyMatchAttraction(name: string) {
  const n = name.trim()
  return BEIJING_ATTRACTIONS.find(
    (a) => a.name === n || n.includes(a.name) || a.name.includes(n),
  )
}

/** 同步：仅从本地景点表匹配，不匹配则返回 null（避免默认落天安门） */
export function resolveCoordsFromLocal(name: string): { lng: number; lat: number } | null {
  const hit = fuzzyMatchAttraction(name)
  if (!hit) return null
  return { lng: hit.lng, lat: hit.lat }
}

/** 异步：本地表 → 会话缓存 → 高德地理编码 */
export async function resolveDestinationCoords(
  name: string,
  city = '北京',
): Promise<{ lng: number; lat: number } | null> {
  const key = name.trim()
  if (!key) return null

  const local = resolveCoordsFromLocal(key)
  if (local) return local

  const cached = readGeoCache()[key]
  if (cached) return cached

  const geo = await geocodeBeijingSpot(key.includes(city) ? key : `${city}${key}`)
  if (geo) {
    writeGeoEntry(key, geo)
    return geo
  }
  return null
}

export async function enrichWaypointCoords(wp: RouteWaypoint): Promise<RouteWaypoint> {
  if (Number.isFinite(wp.lng) && Number.isFinite(wp.lat)) {
    const local = resolveCoordsFromLocal(wp.name)
    const isTiananmenDefault =
      Math.abs(wp.lng - 116.397428) < 0.0001 && Math.abs(wp.lat - 39.90923) < 0.0001
    if (!isTiananmenDefault || !local) {
      if (!isTiananmenDefault) return wp
    }
  }
  const coords = await resolveDestinationCoords(wp.name, wp.city ?? '北京')
  if (!coords) return wp
  return { ...wp, lng: coords.lng, lat: coords.lat }
}

/** 批量补全坐标（限流，避免打爆高德） */
export async function enrichWaypointList(waypoints: RouteWaypoint[]): Promise<RouteWaypoint[]> {
  const out: RouteWaypoint[] = []
  for (const wp of waypoints) {
    out.push(await enrichWaypointCoords(wp))
    await new Promise((r) => window.setTimeout(r, 120))
  }
  return out
}
