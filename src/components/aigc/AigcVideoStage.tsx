import { useEffect, useRef, useState } from 'react'
import type { AigcPipelinePhase } from '../../lib/aigc/pipeline'

type AigcVideoStageProps = {
  phase: AigcPipelinePhase
  progressDetail?: string
  mergedUrl: string | null
  clipUrls: string[]
  playbackMode: 'merged' | 'playlist'
  title?: string
  narration?: string
}

const VIDEO_PREVIEW_HEIGHT_PX = 432

export function AigcVideoStage({
  phase,
  progressDetail,
  mergedUrl,
  clipUrls,
  playbackMode,
  title,
  narration,
}: AigcVideoStageProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const [segmentIndex, setSegmentIndex] = useState(0)
  const [viewMode, setViewMode] = useState<'segment' | 'merged'>('segment')

  const hasMultipleSegments = clipUrls.length > 1
  const canShowMerged = Boolean(mergedUrl && playbackMode === 'merged' && hasMultipleSegments)

  useEffect(() => {
    setSegmentIndex(0)
    setViewMode('segment')
  }, [clipUrls, mergedUrl, playbackMode])

  const currentSrc =
    viewMode === 'merged' && mergedUrl
      ? mergedUrl
      : clipUrls[segmentIndex] ?? mergedUrl ?? clipUrls[0] ?? null

  const goPrev = () => {
    if (viewMode === 'merged') return
    setSegmentIndex((i) => Math.max(0, i - 1))
  }

  const goNext = () => {
    if (viewMode === 'merged') return
    setSegmentIndex((i) => Math.min(clipUrls.length - 1, i + 1))
  }

  const busy = phase === 'analyzing' || phase === 'generating' || phase === 'merging'
  const showNav = phase === 'ready' && hasMultipleSegments

  return (
    <div className="flex flex-col rounded-[28px] border border-white/80 bg-white/65 p-4 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl">
      <div className="mb-3 flex shrink-0 items-center justify-between gap-2">
        <p className="font-body text-[13px] font-bold uppercase tracking-[0.12em] text-[var(--ds-accent-foreground)]">
          AI 动画预览
        </p>
        {showNav && canShowMerged ? (
          <div className="flex rounded-full border border-[var(--ds-border)]/50 bg-white/80 p-0.5 text-[10px]">
            <button
              type="button"
              className={`rounded-full px-2.5 py-0.5 ${viewMode === 'segment' ? 'bg-[var(--ds-primary)] text-white' : 'text-[var(--ds-muted-foreground)]'}`}
              onClick={() => setViewMode('segment')}
            >
              分段
            </button>
            <button
              type="button"
              className={`rounded-full px-2.5 py-0.5 ${viewMode === 'merged' ? 'bg-[var(--ds-primary)] text-white' : 'text-[var(--ds-muted-foreground)]'}`}
              onClick={() => setViewMode('merged')}
            >
              完整版
            </button>
          </div>
        ) : null}
      </div>

      <div
        style={{ height: VIDEO_PREVIEW_HEIGHT_PX }}
        className="relative shrink-0 w-full overflow-hidden rounded-2xl border border-[var(--ds-border)]/40 bg-[color-mix(in_srgb,var(--ds-background)_40%,white)]"
      >
        {currentSrc ? (
          <video
            ref={videoRef}
            key={currentSrc}
            src={currentSrc}
            className="h-full w-full object-contain"
            controls
            playsInline
            onEnded={() => {
              if (viewMode === 'segment' && segmentIndex < clipUrls.length - 1) {
                setSegmentIndex((i) => i + 1)
              }
            }}
          />
        ) : (
          <div className="flex h-full flex-col items-center justify-center gap-2 px-6 text-center text-sm text-[var(--ds-muted-foreground)]">
            {busy ? (
              <>
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-[var(--ds-primary)] border-t-transparent" />
                <p>{progressDetail ?? 'AI 创作中…'}</p>
              </>
            ) : (
              <>
                <p className="font-display text-lg text-[var(--ds-foreground)]">等待你的旅行记忆</p>
                <p className="text-xs">左侧上传照片后，点击下方按钮开始生成</p>
              </>
            )}
          </div>
        )}
        {busy && currentSrc ? (
          <div className="absolute inset-x-0 bottom-0 bg-black/50 px-3 py-2 text-center text-xs text-white">
            {progressDetail}
          </div>
        ) : null}
      </div>

      {showNav ? (
        <div className="mt-3 flex items-center justify-center gap-3">
          <button
            type="button"
            disabled={viewMode === 'merged' || segmentIndex === 0}
            onClick={goPrev}
            className="rounded-full border border-[var(--ds-border)]/60 bg-white/90 px-4 py-1.5 text-xs font-semibold text-[var(--ds-foreground)] disabled:cursor-not-allowed disabled:opacity-40"
          >
            上一个
          </button>
          <span className="min-w-[4.5rem] text-center text-[11px] text-[var(--ds-muted-foreground)]">
            {viewMode === 'merged' ? '完整版' : `第 ${segmentIndex + 1}/${clipUrls.length} 段`}
          </span>
          <button
            type="button"
            disabled={viewMode === 'merged' || segmentIndex >= clipUrls.length - 1}
            onClick={goNext}
            className="rounded-full border border-[var(--ds-border)]/60 bg-white/90 px-4 py-1.5 text-xs font-semibold text-[var(--ds-foreground)] disabled:cursor-not-allowed disabled:opacity-40"
          >
            下一个
          </button>
        </div>
      ) : null}

      {showNav && viewMode === 'segment' && !canShowMerged ? (
        <p className="mt-2 text-center text-[10px] text-[var(--ds-muted-foreground)]">
          后端 FFmpeg 合并未就绪，使用分段预览与连播演示
        </p>
      ) : null}

      {title ? (
        <div className="mt-4 rounded-xl border border-[var(--ds-border)]/40 bg-white/70 p-3">
          <p className="text-sm font-semibold text-[var(--ds-foreground)]">{title}</p>
          {narration ? <p className="mt-1 text-xs leading-relaxed text-[var(--ds-muted-foreground)]">{narration}</p> : null}
        </div>
      ) : null}

      {clipUrls.length > 0 && phase === 'ready' ? (
        <p className="mt-2 text-[10px] text-[var(--ds-muted-foreground)]">
          智谱视频链接约 30 天有效，请及时下载保存。
        </p>
      ) : null}
    </div>
  )
}
