import { useEffect, useId, useRef, useState } from 'react'
import type { MacroRoutePlan, RouteWaypoint, TransportMode } from '../../types/macroRoute'
import { hasAmapJsKey } from '../../lib/amap/config'
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
  const mapRef = useRef<{ destroy: () => void; add: (o: unknown) => void; remove: (o: unknown) => void; setFitView: (o?: unknown[]) => void } | null>(null)
  const routeOverlayRef = useRef<unknown | null>(null)
  const geoMarkerRef = useRef<unknown | null>(null)
  const [mapError, setMapError] = useState<string | null>(null)

  const legFrom = plan.waypoints[activeLegIndex] ?? plan.waypoints[0]
  const legTo = plan.waypoints[activeLegIndex + 1] ?? activeWaypoint ?? plan.waypoints[1]

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
      } catch (err) {
        setMapError(err instanceof Error ? err.message : '地图加载失败')
      }
    })()

    return () => {
      destroyed = true
      try {
        mapRef.current?.destroy()
      } catch {
        /* ignore */
      }
      mapRef.current = null
      if (container) container.innerHTML = ''
    }
  }, [instanceId])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !window.AMap || !legFrom || !legTo) return

    const AMap = window.AMap
    if (routeOverlayRef.current) {
      try {
        map.remove(routeOverlayRef.current)
      } catch {
        /* ignore */
      }
      routeOverlayRef.current = null
    }

    const plugin = modeToPlugin(plan.transportMode)
    AMap.plugin(`AMap.${plugin}`, () => {
      const policy =
        plan.transportMode === 'transit'
          ? AMap.TransferPolicy?.LEAST_TIME ?? 0
          : undefined

      const service =
        plugin === 'Transfer'
          ? new AMap.Transfer({ map, city: '北京市', policy })
          : plugin === 'Driving'
            ? new AMap.Driving({ map, policy })
            : plugin === 'Riding'
              ? new AMap.Riding({ map, policy })
              : new AMap.Walking({ map, policy })

      const start = new AMap.LngLat(legFrom.lng, legFrom.lat)
      const end = new AMap.LngLat(legTo.lng, legTo.lat)

      service.search(start, end, (status: string) => {
          if (status !== 'complete') {
            setMapError('路线规划失败，请检查网络或 Key 配置')
          } else {
            setMapError(null)
            routeOverlayRef.current = service
          }
        },
      )
    })

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
  }, [plan.transportMode, legFrom, legTo, activeLegIndex])

  if (!hasAmapJsKey()) {
    return (
      <div className={`flex min-h-[360px] items-center justify-center rounded-[2rem] border border-dashed bg-white/70 text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        请配置 VITE_AMAP_JS_KEY
      </div>
    )
  }

  return (
    <div className={`relative overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] ${className}`}>
      <div ref={containerRef} className="h-full w-full min-h-[360px]" />
      {mapError ? (
        <div className="absolute bottom-3 left-3 right-3 rounded-xl bg-white/90 px-3 py-2 text-center text-xs text-[var(--ds-destructive)]">
          {mapError}
        </div>
      ) : null}
    </div>
  )
}
