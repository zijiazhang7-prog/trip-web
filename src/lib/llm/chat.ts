type ChatMessage = { role: 'system' | 'user' | 'assistant'; content: string }

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

  const data = (await res.json()) as OpenAiChatResponse
  if (!res.ok) {
    throw new Error(data.error?.message ?? `LLM 请求失败 (${res.status})`)
  }

  const text = extractMessageText(data.choices?.[0]?.message)
  if (!text) throw new Error('LLM 返回为空')
  return text
}

export function extractJsonObject<T>(raw: string): T {
  const fenced = raw.match(/```(?:json)?\s*([\s\S]*?)```/i)
  const candidate = (fenced?.[1] ?? raw).trim()
  const start = candidate.indexOf('{')
  const end = candidate.lastIndexOf('}')
  if (start < 0 || end <= start) {
    throw new Error('未找到有效 JSON')
  }
  return JSON.parse(candidate.slice(start, end + 1)) as T
}

export type { ChatMessage }
