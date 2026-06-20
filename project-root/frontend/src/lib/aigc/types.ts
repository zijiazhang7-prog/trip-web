export type AnimationScene = {
  order: number
  mediaId?: number
  fileUrl: string
  visualDescription?: string
  durationMs: number
  motion: string
  transition?: string
  subtitle?: string
  narration?: string
}

export type AnimationScript = {
  schemaVersion: string
  aspectRatio: string
  totalDurationMs: number
  backgroundMusic?: string
  scenes: AnimationScene[]
}

export type DiaryAnimationResult = {
  id: number
  diaryId: number
  provider: string
  title: string
  narration: string
  status: string
  script: AnimationScript
}

export type AigcAnimationPhase =
  | 'idle'
  | 'uploading'
  | 'creating'
  | 'generating'
  | 'rendering'
  | 'ready'
  | 'error'

export type AigcAnimationPipelineResult = {
  animation: DiaryAnimationResult
  videoBlobUrl: string | null
  videoBlob: Blob | null
}
