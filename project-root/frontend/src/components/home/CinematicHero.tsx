import { useEffect, useRef, useState } from 'react'
import { BlurText } from '../effects/BlurText'

const VIDEO_SRC = '/video/beijing.mp4'
const POSTER_SRC = '/images/recommend-hero-new.png'

type CinematicHeroProps = {
  onStartStory?: () => void
}

export function CinematicHero({ onStartStory }: CinematicHeroProps) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const rafRef = useRef(0)
  const [videoReady, setVideoReady] = useState(false)
  const [videoFailed, setVideoFailed] = useState(false)

  useEffect(() => {
    const video = videoRef.current
    if (!video) return

    const fadeDuration = 0.5
    let timeoutId: ReturnType<typeof setTimeout> | undefined

    const applyOpacityFromTime = () => {
      if (!videoReady || videoFailed) return
      const duration = video.duration
      if (!Number.isFinite(duration) || duration <= 0) return

      const t = video.currentTime
      let opacity = 1

      if (t < fadeDuration) {
        opacity = t / fadeDuration
      } else if (t > duration - fadeDuration) {
        opacity = Math.max(0, (duration - t) / fadeDuration)
      }

      video.style.opacity = String(opacity)
    }

    const tick = () => {
      applyOpacityFromTime()
      rafRef.current = requestAnimationFrame(tick)
    }

    const onEnded = () => {
      video.style.opacity = '0'
      timeoutId = setTimeout(() => {
        video.currentTime = 0
        void video.play()
      }, 100)
    }

    const onCanPlay = () => {
      setVideoFailed(false)
      setVideoReady(true)
      video.style.opacity = '1'
      void video.play().catch(() => setVideoFailed(true))
    }

    const onError = () => {
      setVideoFailed(true)
      setVideoReady(false)
      video.style.opacity = '0'
    }

    video.addEventListener('canplay', onCanPlay)
    video.addEventListener('ended', onEnded)
    video.addEventListener('error', onError)
    rafRef.current = requestAnimationFrame(tick)

    return () => {
      video.removeEventListener('canplay', onCanPlay)
      video.removeEventListener('ended', onEnded)
      video.removeEventListener('error', onError)
      cancelAnimationFrame(rafRef.current)
      if (timeoutId !== undefined) clearTimeout(timeoutId)
    }
  }, [videoReady, videoFailed])

  return (
    <div className="relative min-h-screen w-full overflow-hidden bg-[var(--ds-forest)] pb-28 md:pb-36">
      <div className="pointer-events-none absolute inset-x-0 top-[280px] bottom-0 z-0 overflow-hidden">
        <img
          src={POSTER_SRC}
          alt=""
          className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-700 ${videoReady && !videoFailed ? 'opacity-0' : 'opacity-90'}`}
          aria-hidden
        />
        <video
          ref={videoRef}
          className="relative h-full w-full object-cover"
          src={VIDEO_SRC}
          poster={POSTER_SRC}
          muted
          playsInline
          autoPlay
          loop
          preload="auto"
          aria-hidden
        />
        <div className="ds-hero-overlay absolute inset-0" aria-hidden />
        <div
          className="absolute inset-x-0 bottom-0 h-[min(58%,440px)] bg-[linear-gradient(to_top,var(--ds-cream)_0%,color-mix(in_srgb,var(--ds-forest)_55%,transparent)_55%,transparent_100%)]"
          aria-hidden
        />
        {videoFailed ? (
          <p className="absolute bottom-6 left-1/2 z-10 -translate-x-1/2 rounded-full bg-black/35 px-4 py-1.5 font-body text-[11px] text-white/85">
            请将 beijing.mp4 放入 public/video/ 目录以启用背景视频
          </p>
        ) : null}
      </div>

      <div
        className="pointer-events-none absolute -right-24 top-1/4 h-80 w-80 bg-[color-mix(in_srgb,var(--ds-moss)_35%,transparent)] blur-3xl"
        style={{ borderRadius: '55% 45% 65% 35% / 45% 55% 40% 60%' }}
        aria-hidden
      />
      <div
        className="pointer-events-none absolute -left-20 bottom-40 h-72 w-72 bg-[color-mix(in_srgb,var(--ds-sage)_25%,transparent)] blur-3xl"
        style={{ borderRadius: '40% 60% 55% 45% / 60% 40% 55% 45%' }}
        aria-hidden
      />

      <main
        className="relative z-10 flex flex-col items-center justify-center px-6 pb-36 text-center md:pb-40"
        style={{ paddingTop: 'calc(8rem - 75px)' }}
      >
        <p className="font-display mb-4 text-[11px] font-bold uppercase tracking-[0.38em] text-[color-mix(in_srgb,var(--ds-cream)_75%,var(--ds-sage))]">
          Discover · 智游行
        </p>
        <BlurText
          text="去远方，不赶路，去感受。"
          delay={90}
          animateBy="letters"
          direction="top"
          stepDuration={0.38}
          className="font-display max-w-5xl justify-center text-5xl font-semibold leading-[1.05] tracking-[-0.02em] text-white drop-shadow-[0_8px_32px_rgba(0,0,0,0.35)] sm:text-7xl md:text-8xl"
        />
        <BlurText
          text="智游行 —— 你的私人旅行手账。"
          delay={55}
          animateBy="letters"
          direction="top"
          stepDuration={0.32}
          className="font-body mt-5 max-w-2xl justify-center text-base leading-relaxed text-[color-mix(in_srgb,white_82%,var(--ds-sage))] sm:text-lg"
        />
        <p className="font-body mx-auto mt-6 max-w-2xl text-sm leading-relaxed text-[color-mix(in_srgb,var(--ds-cream)_88%,white)] sm:text-base">
          智游行为你推荐风景、规划路线、记录回忆。从出发到归来，我们陪你走完整个故事。
        </p>
        <button
          type="button"
          onClick={onStartStory}
          className="cursor-target font-body mt-9 rounded-full border border-[color-mix(in_srgb,white_25%,transparent)] bg-[linear-gradient(135deg,var(--ds-moss),var(--ds-forest))] px-12 py-[1.1rem] text-base font-bold text-[var(--ds-cream)] shadow-[0_12px_40px_-10px_rgba(0,0,0,0.45)] transition duration-300 ease-out hover:scale-105 hover:brightness-110 active:scale-95 sm:px-14 sm:py-5"
        >
          开始你的故事
        </button>
      </main>
    </div>
  )
}
