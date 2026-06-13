import { useEffect, useMemo, useState } from 'react'
import {
  fetchDestinationMapEdges,
  fetchDestinationMapNodes,
  quickDisplayGpsCoords,
  type MapCatalogEdge,
  type MapNodeOption,
} from '../../api/mapNode'
import {
  type RoutePlanVO,
  type RouteStrategyType,
  type RouteTransportType,
} from '../../api/route'
import { hasStoredToken } from '../../api/http'
import { isBeijingAreaCoord } from '../../lib/geo/beijingCoord'
import { resolveDestinationCoords } from '../../lib/geo/resolveCoords'
import { filterBeijingPolyline } from '../../lib/route/internalRoutePolyline'
import { executeInternalRoutePlan } from '../../lib/route/planInternalRoute'
import { resolveDefaultStartNode, resolveDefaultTargetNode } from '../../lib/route/resolveStartNode'
import { AmapMapView } from './AmapMapView'
import { MapViewModeToggle, type MapViewMode } from './MapViewModeToggle'
import { RoadGraphView } from './RoadGraphView'
import type { MacroRoutePlan, RouteWaypoint } from '../../types/macroRoute'

const UNIVERSAL_STUDIOS_CENTER: [number, number] = [116.681128, 39.852226]

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
  modalOpen?: boolean
}

function isValidCoord(lng: number, lat: number): boolean {
  return Number.isFinite(lng) && Number.isFinite(lat) && !(lng === 0 && lat === 0)
}

export function WaypointInternalNavPanel({ waypoint, modalOpen = true }: WaypointInternalNavPanelProps) {
  const destinationId = waypoint.destinationId
  const [nodes, setNodes] = useState<MapNodeOption[]>([])
  const [catalogEdges, setCatalogEdges] = useState<MapCatalogEdge[]>([])
  const [usedPlaceFallback, setUsedPlaceFallback] = useState(false)
  const [loading, setLoading] = useState(false)
  const [startNodeId, setStartNodeId] = useState<number | null>(null)
  const [targetNodeId, setTargetNodeId] = useState<number | null>(null)
  const [strategy, setStrategy] = useState<RouteStrategyType>('shortest_distance')
  const [transport, setTransport] = useState<RouteTransportType>('walk')
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [previewPlan, setPreviewPlan] = useState<MacroRoutePlan | null>(null)
  const [routeResult, setRouteResult] = useState<RoutePlanVO | null>(null)
  const [mapViewMode, setMapViewMode] = useState<MapViewMode>('route')
  const [scenicCenter, setScenicCenter] = useState<[number, number]>(UNIVERSAL_STUDIOS_CENTER)
  const [mapMounted, setMapMounted] = useState(false)

  useEffect(() => {
    let cancelled = false
    void (async () => {
      if (isValidCoord(waypoint.lng, waypoint.lat) && isBeijingAreaCoord(waypoint.lng, waypoint.lat)) {
        if (!cancelled) setScenicCenter([waypoint.lng, waypoint.lat])
        return
      }
      const geo = await resolveDestinationCoords(waypoint.name, '北京')
      if (!cancelled && geo && isBeijingAreaCoord(geo.lng, geo.lat)) {
        setScenicCenter([geo.lng, geo.lat])
      }
    })()
    return () => {
      cancelled = true
    }
  }, [waypoint.lng, waypoint.lat, waypoint.name])

  useEffect(() => {
    if (typeof destinationId !== 'number' || destinationId <= 0) return
    let cancelled = false
    setLoading(true)
    setError(null)
    setPreviewPlan(null)
    const center = { lng: scenicCenter[0], lat: scenicCenter[1] }
    void (async () => {
      try {
        const [res, edges] = await Promise.all([
          fetchDestinationMapNodes(destinationId),
          fetchDestinationMapEdges(destinationId),
        ])
        const displayNodes = quickDisplayGpsCoords(res.nodes, center)
        if (cancelled) return
        setUsedPlaceFallback(res.usedPlaceFallback)
        setNodes(displayNodes)
        setCatalogEdges(edges)
        const pick = displayNodes.filter((n) => !/^OSM/i.test(n.nodeName?.trim() ?? ''))
        const defaultStart = resolveDefaultStartNode(pick.length ? pick : displayNodes)
        const defaultTarget = resolveDefaultTargetNode(pick.length ? pick : displayNodes, defaultStart)
        setStartNodeId(defaultStart)
        setTargetNodeId(defaultTarget)
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
  }, [destinationId, waypoint.name, scenicCenter])

  useEffect(() => {
    if (!modalOpen) {
      setMapMounted(false)
      return undefined
    }
    const timer = window.setTimeout(() => setMapMounted(true), 120)
    return () => {
      window.clearTimeout(timer)
      setMapMounted(false)
    }
  }, [modalOpen])

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.nodeId, n])), [nodes])

  const mapPolyline = useMemo(
    () => filterBeijingPolyline(previewPlan?.polyline ?? []),
    [previewPlan?.polyline],
  )

  const mapWaypoints = useMemo((): RouteWaypoint[] => {
    if (previewPlan?.waypoints.length) return previewPlan.waypoints

    const markers: RouteWaypoint[] = []
    const push = (id: number | null) => {
      if (id == null) return
      const n = nodeById.get(id)
      if (!n || !isValidCoord(n.lng, n.lat)) return
      markers.push({
        id: n.nodeId,
        name: n.nodeName,
        lng: n.lng,
        lat: n.lat,
        destinationId,
      })
    }
    push(startNodeId)
    if (targetNodeId != null && targetNodeId !== startNodeId) push(targetNodeId)
    return markers
  }, [previewPlan, nodeById, startNodeId, targetNodeId, destinationId])

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
      const buildVisitSequence = (
        vo: RoutePlanVO | null,
        start: number | null,
        targets: number[],
        _returnToStart: boolean,
      ) => {
        if (vo?.pathNodes?.length) return vo.pathNodes.map((n) => n.nodeId)
        const ordered: number[] = []
        if (start != null) ordered.push(start)
        if (targets[0] != null && !ordered.includes(targets[0])) ordered.push(targets[0])
        return ordered
      }
      const { vo, macro } = await executeInternalRoutePlan({
        destinationId,
        startNodeId,
        targetNodeIds: [targetNodeId],
        returnToStart: false,
        strategy,
        transport,
        nodeById,
        scenicCenter,
        destinationName: waypoint.name,
        usedPlaceFallback,
        buildVisitSequence,
      })
      setRouteResult(vo)
      setPreviewPlan(macro)
    } catch (err) {
      setError(err instanceof Error ? err.message : '内部路线失败')
      setPreviewPlan(null)
      setRouteResult(null)
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
      <p className="font-body text-xs font-semibold text-[var(--ds-foreground)]">
        {waypoint.name} · 高德景区地图
      </p>

      {loading ? (
        <p className="text-xs text-[var(--ds-muted-foreground)]">加载景区节点并定位…</p>
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

      <div className="mb-2 flex items-center justify-between gap-2">
        <span className="text-[10px] text-[var(--ds-muted-foreground)]">地图预览</span>
        <MapViewModeToggle mode={mapViewMode} onChange={setMapViewMode} />
      </div>

      <div className="h-[min(48vh,420px)] overflow-hidden rounded-xl border border-[var(--ds-border)]/40 bg-white">
        {mapViewMode === 'road-graph' ? (
          <RoadGraphView
            className="h-full min-h-[min(48vh,420px)] rounded-xl"
            nodeCatalog={nodes}
            catalogEdges={catalogEdges}
            routePathNodes={routeResult?.pathNodes ?? []}
            routePathEdges={routeResult?.pathEdges}
            startNodeId={startNodeId}
            endNodeId={targetNodeId}
          />
        ) : mapMounted ? (
          <AmapMapView
            key={`internal-amap-${destinationId}`}
            className="h-full min-h-[min(48vh,420px)] rounded-xl"
            waypoints={mapWaypoints}
            polyline={mapPolyline}
            activeId={startNodeId}
            defaultCenter={scenicCenter}
            defaultZoom={17}
            maxFitZoom={18}
            singlePointZoom={17}
            routeStrokeColor="#2f7fd4"
            endpointMarkers={Boolean(previewPlan?.polyline?.length)}
          />
        ) : (
          <div className="flex h-full min-h-[min(48vh,420px)] items-center justify-center text-xs text-[var(--ds-muted-foreground)]">
            {loading ? '正在加载节点…' : '准备景区地图…'}
          </div>
        )}
      </div>

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
