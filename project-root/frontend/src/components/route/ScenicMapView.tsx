import type { PathNodeResult } from '../../api/route'
import type { MapNodeOption } from '../../api/mapNode'
import {
  arrowPointsAlongPath,
  layoutAllCatalogPoints,
  layoutScenicPoints,
  type ScenicMapPoint,
} from '../../lib/route/scenicMapLayout'

type ScenicMapViewProps = {
  nodeCatalog: MapNodeOption[]
  pathNodes: PathNodeResult[]
  startNodeId?: number | null
  endNodeId?: number | null
  className?: string
}

function pointById(points: ScenicMapPoint[], id: number | null | undefined): ScenicMapPoint | null {
  if (id == null) return null
  return points.find((p) => p.nodeId === id) ?? null
}

export function ScenicMapView({
  nodeCatalog,
  pathNodes,
  startNodeId,
  endNodeId,
  className = '',
}: ScenicMapViewProps) {
  const routePoints = layoutScenicPoints(pathNodes, nodeCatalog)
  const allPoints = layoutAllCatalogPoints(nodeCatalog)
  const routeIds = new Set(pathNodes.map((n) => n.nodeId))
  const start = pointById(routePoints, startNodeId) ?? routePoints[0] ?? null
  const end = pointById(routePoints, endNodeId) ?? routePoints[routePoints.length - 1] ?? null
  const polyline = routePoints.map((p) => `${p.x},${p.y}`).join(' ')
  const arrows = arrowPointsAlongPath(routePoints)

  if (!nodeCatalog.length) {
    return (
      <div className={`flex h-full items-center justify-center font-body text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        暂无景区节点数据
      </div>
    )
  }

  return (
    <div className={`relative h-full w-full overflow-hidden rounded-xl bg-[#e8f0e6] ${className}`}>
      <svg viewBox="0 0 100 100" className="h-full w-full" aria-label="景区内部地图">
        <defs>
          <linearGradient id="scenicWater" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#b8d4e8" />
            <stop offset="100%" stopColor="#8eb8d4" />
          </linearGradient>
          <linearGradient id="scenicRoute" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stopColor="#2f7fd4" />
            <stop offset="100%" stopColor="#1a5fb4" />
          </linearGradient>
          <marker id="routeArrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="4" markerHeight="4" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#1a5fb4" />
          </marker>
        </defs>

        <rect x="0" y="0" width="100" height="100" fill="#f2f6ef" />
        <ellipse cx="42" cy="58" rx="22" ry="16" fill="url(#scenicWater)" opacity="0.85" />
        <rect x="8" y="12" width="28" height="22" rx="3" fill="#e8d4f0" opacity="0.55" />
        <rect x="62" y="18" width="30" height="26" rx="3" fill="#dce8c8" opacity="0.6" />
        <rect x="55" y="62" width="36" height="28" rx="3" fill="#f5e6b8" opacity="0.55" />

        {allPoints
          .filter((p) => !routeIds.has(p.nodeId))
          .map((p) => (
            <g key={`poi-${p.nodeId}`} opacity="0.72">
              <circle cx={p.x} cy={p.y} r="1.8" fill="#6b8f71" />
              <text x={p.x} y={p.y - 2.8} textAnchor="middle" fontSize="2.2" fill="#3d5a42">
                {p.label.length > 6 ? `${p.label.slice(0, 6)}…` : p.label}
              </text>
            </g>
          ))}

        {routePoints.length > 1 ? (
          <polyline
            points={polyline}
            fill="none"
            stroke="url(#scenicRoute)"
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
            markerMid="url(#routeArrow)"
          />
        ) : null}

        {arrows.map((raw, i) => {
          const [mx, my, deg] = raw.split(',')
          return (
            <text
              key={`arrow-${i}`}
              x={mx}
              y={my}
              fontSize="3.5"
              fill="#ffffff"
              fontWeight="bold"
              textAnchor="middle"
              dominantBaseline="middle"
              transform={`rotate(${deg} ${mx} ${my})`}
            >
              ›
            </text>
          )
        })}

        {routePoints.map((p, i) => {
          const isStart = start && p.nodeId === start.nodeId
          const isEnd = end && p.nodeId === end.nodeId
          if (isStart || isEnd) return null
          return (
            <g key={`route-node-${p.nodeId}-${i}`}>
              <circle cx={p.x} cy={p.y} r="2.2" fill="#4a90d9" stroke="#fff" strokeWidth="0.5" />
            </g>
          )
        })}

        {start ? (
          <g>
            <circle cx={start.x} cy={start.y} r="3.6" fill="#3d9a5a" stroke="#fff" strokeWidth="0.6" />
            <text x={start.x} y={start.y + 0.8} textAnchor="middle" fontSize="3.2" fill="#fff" fontWeight="bold">
              起
            </text>
            <text x={start.x} y={start.y - 5} textAnchor="middle" fontSize="2.6" fill="#2d5a3a" fontWeight="600">
              {start.label.length > 8 ? `${start.label.slice(0, 8)}…` : start.label}
            </text>
          </g>
        ) : null}

        {end && end.nodeId !== start?.nodeId ? (
          <g>
            <circle cx={end.x} cy={end.y} r="3.6" fill="#d94a4a" stroke="#fff" strokeWidth="0.6" />
            <text x={end.x} y={end.y + 0.8} textAnchor="middle" fontSize="3.2" fill="#fff" fontWeight="bold">
              终
            </text>
            <text x={end.x} y={end.y - 5} textAnchor="middle" fontSize="2.6" fill="#6b2d2d" fontWeight="600">
              {end.label.length > 8 ? `${end.label.slice(0, 8)}…` : end.label}
            </text>
          </g>
        ) : null}
      </svg>

      <p className="pointer-events-none absolute bottom-1 left-2 font-body text-[9px] text-[#6b8076]/80">
        景区道路图 · 节点坐标
      </p>
    </div>
  )
}
