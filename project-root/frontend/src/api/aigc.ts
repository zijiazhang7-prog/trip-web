import { coerceSpringPage } from './coercePage'
import type { ApiEnvelope } from './http'
import { TOKEN_STORAGE_KEY } from './http'
import type { DiaryAnimationResult } from '../lib/aigc/types'

export class AigcApiError extends Error {
  code?: string
  step: string

  constructor(step: string, message: string, code?: string) {
    super(message)
    this.name = 'AigcApiError'
    this.step = step
    this.code = code
  }
}

type UploadFileResponse = {
  fileUrl: string
}

type CreateDiaryResponse = {
  diaryId: number
}

type DestinationVO = {
  id: number
  name: string
}

type DiaryMediaVO = {
  id: number
  mediaType: string
  fileUrl: string
  sortNo?: number
}

type DiaryDetailVO = {
  id: number
  title?: string
  destinationName?: string
  mediaList?: DiaryMediaVO[]
}

async function requestWithCode<T>(step: string, path: string, init: RequestInit & { timeoutMs?: number } = {}): Promise<T> {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  const headers = new Headers(init.headers)
  if (!headers.has('Content-Type') && init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)

  const timeoutMs = init.timeoutMs ?? 12_000
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), timeoutMs)

  try {
    const response = await fetch(path, { ...init, headers, signal: controller.signal })
    let payload: ApiEnvelope<T> | null = null
    try {
      payload = (await response.json()) as ApiEnvelope<T>
    } catch {
      payload = null
    }

    if (!response.ok || !payload?.success) {
      const message = payload?.message || `HTTP ${response.status}`
      throw new AigcApiError(step, message, payload?.code)
    }
    return payload.data
  } catch (err) {
    if (err instanceof AigcApiError) throw err
    if (err instanceof Error && err.name === 'AbortError') {
      throw new AigcApiError(step, '请求超时，请检查网络或稍后重试')
    }
    if (err instanceof TypeError) {
      throw new AigcApiError(step, '网络连接失败，请检查后端服务是否可用')
    }
    throw new AigcApiError(step, err instanceof Error ? err.message : '请求失败')
  } finally {
    window.clearTimeout(timer)
  }
}

function normalizeFileUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

export async function fetchAigcDestinations(): Promise<DestinationVO[]> {
  const attempts = [
    '/api/v1/destinations/search?keyword=&pageNum=1&pageSize=30',
    '/api/v1/destinations/search?keyword=北京&pageNum=1&pageSize=30',
    '/api/v1/destinations/recommend?pageNum=1&pageSize=30',
  ]
  for (const path of attempts) {
    try {
      const raw = await requestWithCode<unknown>('destinations', path, { method: 'GET', timeoutMs: 15_000 })
      const page = coerceSpringPage<DestinationVO>(raw)
      const list = (page.list ?? []).filter((d) => typeof d.id === 'number' && d.id > 0 && d.name)
      if (list.length) return list
    } catch {
      // try next source
    }
  }
  return []
}

export async function uploadAigcImage(file: File, timeoutMs = 90_000): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  form.append('bizType', 'diary')
  const data = await requestWithCode<UploadFileResponse>('upload', '/api/v1/files/upload', {
    method: 'POST',
    body: form,
    timeoutMs,
  })
  if (!data.fileUrl?.startsWith('/files/')) {
    throw new AigcApiError('upload', `文件地址无效: ${data.fileUrl ?? '(空)'}`)
  }
  return normalizeFileUrl(data.fileUrl)
}

export async function createAigcScratchDiary(input: {
  destinationId: number
  title: string
  contentText: string
  fileUrls: string[]
}): Promise<number> {
  const data = await requestWithCode<CreateDiaryResponse>('create-diary', '/api/v1/diaries', {
    method: 'POST',
    body: JSON.stringify({
      destinationId: input.destinationId,
      title: input.title,
      contentText: input.contentText,
      visibility: 'private',
      mediaList: input.fileUrls.map((fileUrl, sortNo) => ({
        mediaType: 'image',
        fileUrl,
        fileName: `aigc-${sortNo + 1}.jpg`,
        sortNo,
      })),
    }),
    timeoutMs: 60_000,
  })
  if (!data.diaryId || data.diaryId <= 0) {
    throw new AigcApiError('create-diary', '日记创建成功但未返回有效 diaryId')
  }
  return data.diaryId
}

export async function fetchAigcDiaryDetail(diaryId: number): Promise<DiaryDetailVO> {
  return requestWithCode<DiaryDetailVO>('fetch-diary', `/api/v1/diaries/${diaryId}`, {
    method: 'GET',
    timeoutMs: 20_000,
  })
}

export async function generateDiaryAnimation(diaryId: number): Promise<DiaryAnimationResult> {
  return requestWithCode<DiaryAnimationResult>('animation', `/api/v1/diaries/${diaryId}/animation`, {
    method: 'POST',
    timeoutMs: 120_000,
  })
}

export async function deleteAigcScratchDiary(diaryId: number): Promise<void> {
  try {
    await requestWithCode<boolean>('cleanup', `/api/v1/diaries/${diaryId}`, { method: 'DELETE', timeoutMs: 20_000 })
  } catch {
    // 清理失败不阻断
  }
}

export function isAnimationBackendUnavailable(err: unknown): boolean {
  if (!(err instanceof AigcApiError)) return false
  return (
    err.code === 'COMMON_006' ||
    err.code === 'AI_002' ||
    err.code === 'AI_007' ||
    err.message.includes('系统内部错误')
  )
}
