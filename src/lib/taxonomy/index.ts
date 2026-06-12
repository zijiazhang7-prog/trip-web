import taxonomyJson from '../../data/taxonomy.json'
import type { Destination } from '../../data/siteData'
import type { DestinationVO } from '../../api/destination'

export type TaxonomyConfig = typeof taxonomyJson

export const TAXONOMY = taxonomyJson

export type ResolvedDestinationTags = {
  destType: string | null
  interests: Set<string>
  rawTags: string[]
}

export type UserTagSelection = {
  destTypes: string[]
  interestTags: string[]
  cuisineTags: string[]
}

export function emptyTagSelection(): UserTagSelection {
  return { destTypes: [], interestTags: [], cuisineTags: [] }
}

export function isTagSelectionEmpty(sel: UserTagSelection): boolean {
  return sel.destTypes.length === 0 && sel.interestTags.length === 0 && sel.cuisineTags.length === 0
}

export function mergeTagSelections(...parts: UserTagSelection[]): UserTagSelection {
  const destTypes = new Set<string>()
  const interestTags = new Set<string>()
  const cuisineTags = new Set<string>()
  for (const p of parts) {
    p.destTypes.forEach((t) => destTypes.add(t))
    p.interestTags.forEach((t) => interestTags.add(t))
    p.cuisineTags.forEach((t) => cuisineTags.add(t))
  }
  return {
    destTypes: [...destTypes],
    interestTags: [...interestTags],
    cuisineTags: [...cuisineTags],
  }
}

/** 解析 tag_json：JSON 数组或管道分隔 */
export function parseRawTags(tagJson: string | undefined | null): string[] {
  if (!tagJson?.trim()) return []
  const trimmed = tagJson.trim()
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed) as unknown
      if (Array.isArray(parsed)) {
        return parsed.map((t) => String(t).trim()).filter(Boolean)
      }
    } catch {
      /* fall through */
    }
  }
  if (trimmed.includes('|')) {
    return trimmed
      .split('|')
      .map((t) => t.trim())
      .filter(Boolean)
  }
  return [trimmed]
}

export function resolveDestinationTags(input: {
  category?: string | null
  tagJson?: string | null
  tags?: string[] | null
}): ResolvedDestinationTags {
  const category = input.category?.trim() ?? ''
  const rawTags = input.tags?.length ? [...input.tags] : parseRawTags(input.tagJson)
  const destType = category ? (TAXONOMY.destTypeByCategory[category as keyof typeof TAXONOMY.destTypeByCategory] ?? null) : null

  const interests = new Set<string>()
  const defaults = category
    ? TAXONOMY.defaultInterestByCategory[category as keyof typeof TAXONOMY.defaultInterestByCategory]
    : undefined
  if (defaults) defaults.forEach((t) => interests.add(t))

  for (const tag of rawTags) {
    const mapped = TAXONOMY.interestByTagKeyword[tag as keyof typeof TAXONOMY.interestByTagKeyword]
    if (mapped) interests.add(mapped)
  }

  return { destType, interests, rawTags }
}

export function resolveDestinationFromVO(vo: DestinationVO): ResolvedDestinationTags {
  return resolveDestinationTags({
    category: vo.category ?? vo.type,
    tags: vo.tags,
  })
}

export function resolveDestinationFromCard(dest: Destination): ResolvedDestinationTags {
  if (dest.taxonomy?.destType != null || dest.taxonomy?.interestTags?.length) {
    return {
      destType: dest.taxonomy.destType ?? null,
      interests: new Set(dest.taxonomy.interestTags ?? []),
      rawTags: dest.taxonomy.apiTags ?? [],
    }
  }
  return resolveDestinationTags({
    category: dest.taxonomy?.apiCategory ?? dest.badge ?? dest.type,
    tags: dest.taxonomy?.apiTags,
  })
}

export function resolveCuisine(foodType: string | undefined | null): string | null {
  if (!foodType?.trim()) return null
  return TAXONOMY.cuisineByFoodType[foodType as keyof typeof TAXONOMY.cuisineByFoodType] ?? null
}

export function foodTypesForCuisines(cuisineTags: string[]): string[] {
  const out = new Set<string>()
  for (const cuisine of cuisineTags) {
    const rows = TAXONOMY.foodTypeByCuisine[cuisine as keyof typeof TAXONOMY.foodTypeByCuisine]
    if (rows) rows.forEach((r) => out.add(r))
  }
  return [...out]
}

export function scoreDestination(tags: ResolvedDestinationTags, sel: UserTagSelection): number {
  if (isTagSelectionEmpty(sel)) return 0
  let score = 0
  const w = TAXONOMY.scoringWeights
  if (tags.destType && sel.destTypes.includes(tags.destType)) {
    score += w.destTypeMatch
  }
  for (const interest of tags.interests) {
    if (sel.interestTags.includes(interest)) score += w.interestMatch
  }
  return score
}

function foodSearchableText(food: {
  name?: string | null
  dish?: string | null
  foodType?: string | null
  tags: string[]
}): string {
  return [food.name, food.dish, food.foodType, ...food.tags]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

/** 与后端 TaxonomyService.resolveFood 对齐：关键词 + foodType 映射 */
export function resolveFoodCuisineTags(food: {
  cuisineTags?: string[] | null
  cuisineTag?: string | null
  foodType?: string | null
  name?: string | null
  dish?: string | null
  tags: string[]
}): string[] {
  const out = new Set<string>()
  for (const t of food.cuisineTags ?? []) {
    if (t?.trim()) out.add(t.trim())
  }
  if (food.cuisineTag?.trim()) out.add(food.cuisineTag.trim())
  if (food.foodType?.trim()) {
    const mapped = resolveCuisine(food.foodType)
    if (mapped) out.add(mapped)
    if (TAXONOMY.cuisineTags.includes(food.foodType as (typeof TAXONOMY.cuisineTags)[number])) {
      out.add(food.foodType)
    }
  }
  for (const t of food.tags) {
    const mapped = resolveCuisine(t)
    if (mapped) out.add(mapped)
    if (TAXONOMY.cuisineTags.includes(t as (typeof TAXONOMY.cuisineTags)[number])) out.add(t)
  }
  const hay = foodSearchableText(food)
  const keywords = TAXONOMY.cuisineKeywords as Record<string, string[]> | undefined
  if (keywords && hay) {
    for (const [cuisine, kws] of Object.entries(keywords)) {
      if (kws.some((kw) => kw && hay.includes(kw.toLowerCase()))) out.add(cuisine)
    }
  }
  return [...out]
}

export function resolveFoodCuisineTag(food: {
  cuisineTags?: string[] | null
  cuisineTag?: string | null
  foodType?: string | null
  name?: string | null
  dish?: string | null
  tags: string[]
}): string | null {
  const tags = resolveFoodCuisineTags(food)
  return tags[0] ?? null
}

export function scoreFood(cuisineTag: string | null | undefined, sel: UserTagSelection): number {
  if (!cuisineTag || sel.cuisineTags.length === 0) return 0
  if (sel.cuisineTags.includes(cuisineTag)) return TAXONOMY.scoringWeights.cuisineMatch
  for (const selected of sel.cuisineTags) {
    const dbTypes =
      TAXONOMY.foodTypeByCuisine[selected as keyof typeof TAXONOMY.foodTypeByCuisine] ?? []
    if (dbTypes.includes(cuisineTag)) return Math.floor(TAXONOMY.scoringWeights.cuisineMatch * 0.85)
  }
  return 0
}

export type FoodSortable = {
  id?: number
  name?: string
  dish?: string
  tags: string[]
  cuisineTags?: string[] | null
  cuisineTag?: string | null
  foodType?: string | null
  heatScore?: number
  ratingScore?: number
  rating?: string
}

export function foodMatchesCuisineTag(food: FoodSortable, cuisineTag: string): boolean {
  if (!cuisineTag.trim()) return true
  return resolveFoodCuisineTags(food).includes(cuisineTag)
}

export function scoreFoodItem(food: FoodSortable, sel: UserTagSelection): number {
  if (!sel.cuisineTags.length) return 0
  const resolved = resolveFoodCuisineTags(food)
  let score = 0
  for (const selected of sel.cuisineTags) {
    if (resolved.includes(selected)) score += TAXONOMY.scoringWeights.cuisineMatch
  }
  return score
}

export function sortDestinationsByTagMatch<T extends Destination>(
  list: T[],
  sel: UserTagSelection,
  aiOrderIds?: number[],
): T[] {
  const aiOrder = aiOrderIds?.length ? new Map(aiOrderIds.map((id, i) => [id, i])) : null
  return [...list].sort((a, b) => {
    const ta = resolveDestinationFromCard(a)
    const tb = resolveDestinationFromCard(b)
    const sa = scoreDestination(ta, sel)
    const sb = scoreDestination(tb, sel)
    if (sb !== sa) return sb - sa
    if (aiOrder) {
      const ai = aiOrder.get(a.id ?? -1) ?? 9999
      const bi = aiOrder.get(b.id ?? -1) ?? 9999
      if (ai !== bi) return ai - bi
    }
    return (b.rating ?? 0) - (a.rating ?? 0)
  })
}

export function sortFoodsByTagMatch<T extends FoodSortable>(
  list: T[],
  sel: UserTagSelection,
  listSort: 'heat' | 'rating' | 'distance' = 'heat',
): T[] {
  return [...list].sort((a, b) => {
    const sa = scoreFoodItem(a, sel)
    const sb = scoreFoodItem(b, sel)
    if (sb !== sa) return sb - sa
    if (listSort === 'rating') {
      return (b.ratingScore ?? Number(b.rating) ?? 0) - (a.ratingScore ?? Number(a.rating) ?? 0)
    }
    return (b.heatScore ?? 0) - (a.heatScore ?? 0)
  })
}

export function interestTagsToLegacyThemes(interestTags: string[]): string[] {
  const out = new Set<string>()
  for (const tag of interestTags) {
    const themes = TAXONOMY.interestToLegacyThemes[tag as keyof typeof TAXONOMY.interestToLegacyThemes]
    if (themes) themes.forEach((t) => out.add(t))
  }
  return [...out]
}

export function legacyThemesToInterestTags(themes: string[] | undefined): string[] {
  if (!themes?.length) return []
  const out = new Set<string>()
  for (const theme of themes) {
    const interests = TAXONOMY.legacyThemeToInterests[theme as keyof typeof TAXONOMY.legacyThemeToInterests]
    if (interests) interests.forEach((t) => out.add(t))
  }
  return [...out]
}
