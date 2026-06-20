/** 解析单段 polyline：支持 "lng,lat;lng,lat" 明文或编码串 */
export function parseAmapStepPolyline(raw: string): [number, number][] {
  if (!raw?.trim()) return []
  if (raw.includes(';')) {
    const coords: [number, number][] = []
    for (const pair of raw.split(';')) {
      const [lng, lat] = pair.split(',').map(Number)
      if (Number.isFinite(lng) && Number.isFinite(lat)) coords.push([lng, lat])
    }
    if (coords.length >= 2) return coords
  }
  return decodeAmapPolyline(raw)
}

/** 解码高德返回的 polyline 编码串为 [lng, lat][] */
export function decodeAmapPolyline(encoded: string): [number, number][] {
  const coords: [number, number][] = []
  let index = 0
  let lat = 0
  let lng = 0

  while (index < encoded.length) {
    let shift = 0
    let result = 0
    let byte: number
    do {
      byte = encoded.charCodeAt(index++) - 63
      result |= (byte & 0x1f) << shift
      shift += 5
    } while (byte >= 0x20)
    const deltaLat = result & 1 ? ~(result >> 1) : result >> 1
    lat += deltaLat

    shift = 0
    result = 0
    do {
      byte = encoded.charCodeAt(index++) - 63
      result |= (byte & 0x1f) << shift
      shift += 5
    } while (byte >= 0x20)
    const deltaLng = result & 1 ? ~(result >> 1) : result >> 1
    lng += deltaLng

    coords.push([lng / 1e5, lat / 1e5])
  }

  return coords
}
