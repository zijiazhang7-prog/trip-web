export const TOKEN_STORAGE_KEY = 'trip_auth_token'

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

  const response = await fetch(path, { ...init, headers })
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
}
