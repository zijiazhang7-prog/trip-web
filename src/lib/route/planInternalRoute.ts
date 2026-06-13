import type { MapNodeOption } from '../../api/mapNode'
import { planMultiRoute, planSingleRoute, type RoutePlanVO, type RouteStrategyType, type RouteTransportType } from '../../api/route'
import type { MacroRoutePlan } from '../../types/macroRoute'
import { macroPlanFromRoutePlanVO } from './backendRoutePlan'
import { buildInternalWalkingPolyline, filterBeijingPolyline } from './internalRoutePolyline'
import { ensureNodeGpsCoords } from '../../api/mapNode'
import { toBackendTransport } from '../../api/route'
import type { RouteWaypoint } from '../../types/macroRoute'
import {
  isBackendPlanErrorRetryable,
  planInternalRouteLocally,
  shouldUseLocalInternalPlan,
} from './localInternalPlan'

export async function executeInternalRoutePlan(input: {
  destinationId: number
  startNodeId: number
  targetNodeIds: number[]
  returnToStart: boolean
  strategy: RouteStrategyType
  transport: RouteTransportType
  nodeById: Map<number, MapNodeOption>
  scenicCenter: [number, number]
  destinationName?: string
  usedPlaceFallback: boolean
  buildVisitSequence: (
    vo: RoutePlanVO | null,
    startNodeId: number | null,
    targetNodeIds: number[],
    returnToStart: boolean,
  ) => number[]
}): Promise<{ vo: RoutePlanVO; macro: MacroRoutePlan }> {
  if (shouldUseLocalInternalPlan(input.usedPlaceFallback)) {
    return planInternalRouteLocally(input)
  }

  try {
    let vo: RoutePlanVO
    if (input.targetNodeIds.length <= 1) {
      const target = input.targetNodeIds[0] ?? input.startNodeId
      vo = await planSingleRoute({
        destinationId: input.destinationId,
        startNodeId: input.startNodeId,
        targetNodeId: target,
        strategyType: input.strategy,
        transportType: input.transport,
      })
    } else {
      vo = await planMultiRoute({
        destinationId: input.destinationId,
        startNodeId: input.startNodeId,
        targetNodeIds: input.targetNodeIds,
        strategyType: input.strategy,
        transportType: input.transport,
        returnToStart: input.returnToStart,
      })
    }
    return buildMacroFromVo(vo, input)
  } catch (err) {
    const msg = err instanceof Error ? err.message : ''
    if (!isBackendPlanErrorRetryable(msg)) throw err
    return planInternalRouteLocally(input)
  }
}

async function buildMacroFromVo(
  vo: RoutePlanVO,
  input: {
    destinationId: number
    startNodeId: number
    targetNodeIds: number[]
    returnToStart: boolean
    transport: RouteTransportType
    nodeById: Map<number, MapNodeOption>
    scenicCenter: [number, number]
    destinationName?: string
    buildVisitSequence: (
      vo: RoutePlanVO | null,
      startNodeId: number | null,
      targetNodeIds: number[],
      returnToStart: boolean,
    ) => number[]
  },
): Promise<{ vo: RoutePlanVO; macro: MacroRoutePlan }> {
  const backendTransport = toBackendTransport(input.transport)
  const visitIds = input.buildVisitSequence(
    vo,
    input.startNodeId,
    input.targetNodeIds,
    input.returnToStart,
  )
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

  const macroBase = macroPlanFromRoutePlanVO({ ...vo, transportType: backendTransport }, input.nodeById)
  const mergedPolyline: [number, number][] = []
  for (let i = 0; i < routeWaypoints.length - 1; i++) {
    const seg = await buildInternalWalkingPolyline(
      routeWaypoints[i],
      routeWaypoints[i + 1],
      macroBase.transportMode,
    )
    if (seg.length >= 2) {
      if (mergedPolyline.length) mergedPolyline.push(...seg.slice(1))
      else mergedPolyline.push(...seg)
    }
  }

  const macro: MacroRoutePlan = {
    ...macroBase,
    waypoints: routeWaypoints,
    polyline: filterBeijingPolyline(mergedPolyline),
    summary: `景区内部 · ${visitIds.length} 站 · 约 ${vo.estimatedTime} 分钟`,
  }

  return { vo, macro }
}
