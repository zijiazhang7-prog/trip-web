import { getDoubaoApiKey, getDoubaoVisionModel, hasDoubaoKey, LLM_ENDPOINTS } from './config'

type DoubaoOutputItem = {
  type?: string
  role?: string
  content?: Array<{ type?: string; text?: string }>
}

type DoubaoResponsesResult = {
  output?: DoubaoOutputItem[]
  error?: { message?: string }
}

function extractDoubaoText(data: DoubaoResponsesResult): string {
  for (const item of data.output ?? []) {
    if (item.type !== 'message') continue
    for (const part of item.content ?? []) {
      if (part.type === 'output_text' && part.text?.trim()) {
        return part.text.trim()
      }
    }
  }
  throw new Error('豆包识图返回为空')
}

export async function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error ?? new Error('读取图片失败'))
    reader.readAsDataURL(file)
  })
}

export async function describeTravelImage(input: {
  imageDataUrl: string
  context?: string
}): Promise<string> {
  if (!hasDoubaoKey()) throw new Error('未配置豆包 API Key')

  const prompt =
    input.context?.trim() ||
    '这是一张旅行照片。请用 1-2 句中文手账风格描述画面中的场景、氛围与情绪，适合作为图片配文，不要列清单。'

  let res: Response
  try {
    res = await fetch(LLM_ENDPOINTS.doubaoResponses, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getDoubaoApiKey()}`,
      },
      body: JSON.stringify({
        model: getDoubaoVisionModel(),
        input: [
          {
            role: 'user',
            content: [
              { type: 'input_image', image_url: input.imageDataUrl },
              { type: 'input_text', text: prompt },
            ],
          },
        ],
      }),
    })
  } catch {
    throw new Error('豆包识图网络请求失败，请检查 VITE_DOUBAO_API_KEY 与 dev 代理 /llm-doubao')
  }

  const data = (await res.json()) as DoubaoResponsesResult
  if (!res.ok) {
    throw new Error(data.error?.message ?? `豆包识图失败 (${res.status})`)
  }

  return extractDoubaoText(data)
}
