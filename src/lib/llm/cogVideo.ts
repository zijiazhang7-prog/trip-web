import { parseFirstJsonValue } from './chat'
import {
  COGVIDEO_FLASH_MODEL,
  getGlmApiKey,
  hasGlmKey,
  LLM_ENDPOINTS,
} from './config'

export type VideoGenTaskStatus = 'PROCESSING' | 'SUCCESS' | 'FAIL'

export type VideoGenSubmitResult = {
  id: string
  taskStatus?: VideoGenTaskStatus
}

export type VideoGenPollResult = {
  taskStatus: VideoGenTaskStatus
  videoUrls: string[]
  coverUrls: string[]
  errorMessage?: string
}

type AsyncVideoResponse = {
  id?: string
  task_status?: string
  video_result?: Array<{ url?: string; cover_image_url?: string }>
  error?: { message?: string }
}

export const COGVIDEO_POLL_INTERVAL_MS = 5000
const MAX_POLL_ATTEMPTS = 180
const MAX_SUBMIT_RETRIES = 2

function authHeaders(): HeadersInit {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${getGlmApiKey()}`,
  }
}

export async function submitImageToVideo(input: {
  imageUrl: string
  prompt: string
}): Promise<VideoGenSubmitResult> {
  if (!hasGlmKey()) throw new Error('未配置智谱 API Key')

  const res = await fetch(LLM_ENDPOINTS.glmVideoGen, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({
      model: COGVIDEO_FLASH_MODEL,
      prompt: input.prompt,
      image_url: input.imageUrl,
      quality: 'speed',
      with_audio: false,
      size: '1280x720',
      fps: 30,
    }),
  })

  const data = parseFirstJsonValue<AsyncVideoResponse>(await res.text())
  if (!res.ok) {
    throw new Error(data.error?.message ?? `视频生成提交失败 (${res.status})`)
  }
  const id = data.id
  if (!id) throw new Error('视频任务未返回 id')
  return {
    id,
    taskStatus: (data.task_status as VideoGenTaskStatus) ?? 'PROCESSING',
  }
}

export async function pollVideoTask(taskId: string): Promise<VideoGenPollResult> {
  if (!hasGlmKey()) throw new Error('未配置智谱 API Key')

  const res = await fetch(LLM_ENDPOINTS.glmAsyncResult(taskId), {
    method: 'GET',
    headers: authHeaders(),
  })
  const data = parseFirstJsonValue<AsyncVideoResponse>(await res.text())
  if (!res.ok) {
    throw new Error(data.error?.message ?? `查询视频任务失败 (${res.status})`)
  }

  const status = (data.task_status ?? 'PROCESSING') as VideoGenTaskStatus
  const videoUrls = (data.video_result ?? []).map((v) => v.url).filter(Boolean) as string[]
  const coverUrls = (data.video_result ?? [])
    .map((v) => v.cover_image_url)
    .filter(Boolean) as string[]

  return {
    taskStatus: status,
    videoUrls,
    coverUrls,
    errorMessage: status === 'FAIL' ? data.error?.message ?? '视频生成失败' : undefined,
  }
}

export async function waitForVideoTask(
  taskId: string,
  onProgress?: (attempt: number) => void,
): Promise<VideoGenPollResult> {
  for (let i = 0; i < MAX_POLL_ATTEMPTS; i++) {
    onProgress?.(i + 1)
    const result = await pollVideoTask(taskId)
    if (result.taskStatus === 'SUCCESS') {
      if (!result.videoUrls.length) throw new Error('视频生成成功但未返回 URL')
      return result
    }
    if (result.taskStatus === 'FAIL') {
      throw new Error(result.errorMessage ?? '视频生成失败')
    }
    await new Promise((r) => setTimeout(r, COGVIDEO_POLL_INTERVAL_MS))
  }
  const waitedMin = Math.round((MAX_POLL_ATTEMPTS * COGVIDEO_POLL_INTERVAL_MS) / 60000)
  throw new Error(`视频生成超时（已等待约 ${waitedMin} 分钟），请减少图片数量或稍后重试`)
}

export async function generateClipFromImage(input: {
  imageDataUrl: string
  prompt: string
  onPoll?: (attempt: number, waitedSec: number) => void
}): Promise<string> {
  let lastError: Error | null = null

  for (let submitTry = 0; submitTry <= MAX_SUBMIT_RETRIES; submitTry++) {
    try {
      const { id } = await submitImageToVideo({
        imageUrl: input.imageDataUrl,
        prompt: input.prompt,
      })
      const done = await waitForVideoTask(id, (attempt) => {
        const waitedSec = Math.round((attempt * COGVIDEO_POLL_INTERVAL_MS) / 1000)
        input.onPoll?.(attempt, waitedSec)
      })
      return done.videoUrls[0]
    } catch (err) {
      lastError = err instanceof Error ? err : new Error('视频生成失败')
      const retryable =
        submitTry < MAX_SUBMIT_RETRIES &&
        (lastError.message.includes('超时') || lastError.message.includes('网络'))
      if (!retryable) throw lastError
      await new Promise((r) => setTimeout(r, 3000))
    }
  }

  throw lastError ?? new Error('视频生成失败')
}
