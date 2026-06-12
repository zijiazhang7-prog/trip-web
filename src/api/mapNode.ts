import { httpRequest } from './http'
import { fetchDestinationPlaces, type PlaceVO } from './place'
import {
  BEIJING_DEFAULT_CENTER,
  isBeijingAreaCoord,
  isSchematicMapCoord,
  SCHEMATIC_MAP_CENTER,
  scatterAroundCenter,
} from '../lib/geo/beijingCoord'
import { resolveDestinationCoords } from '../lib/geo/resolveCoords'

export type MapNodeOption = {
  nodeId: number
  nodeName: string
  lng: number
  lat: number
  placeType?: string
  /** 展示用铺点坐标，规划路线前需地理编码 */
  needsGeocode?: boolean
}

function nodeCoords(
  lng: number | string | undefined,
  lat: number | string | undefined,
  index: number,
  total: number,
  coordSpace: 'scenic' | 'gps' = 'scenic',
): { lng: number; lat: number } {
  const lngN = typeof lng === 'string' ? Number(lng) : lng
  const latN = typeof lat === 'string' ? Number(lat) : lat
  if (Number.isFinite(lngN) && Number.isFinite(latN)) {
    const lngV = lngN as number
    const latV = latN as number
    if (isBeijingAreaCoord(lngV, latV)) return { lng: lngV, lat: latV }
    if (isSchematicMapCoord(lngV, latV)) return { lng: lngV, lat: latV }
  }
  const center = coordSpace === 'gps' ? BEIJING_DEFAULT_CENTER : SCHEMATIC_MAP_CENTER
  return scatterAroundCenter(center, index, Math.max(total, 1))
}

function placesToNodeOptions(places: PlaceVO[]): MapNodeOption[] {
  return places.map((p, i) => {
    const { lng, lat } = nodeCoords(p.lng, p.lat, i, places.length)
    return {
      nodeId: p.id,
      nodeName: p.name,
      lng,
      lat,
      placeType: p.placeType,
    }
  })
}

export type MapNodesResult = {
  nodes: MapNodeOption[]
  usedPlaceFallback: boolean
}

/** 优先尝试公开 map-nodes；否则用场所列表作为节点候选（nodeId 取 place.id）。 */
export async function fetchDestinationMapNodes(destinationId: number): Promise<MapNodesResult> {
  try {
    const raw = await httpRequest<MapNodeOption[] | { list?: MapNodeOption[] }>(
      `/api/v1/destinations/${destinationId}/map-nodes`,
      { method: 'GET' },
    )
    const list = Array.isArray(raw) ? raw : (raw.list ?? [])
    if (list.length) {
      return {
        usedPlaceFallback: false,
        nodes: list.map((n, i) => {
          const { lng, lat } = nodeCoords(n.lng, n.lat, i, list.length)
          return {
            nodeId: n.nodeId ?? (n as { id?: number }).id ?? i + 1,
            nodeName: n.nodeName ?? `节点 ${i + 1}`,
            lng,
            lat,
            placeType: n.placeType,
          }
        }),
      }
    }
  } catch {
    /* 远端可能尚未提供 map-nodes，走场所回退 */
  }
  const places = await fetchDestinationPlaces(destinationId)
  return { nodes: placesToNodeOptions(places), usedPlaceFallback: true }
}

/** 快速把示意坐标铺到景区中心附近，供地图即时展示（不阻塞网络） */
export function quickDisplayGpsCoords(
  nodes: MapNodeOption[],
  centerHint: { lng: number; lat: number } = BEIJING_DEFAULT_CENTER,
): MapNodeOption[] {
  return nodes.map((n, i) => {
    if (isBeijingAreaCoord(n.lng, n.lat) && !n.needsGeocode) {
      return { ...n, needsGeocode: false }
    }
    const fb = scatterAroundCenter(centerHint, i, nodes.length)
    return { ...n, lng: fb.lng, lat: fb.lat, needsGeocode: true }
  })
}

/** 单节点：非 GPS 时尝试地理编码，失败则落在景区中心附近 */
export async function ensureNodeGpsCoords(
  node: MapNodeOption,
  destinationHint?: string,
  centerHint?: { lng: number; lat: number },
  index = 0,
  total = 1,
): Promise<MapNodeOption> {
  if (isBeijingAreaCoord(node.lng, node.lat) && !node.needsGeocode) {
    return { ...node, needsGeocode: false }
  }

  const hint = destinationHint?.trim() || '北京'
  const scatterCenter = centerHint ?? BEIJING_DEFAULT_CENTER
  const queries = [
    `${hint} ${node.nodeName}`,
    `北京环球度假区 ${node.nodeName}`,
    `北京${hint}${node.nodeName}`,
    `北京${node.nodeName}`,
    node.nodeName,
  ]
  for (const q of queries) {
    try {
      const resolved = await resolveDestinationCoords(q, '北京')
      if (resolved && isBeijingAreaCoord(resolved.lng, resolved.lat)) {
        return { ...node, lng: resolved.lng, lat: resolved.lat, needsGeocode: false }
      }
    } catch {
      /* 尝试下一个查询词 */
    }
  }
  const fb = scatterAroundCenter(scatterCenter, index, Math.max(total, 1))
  return { ...node, lng: fb.lng, lat: fb.lat, needsGeocode: true }
}
