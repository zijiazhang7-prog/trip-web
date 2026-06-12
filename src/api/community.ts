import { excerptFromContent, parseHandAccountMeta } from '../features/diary/publish'
import { httpRequest } from './http'

type DiaryMedia = {
  id: number
  mediaType: string
  fileUrl: string
}

type DiaryItem = {
  id: number
  userId?: number
  username?: string
  destinationName?: string
  title?: string
  contentText?: string
  heatScore?: number
  ratingScore?: number
  ratingCount?: number
  mediaList?: DiaryMedia[]
}

type CreateDiaryRequest = {
  destinationId: number
  routeHistoryId?: number
  title: string
  contentText: string
  visibility: 'public' | 'private'
  mediaList: Array<{ mediaType: 'image' | 'video'; fileUrl: string }>
}

type CreateDiaryResponse = {
  diaryId: number
}

type UploadFileResponse = {
  fileUrl: string
}

type PageResult<T> = {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
  pages: number
}

export type CommunityFeedItem = {
  id: number
  userId?: number
  name: string
  location: string
  excerpt: string
  imgs: string[]
  videos?: string[]
  likes: number
  comments: number
  ratingScore?: number | null
  ratingCount?: number
  avatar: string
  title?: string
  fullText?: string
  handAccount?: boolean
  bookTitle?: string
  days?: number
  coverUrl?: string
}

const DEFAULT_AVATAR = 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=100'

function normalizeAssetUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

function toFeedItem(item: DiaryItem): CommunityFeedItem {
  const meta = parseHandAccountMeta(item.contentText)
  const mediaImgs = (item.mediaList || [])
    .filter((m) => m.mediaType !== 'video')
    .map((media) => normalizeAssetUrl(media.fileUrl))
  const coverFromMeta = meta?.coverUrl ? normalizeAssetUrl(meta.coverUrl) : undefined
  const cover =
    coverFromMeta ||
    (meta && mediaImgs[0] ? mediaImgs[0] : undefined)
  const videos = (item.mediaList || [])
    .filter((m) => m.mediaType === 'video')
    .map((media) => normalizeAssetUrl(media.fileUrl))
  const imgs = cover ? [cover, ...mediaImgs.filter((u) => u !== cover)].slice(0, 3) : mediaImgs.slice(0, 3)
  return {
    id: item.id,
    userId: item.userId,
    name: item.username || '旅行者',
    location: item.destinationName || '未知地点',
    excerpt: excerptFromContent(item.contentText, item.title),
    imgs,
    videos,
    likes: Math.max(0, Math.round(item.heatScore || 0)),
    comments: 0,
    ratingScore: item.ratingScore ?? null,
    ratingCount: item.ratingCount ?? 0,
    avatar: DEFAULT_AVATAR,
    title: meta?.bookTitle || item.title || '未命名手账',
    fullText: item.contentText || '',
    handAccount: Boolean(meta),
    bookTitle: meta?.bookTitle,
    days: meta?.days,
    coverUrl: cover,
  }
}

export async function fetchCommunityFeed(sortBy: 'latest' | 'heat' = 'latest'): Promise<CommunityFeedItem[]> {
  const query = new URLSearchParams({
    sortBy,
    pageNum: '1',
    pageSize: '20',
  })
  const page = await httpRequest<PageResult<DiaryItem>>(`/api/v1/diaries?${query.toString()}`, {
    method: 'GET',
  })
  return (page.list || []).map(toFeedItem)
}

export async function searchCommunityByTitle(title: string): Promise<CommunityFeedItem[]> {
  const query = new URLSearchParams({ title: title.trim() })
  const page = await httpRequest<PageResult<DiaryItem>>(`/api/v1/diaries/search/title?${query.toString()}`, {
    method: 'GET',
  })
  return (page.list || []).map(toFeedItem)
}

export async function searchCommunityByKeyword(keyword: string): Promise<CommunityFeedItem[]> {
  const query = new URLSearchParams({ keyword: keyword.trim(), pageNum: '1', pageSize: '30' })
  const page = await httpRequest<PageResult<DiaryItem>>(`/api/v1/diaries/search/fulltext?${query.toString()}`, {
    method: 'GET',
  })
  return (page.list || []).map(toFeedItem)
}

export async function publishCommunityDiary(
  input: CreateDiaryRequest,
  timeoutMs = 60_000,
): Promise<number> {
  const data = await httpRequest<CreateDiaryResponse>('/api/v1/diaries', {
    method: 'POST',
    body: JSON.stringify(input),
    timeoutMs,
  })
  return data.diaryId
}

export async function fetchCommunityDiaryDetail(id: number): Promise<CommunityFeedItem> {
  const item = await httpRequest<DiaryItem>(`/api/v1/diaries/${id}`, { method: 'GET' })
  return toFeedItem(item)
}

export async function fetchDiariesByDestination(
  destinationId: number,
  sortBy: 'latest' | 'heat' = 'heat',
): Promise<CommunityFeedItem[]> {
  const query = new URLSearchParams({
    sortBy,
    pageNum: '1',
    pageSize: '30',
  })
  const page = await httpRequest<PageResult<DiaryItem>>(
    `/api/v1/destinations/${destinationId}/diaries?${query.toString()}`,
    { method: 'GET' },
  )
  return (page.list || []).map(toFeedItem)
}

export async function deleteCommunityDiary(diaryId: number): Promise<boolean> {
  return httpRequest<boolean>(`/api/v1/diaries/${diaryId}`, { method: 'DELETE' })
}

export async function uploadCommunityMedia(
  file: File,
  refId?: number,
  timeoutMs = 90_000,
): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  form.append('bizType', 'diary')
  if (typeof refId === 'number') form.append('refId', String(refId))
  const data = await httpRequest<UploadFileResponse>('/api/v1/files/upload', {
    method: 'POST',
    body: form,
    timeoutMs,
  })
  return data.fileUrl
}
