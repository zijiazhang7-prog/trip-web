import { chatCompletion, extractJsonObject } from './chat'
import { getDeepSeekApiKey, hasDeepSeekKey, LLM_ENDPOINTS } from './config'

export type RecommendReasonInput = {
  destinationName: string
  destinationType?: string
  travelerThemes?: string[]
}

export type GroupNegotiationInput = {
  travelers: Array<{ name: string; themes: string[] }>
  candidateDestinations: string[]
}

export type DestinationForRanking = {
  id: number
  name: string
  type: string
  badge: string
  reason: string
}

export async function explainRecommendation(input: RecommendReasonInput): Promise<string> {
  if (!hasDeepSeekKey()) throw new Error('未配置 DeepSeek API Key')

  return chatCompletion({
    url: LLM_ENDPOINTS.deepseekChat,
    apiKey: getDeepSeekApiKey(),
    model: 'deepseek-chat',
    temperature: 0.6,
    maxTokens: 512,
    messages: [
      {
        role: 'system',
        content:
          '你是旅行规划助手。用 2-3 句温暖、具体的中文说明该目的地为何适合用户，避免空泛套话，可点出体验亮点。',
      },
      {
        role: 'user',
        content: [
          `目的地：${input.destinationName}`,
          input.destinationType ? `类型：${input.destinationType}` : '',
          input.travelerThemes?.length ? `偏好：${input.travelerThemes.join('、')}` : '',
        ]
          .filter(Boolean)
          .join('\n'),
      },
    ],
  })
}

export async function negotiateGroupPreferences(input: GroupNegotiationInput): Promise<string> {
  if (!hasDeepSeekKey()) throw new Error('未配置 DeepSeek API Key')

  const travelers = input.travelers
    .map((t) => `${t.name}：${t.themes.join('、') || '未填写'}`)
    .join('\n')

  return chatCompletion({
    url: LLM_ENDPOINTS.deepseekChat,
    apiKey: getDeepSeekApiKey(),
    model: 'deepseek-chat',
    temperature: 0.5,
    maxTokens: 800,
    messages: [
      {
        role: 'system',
        content:
          '你是多人旅行决策助手。根据每位旅伴偏好，给出折中建议：推荐 1 个首选目的地 + 1 个备选，并说明如何安排行程让大家都满意。输出简洁分点。',
      },
      {
        role: 'user',
        content: `旅伴偏好：\n${travelers}\n\n候选目的地：${input.candidateDestinations.join('、')}`,
      },
    ],
  })
}

/** 根据用户自由描述 + 标签，对目的地 ID 按匹配度排序（最相关在前） */
export async function rankDestinationsByPreference(input: {
  userText: string
  selectedTags?: string[]
  destinations: DestinationForRanking[]
}): Promise<number[]> {
  if (!hasDeepSeekKey()) throw new Error('未配置 DeepSeek API Key')
  if (!input.destinations.length) return []

  const catalog = input.destinations.map((d) => ({
    id: d.id,
    name: d.name,
    type: d.type,
    tags: d.badge,
    description: d.reason.slice(0, 120),
  }))

  const raw = await chatCompletion({
    url: LLM_ENDPOINTS.deepseekChat,
    apiKey: getDeepSeekApiKey(),
    model: 'deepseek-chat',
    temperature: 0.3,
    maxTokens: 1024,
    jsonMode: true,
    messages: [
      {
        role: 'system',
        content: `你是旅游目的地匹配助手。根据用户旅行意向，从候选列表中按匹配度排序。
只输出 JSON：{"rankedIds":[数字id数组，最匹配在前]}
规则：结合用户文字与已选标签；只使用候选中的 id；全部 id 都要出现；无法判断的放后面。`,
      },
      {
        role: 'user',
        content: [
          `用户描述：${input.userText}`,
          input.selectedTags?.length ? `已选标签：${input.selectedTags.join('、')}` : '',
          `候选目的地：${JSON.stringify(catalog)}`,
        ]
          .filter(Boolean)
          .join('\n'),
      },
    ],
  })

  const parsed = extractJsonObject<{ rankedIds?: number[] }>(raw)
  const validIds = new Set(input.destinations.map((d) => d.id))
  const ranked = (parsed.rankedIds ?? []).filter((id) => validIds.has(id))
  const rest = input.destinations.map((d) => d.id).filter((id) => !ranked.includes(id))
  return [...ranked, ...rest]
}
