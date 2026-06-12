import { useEffect, useMemo, useState } from 'react'
import { fetchDestinationMapNodes, type MapNodeOption } from '../../api/mapNode'
import {
  planMultiRoute,
  planSingleRoute,
  toBackendTransport,
  type PathNodeResult,
  type RoutePlanVO,
  type RouteStrategyType,
  type RouteTransportType,
} from '../../api/route'
import { hasStoredToken } from '../../api/http'
import { macroPlanFromRoutePlanVO } from '../../lib/route/backendRoutePlan'
import { enrichInternalRoutePolyline } from '../../lib/route/internalRoutePolyline'
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

type InternalRoutePlanModalProps = {
  open: boolean
  onClose: () => void
  selectedWaypoints: RouteWaypoint[]
  onApplyPlan: (plan: MacroRoutePlan) => void
}

function buildPreviewPathNodes(
  nodes: MapNodeOption[],
  startNodeId: number | null,
  targetNodeIds: number[],
  result: RoutePlanVO | null,
): PathNodeResult[] {
  if (result?.pathNodes?.length) return result.pathNodes

  const ordered: number[] = []
  if (startNodeId != null) ordered.push(startNodeId)
  for (const id of targetNodeIds) {
    if (!ordered.includes(id)) ordered.push(id)
  }

  if (ordered.length === 0) {
    return nodes.map((n) => ({ nodeId: n.nodeId, nodeName: n.nodeName }))
  }

  return ordered.map((id) => {
    const hit = nodes.find((n) => n.nodeId === id)
    return { nodeId: id, nodeName: hit?.nodeName ?? `节点 ${id}` }
  })
}

export function InternalRoutePlanModal({
  open,
  onClose,
  selectedWaypoints,
  onApplyPlan,
}: InternalRoutePlanModalProps) {
  const destinationOptions = useMemo(
    () =>
      selectedWaypoints.filter(
        (wp) => typeof wp.destinationId === 'number' && wp.destinationId > 0,
      ),
    [selectedWaypoints],
  )

  const [destinationId, setDestinationId] = useState<number | null>(null)
  const [nodes, setNodes] = useState<MapNodeOption[]>([])
  const [usedPlaceFallback, setUsedPlaceFallback] = useState(false)
  const [loadingNodes, setLoadingNodes] = useState(false)
  const [startNodeId, setStartNodeId] = useState<number | null>(null)
  const [targetNodeIds, setTargetNodeIds] = useState<number[]>([])
  const [strategy, setStrategy] = useState<RouteStrategyType>('shortest_distance')
  const [transport, setTransport] = useState<RouteTransportType>('walk')
  const [returnToStart, setReturnToStart] = useState(true)
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<RoutePlanVO | null>(null)
  const [previewPlan, setPreviewPlan] = useState<MacroRoutePlan | null>(null)

  useEffect(() => {
    if (!open) return
    const first = destinationOptions[0]?.destinationId ?? null
    setDestinationId(first)
    setResult(null)
    setPreviewPlan(null)
    setError(null)
  }, [open, destinationOptions])

  useEffect(() => {
    if (!open || destinationId == null) return
    let cancelled = false
    setLoadingNodes(true)
    setError(null)
    void (async () => {
      try {
        const res = await fetchDestinationMapNodes(destinationId)
        if (cancelled) return
        setNodes(res.nodes)
        setUsedPlaceFallback(res.usedPlaceFallback)
        setStartNodeId(res.nodes[0]?.nodeId ?? null)
        setTargetNodeIds(res.nodes.length > 1 ? [res.nodes[1].nodeId] : [])
      } catch (err) {
        if (!cancelled) {
          setNodes([])
          setError(err instanceof Error ? err.message : '加载景区节点失败')
        }
      } finally {
        if (!cancelled) setLoadingNodes(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [open, destinationId])

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.nodeId, n])), [nodes])

  const mapPathNodes = useMemo(
    () => buildPreviewPathNodes(nodes, startNodeId, targetNodeIds, result),
    [nodes, startNodeId, targetNodeIds, result],
  )

  const mapWaypoints = useMemo((): RouteWaypoint[] => {
    if (previewPlan?.waypoints.length) return previewPlan.waypoints

    const highlight = new Set<number>()
    if (startNodeId != null) highlight.add(startNodeId)
    targetNodeIds.forEach((id) => highlight.add(id))
    const showAll = highlight.size === 0

    return nodes
      .filter((n) => showAll || highlight.has(n.nodeId))
      .map((n) => ({
        id: n.nodeId,
        name: n.nodeName,
        lng: n.lng,
        lat: n.lat,
        destinationId: destinationId ?? undefined,
      }))
  }, [previewPlan, nodes, startNodeId, targetNodeIds, destinationId])

  const mapPolyline = previewPlan?.polyline ?? []
  const hasPlannedRoute = Boolean(previewPlan?.waypoints.length)

  const toggleTarget = (id: number) => {
    setTargetNodeIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  const handlePlan = async () => {
    if (!hasStoredToken()) {
      setError('请先登录后再使用景区内部路线规划')
      return
    }
    if (destinationId == null || startNodeId == null) {
      setError('请选择目的地和起点')
      return
    }
    setPlanning(true)
    setError(null)
    try {
      const backendTransport = toBackendTransport(transport)
      let vo: RoutePlanVO
      if (targetNodeIds.length <= 1) {
        const target = targetNodeIds[0] ?? startNodeId
        vo = await planSingleRoute({
          destinationId,
          startNodeId,
          targetNodeId: target,
          strategyType: strategy,
          transportType: transport,
        })
      } else {
        vo = await planMultiRoute({
          destinationId,
          startNodeId,
          targetNodeIds,
          strategyType: strategy,
          transportType: transport,
          returnToStart,
        })
      }
      setResult(vo)
      let macro = macroPlanFromRoutePlanVO({ ...vo, transportType: backendTransport }, nodeById)
      macro = await enrichInternalRoutePolyline(macro)
      setPreviewPlan(macro)
    } catch (err) {
      setError(err instanceof Error ? err.message : '内部路线规划失败')
      setResult(null)
      setPreviewPlan(null)
    } finally {
      setPlanning(false)
    }
  }

  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[300] flex items-center justify-center bg-black/45 p-3 sm:p-5"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="flex h-[min(92vh,880px)] w-full max-w-[min(96vw,78rem)] flex-col overflow-hidden rounded-[2rem] border border-white/80 bg-white shadow-2xl lg:flex-row">
        {/* 左侧：表单 */}
        <div className="flex min-h-0 flex-1 flex-col border-b border-[var(--ds-border)]/60 lg:max-w-[26rem] lg:flex-none lg:border-b-0 lg:border-r">
          <div className="flex items-start justify-between gap-3 border-b border-[var(--ds-border)]/40 px-5 py-4">
            <div>
              <h2 className="font-display text-xl font-semibold text-[var(--ds-foreground)]">
                景区 / 校园内部路线
              </h2>
              <p className="mt-1 font-body text-xs leading-relaxed text-[var(--ds-muted-foreground)]">
                后端道路图规划 · 右侧地图展示节点与路线
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="shrink-0 rounded-full border border-[var(--ds-border)] px-3 py-1 text-xs text-[var(--ds-muted-foreground)]"
            >
              关闭
            </button>
          </div>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-5 py-4">
            {!hasStoredToken() ? (
              <p className="rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
                内部路线规划需要登录后使用。
              </p>
            ) : null}

            <label className="block font-body text-sm">
              <span className="mb-1 block text-[var(--ds-muted-foreground)]">目的地</span>
              <select
                value={destinationId ?? ''}
                onChange={(e) => setDestinationId(Number(e.target.value) || null)}
                className="w-full rounded-xl border border-[var(--ds-border)] px-3 py-2 text-sm"
              >
                {destinationOptions.length === 0 ? (
                  <option value="">请先在下方勾选带编号的目的地</option>
                ) : (
                  destinationOptions.map((wp) => (
                    <option key={String(wp.id)} value={wp.destinationId}>
                      {wp.name}
                    </option>
                  ))
                )}
              </select>
            </label>

            {usedPlaceFallback ? (
              <p className="text-xs text-amber-800">
                节点坐标来自场所列表回退，地图以示意图展示；有精确坐标时将自动切换高德底图。
              </p>
            ) : null}

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
              <label className="block font-body text-sm">
                <span className="mb-1 block text-[var(--ds-muted-foreground)]">起点</span>
                <select
                  value={startNodeId ?? ''}
                  onChange={(e) => setStartNodeId(Number(e.target.value) || null)}
                  disabled={loadingNodes}
                  className="w-full rounded-xl border border-[var(--ds-border)] px-3 py-2 text-sm"
                >
                  {nodes.map((n) => (
                    <option key={n.nodeId} value={n.nodeId}>
                      {n.nodeName}
                    </option>
                  ))}
                </select>
              </label>
              <div>
                <span className="mb-1 block font-body text-sm text-[var(--ds-muted-foreground)]">
                  途经点（可多选）
                </span>
                <div className="max-h-24 overflow-y-auto rounded-xl border border-[var(--ds-border)] p-2">
                  {loadingNodes ? (
                    <p className="text-xs text-[var(--ds-muted-foreground)]">加载节点…</p>
                  ) : (
                    nodes.map((n) => (
                      <label
                        key={n.nodeId}
                        className="flex cursor-pointer items-center gap-2 py-1 text-xs"
                      >
                        <input
                          type="checkbox"
                          checked={targetNodeIds.includes(n.nodeId)}
                          onChange={() => toggleTarget(n.nodeId)}
                        />
                        {n.nodeName}
                      </label>
                    ))
                  )}
                </div>
              </div>
            </div>

            <div>
              <span className="mb-2 block font-body text-xs font-semibold text-[var(--ds-muted-foreground)]">
                规划策略
              </span>
              <div className="flex flex-wrap gap-2">
                {STRATEGIES.map((s) => (
                  <button
                    key={s.value}
                    type="button"
                    onClick={() => setStrategy(s.value)}
                    className={`rounded-full px-4 py-1.5 text-xs font-semibold transition ${
                      strategy === s.value
                        ? 'bg-[var(--ds-primary)] text-white'
                        : 'border border-[var(--ds-primary)]/20 bg-white text-[var(--ds-primary)]'
                    }`}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <span className="mb-2 block font-body text-xs font-semibold text-[var(--ds-muted-foreground)]">
                交通方式
              </span>
              <div className="flex flex-wrap gap-2">
                {TRANSPORTS.map((t) => (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => setTransport(t.value)}
                    className={`rounded-full px-4 py-1.5 text-xs font-semibold transition ${
                      transport === t.value
                        ? 'bg-[var(--ds-primary)] text-white'
                        : 'border border-[var(--ds-primary)]/20 bg-white text-[var(--ds-primary)]'
                    }`}
                  >
                    {t.label}
                  </button>
                ))}
              </div>
            </div>

            <label className="flex items-center gap-2 font-body text-sm text-[var(--ds-foreground)]">
              <input
                type="checkbox"
                checked={returnToStart}
                onChange={(e) => setReturnToStart(e.target.checked)}
              />
              游览后返回起点（多点规划）
            </label>

            {error ? (
              <p className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
                {error}
              </p>
            ) : null}

            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                disabled={planning || destinationId == null}
                onClick={() => void handlePlan()}
                className="rounded-full bg-[var(--ds-primary)] px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-50"
              >
                {planning ? '规划中…' : '生成内部路线'}
              </button>
              {previewPlan ? (
                <button
                  type="button"
                  onClick={() => {
                    onApplyPlan(previewPlan)
                    onClose()
                  }}
                  className="rounded-full border border-[var(--ds-primary)] px-5 py-2.5 text-sm font-semibold text-[var(--ds-primary)]"
                >
                  显示在主地图
                </button>
              ) : null}
            </div>

            {result ? (
              <div className="rounded-xl border border-[var(--ds-primary)]/15 bg-[var(--ds-muted)]/40 p-3">
                <p className="font-body text-sm font-semibold text-[var(--ds-foreground)]">
                  {result.routeSummary ?? '路线已生成'}
                </p>
                <p className="mt-1 font-body text-xs text-[var(--ds-muted-foreground)]">
                  总距离 {Number(result.totalDistance).toFixed(0)} m · 约 {result.estimatedTime} 分钟
                </p>
                <ol className="mt-2 max-h-28 list-decimal space-y-1 overflow-y-auto pl-5 font-body text-xs text-[var(--ds-foreground)]">
                  {(result.pathNodes ?? []).map((n) => (
                    <li key={n.nodeId}>{n.nodeName}</li>
                  ))}
                </ol>
              </div>
            ) : null}
          </div>
        </div>

        {/* 右侧：地图 */}
        <div className="flex min-h-[280px] min-w-0 flex-1 flex-col bg-[var(--ds-muted)]/30 p-4 lg:min-h-0">
          <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
            <p className="font-body text-sm font-semibold text-[var(--ds-foreground)]">
              {hasPlannedRoute ? '规划路线预览' : '景区节点分布'}
            </p>
            <p className="font-body text-xs text-[var(--ds-muted-foreground)]">
              {loadingNodes
                ? '加载节点…'
                : hasPlannedRoute
                  ? '已贴合道路图路径'
                  : '选择起点与途经点后生成路线'}
            </p>
          </div>

          <div className="relative min-h-0 flex-1">
            {loadingNodes ? (
              <div className="flex h-full min-h-[240px] items-center justify-center rounded-[1.5rem] border border-[var(--ds-border)]/50 bg-white font-body text-sm text-[var(--ds-muted-foreground)]">
                正在加载景区节点…
              </div>
            ) : nodes.length === 0 ? (
              <div className="flex h-full min-h-[240px] items-center justify-center rounded-[1.5rem] border border-dashed border-[var(--ds-border)] bg-white/80 px-6 text-center font-body text-sm text-[var(--ds-muted-foreground)]">
                请先选择带编号的目的地，地图将展示景区内节点位置
              </div>
            ) : usedPlaceFallback ? (
              <div className="h-full min-h-[240px] overflow-hidden rounded-[1.5rem] border border-[var(--ds-border)]/50 bg-white">
                <RoutePathSvg
                  pathNodes={mapPathNodes}
                  nodeCatalog={nodes}
                  className="h-full min-h-[240px] p-4"
                />
              </div>
            ) : (
              <AmapMapView
                key={`internal-map-${destinationId ?? 'none'}`}
                className="h-full min-h-[240px]"
                waypoints={mapWaypoints}
                polyline={mapPolyline}
                activeId={startNodeId}
                maxFitZoom={17}
                singlePointZoom={17}
              />
            )}
          </div>

          <p className="mt-3 font-body text-[11px] leading-relaxed text-[var(--ds-muted-foreground)]">
            高德底图可展示景区范围内的节点与规划路线；官方「室内地图」仅覆盖部分商场/场馆，本功能以项目自建道路图节点坐标为主，与城市导航互补。
          </p>
        </div>
      </div>
    </div>
  )
}
