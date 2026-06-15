import type { MapNodeOption } from '../../api/mapNode'
import type { RoutePlanVO } from '../../api/route'
import type { MacroRoutePlan, RouteWaypoint, TransportMode } from '../../types/macroRoute'

function isTechnicalNodeName(name: string | undefined | null): boolean {
  if (!name?.trim()) return true
  return /^OSM/i.test(name.trim())
}

function friendlyNodeName(rawName: string | undefined | null, catalog?: MapNodeOption): string {
  const catalogName = catalog?.nodeName?.trim()
  if (catalogName && !isTechnicalNodeName(catalogName)) return catalogName
  const name = rawName?.trim()
  if (name && !isTechnicalNodeName(name)) return name
  return `途经点`
}

function uiTransportToMacro(t: string): TransportMode {
  if (t === 'bike') return 'bicycling'
  if (t === 'cart' || t === 'drive') return 'driving'
  if (t === 'transit') return 'transit'
  return 'walking'
}

/** 将后端图路线结果转为地图可展示的 MacroRoutePlan */
export function macroPlanFromRoutePlanVO(
  vo: RoutePlanVO,
  nodeById: Map<number, MapNodeOption>,
): MacroRoutePlan {
  const waypoints: RouteWaypoint[] = (vo.pathNodes ?? []).map((n) => {
    const opt = nodeById.get(n.nodeId)
    return {
      id: n.nodeId,
      name: friendlyNodeName(n.nodeName, opt),
      lng: opt?.lng ?? 0,
      lat: opt?.lat ?? 0,
      destinationId: vo.destinationId,
    }
  })

  const polyline: [number, number][] = []
  for (const wp of waypoints) {
    if (wp.lng !== 0 || wp.lat !== 0) polyline.push([wp.lng, wp.lat])
  }

  const totalDistance = Number(vo.totalDistance) || 0
  const estimatedMinutes = Number(vo.estimatedTime) || 0

  return {
    transportMode: uiTransportToMacro(vo.transportType),
    waypoints,
    segments: [],
    totalDistance,
    totalDuration: estimatedMinutes * 60,
    polyline,
    summary:
      vo.routeSummary ??
      `景区内部 · ${vo.strategyType === 'shortest_time' ? '最短时间' : '最短距离'} · ${waypoints.length} 站`,
    createdAt: Date.now(),
  }
}
