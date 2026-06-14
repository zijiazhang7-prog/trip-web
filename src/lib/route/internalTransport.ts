import type { RouteTransportType } from '../../api/route'

export type VenueNavKind = 'campus' | 'scenic' | 'unknown'

export type InternalTransportOption = {
  value: RouteTransportType
  label: string
  hint?: string
}

/** 根据目的地名称/类型推断校园或景区，用于切换交通方式选项 */
export function detectVenueNavKind(name?: string, destType?: string): VenueNavKind {
  const hay = `${name ?? ''} ${destType ?? ''}`.toLowerCase()
  if (/校园|大学|学院|school|campus|校区/.test(hay)) return 'campus'
  if (/景区|景点|公园|博物|度假|影城|旅游|scenic|森林|湿地/.test(hay)) return 'scenic'
  return 'unknown'
}

/** 校园：步行+骑行+混合；景区：步行+电瓶车+混合（对接后端 walk/bike/cart/mixed） */
export function internalTransportOptions(kind: VenueNavKind): InternalTransportOption[] {
  if (kind === 'campus') {
    return [
      { value: 'walk', label: '步行' },
      { value: 'bike', label: '骑行' },
      { value: 'mixed', label: '混合最优', hint: '步行与骑行组合的最短时间' },
    ]
  }
  if (kind === 'scenic') {
    return [
      { value: 'walk', label: '步行' },
      { value: 'cart', label: '电瓶车' },
      { value: 'mixed', label: '混合最优', hint: '步行与电瓶车组合的最短时间' },
    ]
  }
  return [
    { value: 'walk', label: '步行' },
    { value: 'bike', label: '骑行' },
    { value: 'cart', label: '电瓶车' },
    { value: 'mixed', label: '混合最优' },
  ]
}

export function normalizeInternalTransport(
  current: RouteTransportType,
  options: InternalTransportOption[],
): RouteTransportType {
  if (options.some((o) => o.value === current)) return current
  return options[0]?.value ?? 'walk'
}
