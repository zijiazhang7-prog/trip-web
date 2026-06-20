export type TextContentPart = { type: 'text'; text: string }
export type ImageContentPart = { type: 'image_url'; image_url: { url: string } }
export type VideoContentPart = { type: 'video_url'; video_url: { url: string } }
export type ContentPart = TextContentPart | ImageContentPart | VideoContentPart

export type ChatMessage = {
  role: 'system' | 'user' | 'assistant'
  content: string | ContentPart[]
}

type ChatMessagePayload = {
  content?: string | Array<{ type?: string; text?: string }>
  reasoning_content?: string
}

type OpenAiChatResponse = {
  choices?: Array<{ message?: ChatMessagePayload }>
  error?: { message?: string }
}

function extractMessageText(message?: ChatMessagePayload): string {
  if (!message) return ''
  const content = message.content
  if (typeof content === 'string' && content.trim()) return content.trim()
  if (Array.isArray(content)) {
    const joined = content
      .map((part) => {
        if (typeof part === 'string') return part
        if (part && typeof part === 'object' && typeof part.text === 'string') return part.text
        return ''
      })
      .join('')
      .trim()
    if (joined) return joined
  }
  if (typeof message.reasoning_content === 'string' && message.reasoning_content.trim()) {
    return message.reasoning_content.trim()
  }
  return ''
}

async function postChat(input: {
  url: string
  apiKey: string
  model: string
  messages: ChatMessage[]
  temperature?: number
  maxTokens?: number
  jsonMode?: boolean
  extraBody?: Record<string, unknown>
}): Promise<string> {
  const body: Record<string, unknown> = {
    model: input.model,
    messages: input.messages,
    temperature: input.temperature ?? 0.7,
    max_tokens: input.maxTokens ?? 4096,
    ...input.extraBody,
  }
  if (input.jsonMode) {
    body.response_format = { type: 'json_object' }
  }

  const res = await fetch(input.url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${input.apiKey}`,
    },
    body: JSON.stringify(body),
  })

  const rawBody = await res.text()
  const data = parseFirstJsonValue<OpenAiChatResponse>(rawBody)
  if (!res.ok) {
    throw new Error(data.error?.message ?? `LLM 请求失败 (${res.status})`)
  }

  const text = extractMessageText(data.choices?.[0]?.message)
  if (!text) throw new Error('LLM 返回为空')
  return text
}

/** 兼容响应体含多余字符或拼接 JSON 的情况 */
export function parseFirstJsonValue<T>(raw: string): T {
  const trimmed = raw.trim()
  if (!trimmed) throw new Error('响应为空')
  try {
    return JSON.parse(trimmed) as T
  } catch {
    const start = trimmed.indexOf('{')
    if (start < 0) throw new Error('响应不是有效 JSON')
    const end = findBalancedJsonEnd(trimmed, start)
    if (end < 0) throw new Error('响应 JSON 不完整')
    return JSON.parse(trimmed.slice(start, end + 1)) as T
  }
}

function findBalancedJsonEnd(text: string, start: number): number {
  let depth = 0
  let inString = false
  let escape = false
  for (let i = start; i < text.length; i++) {
    const ch = text[i]
    if (inString) {
      if (escape) escape = false
      else if (ch === '\\') escape = true
      else if (ch === '"') inString = false
      continue
    }
    if (ch === '"') {
      inString = true
      continue
    }
    if (ch === '{') depth++
    else if (ch === '}') {
      depth--
      if (depth === 0) return i
    }
  }
  return -1
}

function stripModelArtifacts(raw: string): string {
  return raw
    .replace(/`[\s\S]*?<\/think>/gi, '')
    .replace(/<\|begin_of_box\|>|<\|end_of_box\|>/g, '')
    .trim()
}

export async function chatCompletion(input: {
  url: string
  apiKey: string
  model: string
  messages: ChatMessage[]
  temperature?: number
  maxTokens?: number
  jsonMode?: boolean
  extraBody?: Record<string, unknown>
}): Promise<string> {
  return postChat(input)
}

export async function multimodalChat(input: {
  url: string
  apiKey: string
  model: string
  messages: ChatMessage[]
  temperature?: number
  maxTokens?: number
  jsonMode?: boolean
  extraBody?: Record<string, unknown>
}): Promise<string> {
  return postChat(input)
}

export function extractJsonObject<T>(raw: string): T {
  const cleaned = stripModelArtifacts(raw)
  const fenced = cleaned.match(/```(?:json)?\s*([\s\S]*?)```/i)
  const candidate = (fenced?.[1] ?? cleaned).trim()
  const start = candidate.indexOf('{')
  if (start < 0) throw new Error('未找到有效 JSON')
  const end = findBalancedJsonEnd(candidate, start)
  if (end < 0) throw new Error('JSON 结构不完整')
  try {
    return JSON.parse(candidate.slice(start, end + 1)) as T
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'JSON 解析失败'
    throw new Error(`分镜脚本解析失败：${msg}`)
  }
}
