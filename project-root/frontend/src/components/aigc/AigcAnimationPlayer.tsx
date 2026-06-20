import { useEffect, useRef, useState, type CSSProperties } from 'react'
import type { AnimationScript } from '../../lib/aigc/types'

type AigcAnimationPlayerProps = {
  script: AnimationScript
  title?: string
  narration?: string
  className?: string
}

function normalizeAssetUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function motionStyle(motion: string, progress: number): CSSProperties {
  const t = progress < 0.5 ? 2 * progress * progress : 1 - Math.pow(-2 * progress + 2, 2) / 2
  switch (motion) {
    case 'zoom_out':
      return { transform: `scale(${1.14 - t * 0.14})` }
    case 'pan_left':
      return { transform: `scale(1.12) translateX(${t * 7}%)` }
    case 'pan_right':
      return { transform: `scale(1.12) translateX(${-t * 7}%)` }
    case 'zoom_in':
    default:
      return { transform: `scale(${1 + t * 0.14})` }
  }
}

export function AigcAnimationPlayer({ script, title, narration, className = '' }: AigcAnimationPlayerProps) {
  const scenes = [...script.scenes].sort((a, b) => a.order - b.order)
  const [sceneIndex, setSceneIndex] = useState(0)
  const [progress, setProgress] = useState(0)
  const [playing, setPlaying] = useState(true)
  const rafRef = useRef<number>(0)
  const startRef = useRef<number>(0)

  const scene = scenes[sceneIndex]
  const durationMs = Math.max(scene?.durationMs ?? 4000, 500)

  useEffect(() => {
    setSceneIndex(0)
    setProgress(0)
    setPlaying(true)
  }, [script])

  useEffect(() => {
    if (!playing || !scene) return undefined

    startRef.current = performance.now()
    const tick = (now: number) => {
      const elapsed = now - startRef.current
      const p = Math.min(1, elapsed / durationMs)
      setProgress(p)
      if (p >= 1) {
        if (sceneIndex < scenes.length - 1) {
          setSceneIndex((i) => i + 1)
          setProgress(0)
        } else {
          setPlaying(false)
        }
        return
      }
      rafRef.current = requestAnimationFrame(tick)
    }
    rafRef.current = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(rafRef.current)
  }, [playing, scene, sceneIndex, durationMs, scenes.length])

  if (!scene) return null

  return (
    <div className={`flex flex-col ${className}`}>
      <div className="relative aspect-video w-full overflow-hidden rounded-2xl bg-[#0f1410]">
        <img
          key={`${scene.order}-${scene.fileUrl}`}
          src={normalizeAssetUrl(scene.fileUrl)}
          alt={scene.subtitle ?? `场景 ${scene.order}`}
          className="h-full w-full object-cover transition-opacity duration-300"
          style={motionStyle(scene.motion, progress)}
          draggable={false}
        />
        {scene.subtitle ? (
          <p className="pointer-events-none absolute inset-x-0 bottom-4 mx-auto max-w-[90%] rounded-xl bg-black/55 px-4 py-2 text-center text-sm font-semibold text-white">
            {scene.subtitle}
          </p>
        ) : null}
        <p className="pointer-events-none absolute right-3 top-3 rounded-full bg-black/45 px-2.5 py-1 text-[10px] text-white/90">
          {sceneIndex + 1} / {scenes.length}
        </p>
      </div>

      <div className="mt-3 flex items-center justify-center gap-3">
        <button
          type="button"
          onClick={() => {
            setSceneIndex(0)
            setProgress(0)
            setPlaying(true)
          }}
          className="rounded-full border border-[var(--ds-border)]/60 bg-white/90 px-4 py-1.5 text-xs font-semibold text-[var(--ds-foreground)]"
        >
          重新播放
        </button>
        {!playing ? (
          <button
            type="button"
            onClick={() => setPlaying(true)}
            className="rounded-full bg-[var(--ds-primary)] px-4 py-1.5 text-xs font-semibold text-white"
          >
            继续
          </button>
        ) : null}
      </div>

      {title ? (
        <div className="mt-4 rounded-xl border border-[var(--ds-border)]/40 bg-white/70 p-3">
          <p className="text-sm font-semibold text-[var(--ds-foreground)]">{title}</p>
          {narration ? <p className="mt-1 text-xs leading-relaxed text-[var(--ds-muted-foreground)]">{narration}</p> : null}
        </div>
      ) : null}
    </div>
  )
}
