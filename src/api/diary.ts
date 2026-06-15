import type {
  CreateDiaryBookRequest,
  GetDiaryBookDetailResponse,
  ListDiaryBooksResponse,
  UpdateDiaryBookRequest,
  UpsertDiaryEntryRequest,
  UploadAssetRequest,
} from '../features/diary/contracts'
import type { DiaryAsset, DiaryBook, DiaryEntry } from '../features/diary/types'
import { createDiaryHttpApi } from './diaryHttp'
import {
  createDiaryBookMock,
  deleteDiaryBookMock,
  getDiaryBookDetailMock,
  listDiaryBooksMock,
  updateDiaryBookMock,
  upsertDiaryEntryMock,
  uploadAssetMock,
} from '../features/diary/mock'

export type DiaryApi = {
  listBooks: () => Promise<ListDiaryBooksResponse>
  createBook: (input: CreateDiaryBookRequest) => Promise<DiaryBook>
  deleteBook: (bookId: string) => Promise<void>
  getBookDetail: (bookId: string) => Promise<GetDiaryBookDetailResponse>
  updateBook: (bookId: string, patch: UpdateDiaryBookRequest) => Promise<DiaryBook>
  upsertEntry: (bookId: string, payload: UpsertDiaryEntryRequest) => Promise<DiaryEntry>
  uploadAsset: (payload: UploadAssetRequest) => Promise<DiaryAsset>
}

const mockDiaryApi: DiaryApi = {
  listBooks: listDiaryBooksMock,
  createBook: createDiaryBookMock,
  deleteBook: deleteDiaryBookMock,
  getBookDetail: getDiaryBookDetailMock,
  updateBook: updateDiaryBookMock,
  upsertEntry: upsertDiaryEntryMock,
  uploadAsset: uploadAssetMock,
}

/** 默认 HTTP 工厂；`VITE_USE_MOCK=true` 时书架全走本地 Mock。 */
export function getDiaryApi(): DiaryApi {
  if (import.meta.env.VITE_USE_MOCK === 'true') return mockDiaryApi
  return createDiaryHttpApi()
}
