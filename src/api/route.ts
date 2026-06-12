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
  orderedTargetNodeIds?: number[]
}

export type RouteStrategyType = 'shortest_distance' | 'shortest_time'

/** UI 交通选项：步行、骑行、汽车、公共交通 */
export type RouteTransportType = 'walk' | 'bike' | 'drive' | 'transit'

type BackendTransportType = 'walk' | 'bike' | 'cart'

export function toBackendTransport(t: RouteTransportType): BackendTransportType {
  if (t === 'bike') return 'bike'
  if (t === 'drive') return 'cart'
  if (t === 'transit') return 'walk'
  return 'walk'
}

export type SingleRoutePlanRequest = {
  destinationId: number
  startNodeId: number
  targetNodeId: number
  strategyType: RouteStrategyType
  transportType?: RouteTransportType
}

export type MultiRoutePlanRequest = {
  destinationId: number
  startNodeId: number
  targetNodeIds: number[]
  strategyType: RouteStrategyType
  transportType?: RouteTransportType
  returnToStart?: boolean
}

export async function planSingleRoute(body: SingleRoutePlanRequest): Promise<RoutePlanVO> {
  const transportType = toBackendTransport(body.transportType ?? 'walk')
  return httpRequest<RoutePlanVO>('/api/v1/routes/plan/single', {
    method: 'POST',
    body: JSON.stringify({
      ...body,
      transportType,
    }),
  })
}

export async function planMultiRoute(body: MultiRoutePlanRequest): Promise<RoutePlanVO> {
  const transportType = toBackendTransport(body.transportType ?? 'walk')
  return httpRequest<RoutePlanVO>('/api/v1/routes/plan/multi', {
    method: 'POST',
    body: JSON.stringify({
      returnToStart: true,
      ...body,
      transportType,
    }),
  })
}

export type RouteHistoryVO = {
  id: number
  destinationId: number
  destinationName?: string
  startNodeId?: number
  startNodeName?: string
  endNodeId?: number
  endNodeName?: string
  strategyType?: string
  transportType?: string
  totalDistance?: number | string
  estimatedTime?: number
  createdAt?: string
  pathNodes?: PathNodeResult[]
  pathEdges?: RoutePathEdgeVO[]
  orderedTargetNodeIds?: number[]
}

type PageResult<T> = {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
  pages: number
}

export async function fetchRouteHistories(params?: {
  pageNum?: number
  pageSize?: number
}): Promise<PageResult<RouteHistoryVO>> {
  const query = new URLSearchParams({
    pageNum: String(params?.pageNum ?? 1),
    pageSize: String(params?.pageSize ?? 10),
  })
  return httpRequest<PageResult<RouteHistoryVO>>(`/api/v1/routes/history?${query}`, { method: 'GET' })
}

export async function fetchRouteHistoryDetail(id: number): Promise<RouteHistoryVO> {
  return httpRequest<RouteHistoryVO>(`/api/v1/routes/history/${id}`, { method: 'GET' })
}
