import { httpRequest } from './http'
import {
  interestTagsToLegacyThemes,
  legacyThemesToInterestTags,
  TAXONOMY,
} from '../lib/taxonomy'

export type UserPreferenceVO = {
  userId?: number
  preferHotLevel?: number
  preferThemeList?: string[]
  preferInterestTags?: string[]
  preferDestTypes?: string[]
  preferFoodTypes?: string[]
  preferFoodType?: string
  preferCrowdLevel?: number
  travelStyle?: string
  customPreferenceText?: string
  updatedAt?: string
}

export type UserPreferenceRequest = {
  preferHotLevel?: number
  preferThemeList?: string[]
  preferInterestTags?: string[]
  preferDestTypes?: string[]
  preferFoodTypes?: string[]
  preferFoodType?: string
  preferCrowdLevel?: number
  travelStyle?: string
  customPreferenceText?: string
}

/** 标准兴趣标签 → 旧版 theme（后端未升级前写入 preferThemeList） */
export function interestTagsToThemeList(interestTags: string[]): string[] {
  return interestTagsToLegacyThemes(interestTags)
}

/** @deprecated 使用 interestTagsToThemeList */
export function tagsToThemeList(tags: string[]): string[] {
  if (tags.every((t) => TAXONOMY.interestTags.includes(t))) {
    return interestTagsToThemeList(tags)
  }
  const legacyMap: Record<string, string> = {
    亲子: '亲子友好型',
    古镇: '人文建筑型',
    海滨: '自然景观型',
    美食: '美食探索型',
    文化: '人文建筑型',
    冒险: '自然景观型',
    放松: '轻松休闲型',
  }
  const out = new Set<string>()
  for (const tag of tags) {
    const theme = legacyMap[tag]
    if (theme) out.add(theme)
  }
  return [...out]
}

export function themeListToInterestTags(themes: string[] | undefined): string[] {
  return legacyThemesToInterestTags(themes)
}

/** @deprecated 使用 themeListToInterestTags */
export function themeListToTags(themes: string[] | undefined): string[] {
  return themeListToInterestTags(themes)
}

export async function fetchMyPreferences(): Promise<UserPreferenceVO> {
  return httpRequest<UserPreferenceVO>('/api/v1/user-preferences/me', { method: 'GET' })
}

export async function saveMyPreferences(body: UserPreferenceRequest): Promise<UserPreferenceVO> {
  return httpRequest<UserPreferenceVO>('/api/v1/user-preferences/me', {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
