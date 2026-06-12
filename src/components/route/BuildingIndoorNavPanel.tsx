import { useCallback, useEffect, useMemo, useState } from 'react'
import { CHAOYANG_JOY_CITY } from '../../data/indoor/amapIndoorMall'
import { hasAmapWebKey } from '../../lib/amap/config'
import {
  fetchAroundPois,
  fetchDirectionLeg,
  searchTextPois,
  type NearbyPoiResult,
} from '../../lib/amap/webService'
import { AmapIndoorMapView, type IndoorBuildingInfo } from './AmapIndoorMapView'

const MALL = CHAOYANG_JOY_CITY

type MallPoi = NearbyPoiResult & { shopId?: string }

function formatDistance(m: number): string {
  if (m < 1000) return `${Math.round(m)} 米`
  return `${(m / 1000).toFixed(1)} 公里`
}

function formatDuration(sec: number): string {
  if (sec < 60) return `约 ${sec} 秒`
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return s > 0 ? `约 ${m} 分 ${s} 秒` : `约 ${m} 分钟`
}

export function BuildingIndoorNavPanel() {
  const [floor, setFloor] = useState(MALL.defaultFloor)
  const [buildingInfo, setBuildingInfo] = useState<IndoorBuildingInfo | null>(null)
  const [shops, setShops] = useState<MallPoi[]>([])
  const [keyword, setKeyword] = useState('')
  const [searching, setSearching] = useState(false)
  const [startId, setStartId] = useState('')
  const [endId, setEndId] = useState('')
  const [planning, setPlanning] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [polyline, setPolyline] = useState<[number, number][]>([])
  const [routeSummary, setRouteSummary] = useState<{ distance: number; duration: number } | null>(null)
  const [steps, setSteps] = useState<string[]>([])

  const handleBuildingReady = useCallback((info: IndoorBuildingInfo | null) => {
    setBuildingInfo(info)
    if (info?.floor != null) setFloor(info.floor)
  }, [])

  const loadDefaultShops = useCallback(async () => {
    setSearching(true)
    setError(null)
    try {
      const [around, named] = await Promise.all([
        fetchAroundPois(MALL.center[0], MALL.center[1], 'shop', 600),
        searchTextPois(`${MALL.name} 商铺`, '北京', 15),
      ])
      const merged = new Map<string, MallPoi>()
      for (const p of [...around, ...named]) {
        if (!Number.isFinite(p.lng) || !Number.isFinite(p.lat)) continue
        merged.set(p.id, { ...p, shopId: p.id })
      }
      const list = [...merged.values()].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
      setShops(list)
      setStartId((prev) => prev || list[0]?.id || '')
      setEndId((prev) => prev || list[1]?.id || list[0]?.id || '')
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载商场商铺失败')
      setShops([])
    } finally {
      setSearching(false)
    }
  }, [])

  useEffect(() => {
    void loadDefaultShops()
  }, [loadDefaultShops])

  const floorOptions = useMemo(() => {
    if (buildingInfo?.floorIndexes?.length) {
      return buildingInfo.floorIndexes.map((idx, i) => ({
        value: idx,
        label: buildingInfo.floorNames[i] ?? `${idx} 层`,
      }))
    }
    return [1, 2, 3, 4, 5, 6].map((f) => ({ value: f, label: `${f} 层` }))
  }, [buildingInfo])

  const shopOptions = useMemo(
    () => shops.map((s) => ({ id: s.id, label: s.name, poi: s })),
    [shops],
  )

  const startPoi = shops.find((s) => s.id === startId)
  const endPoi = shops.find((s) => s.id === endId)

  const markers = useMemo(() => {
    const out: { id: string; name: string; lng: number; lat: number; role: 'start' | 'end' }[] = []
    if (startPoi) out.push({ id: startPoi.id, name: startPoi.name, lng: startPoi.lng, lat: startPoi.lat, role: 'start' })
    if (endPoi && endPoi.id !== startPoi?.id) {
      out.push({ id: endPoi.id, name: endPoi.name, lng: endPoi.lng, lat: endPoi.lat, role: 'end' })
    }
    return out
  }, [startPoi, endPoi])

  const handleSearch = async () => {
    const q = keyword.trim()
    if (!q) {
      void loadDefaultShops()
      return
    }
    setSearching(true)
    setError(null)
    try {
      const results = await searchTextPois(`${MALL.name} ${q}`, '北京', 20)
      setShops(results.map((p) => ({ ...p, shopId: p.id })))
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索失败')
    } finally {
      setSearching(false)
    }
  }

  const handlePlan = async () => {
    if (!startPoi || !endPoi) {
      setError('请选择起点与终点商铺')
      return
    }
    if (!hasAmapWebKey()) {
      setError('未配置 VITE_AMAP_WEB_KEY，无法规划步行路线')
      return
    }
    setPlanning(true)
    setError(null)
    try {
      const leg = await fetchDirectionLeg(
        'walking',
        { lng: startPoi.lng, lat: startPoi.lat },
        { lng: endPoi.lng, lat: endPoi.lat },
      )
      setPolyline(leg.polyline)
      setRouteSummary({ distance: leg.distance, duration: leg.duration })
      setSteps((leg.steps ?? []).map((s) => s.instruction).filter(Boolean) as string[])
    } catch (err) {
      setPolyline([])
      setRouteSummary(null)
      setSteps([])
      setError(err instanceof Error ? err.message : '室内步行路线规划失败')
    } finally {
      setPlanning(false)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <p className="font-body text-sm leading-relaxed text-[var(--ds-muted-foreground)]">
        使用高德官方室内地图（<strong className="text-[var(--ds-foreground)]">{MALL.name}</strong>，
        POI <code className="text-xs">{MALL.indoorPoiId}</code>）：展示真实商场楼层与商铺，步行路线由高德路径规划生成。
      </p>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(240px,34%)]">
        <div className="space-y-3">
          <div className="flex flex-wrap gap-2">
            {floorOptions.map((f) => (
              <button
                key={f.value}
                type="button"
                onClick={() => setFloor(f.value)}
                className={`rounded-full px-3 py-1 text-xs font-semibold ${
                  floor === f.value
                    ? 'bg-[var(--ds-primary)] text-white'
                    : 'border border-[var(--ds-primary)]/20 text-[var(--ds-primary)]'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          <div className="h-[min(52vh,480px)] overflow-hidden rounded-xl border border-[var(--ds-border)]/50 bg-white">
            <AmapIndoorMapView
              indoorPoiId={MALL.indoorPoiId}
              center={MALL.center}
              floor={floor}
              shopId={endPoi?.shopId ?? endPoi?.id}
              polyline={polyline}
              markers={markers}
              onBuildingReady={handleBuildingReady}
              className="h-full min-h-[280px]"
            />
          </div>

          {buildingInfo ? (
            <p className="text-[10px] text-[var(--ds-muted-foreground)]">
              已加载室内建筑：{buildingInfo.name}（当前 {buildingInfo.floor} 层）
            </p>
          ) : (
            <p className="text-[10px] text-[var(--ds-muted-foreground)]">
              地图右下角可切换楼层；若室内图未显示请放大地图或检查 Key 权限。
            </p>
          )}
        </div>

        <div className="flex flex-col gap-3 rounded-xl border border-[var(--ds-border)]/40 bg-[color-mix(in_srgb,var(--ds-background)_60%,white)] p-4">
          <div>
            <p className="mb-1 font-body text-xs font-semibold">搜索商铺</p>
            <div className="flex gap-2">
              <input
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && void handleSearch()}
                placeholder={MALL.searchHint}
                className="min-w-0 flex-1 rounded-lg border border-[var(--ds-border)] px-2 py-1.5 text-xs"
              />
              <button
                type="button"
                disabled={searching}
                onClick={() => void handleSearch()}
                className="shrink-0 rounded-full bg-[var(--ds-primary)] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
              >
                搜
              </button>
            </div>
          </div>

          <label className="font-body text-xs font-semibold">
            起点商铺
            <select
              value={startId}
              onChange={(e) => setStartId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-[var(--ds-border)] bg-white px-2 py-1.5 text-xs"
            >
              {shopOptions.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>

          <label className="font-body text-xs font-semibold">
            终点商铺
            <select
              value={endId}
              onChange={(e) => setEndId(e.target.value)}
              className="mt-1 w-full rounded-lg border border-[var(--ds-border)] bg-white px-2 py-1.5 text-xs"
            >
              {shopOptions.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>

          <button
            type="button"
            disabled={planning || shops.length === 0}
            onClick={() => void handlePlan()}
            className="w-full rounded-full bg-[var(--ds-primary)] py-2 text-xs font-semibold text-white disabled:opacity-50"
          >
            {planning ? '规划中…' : '规划商场内步行路线'}
          </button>

          {routeSummary ? (
            <div className="border-t border-[var(--ds-border)]/40 pt-3">
              <p className="mb-2 text-xs font-semibold text-[var(--ds-primary)]">
                步行 {formatDistance(routeSummary.distance)} · {formatDuration(routeSummary.duration)}
              </p>
              <ol className="max-h-[min(28vh,240px)] space-y-1.5 overflow-y-auto pr-1 text-xs text-[var(--ds-muted-foreground)]">
                {steps.map((s, i) => (
                  <li key={`${i}-${s.slice(0, 12)}`}>
                    {i + 1}. {s}
                  </li>
                ))}
              </ol>
            </div>
          ) : null}

          {error ? <p className="text-xs text-red-700">{error}</p> : null}
          {searching ? <p className="text-[10px] text-[var(--ds-muted-foreground)]">正在加载商铺…</p> : null}
        </div>
      </div>
    </div>
  )
}
