import { useCallback, useEffect, useId, useRef, useState } from 'react'
import { BEIJING_CENTER, BEIJING_DEFAULT_ZOOM } from '../../types/macroRoute'
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
  return (
    Number.isFinite(lng) &&
    Number.isFinite(lat) &&
    Math.abs(lng) <= 180 &&
    Math.abs(lat) <= 90 &&
    !(lng === 0 && lat === 0)
  )
}

const SINGLE_POINT_ZOOM = 16
const ROUTE_MAX_ZOOM = 15

function applyMapViewport(
  map: {
    setCenter: (center: [number, number]) => void
    setZoom: (zoom: number) => void
    setFitView: (o?: unknown[], immediately?: boolean, avoid?: number[], maxZoom?: number) => void
    getZoom?: () => number
  },
  validWaypoints: RouteWaypoint[],
  safeLine: [number, number][],
  overlays: unknown[],
): void {
  if (safeLine.length >= 2 && overlays.length > 0) {
    map.setFitView(overlays, false, [48, 48, 48, 48], ROUTE_MAX_ZOOM)
    window.setTimeout(() => {
      const z = map.getZoom?.()
      if (typeof z === 'number' && z < 11) map.setZoom(11)
    }, 150)
    return
  }

  if (validWaypoints.length >= 2 && overlays.length > 0) {
    map.setFitView(overlays, false, [48, 48, 48, 48], ROUTE_MAX_ZOOM)
    window.setTimeout(() => {
      const z = map.getZoom?.()
      if (typeof z === 'number' && z < 11) map.setZoom(11)
    }, 150)
    return
  }

  if (validWaypoints.length === 1) {
    map.setCenter([validWaypoints[0].lng, validWaypoints[0].lat])
    map.setZoom(SINGLE_POINT_ZOOM)
    return
  }

  map.setCenter(BEIJING_CENTER)
  map.setZoom(BEIJING_DEFAULT_ZOOM)
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
    getZoom?: () => number
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
          zoom: BEIJING_DEFAULT_ZOOM,
          center: BEIJING_CENTER,
          viewMode: '2D',
          zooms: [10, 18],
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

      const validWaypoints = waypoints.filter((wp) => isValidCoord(wp.lng, wp.lat))

      if (next.length) {
        map.add(next)
        overlaysRef.current = next
      }

      applyMapViewport(map, validWaypoints, safeLine, next)
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
      {/* 地图容器：不加覆盖层，让瓦片保持可见 */}
      <div ref={containerRef} className="h-full w-full min-h-[320px]" />
      {/* 错误/警告仅在底部小条展示，不遮挡瓦片 */}
      {mapError ? (
        <div className="absolute bottom-3 left-3 right-3 z-10 rounded-xl border border-amber-200/80 bg-amber-50/90 px-3 py-2 text-center font-body text-xs text-amber-800 backdrop-blur-sm">
          {mapError}
        </div>
      ) : null}
    </div>
  )
}
