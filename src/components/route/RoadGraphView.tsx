import { useId } from 'react'
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
  catalogEdges?: MapCatalogEdge[]
  routePathNodes?: PathNodeResult[]
  routePathEdges?: RoutePathEdgeVO[]
  startNodeId?: number | null
  endNodeId?: number | null
  className?: string
}

const GRAPH_GREEN = '#3d6b52'
const GRAPH_GREEN_LIGHT = '#6b9580'
const GRAPH_ROUTE = '#2d5844'

function edgeKey(from: number, to: number): string {
  return `${from}->${to}`
}

function truncateLabel(label: string, max = 14): string {
  const t = label.trim()
  if (t.length <= max) return t
  return `${t.slice(0, max)}…`
}

export function RoadGraphView({
  nodeCatalog,
  catalogEdges = [],
  routePathNodes = [],
  routePathEdges = [],
  startNodeId: _startNodeId,
  endNodeId: _endNodeId,
  className = '',
}: RoadGraphViewProps) {
  const arrowMarkerId = useId().replace(/:/g, '')
  const allPoints = layoutAllCatalogPoints(nodeCatalog)
  const routePoints = routePathNodes.length > 0 ? layoutScenicPoints(routePathNodes, nodeCatalog) : []
  const pointMap = new Map(allPoints.map((p) => [p.nodeId, p]))
  const routeIds = new Set(routePathNodes.map((n) => n.nodeId))
  const highlightedEdgeKeys = new Set<string>()

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
      const from = pointMap.get(edge.fromNodeId) ?? routePoints.find((p) => p.nodeId === edge.fromNodeId)
      const to = pointMap.get(edge.toNodeId) ?? routePoints.find((p) => p.nodeId === edge.toNodeId)
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
    <div className={`relative h-full w-full overflow-hidden rounded-xl bg-white ${className}`}>
      <svg viewBox="0 0 100 100" className="h-full w-full" aria-label="景区道路图">
        <defs>
          <marker
            id={arrowMarkerId}
            viewBox="0 0 10 10"
            refX="8.5"
            refY="5"
            markerWidth="2.8"
            markerHeight="2.8"
            orient="auto"
          >
            <path d="M 0 0 L 10 5 L 0 10 z" fill={GRAPH_ROUTE} />
          </marker>
        </defs>

        <rect x="0" y="0" width="100" height="100" fill="#ffffff" />

        {catalogLines
          .filter((line) => !highlightedEdgeKeys.has(line.key))
          .map((line) => (
            <line
              key={`catalog-${line.key}`}
              x1={line.from.x}
              y1={line.from.y}
              x2={line.to.x}
              y2={line.to.y}
              stroke={GRAPH_GREEN_LIGHT}
              strokeWidth="0.7"
              strokeLinecap="round"
            />
          ))}

        {routeLines.map((line) => (
          <line
            key={`route-${line.key}`}
            x1={line.from.x}
            y1={line.from.y}
            x2={line.to.x}
            y2={line.to.y}
            stroke={GRAPH_ROUTE}
            strokeWidth="1.15"
            strokeLinecap="round"
            markerEnd={`url(#${arrowMarkerId})`}
          />
        ))}

        {allPoints.map((p) => {
          const onRoute = routeIds.has(p.nodeId)
          const labelY = p.y > 88 ? p.y - 3.4 : p.y + 4
          return (
            <g key={`node-${p.nodeId}`}>
              {onRoute ? (
                <circle
                  cx={p.x}
                  cy={p.y}
                  r="3.1"
                  fill="none"
                  stroke={GRAPH_ROUTE}
                  strokeWidth="0.45"
                  opacity="0.55"
                />
              ) : null}
              <circle
                cx={p.x}
                cy={p.y}
                r={onRoute ? 2.6 : 2.1}
                fill={onRoute ? GRAPH_ROUTE : GRAPH_GREEN}
                stroke="#fff"
                strokeWidth="0.4"
              />
              <text
                x={p.x + 3}
                y={labelY}
                textAnchor="start"
                fontSize="2.2"
                fill={onRoute ? '#142a20' : '#1a2e24'}
                fontFamily="system-ui, sans-serif"
                fontWeight={onRoute ? '600' : '500'}
              >
                {`${p.nodeId}. ${truncateLabel(p.label)}`}
              </text>
            </g>
          )
        })}
      </svg>

      <p className="pointer-events-none absolute bottom-1 left-2 font-body text-[9px] text-[#6b8076]/85">
        道路图 · 全部节点；选中路线以箭头连线标示
      </p>
    </div>
  )
}
