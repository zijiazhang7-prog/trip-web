import { gsap } from 'gsap'
import { useCallback, useEffect, useMemo, useRef } from 'react'
import './TargetCursor.css'

export type TargetCursorProps = {
  targetSelector?: string
  spinDuration?: number
  hideDefaultCursor?: boolean
  hoverDuration?: number
  parallaxOn?: boolean
}

function useIsMobile() {
  return useMemo(() => {
    if (typeof window === 'undefined') return false
    const hasTouchScreen =
      'ontouchstart' in window || (navigator.maxTouchPoints ?? 0) > 0
    const isSmallScreen = window.innerWidth <= 768
    const ua = navigator.userAgent || navigator.vendor || ''
    const mobileRegex = /android|webos|iphone|ipad|ipod|blackberry|iemobile|opera mini/i
    return (hasTouchScreen && isSmallScreen) || mobileRegex.test(ua.toLowerCase())
  }, [])
}

const BASE = 48
const PAD = 6

export function TargetCursor({
  targetSelector = '.cursor-target',
  spinDuration = 2,
  hideDefaultCursor = true,
  hoverDuration = 0.2,
  parallaxOn = true,
}: TargetCursorProps) {
  const rootRef = useRef<HTMLDivElement>(null)
  const innerRef = useRef<HTMLDivElement>(null)
  const frameRef = useRef<HTMLDivElement>(null)
  const spinTl = useRef<gsap.core.Timeline | null>(null)
  const lockedRef = useRef(false)
  const lastTargetRef = useRef<Element | null>(null)
  const mouseRef = useRef({ x: 0, y: 0 })
  const isMobile = useIsMobile()

  const startSpin = useCallback(() => {
    const frame = frameRef.current
    if (!frame) return
    spinTl.current?.kill()
    spinTl.current = gsap.timeline({ repeat: -1 })
    spinTl.current.to(frame, {
      rotation: '+=360',
      duration: spinDuration,
      ease: 'none',
    })
  }, [spinDuration])

  useEffect(() => {
    if (isMobile) return

    const root = rootRef.current
    const inner = innerRef.current
    const frame = frameRef.current
    if (!root || !inner || !frame) return

    mouseRef.current = {
      x: window.innerWidth / 2,
      y: window.innerHeight / 2,
    }

    const prevCursor = document.body.style.cursor
    if (hideDefaultCursor) document.body.style.cursor = 'none'

    gsap.set(inner, { x: mouseRef.current.x, y: mouseRef.current.y })

    const lockOn = (el: Element) => {
      lockedRef.current = true
      root.classList.add('is-locked')
      spinTl.current?.pause()
      gsap.set(frame, { rotation: 0 })

      const r = el.getBoundingClientRect()
      const cx = r.left + r.width / 2
      const cy = r.top + r.height / 2
      const w = Math.max(BASE, r.width + PAD * 2)
      const h = Math.max(BASE, r.height + PAD * 2)

      gsap.to(inner, {
        x: cx,
        y: cy,
        duration: hoverDuration,
        ease: 'power3.out',
        overwrite: 'auto',
      })

      gsap.to(frame, {
        width: w,
        height: h,
        duration: hoverDuration,
        ease: 'power3.out',
      })

      if (parallaxOn) {
        const corners = root.querySelectorAll('.target-cursor-corner')
        gsap.fromTo(
          corners,
          { scale: 1 },
          {
            scale: 1.06,
            duration: hoverDuration * 0.75,
            stagger: 0.04,
            yoyo: true,
            repeat: 1,
            ease: 'power2.out',
          },
        )
      }
    }

    const lockOff = () => {
      lockedRef.current = false
      root.classList.remove('is-locked')

      gsap.to(frame, {
        width: BASE,
        height: BASE,
        duration: hoverDuration,
        ease: 'power3.out',
      })

      gsap.to(inner, {
        x: mouseRef.current.x,
        y: mouseRef.current.y,
        duration: 0.28,
        ease: 'power3.out',
      })

      startSpin()
    }

    const onMove = (e: MouseEvent) => {
      mouseRef.current.x = e.clientX
      mouseRef.current.y = e.clientY

      const under = document.elementFromPoint(e.clientX, e.clientY)
      const hit = under?.closest(targetSelector) ?? null

      if (hit !== lastTargetRef.current) {
        if (lastTargetRef.current) lockOff()
        lastTargetRef.current = hit
        if (hit) lockOn(hit)
      }

      if (!lockedRef.current) {
        gsap.to(inner, {
          x: e.clientX,
          y: e.clientY,
          duration: 0.12,
          ease: 'power3.out',
          overwrite: 'auto',
        })
      }
    }

    document.addEventListener('mousemove', onMove)
    startSpin()

    return () => {
      document.removeEventListener('mousemove', onMove)
      spinTl.current?.kill()
      spinTl.current = null
      lastTargetRef.current = null
      lockedRef.current = false
      document.body.style.cursor = prevCursor
    }
  }, [hideDefaultCursor, hoverDuration, isMobile, parallaxOn, startSpin, targetSelector])

  if (isMobile) return null

  return (
    <div ref={rootRef} className="target-cursor-root" aria-hidden>
      <div ref={innerRef} className="target-cursor-inner">
        <div ref={frameRef} className="target-cursor-frame">
          <span className="target-cursor-corner tl" />
          <span className="target-cursor-corner tr" />
          <span className="target-cursor-corner br" />
          <span className="target-cursor-corner bl" />
          <span className="target-cursor-dot" />
        </div>
      </div>
    </div>
  )
}

export default TargetCursor
