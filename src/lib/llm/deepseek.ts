import { chatCompletion } from './chat'
import { getDeepSeekApiKey, hasDeepSeekKey, LLM_ENDPOINTS } from './config'

export type RecommendReasonInput = {
  destinationName: string
  destinationType?: string
  travelerThemes?: string[]
  budget?: string
  travelTime?: string
}

export type GroupNegotiationInput = {
  travelers: Array<{ name: string; themes: string[] }>
  candidateDestinations: string[]
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
          input.budget ? `预算：${input.budget}` : '',
          input.travelTime ? `出行时长：${input.travelTime}` : '',
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
