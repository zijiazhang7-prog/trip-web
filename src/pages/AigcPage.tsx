import { useCallback, useEffect, useState } from 'react'
import { fetchAigcDestinations } from '../api/aigc'
import { AigcImageStrip } from '../components/aigc/AigcImageStrip'
import { AigcVideoStage } from '../components/aigc/AigcVideoStage'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { PrimaryButton } from '../components/ui/PrimaryButton'
import { runAigcAnimationPipeline } from '../lib/aigc/animationPipeline'
import type { AigcImageItem } from '../lib/aigc/pipeline'
import type { AigcAnimationPhase, DiaryAnimationResult } from '../lib/aigc/types'
import { AIGC_MAX_IMAGES } from '../lib/llm/config'
import { hasStoredToken } from '../api/http'

function uid(prefix: string): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

export function AigcPage() {
  const [images, setImages] = useState<AigcImageItem[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [phase, setPhase] = useState<AigcAnimationPhase>('idle')
  const [progressDetail, setProgressDetail] = useState<string>()
  const [error, setError] = useState<string | null>(null)
  const [animation, setAnimation] = useState<DiaryAnimationResult | null>(null)
  const [videoUrl, setVideoUrl] = useState<string | null>(null)
  const [videoBlob, setVideoBlob] = useState<Blob | null>(null)

  const [destinations, setDestinations] = useState<Array<{ id: number; name: string }>>([])
  const [destinationId, setDestinationId] = useState<number | null>(null)
  const [loadingDest, setLoadingDest] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoadingDest(true)
    void (async () => {
      try {
        const list = await fetchAigcDestinations()
        if (cancelled) return
        setDestinations(list)
        setDestinationId(list[0]?.id ?? null)
      } catch {
        if (!cancelled) setDestinations([])
      } finally {
        if (!cancelled) setLoadingDest(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

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

  const resetOutput = () => {
    if (videoUrl?.startsWith('blob:')) URL.revokeObjectURL(videoUrl)
    setVideoUrl(null)
    setVideoBlob(null)
    setAnimation(null)
  }

  const handleStart = async () => {
    if (!hasStoredToken()) {
      setError('请先登录后再生成旅行动画')
      return
    }
    if (destinationId == null) {
      setError('请选择关联目的地（后端创建日记素材需要）')
      return
    }
    if (!images.length) {
      setError('请先上传至少一张旅行照片')
      return
    }
    setError(null)
    resetOutput()
    try {
      const result = await runAigcAnimationPipeline(
        images,
        destinationId,
        destinations.find((d) => d.id === destinationId)?.name ?? '旅行目的地',
        (p, detail) => {
        setPhase(p)
        setProgressDetail(detail)
      })
      setAnimation(result.animation)
      setVideoUrl(result.videoBlobUrl)
      setVideoBlob(result.videoBlob)
      setPhase('ready')
    } catch (err) {
      setPhase('error')
      setError(err instanceof Error ? err.message : '旅行动画生成失败')
    }
  }

  const busy = phase === 'uploading' || phase === 'creating' || phase === 'generating' || phase === 'rendering'

  return (
    <div className="mx-auto max-w-[1320px] animate-fade-rise px-5 py-8 md:px-10">
      <PageHeader
        eyebrow="AI Memory"
        title="旅行照片 · AI 动画"
        description="上传旅行照片，由后端生成分镜脚本（路线 A），并在浏览器中渲染为可下载的旅行动画视频。"
      />

      {!hasStoredToken() ? (
        <InlineNotice variant="info">请先登录，以便上传图片并调用后端动画接口。</InlineNotice>
      ) : null}

      {error ? (
        <InlineNotice variant="error" onRetry={() => void handleStart()}>
          {error}
        </InlineNotice>
      ) : null}

      <div className="mb-5 rounded-2xl border border-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)] bg-white/70 px-4 py-3">
        <label className="block font-body text-sm">
          <span className="mb-1 block text-[var(--ds-muted-foreground)]">关联目的地</span>
          <select
            value={destinationId ?? ''}
            onChange={(e) => setDestinationId(Number(e.target.value) || null)}
            disabled={loadingDest || busy}
            className="w-full max-w-md rounded-xl border border-[var(--ds-border)] px-3 py-2 text-sm"
          >
            {loadingDest ? <option value="">加载目的地…</option> : null}
            {!loadingDest && destinations.length === 0 ? (
              <option value="">暂无可用目的地</option>
            ) : null}
            {destinations.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </label>
        <p className="mt-2 text-xs text-[var(--ds-muted-foreground)]">
          照片将上传至私有素材日记，仅用于本次动画生成，不会发布到社群。
        </p>
      </div>

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
          animation={animation}
          videoUrl={videoUrl}
          videoBlob={videoBlob}
        />
      </section>

      <div className="rounded-[28px] border border-white/80 bg-white/65 p-5 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl">
        <PrimaryButton
          fullWidth
          disabled={busy || images.length === 0 || destinationId == null}
          className="py-4 text-base"
          onClick={() => void handleStart()}
        >
          {busy ? progressDetail ?? '生成中…' : '生成旅行动画视频'}
        </PrimaryButton>
        {!busy && images.length > 0 ? (
          <p className="mt-3 text-center text-xs text-[var(--ds-muted-foreground)]">
            路线 A：后端分镜 + 前端 Ken Burns 渲染，通常 1–2 分钟完成（视图片数量而定）。
          </p>
        ) : null}
      </div>
    </div>
  )
}
