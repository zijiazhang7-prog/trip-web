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
  doubaoResponses: dev ? '/llm-doubao/api/v3/responses' : 'https://ark.cn-beijing.volces.com/api/v3/responses',
} as const
