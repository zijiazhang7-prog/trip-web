import type { PathNodeResult } from '../../api/route'
import type { MapNodeOption } from '../../api/mapNode'

type RoutePathSvgProps = {
  pathNodes: PathNodeResult[]
  nodeCatalog: MapNodeOption[]
  className?: string
}

function layoutPoints(
  pathNodes: PathNodeResult[],
  catalog: MapNodeOption[],
): { x: number; y: number; label: string }[] {
  const byId = new Map(catalog.map((n) => [n.nodeId, n]))
  const coords = pathNodes.map((p, i) => {
    const hit = byId.get(p.nodeId)
    if (hit) return { x: hit.lng, y: hit.lat, label: p.nodeName || hit.nodeName }
    const angle = (i / Math.max(pathNodes.length, 1)) * Math.PI * 2
    return {
      x: 50 + Math.cos(angle) * 35,
      y: 50 + Math.sin(angle) * 35,
      label: p.nodeName,
    }
  })
  if (coords.length <= 1) return coords

  const xs = coords.map((c) => c.x)
  const ys = coords.map((c) => c.y)
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  const pad = 12
  const w = maxX - minX || 1
  const h = maxY - minY || 1

  return coords.map((c) => ({
    x: pad + ((c.x - minX) / w) * (100 - pad * 2),
    y: pad + ((c.y - minY) / h) * (100 - pad * 2),
    label: c.label,
  }))
}

export function RoutePathSvg({ pathNodes, nodeCatalog, className = '' }: RoutePathSvgProps) {
  const points = layoutPoints(pathNodes, nodeCatalog)
  if (!points.length) {
    return (
      <div className={`flex h-full items-center justify-center font-body text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        选择节点并生成路线后，将在此展示路径
      </div>
    )
  }

  const polyline = points.map((p) => `${p.x},${p.y}`).join(' ')

  return (
    <svg viewBox="0 0 100 100" className={`h-full w-full ${className}`} aria-label="路线示意图">
      <defs>
        <linearGradient id="routeStroke" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="var(--ds-primary)" />
          <stop offset="100%" stopColor="var(--ds-secondary)" />
        </linearGradient>
      </defs>
      {points.length > 1 ? (
        <polyline
          points={polyline}
          fill="none"
          stroke="url(#routeStroke)"
          strokeWidth="1.2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      ) : null}
      {points.map((p, i) => (
        <g key={`${p.label}-${i}`}>
          <circle cx={p.x} cy={p.y} r="3.2" fill="var(--ds-primary)" />
          <text
            x={p.x}
            y={p.y - 5}
            textAnchor="middle"
            fontSize="4"
            fill="var(--ds-accent-foreground)"
          >
            {i + 1}. {p.label.length > 8 ? `${p.label.slice(0, 8)}…` : p.label}
          </text>
        </g>
      ))}
    </svg>
  )
}
