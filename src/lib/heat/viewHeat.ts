const STORAGE_KEY = 'trip_view_heat_bumps'

type BumpMap = Record<string, number>

function readBumps(): BumpMap {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as BumpMap) : {}
  } catch {
    return {}
  }
}

function writeBumps(map: BumpMap): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(map))
  } catch {
    /* ignore quota */
  }
}

function bumpKey(type: 'destination' | 'food' | 'diary', id: number): string {
  return `${type}:${id}`
}

/** 客户端浏览量 +1（与后端 heatScore 叠加展示；日记详情 GET 会由后端自增） */
export function recordClientView(type: 'destination' | 'food' | 'diary', id: number): number {
  if (!Number.isFinite(id) || id <= 0) return 0
  const map = readBumps()
  const key = bumpKey(type, id)
  map[key] = (map[key] ?? 0) + 1
  writeBumps(map)
  return map[key]
}

export function getClientViewBump(type: 'destination' | 'food' | 'diary', id: number): number {
  return readBumps()[bumpKey(type, id)] ?? 0
}

export function displayHeatScore(
  serverHeat: number | null | undefined,
  type: 'destination' | 'food' | 'diary',
  id: number | null | undefined,
): number {
  const base = Math.max(0, Math.round(serverHeat ?? 0))
  if (id == null || id <= 0) return base
  return base + getClientViewBump(type, id)
}
