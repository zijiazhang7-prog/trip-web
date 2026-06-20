import { useEffect, useRef } from 'react'
import { AigcAnimationPlayer } from './AigcAnimationPlayer'
import type { AigcAnimationPhase } from '../../lib/aigc/types'
import type { DiaryAnimationResult } from '../../lib/aigc/types'

type AigcVideoStageProps = {
  phase: AigcAnimationPhase
  progressDetail?: string
  animation: DiaryAnimationResult | null
  videoUrl: string | null
  videoBlob: Blob | null
}

const STAGE_HEIGHT_PX = 432

export function AigcVideoStage({
  phase,
  progressDetail,
  animation,
  videoUrl,
  videoBlob,
}: AigcVideoStageProps) {
  const downloadRef = useRef<HTMLAnchorElement>(null)

  const busy =
    phase === 'uploading' || phase === 'creating' || phase === 'generating' || phase === 'rendering'

  useEffect(() => {
    return () => {
      if (videoUrl?.startsWith('blob:')) URL.revokeObjectURL(videoUrl)
    }
  }, [videoUrl])

  const handleDownload = () => {
    if (!videoBlob || !downloadRef.current) return
    const url = URL.createObjectURL(videoBlob)
    downloadRef.current.href = url
    downloadRef.current.download = `${animation?.title ?? '旅行动画'}.webm`
    downloadRef.current.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="flex flex-col rounded-[28px] border border-white/80 bg-white/65 p-4 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl">
      <div className="mb-3 flex shrink-0 items-center justify-between gap-2">
        <p className="font-body text-[13px] font-bold uppercase tracking-[0.12em] text-[var(--ds-accent-foreground)]">
          旅行动画预览
        </p>
          {animation?.provider === 'local-fallback' ? (
          <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-[10px] text-amber-900">
            本地分镜
          </span>
        ) : animation?.provider ? (
          <span className="rounded-full bg-[color-mix(in_srgb,var(--ds-primary)_10%,white)] px-2.5 py-0.5 text-[10px] text-[var(--ds-primary)]">
            {animation.provider}
          </span>
        ) : null}
      </div>

      <div
        style={{ minHeight: STAGE_HEIGHT_PX }}
        className="relative shrink-0 w-full overflow-hidden rounded-2xl border border-[var(--ds-border)]/40 bg-[color-mix(in_srgb,var(--ds-background)_40%,white)]"
      >
        {phase === 'ready' && videoUrl ? (
          <video
            key={videoUrl}
            src={videoUrl}
            className="h-full w-full object-contain"
            style={{ minHeight: STAGE_HEIGHT_PX }}
            controls
            playsInline
            autoPlay
            loop
          />
        ) : phase === 'ready' && animation?.script ? (
          <div className="p-3">
            <AigcAnimationPlayer
              script={animation.script}
              title={animation.title}
              narration={animation.narration}
            />
          </div>
        ) : (
          <div
            className="flex flex-col items-center justify-center gap-2 px-6 text-center text-sm text-[var(--ds-muted-foreground)]"
            style={{ minHeight: STAGE_HEIGHT_PX }}
          >
            {busy ? (
              <>
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-[var(--ds-primary)] border-t-transparent" />
                <p>{progressDetail ?? '正在生成旅行动画…'}</p>
              </>
            ) : (
              <>
                <p className="font-display text-lg text-[var(--ds-foreground)]">等待你的旅行照片</p>
                <p className="text-xs">上传照片并选择目的地后，将走后端分镜并渲染为视频</p>
              </>
            )}
          </div>
        )}
      </div>

      {phase === 'ready' && videoBlob ? (
        <div className="mt-3 flex justify-center">
          <button
            type="button"
            onClick={handleDownload}
            className="rounded-full bg-[var(--ds-primary)] px-5 py-2 text-xs font-semibold text-white"
          >
            下载视频 (.webm)
          </button>
          <a ref={downloadRef} className="hidden" href="#" download />
        </div>
      ) : null}

      {phase === 'ready' && animation && !videoUrl ? (
        <p className="mt-2 text-center text-[10px] text-[var(--ds-muted-foreground)]">
          视频渲染未成功，已切换为分镜实时预览模式
        </p>
      ) : null}
    </div>
  )
}
