import { useCallback, useState } from 'react'
import { AigcImageStrip } from '../components/aigc/AigcImageStrip'
import { AigcVideoStage } from '../components/aigc/AigcVideoStage'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import type { AigcImageItem, AigcPipelinePhase } from '../lib/aigc/pipeline'
import { runAigcPipeline } from '../lib/aigc/pipeline'
import { AIGC_MAX_IMAGES, hasGlmKey } from '../lib/llm/config'

function uid(prefix: string): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

export function AigcPage() {
  const [images, setImages] = useState<AigcImageItem[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [phase, setPhase] = useState<AigcPipelinePhase>('idle')
  const [progressDetail, setProgressDetail] = useState<string>()
  const [error, setError] = useState<string | null>(null)
  const [clipUrls, setClipUrls] = useState<string[]>([])
  const [mergedUrl, setMergedUrl] = useState<string | null>(null)
  const [playbackMode, setPlaybackMode] = useState<'merged' | 'playlist'>('playlist')
  const [title, setTitle] = useState<string>()
  const [narration, setNarration] = useState<string>()

  const handleUpload = useCallback((files: FileList | null) => {
    if (!files?.length) return
    setError(null)
    const next: AigcImageItem[] = []
    for (const file of Array.from(files)) {
      if (!file.type.startsWith('image/')) continue
      if (images.length + next.length >= AIGC_MAX_IMAGES) break
      const id = uid('aigc')
      next.push({
        id,
        file,
        previewUrl: URL.createObjectURL(file),
      })
    }
    if (!next.length) return
    setImages((prev) => [...prev, ...next])
    setActiveId((prev) => prev ?? next[0].id)
  }, [images.length])

  const removeImage = (id: string) => {
    setImages((prev) => {
      const target = prev.find((p) => p.id === id)
      if (target) URL.revokeObjectURL(target.previewUrl)
      const rest = prev.filter((p) => p.id !== id)
      setActiveId((cur) => (cur === id ? rest[0]?.id ?? null : cur))
      return rest
    })
  }

  const moveImage = (id: string, dir: -1 | 1) => {
    setImages((prev) => {
      const idx = prev.findIndex((p) => p.id === id)
      if (idx < 0) return prev
      const j = idx + dir
      if (j < 0 || j >= prev.length) return prev
      const copy = [...prev]
      ;[copy[idx], copy[j]] = [copy[j], copy[idx]]
      return copy
    })
  }

  const handleStart = async () => {
    if (!hasGlmKey()) {
      setError('未配置 VITE_GLM_API_KEY，无法调用 GLM-4.6V-Flash 与 CogVideoX-Flash')
      return
    }
    if (!images.length) {
      setError('请先上传至少一张旅行照片')
      return
    }
    setError(null)
    setClipUrls([])
    setMergedUrl(null)
    setTitle(undefined)
    setNarration(undefined)
    try {
      const result = await runAigcPipeline(images, (p, detail) => {
        setPhase(p)
        setProgressDetail(detail)
      })
      setClipUrls(result.clipUrls)
      setMergedUrl(result.mergedUrl)
      setPlaybackMode(result.playbackMode)
      setTitle(result.storyboard.title)
      setNarration(result.storyboard.narration)
      setPhase('ready')
    } catch (err) {
      setPhase('error')
      setError(err instanceof Error ? err.message : 'AI 动画生成失败')
    }
  }

  const busy = phase === 'analyzing' || phase === 'generating' || phase === 'merging'

  return (
    <div className="mx-auto max-w-[1320px] animate-fade-rise px-5 py-8 md:px-10">
      <PageHeader
        eyebrow="AI Memory"
        title="旅行照片 · AI 动画"
        description="上传景点或校园旅行照片，智谱 GLM-4.6V-Flash 理解画面并编写分镜，CogVideoX-Flash 将记忆化为动态短片。"
      />

      {!hasGlmKey() ? (
        <InlineNotice variant="info">
          请在 .env 配置 VITE_GLM_API_KEY（智谱开放平台），以使用 GLM-4.6V-Flash 与 CogVideoX-Flash。
        </InlineNotice>
      ) : null}

      {error ? (
        <InlineNotice variant="error" onRetry={() => void handleStart()}>
          {error}
        </InlineNotice>
      ) : null}

      <section className="mb-6 grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <AigcImageStrip
          images={images}
          activeId={activeId}
          onSelect={setActiveId}
          onRemove={removeImage}
          onMoveUp={(id) => moveImage(id, -1)}
          onMoveDown={(id) => moveImage(id, 1)}
          onUpload={handleUpload}
          maxImages={AIGC_MAX_IMAGES}
          disabled={busy}
        />
        <AigcVideoStage
          phase={phase}
          progressDetail={progressDetail}
          mergedUrl={mergedUrl}
          clipUrls={clipUrls}
          playbackMode={playbackMode}
          title={title}
          narration={narration}
        />
      </section>

      <div className="rounded-[28px] border border-white/80 bg-white/65 p-5 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl">
        <PrimaryButton
          fullWidth
          disabled={busy || images.length === 0}
          className="py-4 text-base"
          onClick={() => void handleStart()}
        >
          {busy ? progressDetail ?? 'AI 创作中…' : '让美好记忆动起来'}
        </PrimaryButton>
        {!busy && images.length > 0 ? (
          <p className="mt-3 text-center text-xs text-[var(--ds-muted-foreground)]">
            建议 2–3 张用于答辩演示；图片越多耗时越长（每张约 3–15 分钟），请保持页面开启。
          </p>
        ) : null}
      </div>
    </div>
  )
}
