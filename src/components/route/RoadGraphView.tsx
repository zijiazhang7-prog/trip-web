import type { RoutePathEdgeVO } from '../../api/route'
import type { MapCatalogEdge, MapNodeOption } from '../../api/mapNode'
import type { PathNodeResult } from '../../api/route'
import {
  layoutAllCatalogPoints,
  layoutScenicPoints,
  type ScenicMapPoint,
} from '../../lib/route/scenicMapLayout'

type RoadGraphViewProps = {
  nodeCatalog: MapNodeOption[]
  /** 景点内全部道路边（灰色底图） */
  catalogEdges?: MapCatalogEdge[]
  /** 规划路线经过的节点（高亮连线） */
  routePathNodes?: PathNodeResult[]
  /** 规划路线实际路径边（优先于节点折线） */
  routePathEdges?: RoutePathEdgeVO[]
  startNodeId?: number | null
  endNodeId?: number | null
  className?: string
}

function pointById(points: ScenicMapPoint[], id: number | null | undefined): ScenicMapPoint | null {
  if (id == null) return null
  return points.find((p) => p.nodeId === id) ?? null
}

function edgeKey(from: number, to: number): string {
  return `${from}->${to}`
}

export function RoadGraphView({
  nodeCatalog,
  catalogEdges = [],
  routePathNodes = [],
  routePathEdges = [],
  startNodeId,
  endNodeId,
  className = '',
}: RoadGraphViewProps) {
  const allPoints = layoutAllCatalogPoints(nodeCatalog)
  const pointMap = new Map(allPoints.map((p) => [p.nodeId, p]))
  const routePoints = routePathNodes.length ? layoutScenicPoints(routePathNodes, nodeCatalog) : []
  const routeIds = new Set(routePathNodes.map((n) => n.nodeId))
  const highlightedEdgeKeys = new Set<string>()

  const start = pointById(allPoints, startNodeId)
  const end =
    pointById(allPoints, endNodeId) ??
    (routePathNodes.length ? pointById(allPoints, routePathNodes[routePathNodes.length - 1]?.nodeId) : null)

  const catalogLines: Array<{ from: ScenicMapPoint; to: ScenicMapPoint; key: string }> = []
  const seenCatalog = new Set<string>()
  for (const edge of catalogEdges) {
    const from = pointMap.get(edge.fromNodeId)
    const to = pointMap.get(edge.toNodeId)
    if (!from || !to) continue
    const fwd = edgeKey(edge.fromNodeId, edge.toNodeId)
    if (!seenCatalog.has(fwd)) {
      seenCatalog.add(fwd)
      catalogLines.push({ from, to, key: fwd })
    }
    if (edge.bidirectional) {
      const rev = edgeKey(edge.toNodeId, edge.fromNodeId)
      if (!seenCatalog.has(rev)) {
        seenCatalog.add(rev)
        catalogLines.push({ from: to, to: from, key: rev })
      }
    }
  }

  const routeLines: Array<{ from: ScenicMapPoint; to: ScenicMapPoint; key: string }> = []
  if (routePathEdges.length) {
    for (const edge of routePathEdges) {
      const from = pointMap.get(edge.fromNodeId)
      const to = pointMap.get(edge.toNodeId)
      if (!from || !to) continue
      const key = edgeKey(edge.fromNodeId, edge.toNodeId)
      highlightedEdgeKeys.add(key)
      routeLines.push({ from, to, key })
    }
  } else if (routePoints.length > 1) {
    for (let i = 0; i < routePoints.length - 1; i++) {
      const key = edgeKey(routePoints[i].nodeId, routePoints[i + 1].nodeId)
      highlightedEdgeKeys.add(key)
      routeLines.push({ from: routePoints[i], to: routePoints[i + 1], key })
    }
  }

  if (!nodeCatalog.length) {
    return (
      <div className={`flex h-full items-center justify-center font-body text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        暂无道路图节点
      </div>
    )
  }

  return (
    <div className={`relative h-full w-full overflow-hidden rounded-xl bg-[#e8f0e6] ${className}`}>
      <svg viewBox="0 0 100 100" className="h-full w-full" aria-label="景区道路图">
        <defs>
          <linearGradient id="roadGraphRoute" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#2f7fd4" />
            <stop offset="100%" stopColor="#1a5fb4" />
          </linearGradient>
          <marker id="roadGraphArrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="2" markerHeight="2" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#1a5fb4" />
          </marker>
        </defs>

        <rect x="0" y="0" width="100" height="100" fill="#f2f6ef" />
        <ellipse cx="42" cy="58" rx="22" ry="16" fill="#b8d4e8" opacity="0.55" />
        <rect x="8" y="12" width="28" height="22" rx="3" fill="#e8d4f0" opacity="0.45" />
        <rect x="62" y="18" width="30" height="26" rx="3" fill="#dce8c8" opacity="0.5" />

        {catalogLines
          .filter((line) => !highlightedEdgeKeys.has(line.key))
          .map((line) => (
            <line
              key={`catalog-${line.key}`}
              x1={line.from.x}
              y1={line.from.y}
              x2={line.to.x}
              y2={line.to.y}
              stroke="#9ab5a0"
              strokeWidth="0.9"
              strokeDasharray="2 1.5"
              opacity="0.85"
            />
          ))}

        {routeLines.map((line) => (
          <line
            key={`route-${line.key}`}
            x1={line.from.x}
            y1={line.from.y}
            x2={line.to.x}
            y2={line.to.y}
            stroke="url(#roadGraphRoute)"
            strokeWidth="1.15"
            strokeLinecap="round"
            markerEnd="url(#roadGraphArrow)"
          />
        ))}

        {allPoints
          .filter((p) => !routeIds.has(p.nodeId) && p.nodeId !== start?.nodeId && p.nodeId !== end?.nodeId)
          .map((p) => (
            <g key={`poi-${p.nodeId}`} opacity="0.8">
              <circle cx={p.x} cy={p.y} r="1.6" fill="#6b8f71" stroke="#fff" strokeWidth="0.3" />
              <text x={p.x} y={p.y - 2.6} textAnchor="middle" fontSize="2" fill="#3d5a42">
                {p.label.length > 5 ? `${p.label.slice(0, 5)}…` : p.label}
              </text>
            </g>
          ))}

        {routePoints.map((p) => {
          const isStart = start && p.nodeId === start.nodeId
          const isEnd = end && p.nodeId === end.nodeId
          if (isStart || isEnd) return null
          return (
            <g key={`route-node-${p.nodeId}`}>
              <circle cx={p.x} cy={p.y} r="2.2" fill="#4a90d9" stroke="#fff" strokeWidth="0.45" />
              <text x={p.x} y={p.y - 3.2} textAnchor="middle" fontSize="2" fill="#1a5fb4" fontWeight="bold">
                {p.label.length > 5 ? `${p.label.slice(0, 5)}…` : p.label}
              </text>
            </g>
          )
        })}

        {start ? (
          <g>
            <circle cx={start.x} cy={start.y} r="3.4" fill="#3d9a5a" stroke="#fff" strokeWidth="0.55" />
            <text x={start.x} y={start.y + 0.7} textAnchor="middle" fontSize="3" fill="#fff" fontWeight="bold">
              起
            </text>
          </g>
        ) : null}

        {end && end.nodeId !== start?.nodeId ? (
          <g>
            <circle cx={end.x} cy={end.y} r="3.4" fill="#d94a4a" stroke="#fff" strokeWidth="0.55" />
            <text x={end.x} y={end.y + 0.7} textAnchor="middle" fontSize="3" fill="#fff" fontWeight="bold">
              终
            </text>
          </g>
        ) : null}
      </svg>

      <p className="pointer-events-none absolute bottom-1 left-2 font-body text-[9px] text-[#6b8076]/85">
        道路图 · 全部节点与道路
      </p>
    </div>
  )
}
