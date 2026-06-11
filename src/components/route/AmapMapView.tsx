import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { BEIJING_CENTER } from '../../types/macroRoute'
import type { RouteWaypoint } from '../../types/macroRoute'
import { getAmapSecurityCode, hasAmapJsKey } from '../../lib/amap/config'
import { loadAmap } from '../../lib/amap/loader'
import { RoutePathSvg } from './RoutePathSvg'

type AmapMapViewProps = {
  waypoints?: RouteWaypoint[]
  polyline?: [number, number][]
  activeId?: string | number | null
  onSelectWaypoint?: (wp: RouteWaypoint) => void
  showGeolocation?: boolean
  className?: string
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function simplifyPolyline(points: [number, number][], max = 500): [number, number][] {
  if (points.length <= max) return points
  const step = Math.ceil(points.length / max)
  return points.filter((_, i) => i % step === 0)
}

function isValidCoord(lng: number, lat: number): boolean {
  return Number.isFinite(lng) && Number.isFinite(lat) && Math.abs(lng) <= 180 && Math.abs(lat) <= 90
}

export function AmapMapView({
  waypoints = [],
  polyline = [],
  activeId,
  onSelectWaypoint,
  showGeolocation = false,
  className = '',
}: AmapMapViewProps) {
  const instanceId = useId()
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<{
    destroy: () => void
    add: (o: unknown | unknown[]) => void
    remove: (o: unknown | unknown[]) => void
    setFitView: (o?: unknown[], immediately?: boolean, avoid?: number[], maxZoom?: number) => void
    setCenter: (center: [number, number]) => void
    setZoom: (zoom: number) => void
    clearMap?: () => void
  } | null>(null)
  const overlaysRef = useRef<unknown[]>([])
  const geoMarkerRef = useRef<unknown | null>(null)
  const [mapError, setMapError] = useState<string | null>(null)
  const [mapReady, setMapReady] = useState(false)

  const stableOnSelect = useCallback(
    (wp: RouteWaypoint) => {
      onSelectWaypoint?.(wp)
    },
    [onSelectWaypoint],
  )

  useEffect(() => {
    if (!hasAmapJsKey() || !containerRef.current) return

    let destroyed = false
    const container = containerRef.current

    ;(async () => {
      await Promise.resolve()
      if (destroyed || !container) return
      try {
        const AMap = await loadAmap()
        if (destroyed || !containerRef.current) return

        if (!getAmapSecurityCode()) {
          setMapError('未配置 VITE_AMAP_SECURITY_CODE，地图瓦片可能无法显示（与后端无关）')
        }

        const map = new AMap.Map(container, {
          zoom: 16,
          center: BEIJING_CENTER,
          viewMode: '2D',
        })
        mapRef.current = map
        window.setTimeout(() => map.resize?.(), 120)
        setMapReady(true)
      } catch (err) {
        setMapError(err instanceof Error ? err.message : '地图加载失败')
      }
    })()

    return () => {
      destroyed = true
      setMapReady(false)
      try {
        const map = mapRef.current
        if (map) {
          map.clearMap?.()
          map.destroy()
        }
      } catch {
        /* 避免 destroy 异常导致 React 崩溃 */
      }
      mapRef.current = null
      overlaysRef.current = []
      geoMarkerRef.current = null
      if (container) container.innerHTML = ''
    }
  }, [instanceId])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !mapReady || !window.AMap) return

    try {
      const AMap = window.AMap
      for (const o of overlaysRef.current) {
        try {
          map.remove(o)
        } catch {
          /* ignore */
        }
      }
      overlaysRef.current = []

      if (geoMarkerRef.current) {
        try {
          map.remove(geoMarkerRef.current)
        } catch {
          /* ignore */
        }
        geoMarkerRef.current = null
      }

      const next: unknown[] = []
      const safeLine = simplifyPolyline(polyline.filter(([lng, lat]) => isValidCoord(lng, lat)))

      if (safeLine.length >= 2) {
        const line = new AMap.Polyline({
          path: safeLine.map(([lng, lat]) => new AMap.LngLat(lng, lat)),
          strokeColor: '#5d7052',
          strokeWeight: 6,
          strokeOpacity: 0.85,
          lineJoin: 'round',
        })
        next.push(line)
      }

      for (const wp of waypoints) {
        if (!isValidCoord(wp.lng, wp.lat)) continue
        const isActive = activeId != null && String(activeId) === String(wp.id)
        const safeName = escapeHtml(wp.name)
        const marker = new AMap.Marker({
          position: new AMap.LngLat(wp.lng, wp.lat),
          title: wp.name,
          label: {
            content: `<div style="padding:2px 6px;border-radius:8px;background:${isActive ? '#5d7052' : '#fff'};color:${isActive ? '#fff' : '#2c2c24'};font-size:11px;border:1px solid #5d7052">${safeName}</div>`,
            direction: 'top',
          },
        })
        if (stableOnSelect) {
          marker.on('click', () => stableOnSelect(wp))
        }
        next.push(marker)
      }

      if (showGeolocation) {
        AMap.plugin('AMap.Geolocation', () => {
          try {
            const geo = new AMap.Geolocation({ enableHighAccuracy: true, timeout: 8000 })
            geo.getCurrentPosition(
              (pos: { position: { lng: number; lat: number } }) => {
                if (!mapRef.current) return
                const m = new AMap.Marker({
                  position: new AMap.LngLat(pos.position.lng, pos.position.lat),
                  title: '我的位置',
                })
                mapRef.current.add(m)
                geoMarkerRef.current = m
              },
              () => {},
            )
          } catch {
            /* 定位不可用时不阻塞页面 */
          }
        })
      }

      if (next.length) {
        map.add(next)
        overlaysRef.current = next
        if (safeLine.length >= 2) {
          map.setFitView(next, false, [48, 48, 48, 48], 17)
        } else if (waypoints.length === 1 && isValidCoord(waypoints[0].lng, waypoints[0].lat)) {
          map.setCenter([waypoints[0].lng, waypoints[0].lat])
          map.setZoom(17)
        } else {
          map.setFitView(next, false, [48, 48, 48, 48], 17)
        }
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : '地图渲染失败'
      queueMicrotask(() => setMapError(msg))
    }
  }, [waypoints, polyline, activeId, mapReady, stableOnSelect, showGeolocation])

  if (!hasAmapJsKey()) {
    const svgNodes = waypoints.map((wp, i) => ({
      nodeId: i + 1,
      nodeName: wp.name,
      lng: wp.lng,
      lat: wp.lat,
    }))
    return (
      <div
        className={`relative overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-[var(--ds-muted)] ${className}`}
      >
        <RoutePathSvg pathNodes={svgNodes} nodeCatalog={svgNodes} className="absolute inset-4" />
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-white/95 to-transparent p-4">
          <p className="font-body text-center text-xs text-[var(--ds-muted-foreground)]">
            配置 <code className="text-[var(--ds-primary)]">VITE_AMAP_JS_KEY</code> 后显示高德地图
          </p>
        </div>
      </div>
    )
  }

  return (
    <div
      className={`relative overflow-hidden rounded-[2rem] border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] ${className}`}
    >
      <div ref={containerRef} className="h-full w-full min-h-[320px]" />
      {mapError ? (
        <div className="absolute inset-0 flex items-center justify-center bg-white/80 p-4 text-center font-body text-sm text-[var(--ds-destructive)]">
          {mapError}
        </div>
      ) : null}
    </div>
  )
}
