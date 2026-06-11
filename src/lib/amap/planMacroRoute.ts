import type { MacroRoutePlan, RouteSegment, RouteWaypoint, TransportMode } from '../../types/macroRoute'
import { BEIJING_ATTRACTIONS } from '../../data/beijingDestinations'
import { hasAmapWebKey } from './config'
import { fetchDirectionLeg, geocodeBeijingSpot } from './webService'
import { optimizeWaypointOrder } from './optimizeOrder'

async function resolveCoordinates(wp: RouteWaypoint): Promise<RouteWaypoint> {
  if (Number.isFinite(wp.lng) && Number.isFinite(wp.lat) && wp.lng !== 0) return wp

  const fallback = BEIJING_ATTRACTIONS.find((a) => a.id === wp.id || a.name === wp.name)
  if (fallback) return { ...wp, lng: fallback.lng, lat: fallback.lat }

  if (hasAmapWebKey()) {
    const geo = await geocodeBeijingSpot(wp.name)
    if (geo) return { ...wp, ...geo }
  }

  throw new Error(`无法定位「${wp.name}」，请检查高德 Key 或离线数据`)
}

function mockLeg(
  from: RouteWaypoint,
  to: RouteWaypoint,
  mode: TransportMode,
): { distance: number; duration: number; polyline: [number, number][] } {
  const dx = to.lng - from.lng
  const dy = to.lat - from.lat
  const dist = Math.sqrt(dx * dx + dy * dy) * 111320
  const speed = mode === 'driving' ? 8 : mode === 'bicycling' ? 4 : 1.2
  return {
    distance: Math.round(dist),
    duration: Math.round(dist / speed),
    polyline: [
      [from.lng, from.lat],
      [to.lng, to.lat],
    ],
  }
}

export async function planMacroRoute(
  selected: RouteWaypoint[],
  transportMode: TransportMode,
): Promise<MacroRoutePlan> {
  if (selected.length < 2) {
    throw new Error('请至少勾选 2 个北京市内景点')
  }

  const resolved = await Promise.all(selected.map(resolveCoordinates))
  const waypoints = optimizeWaypointOrder(resolved)

  const segments: RouteSegment[] = []
  const polyline: [number, number][] = []
  let totalDistance = 0
  let totalDuration = 0

  for (let i = 0; i < waypoints.length - 1; i++) {
    const from = waypoints[i]
    const to = waypoints[i + 1]

    let leg: { distance: number; duration: number; polyline: [number, number][]; instruction?: string }

    if (hasAmapWebKey()) {
      try {
        leg = await fetchDirectionLeg(transportMode, from, to)
      } catch {
        leg = mockLeg(from, to, transportMode)
      }
    } else {
      leg = mockLeg(from, to, transportMode)
    }

    segments.push({
      fromName: from.name,
      toName: to.name,
      fromLng: from.lng,
      fromLat: from.lat,
      toLng: to.lng,
      toLat: to.lat,
      distance: leg.distance,
      duration: leg.duration,
      transportMode,
      instruction: leg.instruction,
    })

    totalDistance += leg.distance
    totalDuration += leg.duration
    if (polyline.length) polyline.push(...leg.polyline.slice(1))
    else polyline.push(...leg.polyline)
  }

  const modeLabel = transportMode === 'driving' ? '驾车' : transportMode === 'bicycling' ? '骑行' : '步行'

  return {
    transportMode,
    waypoints,
    segments,
    totalDistance,
    totalDuration,
    polyline,
    summary: `北京市内 ${waypoints.length} 站 · ${modeLabel} · 已按优化顺序排列`,
    createdAt: Date.now(),
  }
}
