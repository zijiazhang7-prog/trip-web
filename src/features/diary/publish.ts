import { fetchRecommendedDestinationsPage } from '../../api/destination'
import { publishCommunityDiary } from '../../api/community'
import {
  injectLayoutIntoContent,
  LAYOUT_PREFIX,
  type HandAccountLayoutPayload,
} from './handAccountLayout'
import { readLayoutFromBlocks } from './layoutPersist'
import {
  buildPublishMediaList,
  clampDiaryContent,
  clampDiaryTitle,
  ensureServerMediaUrl,
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
  const coverUrl =
    book.coverAssetUrl?.startsWith('/files/') || book.coverAssetUrl?.startsWith('http')
      ? book.coverAssetUrl
      : ''
  const meta: HandAccountPayloadMeta = {
    handAccount: true,
    bookTitle: book.title,
    days: book.days,
    coverUrl,
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
  if (!contentText?.includes(META_PREFIX)) return null
  const line = contentText.split('\n').find((l) => l.startsWith(META_PREFIX))
  if (!line) return null
  try {
    const parsed = JSON.parse(line.slice(META_PREFIX.length)) as HandAccountPayloadMeta
    return parsed.handAccount ? parsed : null
  } catch {
    return null
  }
}

export function patchMetaCoverUrl(contentText: string, coverUrl: string): string {
  const meta = parseHandAccountMeta(contentText)
  if (!meta || !coverUrl.startsWith('/files/')) return contentText
  const metaLine = `${META_PREFIX}${JSON.stringify({ ...meta, coverUrl })}`
  const lines = contentText.split('\n')
  const idx = lines.findIndex((l) => l.startsWith(META_PREFIX))
  if (idx < 0) return contentText
  lines[idx] = metaLine
  return lines.join('\n')
}

export function stripHandAccountMachineLines(contentText: string | undefined): string {
  if (!contentText) return ''
  return contentText
    .split('\n')
    .filter((line) => !line.startsWith(META_PREFIX) && !line.startsWith(LAYOUT_PREFIX))
    .join('\n')
    .trim()
}

async function resolveLayoutForPublish(
  entry: DiaryEntry,
  fileCache?: Map<string, File>,
): Promise<HandAccountLayoutPayload> {
  const { textLayers, stickerLayers } = readLayoutFromBlocks(entry.blocks)
  const paperBlock = entry.blocks.find((b) => b.type === 'paperStyle')
  let paperUrl = paperBlock?.type === 'paperStyle' ? paperBlock.paperUrl : undefined
  if (paperUrl) {
    const uploaded = await ensureServerMediaUrl(paperUrl, fileCache, 'paper-style')
    if (uploaded) paperUrl = uploaded
  }

  const resolvedStickers = await Promise.all(
    stickerLayers.map(async (s) => {
      const uploaded = await ensureServerMediaUrl(s.url, fileCache, s.id)
      return { ...s, url: uploaded ?? s.url }
    }),
  )

  return { paperUrl, textLayers, stickerLayers: resolvedStickers }
}

async function buildPublishContentText(
  book: DiaryBook,
  entry: DiaryEntry,
  leftText: string,
  rightText: string,
  fileCache?: Map<string, File>,
): Promise<{ contentText: string; coverUrl: string }> {
  const coverUploaded = book.coverAssetUrl
    ? await ensureServerMediaUrl(book.coverAssetUrl, fileCache, `cover-${book.id}`)
    : null
  const { contentText: base, meta } = buildHandAccountContent(book, entry, leftText, rightText)
  const coverUrl = coverUploaded ?? meta.coverUrl
  const metaLine = `${META_PREFIX}${JSON.stringify({ ...meta, coverUrl })}`
  const plain = stripHandAccountMachineLines(base)
  const layout = await resolveLayoutForPublish(entry, fileCache)
  return { contentText: injectLayoutIntoContent(`${metaLine}\n\n${plain}`, layout), coverUrl }
}

export function excerptFromContent(contentText: string | undefined, title?: string): string {
  const meta = parseHandAccountMeta(contentText)
  const body = stripHandAccountMachineLines(contentText)
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
  const mediaList = await buildPublishMediaList(params.book, params.entry, params.fileCache)
  let { contentText, coverUrl } = await buildPublishContentText(
    params.book,
    params.entry,
    params.leftText,
    params.rightText,
    params.fileCache,
  )
  if (!coverUrl) {
    const coverMedia = mediaList.find((m) => m.mediaType === 'image')?.fileUrl
    if (coverMedia) {
      coverUrl = coverMedia
      contentText = patchMetaCoverUrl(contentText, coverMedia)
    }
  }
  const plain = [params.leftText, params.rightText].filter(Boolean).join('\n\n')
  const destId = await resolvePublishDestinationId(params.destinationId)

  const payload = {
    destinationId: destId,
    title: clampDiaryTitle(`${params.book.title} · ${params.entry.title}`),
    contentText: clampDiaryContent(contentText, plain || '（图文手账）'),
    visibility: params.visibility,
    mediaList,
  }
  try {
    return await publishCommunityDiary(payload)
  } catch (first) {
    if (mediaList.length === 0) throw first
    return publishCommunityDiary({ ...payload, mediaList: [] })
  }
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
  const mediaList = await buildPublishMediaList(params.book, params.entry, params.fileCache)
  let { contentText, coverUrl } = await buildPublishContentText(
    params.book,
    params.entry,
    params.bodyText,
    '',
    params.fileCache,
  )
  if (!coverUrl) {
    const coverMedia = mediaList.find((m) => m.mediaType === 'image')?.fileUrl
    if (coverMedia) {
      coverUrl = coverMedia
      contentText = patchMetaCoverUrl(contentText, coverMedia)
    }
  }
  const destId = await resolvePublishDestinationId(params.destinationId)

  const payload = {
    destinationId: destId,
    title: clampDiaryTitle(params.title.trim() || params.book.title),
    contentText: clampDiaryContent(contentText, params.bodyText.trim() || '（图文手账）'),
    visibility: 'public' as const,
    mediaList,
  }
  try {
    return await publishCommunityDiary(payload)
  } catch (first) {
    if (mediaList.length === 0) throw first
    return publishCommunityDiary({ ...payload, mediaList: [] })
  }
}

export async function uploadDiaryFile(file: File): Promise<string> {
  const { uploadCommunityMedia } = await import('../../api/community')
  return uploadCommunityMedia(file)
}
