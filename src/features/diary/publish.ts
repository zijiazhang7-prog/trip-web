import { fetchRecommendedDestinationsPage } from '../../api/destination'
import { publishCommunityDiary } from '../../api/community'
import {
  buildPublishMediaList,
  clampDiaryContent,
  clampDiaryTitle,
} from './publishMedia'
import type { DiaryBook, DiaryEntry } from './types'

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
    contentText: `${jsonLine}\n\n${plain || '（图文手账）'}`,
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

export function entryPlainText(entry: DiaryEntry): string {
  const textBlock = entry.blocks.find((b) => b.type === 'text')
  if (!textBlock || textBlock.type !== 'text') return ''
  return textBlock.text.replace(/\n\n---PAGE_BREAK---\n\n/g, '\n\n').trim()
}

async function resolvePublishDestinationId(preferred: number | null | undefined): Promise<number> {
  if (typeof preferred === 'number' && preferred > 0) return preferred
  try {
    const page = await fetchRecommendedDestinationsPage({ pageSize: 5, sortBy: 'heat' })
    const hit = page.list.find((d) => typeof d.id === 'number' && d.id > 0)
    if (hit?.id) return hit.id
  } catch {
    /* 使用兜底 */
  }
  return 1
}

export async function publishHandAccountEntry(params: {
  book: DiaryBook
  entry: DiaryEntry
  leftText: string
  rightText: string
  destinationId: number
  visibility: 'public' | 'private'
  fileCache?: Map<string, File>
}): Promise<number> {
  const { contentText } = buildHandAccountContent(
    params.book,
    params.entry,
    params.leftText,
    params.rightText,
  )
  const plain = [params.leftText, params.rightText].filter(Boolean).join('\n\n')
  const mediaList = await buildPublishMediaList(params.book, params.entry, params.fileCache)
  const destId = await resolvePublishDestinationId(params.destinationId)

  return publishCommunityDiary({
    destinationId: destId,
    title: clampDiaryTitle(`${params.book.title} · ${params.entry.title}`),
    contentText: clampDiaryContent(contentText, plain || '（图文手账）'),
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
  fileCache?: Map<string, File>
}): Promise<number> {
  const { contentText } = buildHandAccountContent(params.book, params.entry, params.bodyText, '')
  const mediaList = await buildPublishMediaList(params.book, params.entry, params.fileCache)
  const destId = await resolvePublishDestinationId(params.destinationId)

  return publishCommunityDiary({
    destinationId: destId,
    title: clampDiaryTitle(params.title.trim() || params.book.title),
    contentText: clampDiaryContent(contentText, params.bodyText.trim() || '（图文手账）'),
    visibility: 'public',
    mediaList,
  })
}

export async function uploadDiaryFile(file: File): Promise<string> {
  const { uploadCommunityMedia } = await import('../../api/community')
  return uploadCommunityMedia(file)
}
