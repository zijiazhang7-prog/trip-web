import { useEffect, useId, useRef, useState } from 'react'
import type { MacroRoutePlan, RouteWaypoint, TransportMode } from '../../types/macroRoute'
import { getAmapSecurityCode, hasAmapJsKey } from '../../lib/amap/config'
import { loadAmap } from '../../lib/amap/loader'

type AmapNavigateMapProps = {
  plan: MacroRoutePlan
  activeWaypoint: RouteWaypoint | null
  activeLegIndex: number
  className?: string
}

function modeToPlugin(mode: TransportMode): 'Walking' | 'Driving' | 'Riding' | 'Transfer' {
  if (mode === 'driving') return 'Driving'
  if (mode === 'bicycling') return 'Riding'
  if (mode === 'transit') return 'Transfer'
  return 'Walking'
}

export function AmapNavigateMap({ plan, activeWaypoint, activeLegIndex, className = '' }: AmapNavigateMapProps) {
  const instanceId = useId()
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<{
    destroy: () => void
    add: (o: unknown) => void
    remove: (o: unknown) => void
    setFitView: (o?: unknown[], immediately?: boolean, avoid?: number[], maxZoom?: number) => void
    setCenter: (center: [number, number]) => void
    setZoom: (zoom: number) => void
    resize?: () => void
  } | null>(null)
  const routeServiceRef = useRef<{ clear?: () => void } | null>(null)
  const geoMarkerRef = useRef<unknown | null>(null)
  // 地图就绪信号：用 ref 而非 state，避免重渲染导致遮罩反复闪烁
  const mapReadyRef = useRef(false)
  const [initError, setInitError] = useState<string | null>(null)

  const legFrom = plan.waypoints[activeLegIndex] ?? plan.waypoints[0]
  const legTo = plan.waypoints[activeLegIndex + 1] ?? activeWaypoint ?? plan.waypoints[1]

  // ── 初始化地图（只执行一次） ──────────────────────────────────────────────
  useEffect(() => {
    if (!hasAmapJsKey() || !containerRef.current) return
    let destroyed = false
    const container = containerRef.current
    mapReadyRef.current = false

    ;(async () => {
      try {
        const AMap = await loadAmap()
        if (destroyed || !containerRef.current) return

        const sec = getAmapSecurityCode()
        if (!sec) {
          setInitError('地图瓦片需要配置 VITE_AMAP_SECURITY_CODE')
        }

        const map = new AMap.Map(container, {
          zoom: 13,
          center: [legFrom.lng, legFrom.lat] as [number, number],
          viewMode: '2D',
        })
        mapRef.current = map

        window.setTimeout(() => {
          if (destroyed) return
          map.resize?.()
          mapReadyRef.current = true
          // 地图就绪后，立即触发路线绘制
          drawRoute()
        }, 200)
      } catch (err) {
        if (!destroyed) setInitError(err instanceof Error ? err.message : '地图加载失败')
      }
    })()

    return () => {
      destroyed = true
      mapReadyRef.current = false
      try { routeServiceRef.current?.clear?.() } catch { /* ignore */ }
      routeServiceRef.current = null
      geoMarkerRef.current = null
      try { mapRef.current?.destroy() } catch { /* ignore */ }
      mapRef.current = null
      if (container) container.innerHTML = ''
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instanceId])

  // ── 绘制贴合道路的路线（地图就绪后或 leg 变化时调用） ──────────────────
  function drawRoute() {
    const map = mapRef.current
    if (!mapReadyRef.current || !map || !window.AMap || !legFrom || !legTo) return

    const AMap = window.AMap

    // 清除上一条路线
    try { routeServiceRef.current?.clear?.() } catch { /* ignore */ }
    routeServiceRef.current = null

    const plugin = modeToPlugin(plan.transportMode)

    AMap.plugin(`AMap.${plugin}`, () => {
      if (!mapRef.current) return
      const m = mapRef.current

      let service: { search: (s: unknown, e: unknown, cb: (status: string) => void) => void; clear?: () => void }
      try {
        if (plugin === 'Transfer') {
          service = new AMap.Transfer({
            map: m,
            city: '北京市',
            policy: AMap.TransferPolicy?.LEAST_TIME ?? 0,
          })
        } else if (plugin === 'Driving') {
          service = new AMap.Driving({ map: m })
        } else if (plugin === 'Riding') {
          service = new AMap.Riding({ map: m })
        } else {
          service = new AMap.Walking({ map: m })
        }
      } catch {
        return
      }

      routeServiceRef.current = service

      const start = new AMap.LngLat(legFrom.lng, legFrom.lat)
      const end = new AMap.LngLat(legTo.lng, legTo.lat)

      service.search(start, end, (status: string) => {
        if (status !== 'complete') return
        // 路线规划成功后，自动适配视野
        window.setTimeout(() => {
          if (!mapRef.current) return
          try {
            mapRef.current.setFitView(undefined, false, [72, 72, 72, 72], 17)
          } catch {
            const midLng = (legFrom.lng + legTo.lng) / 2
            const midLat = (legFrom.lat + legTo.lat) / 2
            mapRef.current.setCenter([midLng, midLat])
            mapRef.current.setZoom(14)
          }
        }, 350)
      })
    })

    // 当前定位蓝点
    AMap.plugin('AMap.Geolocation', () => {
      if (!mapRef.current || !window.AMap) return
      try {
        const geo = new window.AMap.Geolocation({ enableHighAccuracy: true, timeout: 8000 })
        geo.getCurrentPosition(
          (pos: { position: { lng: number; lat: number } }) => {
            if (!mapRef.current || !window.AMap) return
            if (geoMarkerRef.current) {
              try { mapRef.current.remove(geoMarkerRef.current) } catch { /* ignore */ }
            }
            const m = new window.AMap.Marker({
              position: new window.AMap.LngLat(pos.position.lng, pos.position.lat),
              title: '我的位置',
              icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
            })
            mapRef.current.add(m)
            geoMarkerRef.current = m
          },
          () => {},
        )
      } catch { /* 定位不可用时不阻塞 */ }
    })
  }

  // ── 当 leg 变化时重新绘制路线 ──────────────────────────────────────────
  useEffect(() => {
    if (mapReadyRef.current) drawRoute()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [plan.transportMode, legFrom?.lng, legFrom?.lat, legTo?.lng, legTo?.lat, activeLegIndex])

  if (!hasAmapJsKey()) {
    return (
      <div className={`flex min-h-[360px] items-center justify-center rounded-[2rem] border border-dashed bg-white/70 text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        请配置 VITE_AMAP_JS_KEY
      </div>
    )
  }

  return (
    <div className={`relative overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] ${className}`}>
      {/* 地图容器：不加任何遮罩，让瓦片直接可见 */}
      <div ref={containerRef} className="h-full w-full min-h-[360px]" />
      {/* 错误提示：仅在底部小条显示，不遮挡地图 */}
      {initError ? (
        <div className="absolute bottom-3 left-3 right-3 z-10 rounded-xl border border-amber-200/80 bg-amber-50/90 px-3 py-2 text-center font-body text-xs text-amber-800 backdrop-blur-sm">
          {initError}
        </div>
      ) : null}
    </div>
  )
}
