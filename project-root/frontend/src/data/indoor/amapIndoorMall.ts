/**
 * 高德官方室内地图已收录商场（POI ID 来自高德 JS API 2.0 官方文档示例）
 * @see https://lbs.amap.com/api/jsapi-v2/guide/layers/official-layers
 */
export type AmapIndoorMallVenue = {
  id: string
  name: string
  address: string
  /** 建筑物 POI ID（indoorid） */
  indoorPoiId: string
  center: [number, number]
  defaultFloor: number
  searchHint: string
}

/** 朝阳大悦城 — 高德室内地图示范建筑之一 */
export const CHAOYANG_JOY_CITY: AmapIndoorMallVenue = {
  id: 'chaoyang-joy-city',
  name: '朝阳大悦城',
  address: '北京市朝阳区朝阳北路101号',
  indoorPoiId: 'B000A8VT15',
  center: [116.518355, 39.92352],
  defaultFloor: 1,
  searchHint: '餐饮、服饰、影院等（商场内商铺）',
}
