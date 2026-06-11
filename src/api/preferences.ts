import { httpRequest } from './http'

export type UserPreferenceVO = {
  userId?: number
  preferHotLevel?: number
  preferThemeList?: string[]
  preferFoodType?: string
  preferCrowdLevel?: number
  travelStyle?: string
  customPreferenceText?: string
  updatedAt?: string
}

export type UserPreferenceRequest = {
  preferHotLevel?: number
  preferThemeList?: string[]
  preferFoodType?: string
  preferCrowdLevel?: number
  travelStyle?: string
  customPreferenceText?: string
}

const TAG_TO_THEME: Record<string, string> = {
  亲子: '亲子友好型',
  古镇: '人文建筑型',
  海滨: '自然景观型',
  美食: '美食探索型',
  文化: '人文建筑型',
  冒险: '自然景观型',
  放松: '轻松休闲型',
}

export function tagsToThemeList(tags: string[]): string[] {
  const out = new Set<string>()
  for (const tag of tags) {
    const theme = TAG_TO_THEME[tag]
    if (theme) out.add(theme)
  }
  return [...out]
}

export function themeListToTags(themes: string[] | undefined): string[] {
  if (!themes?.length) return []
  const reverse = Object.entries(TAG_TO_THEME)
  const tags = new Set<string>()
  for (const theme of themes) {
    for (const [tag, mapped] of reverse) {
      if (mapped === theme || theme.includes(tag)) tags.add(tag)
    }
  }
  return [...tags]
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
