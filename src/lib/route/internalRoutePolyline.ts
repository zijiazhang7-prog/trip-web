import { fetchDirectionLegViaJsApi } from '../amap/jsDirection'
import { hasAmapJsKey, hasAmapWebKey } from '../amap/config'
import { fetchDirectionLeg, type LegResult } from '../amap/webService'
import { isBeijingAreaCoord } from '../geo/beijingCoord'
import type { MacroRoutePlan, RouteWaypoint, TransportMode } from '../../types/macroRoute'

type DirectionMode = 'driving' | 'walking' | 'bicycling'

function transportToDirectionMode(transportMode: TransportMode): DirectionMode {
  if (transportMode === 'bicycling') return 'bicycling'
  if (transportMode === 'driving') return 'driving'
  return 'walking'
}

async function fetchRoadDirectionLeg(
  mode: DirectionMode,
  from: { lng: number; lat: number },
  to: { lng: number; lat: number },
): Promise<LegResult> {
  if (hasAmapWebKey()) {
    try {
      const leg = await fetchDirectionLeg(mode, from, to)
      if (leg.polyline.length >= 2) return leg
    } catch {
      /* Web 服务失败时尝试 JS API */
    }
  }
  if (hasAmapJsKey()) {
    return fetchDirectionLegViaJsApi(mode, from, to)
  }
  throw new Error('未配置高德 Key，无法规划沿路路线')
}

export function filterBeijingPolyline(points: [number, number][]): [number, number][] {
  return points.filter(([lng, lat]) => isBeijingAreaCoord(lng, lat))
}

function isValidCoord(lng: number, lat: number): boolean {
  return Number.isFinite(lng) && Number.isFinite(lat) && !(lng === 0 && lat === 0)
}

function dedupeAdjacent(points: [number, number][]): [number, number][] {
  const out: [number, number][] = []
  for (const p of points) {
    const prev = out[out.length - 1]
    if (prev && Math.abs(prev[0] - p[0]) < 1e-6 && Math.abs(prev[1] - p[1]) < 1e-6) continue
    out.push(p)
  }
  return out
}

/** 用高德步行/骑行分段规划，把图论直线折线换成沿路路径 */
export async function enrichInternalRoutePolyline(
  plan: MacroRoutePlan,
  options?: { endpointsOnly?: boolean },
): Promise<MacroRoutePlan> {
  let anchors = plan.waypoints.filter((wp) => isValidCoord(wp.lng, wp.lat))
  if (options?.endpointsOnly && anchors.length >= 2) {
    anchors = [anchors[0], anchors[anchors.length - 1]]
  }
  if (anchors.length < 2) return plan

  const mode = transportToDirectionMode(plan.transportMode)
  const merged: [number, number][] = []

  for (let i = 0; i < anchors.length - 1; i++) {
    const from = anchors[i]
    const to = anchors[i + 1]
    try {
      const leg = await fetchRoadDirectionLeg(mode, { lng: from.lng, lat: from.lat }, { lng: to.lng, lat: to.lat })
      if (leg.polyline.length >= 2) {
        if (merged.length) merged.push(...leg.polyline.slice(1))
        else merged.push(...leg.polyline)
        continue
      }
    } catch {
      /* 单段失败则退回直线 */
    }
    if (!merged.length) merged.push([from.lng, from.lat])
    merged.push([to.lng, to.lat])
  }

  const polyline = dedupeAdjacent(merged)
  const beijingLine = filterBeijingPolyline(polyline)
  if (beijingLine.length < 2) return plan
  return { ...plan, polyline: beijingLine }
}

/** 景区内部：仅用起终点节点坐标请求高德沿路折线 */
export async function buildInternalWalkingPolyline(
  start: { lng: number; lat: number; nodeName?: string },
  end: { lng: number; lat: number; nodeName?: string },
  transportMode: MacroRoutePlan['transportMode'] = 'walking',
): Promise<[number, number][]> {
  if (!isBeijingAreaCoord(start.lng, start.lat) || !isBeijingAreaCoord(end.lng, end.lat)) {
    return []
  }
  const mode = transportToDirectionMode(transportMode)
  try {
    const leg = await fetchRoadDirectionLeg(mode, start, end)
    const line = filterBeijingPolyline(leg.polyline)
    if (line.length >= 2) return line
  } catch {
    /* 退回直线 */
  }
  return [
    [start.lng, start.lat],
    [end.lng, end.lat],
  ]
}

export function markerWaypointsForInternalPlan(
  waypoints: RouteWaypoint[],
  startId: number | null,
  endId: number | null,
  catalog: Map<number, { nodeName: string; lng: number; lat: number }>,
): RouteWaypoint[] {
  const markers: RouteWaypoint[] = []
  const push = (id: number | null) => {
    if (id == null) return
    const node = catalog.get(id)
    if (!node || markers.some((m) => m.id === id)) return
    markers.push({
      id,
      name: node.nodeName,
      lng: node.lng,
      lat: node.lat,
      destinationId: waypoints[0]?.destinationId,
    })
  }
  push(startId)
  push(endId)
  if (markers.length) return markers
  return waypoints.filter((wp) => wp.name !== '途经点' && !/^OSM/i.test(wp.name)).slice(0, 2)
}

export function markerWaypointsWithPolyline(
  waypoints: RouteWaypoint[],
  startId: number | null,
  endId: number | null,
  catalog: Map<number, { nodeName: string; lng: number; lat: number }>,
  polyline?: [number, number][],
): RouteWaypoint[] {
  let markers = markerWaypointsForInternalPlan(waypoints, startId, endId, catalog)
  if (!polyline || polyline.length < 2) return markers

  const startPt = polyline[0]
  const endPt = polyline[polyline.length - 1]
  markers = markers.map((m, i) => {
    if (isBeijingAreaCoord(m.lng, m.lat)) return m
    const pt = i === 0 ? startPt : endPt
    return { ...m, lng: pt[0], lat: pt[1] }
  })
  return markers
}
