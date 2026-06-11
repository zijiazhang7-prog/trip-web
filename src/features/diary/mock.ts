import type {
  CreateDiaryBookRequest,
  CreateRouteSketchTaskRequest,
  GetDiaryBookDetailResponse,
  GetRouteSketchTaskResponse,
  ListDiaryBooksResponse,
  UpdateDiaryBookRequest,
  UpsertDiaryEntryRequest,
  UploadAssetRequest,
} from './contracts'
import type { DiaryBook, DiaryEntry, RouteSketchTask } from './types'

function uid(prefix: string): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

const booksStore: DiaryBook[] = []
const entriesStore: DiaryEntry[] = []
const routeTasksStore: RouteSketchTask[] = []

function delay(ms = 120): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function listDiaryBooksMock(): Promise<ListDiaryBooksResponse> {
  await delay()
  return { items: [...booksStore] }
}

export async function createDiaryBookMock(input: CreateDiaryBookRequest): Promise<DiaryBook> {
  await delay()
  const created: DiaryBook = {
    id: uid('book'),
    title: input.title,
    startDate: input.startDate,
    days: input.days,
    templateId: input.templateId,
    coverAssetUrl: '',
  }
  booksStore.unshift(created)
  entriesStore.push({
    id: uid('entry'),
    bookId: created.id,
    dayIndex: 1,
    title: '新的旅程',
    entryDate: input.startDate,
    blocks: [{ id: uid('blk_txt'), type: 'text', text: '' }],
  })
  return created
}

export async function deleteDiaryBookMock(bookId: string): Promise<void> {
  await delay()
  const bookIdx = booksStore.findIndex((it) => it.id === bookId)
  if (bookIdx < 0) return
  booksStore.splice(bookIdx, 1)
  for (let i = entriesStore.length - 1; i >= 0; i -= 1) {
    if (entriesStore[i].bookId === bookId) entriesStore.splice(i, 1)
  }
}

export async function getDiaryBookDetailMock(bookId: string): Promise<GetDiaryBookDetailResponse> {
  await delay()
  const book = booksStore.find((it) => it.id === bookId)
  if (!book) throw new Error('Diary book not found')
  return {
    book,
    entries: entriesStore.filter((entry) => entry.bookId === bookId),
  }
}

export async function updateDiaryBookMock(bookId: string, patch: UpdateDiaryBookRequest): Promise<DiaryBook> {
  await delay()
  const idx = booksStore.findIndex((it) => it.id === bookId)
  if (idx < 0) throw new Error('Diary book not found')
  booksStore[idx] = { ...booksStore[idx], ...patch }
  return booksStore[idx]
}

export async function upsertDiaryEntryMock(
  bookId: string,
  payload: UpsertDiaryEntryRequest,
): Promise<DiaryEntry> {
  await delay()
  const idx = entriesStore.findIndex((entry) => entry.bookId === bookId && entry.dayIndex === payload.dayIndex)
  const next: DiaryEntry = {
    id: idx >= 0 ? entriesStore[idx].id : uid('entry'),
    bookId,
    dayIndex: payload.dayIndex,
    title: payload.title,
    entryDate: payload.entryDate,
    blocks: payload.blocks,
  }
  if (idx >= 0) {
    entriesStore[idx] = next
  } else {
    entriesStore.push(next)
  }
  return next
}

export async function uploadAssetMock(input: UploadAssetRequest) {
  await delay()
  return {
    id: uid('asset'),
    url: `https://mock-assets.local/${encodeURIComponent(input.fileName)}`,
    mimeType: input.mimeType,
    sizeBytes: input.sizeBytes,
  }
}

export async function createRouteSketchTaskMock(
  input: CreateRouteSketchTaskRequest,
): Promise<RouteSketchTask> {
  await delay()
  const task: RouteSketchTask = {
    taskId: uid('route_task'),
    status: 'succeeded',
    resultUrl: `https://mock-assets.local/route-sketch/${encodeURIComponent(input.bookId)}.png`,
  }
  routeTasksStore.push(task)
  return task
}

export async function getRouteSketchTaskMock(taskId: string): Promise<GetRouteSketchTaskResponse> {
  await delay()
  const task = routeTasksStore.find((it) => it.taskId === taskId)
  if (!task) throw new Error('Route sketch task not found')
  return { task }
}
