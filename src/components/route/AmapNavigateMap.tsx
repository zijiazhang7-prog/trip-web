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
  const routeOverlayRef = useRef<unknown | null>(null)
  const geoMarkerRef = useRef<unknown | null>(null)
  const [mapReady, setMapReady] = useState(false)
  const [mapError, setMapError] = useState<string | null>(null)

  const legFrom = plan.waypoints[activeLegIndex] ?? plan.waypoints[0]
  const legTo = plan.waypoints[activeLegIndex + 1] ?? activeWaypoint ?? plan.waypoints[1]

  // ── 初始化地图（只执行一次） ──────────────────────────────────────────────
  useEffect(() => {
    if (!hasAmapJsKey() || !containerRef.current) return
    let destroyed = false
    const container = containerRef.current

    ;(async () => {
      try {
        const AMap = await loadAmap()
        if (destroyed || !containerRef.current) return
        const map = new AMap.Map(container, {
          zoom: 13,
          center: [legFrom.lng, legFrom.lat],
          viewMode: '2D',
        })
        mapRef.current = map
        window.setTimeout(() => {
          map.resize?.()
          setMapReady(true)
        }, 120)
        if (!getAmapSecurityCode()) {
          setMapError('未配置 VITE_AMAP_SECURITY_CODE，地图瓦片可能无法显示')
        }
      } catch (err) {
        setMapError(err instanceof Error ? err.message : '地图加载失败')
      }
    })()

    return () => {
      destroyed = true
      setMapReady(false)
      try {
        mapRef.current?.destroy()
      } catch {
        /* ignore */
      }
      mapRef.current = null
      if (container) container.innerHTML = ''
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- 仅初始化一次，instanceId 保证组件级唯一
  }, [instanceId])

  // ── 绘制路线（地图就绪 + legFrom/legTo 变化时执行） ─────────────────────
  useEffect(() => {
    if (!mapReady) return
    const map = mapRef.current
    if (!map || !window.AMap || !legFrom || !legTo) return

    const AMap = window.AMap

    // 清除旧路线覆盖物
    if (routeOverlayRef.current) {
      try {
        ;(routeOverlayRef.current as { clear?: () => void }).clear?.()
      } catch {
        /* ignore */
      }
      routeOverlayRef.current = null
    }

    const plugin = modeToPlugin(plan.transportMode)
    const policy = plan.transportMode === 'transit'
      ? (AMap.TransferPolicy?.LEAST_TIME ?? 0)
      : undefined

    AMap.plugin(`AMap.${plugin}`, () => {
      const serviceOpts =
        plugin === 'Transfer'
          ? { map, city: '北京市', policy }
          : { map, policy }

      const service =
        plugin === 'Transfer'
          ? new AMap.Transfer(serviceOpts)
          : plugin === 'Driving'
            ? new AMap.Driving(serviceOpts)
            : plugin === 'Riding'
              ? new AMap.Riding(serviceOpts)
              : new AMap.Walking(serviceOpts)

      const start = new AMap.LngLat(legFrom.lng, legFrom.lat)
      const end = new AMap.LngLat(legTo.lng, legTo.lat)

      service.search(start, end, (status: string) => {
        if (status !== 'complete') {
          setMapError('路线规划失败，请检查网络或 Key 配置')
          return
        }
        setMapError(null)
        routeOverlayRef.current = service
        // 自动适配视野，让整条路线都在屏幕内
        window.setTimeout(() => {
          if (!mapRef.current) return
          try {
            mapRef.current.setFitView(undefined, false, [80, 80, 80, 80], 17)
          } catch {
            // setFitView 在极端情况下可能抛错，降级到居中
            const midLng = (legFrom.lng + legTo.lng) / 2
            const midLat = (legFrom.lat + legTo.lat) / 2
            mapRef.current.setCenter([midLng, midLat])
            mapRef.current.setZoom(14)
          }
        }, 400)
      })
    })

    // 获取用户当前位置并在地图上标记
    AMap.plugin('AMap.Geolocation', () => {
      const geo = new AMap.Geolocation({ enableHighAccuracy: true, timeout: 10000 })
      geo.getCurrentPosition(
        (pos: { position: { lng: number; lat: number } }) => {
          if (!mapRef.current) return
          if (geoMarkerRef.current) {
            try {
              mapRef.current.remove(geoMarkerRef.current)
            } catch {
              /* ignore */
            }
          }
          const m = new AMap.Marker({
            position: new AMap.LngLat(pos.position.lng, pos.position.lat),
            title: '我的位置',
            icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
          })
          mapRef.current.add(m)
          geoMarkerRef.current = m
        },
        () => {},
      )
    })
  }, [mapReady, plan.transportMode, legFrom, legTo, activeLegIndex])

  if (!hasAmapJsKey()) {
    return (
      <div className={`flex min-h-[360px] items-center justify-center rounded-[2rem] border border-dashed bg-white/70 text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        请配置 VITE_AMAP_JS_KEY
      </div>
    )
  }

  return (
    <div className={`relative overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] ${className}`}>
      {!mapReady && (
        <div className="absolute inset-0 z-10 flex items-center justify-center bg-[var(--ds-muted)] rounded-[2rem]">
          <span className="font-body text-sm text-[var(--ds-muted-foreground)]">地图加载中…</span>
        </div>
      )}
      <div ref={containerRef} className="h-full w-full min-h-[360px]" />
      {mapError ? (
        <div className="absolute bottom-3 left-3 right-3 rounded-xl bg-white/90 px-3 py-2 text-center text-xs text-[var(--ds-destructive)]">
          {mapError}
        </div>
      ) : null}
    </div>
  )
}
