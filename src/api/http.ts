export const TOKEN_STORAGE_KEY = 'trip_auth_token'

const DEFAULT_TIMEOUT_MS = 12_000

export type ApiEnvelope<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

function getStoredToken(): string | null {
  try {
    return window.localStorage.getItem(TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

export function setStoredToken(token: string): void {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearStoredToken(): void {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export function hasStoredToken(): boolean {
  return Boolean(getStoredToken())
}

export async function httpRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getStoredToken()
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json')
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)

  const timeoutMs =
    typeof init.signal === 'object' && init.signal != null
      ? undefined
      : DEFAULT_TIMEOUT_MS

  const controller = timeoutMs ? new AbortController() : null
  const timer =
    controller && timeoutMs
      ? window.setTimeout(() => controller.abort(), timeoutMs)
      : undefined

  try {
    const response = await fetch(path, {
      ...init,
      headers,
      signal: controller?.signal ?? init.signal,
    })
    let payload: ApiEnvelope<T> | null
    try {
      payload = (await response.json()) as ApiEnvelope<T>
    } catch {
      payload = null
    }

    if (!response.ok) {
      const message = payload?.message || `HTTP ${response.status}`
      throw new Error(message)
    }
    if (!payload) throw new Error('服务返回格式错误')
    if (!payload.success) throw new Error(payload.message || '请求失败')
    return payload.data
  } catch (err) {
    if (err instanceof Error && err.name === 'AbortError') {
      throw new Error('请求超时，请检查网络或稍后重试')
    }
    if (err instanceof TypeError) {
      throw new Error('网络连接失败，请检查后端服务是否可用')
    }
    throw err
  } finally {
    if (timer != null) window.clearTimeout(timer)
  }
}
