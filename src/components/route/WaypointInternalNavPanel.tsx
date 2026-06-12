import { useEffect, useMemo, useState } from 'react'
import {
  enrichMapNodesGpsCoords,
  fetchDestinationMapNodes,
  type MapNodeOption,
} from '../../api/mapNode'
import {
  planSingleRoute,
  toBackendTransport,
  type RouteStrategyType,
  type RouteTransportType,
} from '../../api/route'
import { hasStoredToken } from '../../api/http'
import { macroPlanFromRoutePlanVO } from '../../lib/route/backendRoutePlan'
import { isBeijingAreaCoord } from '../../lib/geo/beijingCoord'
import { resolveDestinationCoords } from '../../lib/geo/resolveCoords'
import {
  enrichInternalRoutePolyline,
  filterBeijingPolyline,
} from '../../lib/route/internalRoutePolyline'
import { AmapMapView } from './AmapMapView'
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
  const [loading, setLoading] = useState(false)
  const [startNodeId, setStartNodeId] = useState<number | null>(null)
  const [targetNodeId, setTargetNodeId] = useState<number | null>(null)
  const [strategy, setStrategy] = useState<RouteStrategyType>('shortest_distance')
  const [transport, setTransport] = useState<RouteTransportType>('walk')
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [previewPlan, setPreviewPlan] = useState<MacroRoutePlan | null>(null)
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
        const res = await fetchDestinationMapNodes(destinationId)
        const gpsNodes = await enrichMapNodesGpsCoords(res.nodes, waypoint.name, center)
        if (cancelled) return
        setNodes(gpsNodes)
        setStartNodeId(gpsNodes[0]?.nodeId ?? null)
        setTargetNodeId(gpsNodes[1]?.nodeId ?? gpsNodes[0]?.nodeId ?? null)
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
  }, [destinationId, waypoint.name])

  useEffect(() => {
    if (!modalOpen || loading || nodes.length === 0) {
      setMapMounted(false)
      return undefined
    }
    const timer = window.setTimeout(() => setMapMounted(true), 300)
    return () => {
      window.clearTimeout(timer)
      setMapMounted(false)
    }
  }, [modalOpen, loading, nodes.length, destinationId])

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.nodeId, n])), [nodes])

  const mapPolyline = useMemo(
    () => filterBeijingPolyline(previewPlan?.polyline ?? []),
    [previewPlan?.polyline],
  )

  const mapWaypoints = useMemo((): RouteWaypoint[] => {
    return nodes.map((n) => ({
      id: n.nodeId,
      name: n.nodeName,
      lng: n.lng,
      lat: n.lat,
      destinationId,
    }))
  }, [nodes, destinationId])

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
      const startNode = nodeById.get(startNodeId)
      const endNode = nodeById.get(targetNodeId)
      if (!startNode || !endNode) {
        throw new Error('起点或终点坐标缺失')
      }
      const enriched = await enrichInternalRoutePolyline(macro)
      const roadLine = filterBeijingPolyline(enriched.polyline)
      setPreviewPlan({
        ...enriched,
        waypoints: [
          {
            id: startNodeId,
            name: startNode.nodeName,
            lng: startNode.lng,
            lat: startNode.lat,
            destinationId,
          },
          {
            id: targetNodeId,
            name: endNode.nodeName,
            lng: endNode.lng,
            lat: endNode.lat,
            destinationId,
          },
        ],
        polyline: roadLine.length >= 2 ? roadLine : filterBeijingPolyline(macro.polyline),
      })
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

      <div className="h-[min(48vh,420px)] overflow-hidden rounded-xl border border-[var(--ds-border)]/40 bg-white">
        {mapMounted ? (
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
          />
        ) : (
          <div className="flex h-full min-h-[min(48vh,420px)] items-center justify-center text-xs text-[var(--ds-muted-foreground)]">
            {loading ? '正在加载景区地图…' : '准备景区地图…'}
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
