import { publishCommunityDiary, uploadCommunityMedia } from '../../api/community'
import type { DiaryBook, DiaryEntry, DiaryContentBlock } from './types'

export type HandAccountPayloadMeta = {
  handAccount: true
  bookTitle: string
  days: number
  coverUrl: string
  entryTitle: string
  startDate: string
}

const META_PREFIX = '[[HANDACCOUNT_JSON]]'

export function buildHandAccountContent(
  book: DiaryBook,
  entry: DiaryEntry,
  leftText: string,
  rightText: string,
): { contentText: string; meta: HandAccountPayloadMeta } {
  const meta: HandAccountPayloadMeta = {
    handAccount: true,
    bookTitle: book.title,
    days: book.days,
    coverUrl: book.coverAssetUrl,
    entryTitle: entry.title,
    startDate: book.startDate,
  }
  const plain = [leftText, rightText].filter(Boolean).join('\n\n')
  const jsonLine = `${META_PREFIX}${JSON.stringify(meta)}`
  return {
    contentText: `${jsonLine}\n\n${plain}`,
    meta,
  }
}

export function parseHandAccountMeta(contentText: string | undefined): HandAccountPayloadMeta | null {
  if (!contentText?.startsWith(META_PREFIX)) return null
  const lineEnd = contentText.indexOf('\n')
  const jsonPart = lineEnd > 0 ? contentText.slice(META_PREFIX.length, lineEnd) : contentText.slice(META_PREFIX.length)
  try {
    const parsed = JSON.parse(jsonPart) as HandAccountPayloadMeta
    return parsed.handAccount ? parsed : null
  } catch {
    return null
  }
}

export function excerptFromContent(contentText: string | undefined, title?: string): string {
  const meta = parseHandAccountMeta(contentText)
  const body = contentText?.replace(/^[^\n]*\n+/, '').trim() ?? ''
  if (meta) return body.slice(0, 120) || `${meta.bookTitle} · ${meta.entryTitle}`
  return body.slice(0, 120) || title || '这位旅行者还没有留下更多文字。'
}

async function resolveMediaUrls(blocks: DiaryContentBlock[]): Promise<Array<{ mediaType: 'image' | 'video'; fileUrl: string }>> {
  const out: Array<{ mediaType: 'image' | 'video'; fileUrl: string }> = []
  for (const block of blocks) {
    if (block.type === 'image' && block.assetUrl) {
      if (block.assetUrl.startsWith('blob:')) continue
      out.push({ mediaType: 'image', fileUrl: block.assetUrl.startsWith('/') ? block.assetUrl : `/${block.assetUrl}` })
    }
    if (block.type === 'video' && block.assetUrl) {
      if (block.assetUrl.startsWith('blob:')) continue
      out.push({ mediaType: 'video', fileUrl: block.assetUrl })
    }
    if (block.type === 'routeSketch' && block.imageUrl) {
      out.push({ mediaType: 'image', fileUrl: block.imageUrl })
    }
  }
  return out
}

export function entryPlainText(entry: DiaryEntry): string {
  const textBlock = entry.blocks.find((b) => b.type === 'text')
  if (!textBlock || textBlock.type !== 'text') return ''
  return textBlock.text.replace(/\n\n---PAGE_BREAK---\n\n/g, '\n\n').trim()
}

async function buildMediaListForBook(book: DiaryBook, entry: DiaryEntry) {
  const mediaList = await resolveMediaUrls(entry.blocks)
  const cover = book.coverAssetUrl
  if (cover && !cover.startsWith('blob:') && !mediaList.some((m) => m.fileUrl === cover)) {
    mediaList.unshift({ mediaType: 'image', fileUrl: cover.startsWith('/') ? cover : `/${cover}` })
  }
  return mediaList
}

export async function publishHandAccountEntry(params: {
  book: DiaryBook
  entry: DiaryEntry
  leftText: string
  rightText: string
  destinationId: number
  visibility: 'public' | 'private'
}): Promise<number> {
  const { contentText } = buildHandAccountContent(
    params.book,
    params.entry,
    params.leftText,
    params.rightText,
  )
  const mediaList = await buildMediaListForBook(params.book, params.entry)
  return publishCommunityDiary({
    destinationId: params.destinationId,
    title: `${params.book.title} · ${params.entry.title}`,
    contentText,
    visibility: params.visibility,
    mediaList,
  })
}

/** 社群页：选手账封面/素材，标题与正文由用户填写 */
export async function publishHandAccountWithCustomText(params: {
  book: DiaryBook
  entry: DiaryEntry
  title: string
  bodyText: string
  destinationId: number
}): Promise<number> {
  const { contentText } = buildHandAccountContent(params.book, params.entry, params.bodyText, '')
  const mediaList = await buildMediaListForBook(params.book, params.entry)
  return publishCommunityDiary({
    destinationId: params.destinationId,
    title: params.title.trim(),
    contentText,
    visibility: 'public',
    mediaList,
  })
}

export async function uploadDiaryFile(file: File): Promise<string> {
  return uploadCommunityMedia(file)
}
