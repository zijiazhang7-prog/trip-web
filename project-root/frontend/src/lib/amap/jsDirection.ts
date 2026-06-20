import { hasAmapJsKey } from './config'
import { loadAmap } from './loader'
import type { LegResult } from './webService'

type DirectionMode = 'driving' | 'walking' | 'bicycling'

type JsRouteStep = {
  instruction?: string
  distance?: number
  path?: Array<{ lng: number; lat: number } | [number, number]>
}

type JsRouteResult = {
  routes?: Array<{
    distance?: number
    time?: number
    steps?: JsRouteStep[]
  }>
}

function modeToPlugin(mode: DirectionMode): 'Driving' | 'Riding' | 'Walking' {
  if (mode === 'driving') return 'Driving'
  if (mode === 'bicycling') return 'Riding'
  return 'Walking'
}

function pathFromSteps(steps: JsRouteStep[] | undefined): [number, number][] {
  const polyline: [number, number][] = []
  for (const step of steps ?? []) {
    for (const pt of step.path ?? []) {
      if (Array.isArray(pt)) {
        polyline.push([pt[0], pt[1]])
      } else if (pt && typeof pt === 'object' && 'lng' in pt && 'lat' in pt) {
        polyline.push([pt.lng, pt.lat])
      }
    }
  }
  return polyline
}

/** 浏览器端高德 JS API 沿路规划（不依赖 Web 服务 Key） */
export async function fetchDirectionLegViaJsApi(
  mode: DirectionMode,
  origin: { lng: number; lat: number },
  destination: { lng: number; lat: number },
): Promise<LegResult> {
  if (!hasAmapJsKey()) throw new Error('未配置 VITE_AMAP_JS_KEY')

  const AMap = await loadAmap()
  const plugin = modeToPlugin(mode)

  return new Promise((resolve, reject) => {
    AMap.plugin(`AMap.${plugin}`, () => {
      let service: { search: (s: unknown, e: unknown, cb: (status: string, result?: unknown) => void) => void }
      try {
        if (plugin === 'Driving') {
          service = new AMap.Driving({}) as typeof service
        } else if (plugin === 'Riding') {
          service = new AMap.Riding({}) as typeof service
        } else {
          service = new AMap.Walking({}) as typeof service
        }
      } catch (err) {
        reject(err instanceof Error ? err : new Error('高德路线服务初始化失败'))
        return
      }

      const start = new AMap.LngLat(origin.lng, origin.lat)
      const end = new AMap.LngLat(destination.lng, destination.lat)

      service.search(start, end, (status, raw) => {
        const result = raw as JsRouteResult | undefined
        if (status !== 'complete') {
          reject(new Error('高德沿路规划失败'))
          return
        }
        const route = result?.routes?.[0]
        const polyline = pathFromSteps(route?.steps)
        if (polyline.length < 2) {
          reject(new Error('未获取到沿路折线'))
          return
        }
        resolve({
          distance: Number(route?.distance) || 0,
          duration: Number(route?.time) || 0,
          polyline,
          instruction: route?.steps?.[0]?.instruction,
        })
      })
    })
  })
}
