import { useEffect, useMemo, useState } from 'react'
import { fetchDestinationMapNodes, type MapNodeOption } from '../../api/mapNode'
import {
  planSingleRoute,
  toBackendTransport,
  type RouteStrategyType,
  type RouteTransportType,
} from '../../api/route'
import { hasStoredToken } from '../../api/http'
import { macroPlanFromRoutePlanVO } from '../../lib/route/backendRoutePlan'
import { AmapMapView } from './AmapMapView'
import { RoutePathSvg } from './RoutePathSvg'
import type { MacroRoutePlan, RouteWaypoint } from '../../types/macroRoute'

const STRATEGIES: { value: RouteStrategyType; label: string }[] = [
  { value: 'shortest_distance', label: '最短距离' },
  { value: 'shortest_time', label: '最短时间' },
]

const TRANSPORTS: { value: RouteTransportType; label: string }[] = [
  { value: 'walk', label: '步行' },
  { value: 'bike', label: '骑行' },
  { value: 'drive', label: '汽车' },
  { value: 'transit', label: '公共交通' },
]

type WaypointInternalNavPanelProps = {
  waypoint: RouteWaypoint
  onApplyPlan?: (plan: MacroRoutePlan) => void
}

export function WaypointInternalNavPanel({ waypoint, onApplyPlan }: WaypointInternalNavPanelProps) {
  const destinationId = waypoint.destinationId
  const [nodes, setNodes] = useState<MapNodeOption[]>([])
  const [usedPlaceFallback, setUsedPlaceFallback] = useState(false)
  const [loading, setLoading] = useState(false)
  const [startNodeId, setStartNodeId] = useState<number | null>(null)
  const [targetNodeId, setTargetNodeId] = useState<number | null>(null)
  const [strategy, setStrategy] = useState<RouteStrategyType>('shortest_distance')
  const [transport, setTransport] = useState<RouteTransportType>('walk')
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [previewPlan, setPreviewPlan] = useState<MacroRoutePlan | null>(null)
  const [mapMode, setMapMode] = useState<'scenic' | 'indoor'>('scenic')

  useEffect(() => {
    if (typeof destinationId !== 'number' || destinationId <= 0) return
    let cancelled = false
    setLoading(true)
    setError(null)
    setPreviewPlan(null)
    void (async () => {
      try {
        const res = await fetchDestinationMapNodes(destinationId)
        if (cancelled) return
        setNodes(res.nodes)
        setUsedPlaceFallback(res.usedPlaceFallback)
        setStartNodeId(res.nodes[0]?.nodeId ?? null)
        setTargetNodeId(res.nodes[1]?.nodeId ?? res.nodes[0]?.nodeId ?? null)
      } catch (err) {
        if (!cancelled) {
          setNodes([])
          setError(err instanceof Error ? err.message : '加载节点失败')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [destinationId])

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.nodeId, n])), [nodes])

  const mapWaypoints = useMemo((): RouteWaypoint[] => {
    if (previewPlan?.waypoints.length) return previewPlan.waypoints
    return nodes.map((n) => ({
      id: n.nodeId,
      name: n.nodeName,
      lng: n.lng,
      lat: n.lat,
      destinationId,
    }))
  }, [previewPlan, nodes, destinationId])

  const handlePlan = async () => {
    if (!hasStoredToken()) {
      setError('请先登录后使用内部路线')
      return
    }
    if (destinationId == null || startNodeId == null || targetNodeId == null) {
      setError('请选择起点与终点')
      return
    }
    setPlanning(true)
    setError(null)
    try {
      const vo = await planSingleRoute({
        destinationId,
        startNodeId,
        targetNodeId,
        strategyType: strategy,
        transportType: transport,
      })
      const macro = macroPlanFromRoutePlanVO(
        { ...vo, transportType: toBackendTransport(transport) },
        nodeById,
      )
      setPreviewPlan(macro)
      onApplyPlan?.(macro)
    } catch (err) {
      setError(err instanceof Error ? err.message : '内部路线失败')
      setPreviewPlan(null)
    } finally {
      setPlanning(false)
    }
  }

  if (typeof destinationId !== 'number' || destinationId <= 0) {
    return (
      <p className="rounded-xl border border-dashed border-[var(--ds-border)] px-3 py-4 text-center font-body text-xs text-[var(--ds-muted-foreground)]">
        该站点无后端编号，无法展开景区内部导航。请从数据库目的地列表中加入路线。
      </p>
    )
  }

  return (
    <div className="mt-3 space-y-3 rounded-xl border border-[var(--ds-primary)]/15 bg-[var(--ds-muted)]/30 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-body text-xs font-semibold text-[var(--ds-foreground)]">
          {waypoint.name} · 景区 / 室内导航
        </p>
        <div className="flex gap-1">
          <button
            type="button"
            onClick={() => setMapMode('scenic')}
            className={`rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${
              mapMode === 'scenic' ? 'bg-[var(--ds-primary)] text-white' : 'border border-[var(--ds-primary)]/25 text-[var(--ds-primary)]'
            }`}
          >
            道路图
          </button>
          <button
            type="button"
            onClick={() => setMapMode('indoor')}
            className={`rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${
              mapMode === 'indoor' ? 'bg-[var(--ds-primary)] text-white' : 'border border-[var(--ds-primary)]/25 text-[var(--ds-primary)]'
            }`}
          >
            高德室内
          </button>
        </div>
      </div>

      {loading ? (
        <p className="text-xs text-[var(--ds-muted-foreground)]">加载景区节点…</p>
      ) : nodes.length === 0 ? (
        <p className="text-xs text-[var(--ds-muted-foreground)]">暂无节点数据</p>
      ) : (
        <div className="grid gap-2 sm:grid-cols-2">
          <label className="block text-xs">
            <span className="text-[var(--ds-muted-foreground)]">起点</span>
            <select
              value={startNodeId ?? ''}
              onChange={(e) => setStartNodeId(Number(e.target.value) || null)}
              className="mt-1 w-full rounded-lg border border-[var(--ds-border)] px-2 py-1.5 text-xs"
            >
              {nodes.map((n) => (
                <option key={n.nodeId} value={n.nodeId}>
                  {n.nodeName}
                </option>
              ))}
            </select>
          </label>
          <label className="block text-xs">
            <span className="text-[var(--ds-muted-foreground)]">终点</span>
            <select
              value={targetNodeId ?? ''}
              onChange={(e) => setTargetNodeId(Number(e.target.value) || null)}
              className="mt-1 w-full rounded-lg border border-[var(--ds-border)] px-2 py-1.5 text-xs"
            >
              {nodes.map((n) => (
                <option key={n.nodeId} value={n.nodeId}>
                  {n.nodeName}
                </option>
              ))}
            </select>
          </label>
        </div>
      )}

      <div className="flex flex-wrap gap-1.5">
        {STRATEGIES.map((s) => (
          <button
            key={s.value}
            type="button"
            onClick={() => setStrategy(s.value)}
            className={`rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${
              strategy === s.value ? 'bg-[var(--ds-primary)] text-white' : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
            }`}
          >
            {s.label}
          </button>
        ))}
      </div>
      <div className="flex flex-wrap gap-1.5">
        {TRANSPORTS.map((t) => (
          <button
            key={t.value}
            type="button"
            onClick={() => setTransport(t.value)}
            className={`rounded-full px-2.5 py-0.5 text-[10px] font-semibold ${
              transport === t.value ? 'bg-[var(--ds-primary)] text-white' : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="h-[200px] overflow-hidden rounded-xl border border-[var(--ds-border)]/40 bg-white">
        {usedPlaceFallback && mapMode === 'scenic' ? (
          <RoutePathSvg
            pathNodes={nodes.map((n) => ({ nodeId: n.nodeId, nodeName: n.nodeName }))}
            nodeCatalog={nodes}
            className="h-full p-2"
          />
        ) : (
          <AmapMapView
            className="h-full min-h-[200px]"
            waypoints={mapWaypoints}
            polyline={previewPlan?.polyline}
            activeId={startNodeId}
            showIndoorMap={mapMode === 'indoor'}
            indoorZoom={18}
            maxFitZoom={18}
            singlePointZoom={18}
          />
        )}
      </div>

      {mapMode === 'indoor' ? (
        <p className="font-body text-[10px] text-[var(--ds-muted-foreground)]">
          高德室内图在 zoom≥17 时自动显示；商场/场馆等有官方室内数据，景区以道路图节点为主。
        </p>
      ) : null}

      {error ? <p className="text-xs text-red-700">{error}</p> : null}

      <button
        type="button"
        disabled={planning || nodes.length === 0}
        onClick={() => void handlePlan()}
        className="w-full rounded-full bg-[var(--ds-primary)] py-2 text-xs font-semibold text-white disabled:opacity-50"
      >
        {planning ? '规划中…' : '生成此站内部路线'}
      </button>
    </div>
  )
}
