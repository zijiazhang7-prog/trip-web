import { uploadCommunityMedia } from '../../api/community'
import type { DiaryBook, DiaryEntry } from './types'

const MAX_TITLE = 150
const MAX_CONTENT = 10000
const MAX_MEDIA = 9

function isServerFileUrl(url: string): boolean {
  return url.startsWith('/files/')
}

async function blobFromUrl(url: string): Promise<Blob | null> {
  try {
    const res = await fetch(url)
    if (!res.ok) return null
    return await res.blob()
  } catch {
    return null
  }
}

async function uploadBlobUrl(url: string, fallbackName: string): Promise<string | null> {
  const blob = await blobFromUrl(url)
  if (!blob || blob.size === 0) return null
  const ext = blob.type.includes('video') ? 'mp4' : blob.type.includes('png') ? 'png' : 'jpg'
  const file = new File([blob], `${fallbackName}.${ext}`, { type: blob.type || 'image/jpeg' })
  return uploadCommunityMedia(file)
}

/** 将 blob / 本地静态资源上传为 /files/ 路径，满足后端校验 */
export async function ensureServerMediaUrl(
  url: string,
  fileCache?: Map<string, File>,
  cacheKey?: string,
): Promise<string | null> {
  if (!url || url.startsWith('data:')) return null
  if (isServerFileUrl(url)) return url

  const cached = cacheKey ? fileCache?.get(cacheKey) : undefined
  if (cached) {
    return uploadCommunityMedia(cached)
  }

  if (url.startsWith('blob:') || url.startsWith('http') || url.startsWith('/')) {
    return uploadBlobUrl(url, cacheKey ?? 'diary')
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
    if (!fileUrl || seen.has(fileUrl) || out.length >= MAX_MEDIA) return
    seen.add(fileUrl)
    out.push({ mediaType, fileUrl })
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

  if (book.coverAssetUrl) {
    const cover = await ensureServerMediaUrl(book.coverAssetUrl, fileCache, `cover-${book.id}`)
    if (cover && !seen.has(cover)) push('image', cover)
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
