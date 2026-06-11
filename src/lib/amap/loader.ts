import { getAmapJsKey, getAmapSecurityCode } from './config'

type AMapNS = typeof window extends { AMap: infer T } ? T : never

let loadPromise: Promise<AMapNS> | null = null

export function loadAmap(): Promise<AMapNS> {
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('地图仅可在浏览器中加载'))
  }
  if (window.AMap) return Promise.resolve(window.AMap)

  if (!loadPromise) {
    const key = getAmapJsKey()
    if (!key) return Promise.reject(new Error('未配置 VITE_AMAP_JS_KEY（Web 端 JS API Key）'))

    const sec = getAmapSecurityCode()
    if (sec) {
      window._AMapSecurityConfig = { securityJsCode: sec }
    }

    loadPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.Driving,AMap.Walking,AMap.Riding,AMap.Geocoder,AMap.PlaceSearch,AMap.Geolocation`
      script.async = true
      script.onload = () => {
        if (window.AMap) resolve(window.AMap)
        else reject(new Error('高德 JS API 加载失败'))
      }
      script.onerror = () => reject(new Error('高德脚本网络错误'))
      document.head.appendChild(script)
    })
  }

  return loadPromise
}
