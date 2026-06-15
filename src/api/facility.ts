import { coerceSpringPage } from './coercePage'
import { httpRequest } from './http'
import type { PageResult } from './destination'

export type NearbyFacilityVO = {
  id: number
  destinationId: number
  placeId?: number
  name: string
  facilityType: string
  description?: string
  lng?: number | string
  lat?: number | string
  reachableDistance?: number | string
  sourceNodeId?: number
  targetNodeId?: number
}

export type NearbyFacilityQuery = {
  destinationId: number
  sourceNodeId: number
  facilityType?: string
  radius?: number
  sortBy?: 'distance' | 'time'
  pageNum?: number
  pageSize?: number
}

export type FacilitySearchQuery = {
  destinationId: number
  sourceNodeId?: number
  keyword?: string
  facilityType?: string
  sortBy?: 'distance' | 'time'
  pageNum?: number
  pageSize?: number
}

export async function searchFacilities(
  params: FacilitySearchQuery,
): Promise<PageResult<NearbyFacilityVO>> {
  const query = new URLSearchParams({
    destinationId: String(params.destinationId),
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 20),
  })
  if (params.sourceNodeId != null) query.set('sourceNodeId', String(params.sourceNodeId))
  if (params.keyword) query.set('keyword', params.keyword)
  if (params.facilityType) query.set('facilityType', params.facilityType)
  if (params.sortBy) query.set('sortBy', params.sortBy)
  const raw = await httpRequest<unknown>(`/api/v1/facilities/search?${query}`, { method: 'GET' })
  const n = coerceSpringPage<NearbyFacilityVO>(raw)
  return {
    list: n.list,
    pageNum: n.pageNum,
    pageSize: n.pageSize,
    total: n.total,
    pages: n.pages,
  }
}

export async function fetchNearbyFacilities(
  params: NearbyFacilityQuery,
): Promise<PageResult<NearbyFacilityVO>> {
  const query = new URLSearchParams({
    destinationId: String(params.destinationId),
    sourceNodeId: String(params.sourceNodeId),
    pageNum: String(params.pageNum ?? 1),
    pageSize: String(params.pageSize ?? 20),
  })
  if (params.facilityType) query.set('facilityType', params.facilityType)
  if (params.radius != null) query.set('radius', String(params.radius))
  if (params.sortBy) query.set('sortBy', params.sortBy)
  const raw = await httpRequest<unknown>(`/api/v1/facilities/nearby?${query}`, { method: 'GET' })
  const n = coerceSpringPage<NearbyFacilityVO>(raw)
  return {
    list: n.list,
    pageNum: n.pageNum,
    pageSize: n.pageSize,
    total: n.total,
    pages: n.pages,
  }
}
