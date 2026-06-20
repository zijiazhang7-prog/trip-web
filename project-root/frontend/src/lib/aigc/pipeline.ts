import { tryMergeVideosRemote } from '../../api/aigcMerge'
import { compressImageForVideo } from './imagePrep'
import { AIGC_MAX_IMAGES } from '../llm/config'
import { generateClipFromImage } from '../llm/cogVideo'
import { buildAigcStoryboard, type AigcStoryboard } from '../llm/glmVision'

export type AigcImageItem = {
  id: string
  file: File
  previewUrl: string
  dataUrl?: string
}

export type AigcPipelinePhase =
  | 'idle'
  | 'analyzing'
  | 'generating'
  | 'merging'
  | 'ready'
  | 'error'

export type AigcPipelineResult = {
  storyboard: AigcStoryboard
  clipUrls: string[]
  mergedUrl: string | null
  playbackMode: 'merged' | 'playlist'
}

export async function runAigcPipeline(
  images: AigcImageItem[],
  onPhase: (phase: AigcPipelinePhase, detail?: string) => void,
): Promise<AigcPipelineResult> {
  if (!images.length) throw new Error('请至少上传一张图片')
  if (images.length > AIGC_MAX_IMAGES) {
    throw new Error(`最多支持 ${AIGC_MAX_IMAGES} 张图片`)
  }

  onPhase('analyzing', '正在理解照片并编写分镜…')
  const dataUrls: string[] = []
  for (const img of images) {
    dataUrls.push(img.dataUrl ?? (await compressImageForVideo(img.file)))
  }

  const storyboard = await buildAigcStoryboard(dataUrls)
  const clipUrls: string[] = []

  onPhase('generating', `正在生成视频 0/${dataUrls.length}`)
  for (let i = 0; i < dataUrls.length; i++) {
    const scene = storyboard.scenes[i]
    const prompt = scene?.motionPrompt || 'gentle travel memory animation, soft camera movement'
    onPhase('generating', `正在生成视频 ${i + 1}/${dataUrls.length}（智谱队列渲染中…）`)
    const url = await generateClipFromImage({
      imageDataUrl: dataUrls[i],
      prompt,
      onPoll: (_attempt, waitedSec) => {
        onPhase(
          'generating',
          `正在生成视频 ${i + 1}/${dataUrls.length} · 已等待 ${waitedSec} 秒`,
        )
      },
    })
    clipUrls.push(url)
  }

  onPhase('merging', '正在合成完整旅行动画…')
  let mergedUrl: string | null = null
  let playbackMode: 'merged' | 'playlist' = 'playlist'

  if (clipUrls.length === 1) {
    mergedUrl = clipUrls[0]
    playbackMode = 'merged'
  } else if (clipUrls.length > 1) {
    const remote = await tryMergeVideosRemote({
      videoUrls: clipUrls,
      title: storyboard.title,
    })
    if (remote?.mergedUrl) {
      mergedUrl = remote.mergedUrl
      playbackMode = 'merged'
    }
  }

  onPhase('ready')
  return {
    storyboard,
    clipUrls,
    mergedUrl,
    playbackMode,
  }
}
