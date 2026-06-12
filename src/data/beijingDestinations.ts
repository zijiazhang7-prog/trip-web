import type { RouteWaypoint } from '../types/macroRoute'

/** 北京市内景点 — 离线演示 + 无经纬度时的兜底坐标 */
export type BeijingAttraction = RouteWaypoint & {
  reason: string
  rating: number
  badge: string
  type: string
  destinationId?: number
}

export const BEIJING_ATTRACTIONS: BeijingAttraction[] = [
  {
    id: 'bj-universal',
    name: '北京环球度假区',
    city: '北京',
    lng: 116.681128,
    lat: 39.852226,
    reason: '主题乐园度假区，哈利波特、小黄人等 IP 园区',
    rating: 4.8,
    badge: '热门',
    type: '主题乐园',
    image: 'https://images.unsplash.com/photo-1597466599360-3bb977156764?q=80&w=800',
  },
  {
    id: 'bj-gugong',
    name: '故宫博物院',
    city: '北京',
    lng: 116.397026,
    lat: 39.918058,
    reason: '明清皇家宫殿，世界文化遗产',
    rating: 4.9,
    badge: '必去',
    type: '文化古迹',
    image: 'https://images.unsplash.com/photo-1508804185872-411db1fb6d22?q=80&w=800',
  },
  {
    id: 'bj-tiantan',
    name: '天坛公园',
    city: '北京',
    lng: 116.410829,
    lat: 39.881913,
    reason: '明清皇帝祭天之所，建筑瑰宝',
    rating: 4.8,
    badge: '经典',
    type: '文化古迹',
    image: 'https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?q=80&w=800',
  },
  {
    id: 'bj-yiheyuan',
    name: '颐和园',
    city: '北京',
    lng: 116.275179,
    lat: 39.999617,
    reason: '皇家园林，昆明湖与长廊',
    rating: 4.8,
    badge: '园林',
    type: '自然风光',
    image: 'https://images.unsplash.com/photo-1586034177555-523c0cc1245a?q=80&w=800',
  },
  {
    id: 'bj-nanluo',
    name: '南锣鼓巷',
    city: '北京',
    lng: 116.403145,
    lat: 39.936302,
    reason: '胡同文化与文艺小店',
    rating: 4.6,
    badge: '胡同',
    type: '都市体验',
    image: 'https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?q=80&w=800',
  },
  {
    id: 'bj-798',
    name: '798艺术区',
    city: '北京',
    lng: 116.495645,
    lat: 39.984104,
    reason: '工业风艺术聚落，展览与咖啡',
    rating: 4.7,
    badge: '文艺',
    type: '都市体验',
    image: 'https://images.unsplash.com/photo-1536924940846-227afb31e2a5?q=80&w=800',
  },
  {
    id: 'bj-tiananmen',
    name: '天安门广场',
    city: '北京',
    lng: 116.397455,
    lat: 39.908775,
    reason: '首都地标，庄严宏伟',
    rating: 4.9,
    badge: '地标',
    type: '文化古迹',
    image: 'https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?q=80&w=800',
  },
  {
    id: 'bj-birdnest',
    name: '鸟巢',
    city: '北京',
    lng: 116.388285,
    lat: 39.992806,
    reason: '奥运地标，夜景出众',
    rating: 4.7,
    badge: '奥运',
    type: '都市体验',
    image: 'https://images.unsplash.com/photo-1477959858617-67f85cf4f1df?q=80&w=800',
  },
  {
    id: 'bj-yuanmingyuan',
    name: '圆明园遗址公园',
    city: '北京',
    lng: 116.30096,
    lat: 40.008759,
    reason: '历史遗址与湖光山色',
    rating: 4.6,
    badge: '遗址',
    type: '文化古迹',
    image: 'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?q=80&w=800',
  },
]

export function filterBeijingAttractions(keyword: string): BeijingAttraction[] {
  const q = keyword.trim().toLowerCase()
  if (!q) return BEIJING_ATTRACTIONS
  return BEIJING_ATTRACTIONS.filter(
    (a) =>
      a.name.toLowerCase().includes(q) ||
      a.type.includes(q) ||
      a.reason.includes(q) ||
      a.badge.includes(q),
  )
}
