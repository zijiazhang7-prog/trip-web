import { extractJsonObject, multimodalChat } from './chat'
import {
  getGlmApiKey,
  GLM_VISION_MODEL,
  hasGlmVisionKey,
  LLM_ENDPOINTS,
} from './config'

export type AigcStoryboard = {
  title: string
  narration: string
  scenes: Array<{
    order: number
    motionPrompt: string
    subtitle: string
  }>
}

async function glmVisionChat(
  content: Array<{ type: 'text'; text: string } | { type: 'image_url'; image_url: { url: string } } | { type: 'video_url'; video_url: { url: string } }>,
  system: string,
  jsonMode = false,
): Promise<string> {
  if (!hasGlmVisionKey()) throw new Error('未配置智谱 API Key（VITE_GLM_API_KEY）')
  return multimodalChat({
    url: LLM_ENDPOINTS.glmChat,
    apiKey: getGlmApiKey(),
    model: GLM_VISION_MODEL,
    temperature: 0.6,
    maxTokens: jsonMode ? 2048 : 800,
    jsonMode,
    extraBody: { thinking: { type: 'disabled' } },
    messages: [
      { role: 'system', content: system },
      { role: 'user', content },
    ],
  })
}

export async function describeTravelImageGlm(input: {
  imageDataUrl: string
  context?: string
}): Promise<string> {
  const prompt =
    input.context?.trim() ||
    '这是一张旅行照片。请用 1-2 句中文手账风格描述画面中的场景、氛围与情绪，适合作为图片配文，不要列清单。'
  return glmVisionChat(
    [
      { type: 'image_url', image_url: { url: input.imageDataUrl } },
      { type: 'text', text: prompt },
    ],
    '你是旅行手账视觉助手，输出简洁温馨的中文配文。',
  )
}

export async function describeTravelVideoGlm(input: {
  videoDataUrl: string
  context?: string
}): Promise<string> {
  const prompt =
    input.context?.trim() ||
    '这是一段旅行短视频。请用 2-3 句中文手账风格概括画面中的场景、氛围与情绪，适合作为视频配文。'
  return glmVisionChat(
    [
      { type: 'video_url', video_url: { url: input.videoDataUrl } },
      { type: 'text', text: prompt },
    ],
    '你是旅行手账视觉助手，根据视频内容输出简洁温馨的中文配文。',
  )
}

function normalizeStoryboard(parsed: AigcStoryboard, imageCount: number): AigcStoryboard {
  if (!parsed.scenes?.length) throw new Error('分镜脚本为空')
  return {
    title: parsed.title?.trim() || '旅行记忆',
    narration: parsed.narration?.trim() || '',
    scenes: parsed.scenes.slice(0, imageCount).map((s, i) => ({
      order: s.order ?? i + 1,
      motionPrompt: s.motionPrompt?.trim() || 'gentle camera movement, travel memory atmosphere',
      subtitle: s.subtitle?.trim() || '',
    })),
  }
}

/** 多图一次请求失败时，逐张识图再让模型只输出 JSON 文本（不传大图） */
async function buildStoryboardFallback(imageDataUrls: string[]): Promise<AigcStoryboard> {
  const captions: string[] = []
  for (let i = 0; i < imageDataUrls.length; i++) {
    const cap = await describeTravelImageGlm({
      imageDataUrl: imageDataUrls[i],
      context: `这是旅行动画第 ${i + 1} 张分镜照片，用一句话客观描述画面元素与氛围。`,
    })
    captions.push(`第${i + 1}张：${cap}`)
  }
  const raw = await glmVisionChat(
    [
      {
        type: 'text',
        text: `根据以下 ${imageDataUrls.length} 张旅行照片描述，生成分镜 JSON。只输出一个 JSON 对象，不要其它文字：
${captions.join('\n')}
格式：
{"title":"标题","narration":"旁白","scenes":[{"order":1,"motionPrompt":"镜头运动描述","subtitle":"字幕"}]}
scenes 数量必须等于 ${imageDataUrls.length}。`,
      },
    ],
    '你是旅游动画分镜导演。只输出合法 JSON。',
    true,
  )
  return normalizeStoryboard(extractJsonObject<AigcStoryboard>(raw), imageDataUrls.length)
}

export async function buildAigcStoryboard(imageDataUrls: string[]): Promise<AigcStoryboard> {
  if (!imageDataUrls.length) throw new Error('请至少上传一张图片')

  try {
    const content: Array<
      | { type: 'text'; text: string }
      | { type: 'image_url'; image_url: { url: string } }
    > = [
      {
        type: 'text',
        text: `以下是 ${imageDataUrls.length} 张旅行照片，按顺序编号 1 到 ${imageDataUrls.length}。请为 AIGC 旅游动画生成分镜脚本。只输出一个 JSON 对象，不要 markdown 代码块，不要任何解释文字：
{"title":"动画标题","narration":"整段旁白","scenes":[{"order":1,"motionPrompt":"图生视频动作描述","subtitle":"中文字幕"}]}
scenes 数量必须等于 ${imageDataUrls.length}，order 从 1 递增。motionPrompt 适合 CogVideoX 轻微镜头运动。`,
      },
    ]
    for (const url of imageDataUrls) {
      content.push({ type: 'image_url', image_url: { url } })
    }

    const raw = await glmVisionChat(
      content,
      '你是旅游照片动画分镜导演。只输出单个 JSON 对象，禁止输出 JSON 以外的内容。',
      true,
    )
    return normalizeStoryboard(extractJsonObject<AigcStoryboard>(raw), imageDataUrls.length)
  } catch {
    return buildStoryboardFallback(imageDataUrls)
  }
}
