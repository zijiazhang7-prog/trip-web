import {
  AigcApiError,
  createAigcScratchDiary,
  deleteAigcScratchDiary,
  fetchAigcDiaryDetail,
  generateDiaryAnimation,
  isAnimationBackendUnavailable,
  uploadAigcImage,
} from '../../api/aigc'
import type { AigcImageItem } from './pipeline'
import { buildLocalAnimationScript } from './localAnimationScript'
import { renderAnimationToVideo } from './renderAnimationVideo'
import type { AigcAnimationPhase, AigcAnimationPipelineResult, DiaryAnimationResult } from './types'
import { prepareAigcUploadFile } from './uploadPrep'

function formatStepError(step: string, err: unknown): Error {
  if (err instanceof AigcApiError) {
    const code = err.code ? `（${err.code}）` : ''
    return new Error(`${step}失败${code}：${err.message}`)
  }
  return new Error(`${step}失败：${err instanceof Error ? err.message : '未知错误'}`)
}

async function resolveAnimationScript(input: {
  diaryId: number | null
  fileUrls: string[]
  destinationName: string
  onPhase: (phase: AigcAnimationPhase, detail?: string) => void
}): Promise<DiaryAnimationResult> {
  if (input.diaryId != null) {
    input.onPhase('generating', '正在请求后端生成分镜脚本…')
    try {
      return await generateDiaryAnimation(input.diaryId)
    } catch (err) {
      if (!isAnimationBackendUnavailable(err)) throw err
      console.warn('[AIGC] backend animation unavailable, using local fallback', err)

      try {
        const diary = await fetchAigcDiaryDetail(input.diaryId)
        const images = (diary.mediaList ?? [])
          .filter((m) => m.mediaType === 'image')
          .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0))
        if (images.length) {
          input.onPhase('generating', '后端动画服务不可用，已切换本地分镜模板…')
          return buildLocalAnimationScript({
            items: images.map((m) => ({ fileUrl: m.fileUrl, mediaId: m.id })),
            destinationName: diary.destinationName ?? input.destinationName,
            diaryTitle: diary.title,
          })
        }
      } catch (fetchErr) {
        console.warn('[AIGC] fetch diary for fallback failed', fetchErr)
      }
    }
  }

  input.onPhase('generating', '使用本地分镜模板生成动画脚本…')
  return buildLocalAnimationScript({
    items: input.fileUrls.map((fileUrl, i) => ({ fileUrl, mediaId: i + 1 })),
    destinationName: input.destinationName,
    diaryTitle: 'AI 旅行动画',
  })
}

export async function runAigcAnimationPipeline(
  images: AigcImageItem[],
  destinationId: number,
  destinationName: string,
  onPhase: (phase: AigcAnimationPhase, detail?: string) => void,
): Promise<AigcAnimationPipelineResult> {
  if (!images.length) throw new Error('请至少上传一张图片')

  let scratchDiaryId: number | null = null
  const fileUrls: string[] = []

  try {
    onPhase('uploading', `正在上传照片 0/${images.length}`)
    for (let i = 0; i < images.length; i++) {
      onPhase('uploading', `正在上传照片 ${i + 1}/${images.length}`)
      try {
        const prepared = await prepareAigcUploadFile(images[i].file)
        const url = await uploadAigcImage(prepared)
        fileUrls.push(url)
      } catch (err) {
        throw formatStepError('图片上传', err)
      }
    }

    onPhase('creating', '正在创建动画素材日记…')
    try {
      scratchDiaryId = await createAigcScratchDiary({
        destinationId,
        title: 'AI 旅行动画素材',
        contentText: '由 AIGC 页面自动生成的旅行照片动画素材，用于分镜脚本生成。',
        fileUrls,
      })
    } catch (err) {
      console.warn('[AIGC] scratch diary create failed, continue with upload-only fallback', err)
      scratchDiaryId = null
    }

    let animation: DiaryAnimationResult
    try {
      animation = await resolveAnimationScript({
        diaryId: scratchDiaryId,
        fileUrls,
        destinationName,
        onPhase,
      })
    } catch (err) {
      throw formatStepError('动画脚本生成', err)
    }

    onPhase('rendering', '正在将分镜渲染为视频…')
    let videoBlob: Blob | null = null
    let videoBlobUrl: string | null = null
    try {
      const rendered = await renderAnimationToVideo(animation.script, (pct, detail) => {
        onPhase('rendering', detail ?? `正在渲染视频 ${pct}%`)
      })
      videoBlob = rendered.blob
      videoBlobUrl = rendered.url
    } catch (renderErr) {
      onPhase('rendering', '视频渲染失败，将使用分镜预览模式')
      console.warn(renderErr)
    }

    onPhase('ready')
    return { animation, videoBlob, videoBlobUrl }
  } catch (err) {
    if (scratchDiaryId != null && err instanceof Error && err.message.includes('图片上传')) {
      await deleteAigcScratchDiary(scratchDiaryId)
    }
    throw err
  }
}
