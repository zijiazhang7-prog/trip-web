/** 判断是否为北京市范围内的有效经纬度 */
export function isBeijingAreaCoord(lng: number, lat: number): boolean {
  return lng >= 115.4 && lng <= 117.6 && lat >= 39.3 && lat <= 41.2
}

export function scatterAroundCenter(
  center: { lng: number; lat: number },
  index: number,
  total: number,
): { lng: number; lat: number } {
  if (total <= 1) return center
  const angle = (index / total) * Math.PI * 2
  const r = 0.008 + (index % 3) * 0.003
  return {
    lng: center.lng + Math.cos(angle) * r,
    lat: center.lat + Math.sin(angle) * r,
  }
}

export const BEIJING_DEFAULT_CENTER = { lng: 116.397428, lat: 39.90923 }

/** 景区道路图示意坐标（非 GPS，用于 RoutePathSvg / ScenicMapView） */
export const SCHEMATIC_MAP_CENTER = { lng: 500, lat: 400 }

export function isSchematicMapCoord(lng: number, lat: number): boolean {
  return (
    Number.isFinite(lng) &&
    Number.isFinite(lat) &&
    !(lng === 0 && lat === 0) &&
    !isBeijingAreaCoord(lng, lat) &&
    lng >= -80 &&
    lng <= 3200 &&
    lat >= -80 &&
    lat <= 3200
  )
}
