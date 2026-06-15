import type { PathNodeResult } from '../../api/route'
import type { MapNodeOption } from '../../api/mapNode'

export type ScenicMapPoint = { x: number; y: number; label: string; nodeId: number }

export function layoutScenicPoints(
  pathNodes: PathNodeResult[],
  catalog: MapNodeOption[],
): ScenicMapPoint[] {
  const byId = new Map(catalog.map((n) => [n.nodeId, n]))
  const coords = pathNodes.map((p, i) => {
    const hit = byId.get(p.nodeId)
    if (hit) {
      return { x: hit.lng, y: hit.lat, label: p.nodeName || hit.nodeName, nodeId: p.nodeId }
    }
    const angle = (i / Math.max(pathNodes.length, 1)) * Math.PI * 2
    return {
      x: 500 + Math.cos(angle) * 180,
      y: 400 + Math.sin(angle) * 140,
      label: p.nodeName,
      nodeId: p.nodeId,
    }
  })
  if (coords.length <= 1) return normalizeToViewBox(coords)

  return normalizeToViewBox(coords)
}

export function layoutAllCatalogPoints(catalog: MapNodeOption[]): ScenicMapPoint[] {
  const coords = catalog.map((n, i) => {
    const angle = (i / Math.max(catalog.length, 1)) * Math.PI * 2
    const hasCoord = Number.isFinite(n.lng) && Number.isFinite(n.lat) && !(n.lng === 0 && n.lat === 0)
    return {
      x: hasCoord ? n.lng : 500 + Math.cos(angle) * 180,
      y: hasCoord ? n.lat : 400 + Math.sin(angle) * 140,
      label: n.nodeName,
      nodeId: n.nodeId,
    }
  })
  return normalizeToViewBox(coords)
}

function normalizeToViewBox(
  coords: { x: number; y: number; label: string; nodeId: number }[],
): ScenicMapPoint[] {
  if (!coords.length) return []
  if (coords.length === 1) {
    return [{ x: 50, y: 50, label: coords[0].label, nodeId: coords[0].nodeId }]
  }

  const xs = coords.map((c) => c.x)
  const ys = coords.map((c) => c.y)
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  const pad = 10
  const w = maxX - minX || 1
  const h = maxY - minY || 1

  return coords.map((c) => ({
    x: pad + ((c.x - minX) / w) * (100 - pad * 2),
    y: pad + ((c.y - minY) / h) * (100 - pad * 2),
    label: c.label,
    nodeId: c.nodeId,
  }))
}

export function arrowPointsAlongPath(points: ScenicMapPoint[]): string[] {
  if (points.length < 2) return []
  const arrows: string[] = []
  for (let i = 0; i < points.length - 1; i++) {
    const a = points[i]
    const b = points[i + 1]
    const mx = (a.x + b.x) / 2
    const my = (a.y + b.y) / 2
    const angle = (Math.atan2(b.y - a.y, b.x - a.x) * 180) / Math.PI
    arrows.push(`${mx},${my},${angle}`)
  }
  return arrows
}
