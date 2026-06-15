import { uploadCommunityMedia } from '../../api/community'
import { readLayoutFromBlocks } from './layoutPersist'
import type { DiaryBook, DiaryEntry } from './types'

const MAX_TITLE = 150
const MAX_CONTENT = 10000
const MAX_MEDIA = 9
const UPLOAD_TIMEOUT_MS = 90_000

function isServerFileUrl(url: string): boolean {
  return url.startsWith('/files/')
}

function shouldAttemptUpload(url: string): boolean {
  if (!url || url.startsWith('data:')) return false
  if (isServerFileUrl(url)) return true
  if (url.includes('/src/') || url.includes('@fs') || url.includes('node_modules')) return false
  return (
    url.startsWith('blob:') ||
    url.startsWith('http') ||
    url.startsWith('/assets/') ||
    url.startsWith('/')
  )
}

function resolveFetchUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:') || url.startsWith('data:')) {
    return url
  }
  if (typeof window !== 'undefined' && url.startsWith('/')) {
    return `${window.location.origin}${url}`
  }
  return url
}

async function blobFromUrl(url: string): Promise<Blob | null> {
  try {
    const res = await fetch(resolveFetchUrl(url))
    if (!res.ok) return null
    const blob = await res.blob()
    if (blob.size === 0) return null
    if (blob.type.includes('text/html')) return null
    return blob
  } catch {
    return null
  }
}

async function uploadBlobUrl(url: string, fallbackName: string): Promise<string | null> {
  const blob = await blobFromUrl(url)
  if (!blob) return null
  const ext = blob.type.includes('video') ? 'mp4' : blob.type.includes('png') ? 'png' : 'jpg'
  const file = new File([blob], `${fallbackName}.${ext}`, { type: blob.type || 'image/jpeg' })
  return uploadCommunityMedia(file, undefined, UPLOAD_TIMEOUT_MS)
}

/** 将 blob / 本地静态资源上传为 /files/ 路径，满足后端校验 */
export async function ensureServerMediaUrl(
  url: string,
  fileCache?: Map<string, File>,
  cacheKey?: string,
): Promise<string | null> {
  if (!shouldAttemptUpload(url)) return null
  if (isServerFileUrl(url)) return url

  try {
    const cached = cacheKey ? fileCache?.get(cacheKey) : undefined
    if (cached) {
      const uploaded = await uploadCommunityMedia(cached, undefined, UPLOAD_TIMEOUT_MS)
      return uploaded.startsWith('/files/') ? uploaded : null
    }

    if (url.startsWith('blob:') || url.startsWith('http') || url.startsWith('/')) {
      const uploaded = await uploadBlobUrl(url, cacheKey ?? 'diary')
      return uploaded?.startsWith('/files/') ? uploaded : null
    }
  } catch {
    return null
  }
  return null
}

export async function buildPublishMediaList(
  book: DiaryBook,
  entry: DiaryEntry,
  fileCache?: Map<string, File>,
): Promise<Array<{ mediaType: 'image' | 'video'; fileUrl: string }>> {
  const out: Array<{ mediaType: 'image' | 'video'; fileUrl: string }> = []
  const seen = new Set<string>()

  const push = (mediaType: 'image' | 'video', fileUrl: string) => {
    if (!fileUrl || !fileUrl.startsWith('/files/') || seen.has(fileUrl) || out.length >= MAX_MEDIA) return
    seen.add(fileUrl)
    out.push({ mediaType, fileUrl })
  }

  if (book.coverAssetUrl) {
    const cover = await ensureServerMediaUrl(book.coverAssetUrl, fileCache, `cover-${book.id}`)
    if (cover) push('image', cover)
  }

  for (const block of entry.blocks) {
    if (block.type === 'image' && block.assetUrl) {
      const uploaded = await ensureServerMediaUrl(block.assetUrl, fileCache, block.id)
      if (uploaded) push('image', uploaded)
    }
    if (block.type === 'video' && block.assetUrl) {
      const uploaded = await ensureServerMediaUrl(block.assetUrl, fileCache, block.id)
      if (uploaded) push('video', uploaded)
    }
    if (block.type === 'routeSketch' && block.imageUrl) {
      const uploaded = await ensureServerMediaUrl(block.imageUrl, fileCache, block.id)
      if (uploaded) push('image', uploaded)
    }
  }

  const { stickerLayers } = readLayoutFromBlocks(entry.blocks)
  for (const sticker of stickerLayers) {
    const uploaded = await ensureServerMediaUrl(sticker.url, fileCache, sticker.id)
    if (uploaded) push(sticker.kind === 'video' ? 'video' : 'image', uploaded)
  }

  return out
}

export function clampDiaryTitle(title: string): string {
  const t = title.trim()
  if (t.length <= MAX_TITLE) return t
  return `${t.slice(0, MAX_TITLE - 1)}…`
}

export function clampDiaryContent(contentText: string, plainFallback: string): string {
  let text = contentText.trim()
  if (!text.replace(/\[\[HANDACCOUNT_JSON\]\][^\n]*/, '').trim() && plainFallback.trim()) {
    const prefix = text.match(/^\[\[HANDACCOUNT_JSON\]\][^\n]*/)?.[0] ?? ''
    text = prefix ? `${prefix}\n\n${plainFallback.trim()}` : plainFallback.trim()
  }
  if (text.length <= MAX_CONTENT) return text
  return `${text.slice(0, MAX_CONTENT - 20)}\n\n（内容已截断）`
}
