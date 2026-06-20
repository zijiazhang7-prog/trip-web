import { httpRequest } from './http'

export type MergeVideosRequest = {
  videoUrls: string[]
  title?: string
}

export type MergeVideosResponse = {
  mergedUrl: string
  durationSec?: number
}

/** 优先后端 FFmpeg 合并；接口不存在时返回 null 由前端连播兜底 */
export async function tryMergeVideosRemote(body: MergeVideosRequest): Promise<MergeVideosResponse | null> {
  try {
    return await httpRequest<MergeVideosResponse>('/api/v1/ai/videos/merge', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  } catch {
    return null
  }
}
