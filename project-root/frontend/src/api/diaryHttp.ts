import type { DiaryApi } from './diary'
import type { GetDiaryBookDetailResponse, ListDiaryBooksResponse } from '../features/diary/contracts'
import type { DiaryBook, DiaryEntry } from '../features/diary/types'
import { parseHandAccountMeta } from '../features/diary/publish'
import {
  createDiaryBookMock,
  deleteDiaryBookMock,
  getDiaryBookDetailMock,
  listDiaryBooksMock,
  updateDiaryBookMock,
  upsertDiaryEntryMock,
  uploadAssetMock,
} from '../features/diary/mock'
import { hasStoredToken, httpRequest } from './http'

type DiaryMedia = {
  mediaType: string
  fileUrl: string
}

type RemoteDiary = {
  id: number
  title?: string
  contentText?: string
  destinationName?: string
  mediaList?: DiaryMedia[]
  createdAt?: string
}

type PageResult<T> = {
  list: T[]
}

const PUBLISHED_PREFIX = 'published-'

function isPublishedBookId(bookId: string): boolean {
  return bookId.startsWith(PUBLISHED_PREFIX)
}

function publishedDiaryId(bookId: string): number {
  return Number(bookId.slice(PUBLISHED_PREFIX.length))
}

async function fetchMyDiaries(): Promise<RemoteDiary[]> {
  const page = await httpRequest<PageResult<RemoteDiary>>('/api/v1/diaries/me?pageNum=1&pageSize=50', {
    method: 'GET',
  })
  return page.list ?? []
}

async function fetchRemoteDiaryDetail(id: number): Promise<RemoteDiary> {
  return httpRequest<RemoteDiary>(`/api/v1/diaries/${id}`, { method: 'GET' })
}

function remoteDiaryToBook(diary: RemoteDiary): DiaryBook {
  const meta = parseHandAccountMeta(diary.contentText)
  const cover =
    meta?.coverUrl ||
    diary.mediaList?.find((m) => m.mediaType !== 'video')?.fileUrl ||
    ''
  return {
    id: `${PUBLISHED_PREFIX}${diary.id}`,
    title: meta?.bookTitle || diary.title || '我的手账',
    startDate: diary.createdAt?.slice(0, 10) ?? new Date().toISOString().slice(0, 10),
    days: meta?.days ?? 1,
    templateId: 'minimal',
    coverAssetUrl: cover,
  }
}

function remoteDiaryToDetail(diary: RemoteDiary): GetDiaryBookDetailResponse {
  const book = remoteDiaryToBook(diary)
  const entry: DiaryEntry = {
    id: `entry-${diary.id}`,
    bookId: book.id,
    dayIndex: 1,
    title: book.title,
    entryDate: book.startDate,
    blocks: [{ id: `blk-${diary.id}`, type: 'text', text: diary.contentText ?? '' }],
  }
  return { book, entries: [entry] }
}

/**
 * 书架编辑仍用本地 Mock；已发布手账从 diaries/me 合并展示。
 */
export function createDiaryHttpApi(): DiaryApi {
  return {
    listBooks: async () => {
      const local = await listDiaryBooksMock()
      if (!hasStoredToken()) return local
      try {
        const remote = (await fetchMyDiaries()).map(remoteDiaryToBook)
        const seen = new Set(local.items.map((b) => b.id))
        const merged = [...local.items]
        for (const book of remote) {
          if (!seen.has(book.id)) merged.push(book)
        }
        return { items: merged } satisfies ListDiaryBooksResponse
      } catch {
        return local
      }
    },
    createBook: createDiaryBookMock,
    deleteBook: async (bookId) => {
      if (isPublishedBookId(bookId)) return
      await deleteDiaryBookMock(bookId)
    },
    getBookDetail: async (bookId) => {
      if (!isPublishedBookId(bookId)) return getDiaryBookDetailMock(bookId)
      const detail = await fetchRemoteDiaryDetail(publishedDiaryId(bookId))
      return remoteDiaryToDetail(detail)
    },
    updateBook: async (bookId, patch) => {
      if (isPublishedBookId(bookId)) {
        const current = await fetchRemoteDiaryDetail(publishedDiaryId(bookId))
        return remoteDiaryToBook({ ...current, title: patch.title ?? current.title })
      }
      return updateDiaryBookMock(bookId, patch)
    },
    upsertEntry: upsertDiaryEntryMock,
    uploadAsset: uploadAssetMock,
  }
}
