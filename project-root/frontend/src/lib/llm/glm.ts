import { chatCompletion, extractJsonObject } from './chat'
import { getGlmApiKey, hasGlmKey, LLM_ENDPOINTS } from './config'

export type DiaryDayAiLayout = {
  title: string
  subtitle?: string
  timeline: Array<{ time: string; place: string; note: string }>
  checklist: string[]
  leftPageText: string
  rightPageText: string
  moodTag?: string
}

export type GenerateDiaryDayInput = {
  bookTitle: string
  dayIndex: number
  entryDate?: string
  places?: string[]
  roughNotes?: string
}

export async function polishDiaryText(input: {
  title: string
  draft: string
}): Promise<string> {
  if (!hasGlmKey()) throw new Error('未配置智谱 API Key')

  return chatCompletion({
    url: LLM_ENDPOINTS.glmChat,
    apiKey: getGlmApiKey(),
    model: 'glm-4.7-flash',
    temperature: 0.8,
    maxTokens: 600,
    extraBody: { thinking: { type: 'disabled' } },
    messages: [
      {
        role: 'system',
        content:
          '你是旅行手账文案助手。将用户草稿润色为 1-2 段诗意、具体、第一人称的中文手账文字，保留真实感，不要营销腔。',
      },
      {
        role: 'user',
        content: `标题：${input.title}\n草稿：${input.draft || '（空白，请根据标题即兴写一段）'}`,
      },
    ],
  })
}

export async function generateDiaryDayLayout(input: GenerateDiaryDayInput): Promise<DiaryDayAiLayout> {
  if (!hasGlmKey()) throw new Error('未配置智谱 API Key')

  const raw = await chatCompletion({
    url: LLM_ENDPOINTS.glmChat,
    apiKey: getGlmApiKey(),
    model: 'glm-4.7-flash',
    temperature: 0.7,
    maxTokens: 2048,
    jsonMode: true,
    extraBody: { thinking: { type: 'disabled' } },
    messages: [
      {
        role: 'system',
        content: `你是旅行手账排版助手。根据输入生成「一日游」手账 JSON，字段：
{
  "title": "当日标题",
  "subtitle": "副标题可选",
  "timeline": [{"time":"09:00","place":"地点","note":"一句话"}],
  "checklist": ["待办/打卡项"],
  "leftPageText": "左页正文，可含时间轴文字",
  "rightPageText": "右页正文，可含清单与心情",
  "moodTag": "心情标签"
}
只输出 JSON，不要 markdown。`,
      },
      {
        role: 'user',
        content: [
          `手账本：${input.bookTitle}`,
          `第 ${input.dayIndex} 天`,
          input.entryDate ? `日期：${input.entryDate}` : '',
          input.places?.length ? `途经：${input.places.join(' → ')}` : '',
          input.roughNotes ? `用户笔记：${input.roughNotes}` : '',
        ]
          .filter(Boolean)
          .join('\n'),
      },
    ],
  })

  return extractJsonObject<DiaryDayAiLayout>(raw)
}
