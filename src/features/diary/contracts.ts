import type { DiaryAsset, DiaryBook, DiaryEntry, RouteSketchTask } from './types'

export type ListDiaryBooksResponse = {
  items: DiaryBook[]
}

export type CreateDiaryBookRequest = {
  title: string
  startDate: string
  days: number
  templateId: DiaryBook['templateId']
}

export type CreateDiaryBookResponse = {
  item: DiaryBook
}

export type GetDiaryBookDetailResponse = {
  book: DiaryBook
  entries: DiaryEntry[]
}

export type UpdateDiaryBookRequest = Partial<
  Pick<DiaryBook, 'title' | 'startDate' | 'days' | 'templateId' | 'coverAssetUrl'>
>

export type UpsertDiaryEntryRequest = {
  dayIndex: number
  title: string
  entryDate: string
  blocks: DiaryEntry['blocks']
}

export type UpsertDiaryEntryResponse = {
  item: DiaryEntry
}

export type UploadAssetRequest = {
  fileName: string
  mimeType: string
  sizeBytes: number
}

export type UploadAssetResponse = {
  asset: DiaryAsset
}

export type CreateRouteSketchTaskRequest = {
  bookId: string
  prompt: string
  waypoints: string[]
}

export type CreateRouteSketchTaskResponse = {
  task: RouteSketchTask
}

export type GetRouteSketchTaskResponse = {
  task: RouteSketchTask
}
