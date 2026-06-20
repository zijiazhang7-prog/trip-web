import type { RouteWaypoint } from '../../types/macroRoute'

function haversineMeters(a: RouteWaypoint, b: RouteWaypoint): number {
  const R = 6371000
  const toRad = (d: number) => (d * Math.PI) / 180
  const dLat = toRad(b.lat - a.lat)
  const dLng = toRad(b.lng - a.lng)
  const x =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(x))
}

/** 最近邻 TSP 近似，得到优化后的游览顺序（非勾选顺序） */
export function optimizeWaypointOrder(points: RouteWaypoint[]): RouteWaypoint[] {
  if (points.length <= 2) return [...points]

  const remaining = [...points]
  const ordered: RouteWaypoint[] = []

  let current = remaining.reduce((best, p) => (p.lat > best.lat ? p : best), remaining[0])
  ordered.push(current)
  remaining.splice(remaining.indexOf(current), 1)

  while (remaining.length > 0) {
    let nearestIdx = 0
    let nearest = Infinity
    for (let i = 0; i < remaining.length; i++) {
      const d = haversineMeters(current, remaining[i])
      if (d < nearest) {
        nearest = d
        nearestIdx = i
      }
    }
    current = remaining.splice(nearestIdx, 1)[0]
    ordered.push(current)
  }

  return ordered
}
