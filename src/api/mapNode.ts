import { httpRequest } from './http'
import { fetchDestinationPlaces, type PlaceVO } from './place'

export type MapNodeOption = {
  nodeId: number
  nodeName: string
  lng: number
  lat: number
  placeType?: string
}

function toCoord(v: number | string | undefined, fallback: number): number {
  const n = typeof v === 'string' ? Number(v) : v
  return Number.isFinite(n) ? (n as number) : fallback
}

function placesToNodeOptions(places: PlaceVO[]): MapNodeOption[] {
  return places.map((p, i) => ({
    nodeId: p.id,
    nodeName: p.name,
    lng: toCoord(p.lng, 40 + i * 35),
    lat: toCoord(p.lat, 30 + i * 28),
    placeType: p.placeType,
  }))
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
        nodes: list.map((n, i) => ({
          nodeId: n.nodeId ?? (n as { id?: number }).id ?? i + 1,
          nodeName: n.nodeName ?? `节点 ${i + 1}`,
          lng: toCoord(n.lng, 50 + i * 30),
          lat: toCoord(n.lat, 40 + i * 25),
          placeType: n.placeType,
        })),
      }
    }
  } catch {
    /* 远端可能尚未提供 map-nodes，走场所回退 */
  }
  const places = await fetchDestinationPlaces(destinationId)
  return { nodes: placesToNodeOptions(places), usedPlaceFallback: true }
}
