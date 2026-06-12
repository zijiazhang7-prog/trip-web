import { useEffect, useId, useRef, useState } from 'react'
import { hasAmapJsKey } from '../../lib/amap/config'
import { loadAmap } from '../../lib/amap/loader'

export type IndoorMapMarker = {
  id: string
  name: string
  lng: number
  lat: number
  role: 'start' | 'end' | 'poi'
}

type AmapIndoorMapViewProps = {
  indoorPoiId: string
  center: [number, number]
  floor: number
  shopId?: string | null
  polyline?: [number, number][]
  markers?: IndoorMapMarker[]
  zoom?: number
  onBuildingReady?: (info: IndoorBuildingInfo | null) => void
  className?: string
}

export type IndoorBuildingInfo = {
  id: string
  name: string
  floor: number
  floorIndexes: number[]
  floorNames: string[]
}

function isValidCoord(lng: number, lat: number): boolean {
  return Number.isFinite(lng) && Number.isFinite(lat) && Math.abs(lng) <= 180 && Math.abs(lat) <= 90
}

export function AmapIndoorMapView({
  indoorPoiId,
  center,
  floor,
  shopId,
  polyline = [],
  markers = [],
  zoom = 18,
  onBuildingReady,
  className = '',
}: AmapIndoorMapViewProps) {
  const instanceId = useId().replace(/:/g, '')
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<{
    destroy: () => void
    add: (o: unknown | unknown[]) => void
    remove: (o: unknown | unknown[]) => void
    setFitView: (o?: unknown[], immediately?: boolean, avoid?: number[], maxZoom?: number) => void
    setCenter: (c: [number, number]) => void
    setZoom: (z: number) => void
    resize?: () => void
  } | null>(null)
  const indoorMapRef = useRef<{
    showIndoorMap: (id: string, fl?: number, shop?: string) => void
    showFloorBar?: () => void
    showFloor?: (fl: number) => void
    getSelectedBuilding?: () => {
      id?: string
      name?: string
      floor?: number
      floor_details?: {
        floor_indexs?: number[]
        floor_names?: string[]
      }
    }
  } | null>(null)
  const overlaysRef = useRef<unknown[]>([])
  const onBuildingReadyRef = useRef(onBuildingReady)
  const [mapReady, setMapReady] = useState(false)
  const [mapError, setMapError] = useState<string | null>(null)

  useEffect(() => {
    onBuildingReadyRef.current = onBuildingReady
  }, [onBuildingReady])

  const publishBuilding = (indoor: NonNullable<typeof indoorMapRef.current>, fl: number) => {
    const raw = indoor.getSelectedBuilding?.()
    if (!raw?.id) {
      onBuildingReadyRef.current?.(null)
      return
    }
    onBuildingReadyRef.current?.({
      id: raw.id,
      name: raw.name ?? '室内建筑',
      floor: raw.floor ?? fl,
      floorIndexes: raw.floor_details?.floor_indexs ?? [],
      floorNames: raw.floor_details?.floor_names ?? [],
    })
  }

  useEffect(() => {
    if (!hasAmapJsKey()) {
      setMapError('未配置 VITE_AMAP_JS_KEY')
      return undefined
    }
    const container = containerRef.current
    if (!container) return undefined

    let destroyed = false
    setMapError(null)
    setMapReady(false)

    void (async () => {
      try {
        const AMap = await loadAmap()
        if (destroyed) return

        await new Promise<void>((resolve) => {
          AMap.plugin(['AMap.IndoorMap'], () => resolve())
        })
        if (destroyed) return

        const IndoorMapCtor = (AMap as { IndoorMap?: new (opts?: Record<string, unknown>) => unknown })
          .IndoorMap
        const createDefaultLayer = (AMap as { createDefaultLayer?: () => unknown }).createDefaultLayer

        if (!IndoorMapCtor || !createDefaultLayer) {
          setMapError('当前高德 SDK 未加载室内地图插件')
          return
        }

        const indoorMap = new IndoorMapCtor({ alwaysShow: true, hideFloorBar: false }) as NonNullable<
          typeof indoorMapRef.current
        >
        indoorMapRef.current = indoorMap

        const map = new AMap.Map(container, {
          zoom,
          center,
          viewMode: '2D',
          zooms: [16, 20],
          showIndoorMap: false,
          layers: [indoorMap, createDefaultLayer()],
        }) as NonNullable<typeof mapRef.current>

        mapRef.current = map
        ;(indoorMap as { setMap?: (m: unknown) => void }).setMap?.(map)

        indoorMap.showIndoorMap(indoorPoiId, floor, shopId ?? undefined)
        try {
          indoorMap.showFloorBar?.()
        } catch {
          /* ignore */
        }

        publishBuilding(indoorMap, floor)
        window.setTimeout(() => {
          if (!destroyed) {
            map.resize?.()
            setMapReady(true)
            publishBuilding(indoorMap, floor)
          }
        }, 400)
      } catch (err) {
        if (!destroyed) setMapError(err instanceof Error ? err.message : '室内地图加载失败')
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
      indoorMapRef.current = null
      overlaysRef.current = []
      if (container) container.innerHTML = ''
    }
  }, [instanceId, indoorPoiId, center[0], center[1], zoom])

  useEffect(() => {
    const indoor = indoorMapRef.current
    if (!indoor || !mapReady) return
    try {
      indoor.showIndoorMap(indoorPoiId, floor, shopId ?? undefined)
      publishBuilding(indoor, floor)
    } catch {
      /* ignore */
    }
  }, [floor, shopId, indoorPoiId, mapReady])

  useEffect(() => {
    const map = mapRef.current
    const AMap = window.AMap
    if (!map || !AMap || !mapReady) return

    for (const o of overlaysRef.current) {
      try {
        map.remove(o)
      } catch {
        /* ignore */
      }
    }
    overlaysRef.current = []

    const next: unknown[] = []
    const safeLine = polyline.filter(([lng, lat]) => isValidCoord(lng, lat))
    if (safeLine.length >= 2) {
      const line = new AMap.Polyline({
        path: safeLine,
        strokeColor: '#2f7fd4',
        strokeWeight: 6,
        strokeOpacity: 0.9,
        lineJoin: 'round',
      })
      next.push(line)
    }

    for (const m of markers) {
      if (!isValidCoord(m.lng, m.lat)) continue
      const color = m.role === 'start' ? '#c45c26' : m.role === 'end' ? '#2f7fd4' : '#5d7052'
      const marker = new AMap.Marker({
        position: [m.lng, m.lat],
        title: m.name,
        label: {
          content: `<div style="background:${color};color:#fff;padding:2px 6px;border-radius:8px;font-size:11px;">${m.name}</div>`,
          direction: 'top',
        },
      })
      next.push(marker)
    }

    if (next.length) {
      map.add(next)
      overlaysRef.current = next
      if (safeLine.length >= 2) {
        map.setFitView(next, false, [40, 40, 40, 40], 19)
      }
    }
  }, [polyline, markers, mapReady])

  if (!hasAmapJsKey()) {
    return (
      <div className={`flex items-center justify-center bg-[var(--ds-muted)]/30 text-sm text-[var(--ds-muted-foreground)] ${className}`}>
        请配置 VITE_AMAP_JS_KEY 以加载高德室内地图
      </div>
    )
  }

  if (mapError) {
    return (
      <div className={`flex items-center justify-center bg-red-50 p-4 text-sm text-red-700 ${className}`}>
        {mapError}
      </div>
    )
  }

  return (
    <div ref={containerRef} className={`h-full w-full min-h-[280px] ${className}`} data-amap-indoor={instanceId} />
  )
}
