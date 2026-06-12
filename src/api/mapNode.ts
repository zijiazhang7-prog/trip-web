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

/** 景区内部导航：将示意坐标转为高德可用的 GPS（按节点名地理编码） */
export async function enrichMapNodesGpsCoords(
  nodes: MapNodeOption[],
  destinationHint?: string,
  centerHint?: { lng: number; lat: number },
): Promise<MapNodeOption[]> {
  const hint = destinationHint?.trim() || '北京'
  const scatterCenter = centerHint ?? BEIJING_DEFAULT_CENTER
  const out: MapNodeOption[] = []
  for (let i = 0; i < nodes.length; i++) {
    const n = nodes[i]
    if (isBeijingAreaCoord(n.lng, n.lat)) {
      out.push(n)
      continue
    }
    const queries = [
      `${hint} ${n.nodeName}`,
      `北京环球度假区 ${n.nodeName}`,
      `北京${hint}${n.nodeName}`,
      `北京${n.nodeName}`,
      n.nodeName,
    ]
    let resolved: { lng: number; lat: number } | null = null
    for (const q of queries) {
      try {
        resolved = await resolveDestinationCoords(q, '北京')
        if (resolved && isBeijingAreaCoord(resolved.lng, resolved.lat)) break
        resolved = null
      } catch {
        /* 尝试下一个查询词 */
      }
    }
    if (resolved) {
      out.push({ ...n, lng: resolved.lng, lat: resolved.lat })
    } else {
      const fb = scatterAroundCenter(scatterCenter, i, nodes.length)
      out.push({ ...n, lng: fb.lng, lat: fb.lat })
    }
    await new Promise((r) => window.setTimeout(r, 80))
  }
  return out
}
