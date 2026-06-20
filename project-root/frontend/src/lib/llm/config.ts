export function getDeepSeekApiKey(): string {
  return import.meta.env.VITE_DEEPSEEK_API_KEY?.trim() ?? ''
}

export function getGlmApiKey(): string {
  return import.meta.env.VITE_GLM_API_KEY?.trim() ?? ''
}

export function getDoubaoApiKey(): string {
  return import.meta.env.VITE_DOUBAO_API_KEY?.trim() ?? ''
}

export function getDoubaoVisionModel(): string {
  return import.meta.env.VITE_DOUBAO_VISION_MODEL?.trim() || 'doubao-seed-2-0-mini-260428'
}

export function hasDeepSeekKey(): boolean {
  return Boolean(getDeepSeekApiKey())
}

export function hasGlmKey(): boolean {
  return Boolean(getGlmApiKey())
}

export function hasGlmVisionKey(): boolean {
  return hasGlmKey()
}

export const GLM_VISION_MODEL = 'glm-4.6v-flash'
export const GLM_TEXT_MODEL = 'glm-4.7-flash'
export const COGVIDEO_FLASH_MODEL = 'cogvideox-flash'
export const AIGC_MAX_IMAGES = 5

export function hasDoubaoKey(): boolean {
  return Boolean(getDoubaoApiKey())
}

export function hasAnyLlmKey(): boolean {
  return hasDeepSeekKey() || hasGlmKey() || hasDoubaoKey()
}

const dev = import.meta.env.DEV

export const LLM_ENDPOINTS = {
  deepseekChat: dev ? '/llm-deepseek/v1/chat/completions' : 'https://api.deepseek.com/v1/chat/completions',
  glmChat: dev ? '/llm-glm/api/paas/v4/chat/completions' : 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
  glmVideoGen: dev ? '/llm-glm/api/paas/v4/videos/generations' : 'https://open.bigmodel.cn/api/paas/v4/videos/generations',
  glmAsyncResult: (taskId: string) =>
    dev
      ? `/llm-glm/api/paas/v4/async-result/${encodeURIComponent(taskId)}`
      : `https://open.bigmodel.cn/api/paas/v4/async-result/${encodeURIComponent(taskId)}`,
  doubaoResponses: dev ? '/llm-doubao/api/v3/responses' : 'https://ark.cn-beijing.volces.com/api/v3/responses',
} as const
