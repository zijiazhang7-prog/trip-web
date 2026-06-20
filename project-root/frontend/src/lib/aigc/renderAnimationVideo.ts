import type { AnimationScript } from './types'

const OUTPUT_WIDTH = 1280
const OUTPUT_HEIGHT = 720
const FPS = 30

function normalizeAssetUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url.startsWith('/') ? url : `/${url}`
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(`图片加载失败: ${url}`))
    img.src = normalizeAssetUrl(url)
  })
}

function sortedScenes(script: AnimationScript) {
  return [...script.scenes].sort((a, b) => a.order - b.order)
}

function motionTransform(motion: string, t: number): { scale: number; offsetX: number; offsetY: number } {
  const eased = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
  switch (motion) {
    case 'zoom_out':
      return { scale: 1.14 - eased * 0.14, offsetX: 0, offsetY: 0 }
    case 'pan_left':
      return { scale: 1.12, offsetX: eased * 0.07, offsetY: 0 }
    case 'pan_right':
      return { scale: 1.12, offsetX: -eased * 0.07, offsetY: 0 }
    case 'zoom_in':
    default:
      return { scale: 1 + eased * 0.14, offsetX: 0, offsetY: 0 }
  }
}

function drawCoverImage(
  ctx: CanvasRenderingContext2D,
  img: HTMLImageElement,
  transform: { scale: number; offsetX: number; offsetY: number },
) {
  const cw = OUTPUT_WIDTH
  const ch = OUTPUT_HEIGHT
  const ir = img.width / img.height
  const cr = cw / ch
  let drawW: number
  let drawH: number
  if (ir > cr) {
    drawH = ch * transform.scale
    drawW = drawH * ir
  } else {
    drawW = cw * transform.scale
    drawH = drawW / ir
  }
  const x = (cw - drawW) / 2 + transform.offsetX * cw
  const y = (ch - drawH) / 2 + transform.offsetY * ch
  ctx.drawImage(img, x, y, drawW, drawH)
}

function drawSubtitle(ctx: CanvasRenderingContext2D, text: string) {
  if (!text.trim()) return
  const padding = 28
  ctx.font = '600 28px system-ui, sans-serif'
  const maxWidth = OUTPUT_WIDTH - padding * 2
  ctx.fillStyle = 'rgba(0,0,0,0.52)'
  const metrics = ctx.measureText(text)
  const boxW = Math.min(maxWidth, metrics.width + 40)
  const boxH = 52
  const boxX = (OUTPUT_WIDTH - boxW) / 2
  const boxY = OUTPUT_HEIGHT - boxH - 36
  roundRect(ctx, boxX, boxY, boxW, boxH, 12)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(text, OUTPUT_WIDTH / 2, boxY + boxH / 2, maxWidth)
}

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number,
) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function sceneAtTime(
  scenes: AnimationScript['scenes'],
  timeMs: number,
): { index: number; localT: number; fade: number } {
  let elapsed = 0
  for (let i = 0; i < scenes.length; i++) {
    const dur = Math.max(scenes[i].durationMs, 500)
    if (timeMs < elapsed + dur) {
      const localT = (timeMs - elapsed) / dur
      const fade = localT > 0.88 && i < scenes.length - 1 ? (localT - 0.88) / 0.12 : 0
      return { index: i, localT: Math.min(localT, 1), fade }
    }
    elapsed += dur
  }
  const last = scenes.length - 1
  return { index: last, localT: 1, fade: 0 }
}

function totalDurationMs(script: AnimationScript): number {
  const scenes = sortedScenes(script)
  return scenes.reduce((sum, s) => sum + Math.max(s.durationMs, 500), 0)
}

export async function renderAnimationToVideo(
  script: AnimationScript,
  onProgress?: (pct: number, detail?: string) => void,
): Promise<{ blob: Blob; url: string }> {
  const scenes = sortedScenes(script)
  if (!scenes.length) throw new Error('动画分镜为空')

  onProgress?.(2, '预加载照片…')
  const images = await Promise.all(scenes.map((s) => loadImage(s.fileUrl)))

  const canvas = document.createElement('canvas')
  canvas.width = OUTPUT_WIDTH
  canvas.height = OUTPUT_HEIGHT
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('无法创建画布')

  const durationMs = totalDurationMs(script)
  const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
    ? 'video/webm;codecs=vp9'
    : 'video/webm'

  const stream = canvas.captureStream(FPS)
  const recorder = new MediaRecorder(stream, { mimeType, videoBitsPerSecond: 4_000_000 })
  const chunks: Blob[] = []

  const done = new Promise<Blob>((resolve, reject) => {
    recorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data)
    }
    recorder.onstop = () => resolve(new Blob(chunks, { type: mimeType.split(';')[0] }))
    recorder.onerror = () => reject(new Error('视频录制失败'))
  })

  recorder.start(200)
  const start = performance.now()

  await new Promise<void>((resolve) => {
    const tick = () => {
      const elapsed = performance.now() - start
      const pct = Math.min(99, Math.round((elapsed / durationMs) * 100))
      onProgress?.(pct, `正在渲染视频 ${pct}%`)

      const { index, localT, fade } = sceneAtTime(scenes, elapsed)
      const current = scenes[index]
      const next = scenes[index + 1]

      ctx.fillStyle = '#0f1410'
      ctx.fillRect(0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT)

      const currentTransform = motionTransform(current.motion, localT)
      drawCoverImage(ctx, images[index], currentTransform)
      drawSubtitle(ctx, current.subtitle ?? '')

      if (fade > 0 && next) {
        ctx.save()
        ctx.globalAlpha = fade
        const nextTransform = motionTransform(next.motion, 0)
        drawCoverImage(ctx, images[index + 1], nextTransform)
        ctx.restore()
      }

      if (elapsed >= durationMs) {
        resolve()
        return
      }
      requestAnimationFrame(tick)
    }
    requestAnimationFrame(tick)
  })

  recorder.stop()
  const blob = await done
  onProgress?.(100, '渲染完成')
  return { blob, url: URL.createObjectURL(blob) }
}
