import { ensureNodeGpsCoords, type MapNodeOption } from '../../api/mapNode'
import type { RoutePathEdgeVO, RoutePlanVO, RouteStrategyType, RouteTransportType } from '../../api/route'
import { toBackendTransport } from '../../api/route'
import type { MacroRoutePlan, RouteWaypoint } from '../../types/macroRoute'
import { macroPlanFromRoutePlanVO } from './backendRoutePlan'
import { buildInternalWalkingPolyline, filterBeijingPolyline } from './internalRoutePolyline'

function buildVisitIds(
  startNodeId: number,
  targetNodeIds: number[],
  returnToStart: boolean,
): number[] {
  const ordered: number[] = [startNodeId]
  for (const id of targetNodeIds) {
    if (!ordered.includes(id)) ordered.push(id)
  }
  if (returnToStart && targetNodeIds.length > 1 && !ordered.includes(startNodeId)) {
    ordered.push(startNodeId)
  }
  return ordered
}

/** 场所回退或后端图规划失败时：按选中节点顺序走本地折线规划 */
export async function planInternalRouteLocally(input: {
  destinationId: number
  startNodeId: number
  targetNodeIds: number[]
  returnToStart: boolean
  strategy: RouteStrategyType
  transport: RouteTransportType
  nodeById: Map<number, MapNodeOption>
  scenicCenter: [number, number]
  destinationName?: string
}): Promise<{ vo: RoutePlanVO; macro: MacroRoutePlan }> {
  const visitIds = buildVisitIds(input.startNodeId, input.targetNodeIds, input.returnToStart)
  const center = { lng: input.scenicCenter[0], lat: input.scenicCenter[1] }
  const routeWaypoints: RouteWaypoint[] = []

  for (let i = 0; i < visitIds.length; i++) {
    const raw = input.nodeById.get(visitIds[i])
    if (!raw) continue
    const resolved = await ensureNodeGpsCoords(
      raw,
      input.destinationName,
      center,
      i,
      visitIds.length,
    )
    routeWaypoints.push({
      id: resolved.nodeId,
      name: resolved.nodeName,
      lng: resolved.lng,
      lat: resolved.lat,
      destinationId: input.destinationId,
    })
  }

  const backendTransport = toBackendTransport(input.transport)
  const pathEdges: RoutePathEdgeVO[] = []
  const mergedPolyline: [number, number][] = []
  let totalDistance = 0

  for (let i = 0; i < routeWaypoints.length - 1; i++) {
    const seg = await buildInternalWalkingPolyline(
      routeWaypoints[i],
      routeWaypoints[i + 1],
      backendTransport === 'bike'
        ? 'bicycling'
        : backendTransport === 'cart'
          ? 'driving'
          : 'walking',
    )
    const dist = seg.length >= 2 ? haversineM(seg) : 0
    totalDistance += dist
    pathEdges.push({
      fromNodeId: Number(routeWaypoints[i].id),
      toNodeId: Number(routeWaypoints[i + 1].id),
      distance: Math.round(dist),
    })
    if (seg.length >= 2) {
      if (mergedPolyline.length) mergedPolyline.push(...seg.slice(1))
      else mergedPolyline.push(...seg)
    }
  }

  const estimatedTime = Math.max(5, Math.round(totalDistance / 80))
  const vo: RoutePlanVO = {
    destinationId: input.destinationId,
    strategyType: input.strategy,
    transportType: backendTransport,
    totalDistance,
    estimatedTime,
    pathNodes: routeWaypoints.map((w) => ({
      nodeId: Number(w.id),
      nodeName: w.name,
    })),
    pathEdges,
    orderedTargetNodeIds: input.targetNodeIds,
    routeSummary: `本地道路图 · ${visitIds.length} 站 · 约 ${estimatedTime} 分钟`,
  }

  const macroBase = macroPlanFromRoutePlanVO(vo, input.nodeById)
  const macro: MacroRoutePlan = {
    ...macroBase,
    waypoints: routeWaypoints,
    polyline: filterBeijingPolyline(mergedPolyline),
    summary: vo.routeSummary,
  }

  return { vo, macro }
}

function haversineM(points: [number, number][]): number {
  let sum = 0
  for (let i = 1; i < points.length; i++) {
    const [lng1, lat1] = points[i - 1]
    const [lng2, lat2] = points[i]
    const r = 6371000
    const dLat = ((lat2 - lat1) * Math.PI) / 180
    const dLng = ((lng2 - lng1) * Math.PI) / 180
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
    sum += 2 * r * Math.asin(Math.sqrt(a))
  }
  return sum
}

export function shouldUseLocalInternalPlan(usedPlaceFallback: boolean): boolean {
  return usedPlaceFallback
}

export function isBackendPlanErrorRetryable(message: string): boolean {
  return (
    message.includes('系统内部错误') ||
    message.includes('起点节点不存在') ||
    message.includes('终点节点不存在') ||
    message.includes('ROUTE_001') ||
    message.includes('ROUTE_002') ||
    message.includes('节点') ||
    message.includes('不可达') ||
    message.includes('HTTP 5')
  )
}
