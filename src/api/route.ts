import { httpRequest } from './http'

export type PathNodeResult = {
  nodeId: number
  nodeName: string
}

export type RoutePathEdgeVO = {
  fromNodeId: number
  toNodeId: number
  distance: number | string
}

export type RoutePlanVO = {
  destinationId: number
  strategyType: string
  transportType: string
  totalDistance: number | string
  estimatedTime: number
  pathNodes: PathNodeResult[]
  pathEdges?: RoutePathEdgeVO[]
  routeSummary?: string
  historyId?: number
}

export type SingleRoutePlanRequest = {
  destinationId: number
  startNodeId: number
  targetNodeId: number
  strategyType: 'shortest_distance' | 'shortest_time'
  transportType?: 'walk' | 'bike' | 'cart'
}

export type MultiRoutePlanRequest = {
  destinationId: number
  startNodeId: number
  targetNodeIds: number[]
  strategyType: 'shortest_distance' | 'shortest_time'
  transportType?: 'walk' | 'bike' | 'cart'
  returnToStart?: boolean
}

export async function planSingleRoute(body: SingleRoutePlanRequest): Promise<RoutePlanVO> {
  return httpRequest<RoutePlanVO>('/api/v1/routes/plan/single', {
    method: 'POST',
    body: JSON.stringify({
      transportType: 'walk',
      ...body,
    }),
  })
}

export async function planMultiRoute(body: MultiRoutePlanRequest): Promise<RoutePlanVO> {
  return httpRequest<RoutePlanVO>('/api/v1/routes/plan/multi', {
    method: 'POST',
    body: JSON.stringify({
      transportType: 'walk',
      returnToStart: true,
      ...body,
    }),
  })
}
