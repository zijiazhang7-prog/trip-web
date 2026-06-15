import { TOKEN_STORAGE_KEY } from '../../api/http'
import type { DiaryBook, DiaryEntry } from './types'

type DiaryStoreSnapshot = {
  books: DiaryBook[]
  entries: DiaryEntry[]
}

function storageKey(): string {
  try {
    const token = window.localStorage.getItem(TOKEN_STORAGE_KEY)
    return token ? `trip_diary_store_v1_${token.slice(-24)}` : 'trip_diary_store_v1_guest'
  } catch {
    return 'trip_diary_store_v1_guest'
  }
}

export function loadDiaryStoreSnapshot(): DiaryStoreSnapshot | null {
  try {
    const raw = window.localStorage.getItem(storageKey())
    if (!raw) return null
    const parsed = JSON.parse(raw) as DiaryStoreSnapshot
    if (!Array.isArray(parsed.books) || !Array.isArray(parsed.entries)) return null
    return parsed
  } catch {
    return null
  }
}

export function persistDiaryStoreSnapshot(books: DiaryBook[], entries: DiaryEntry[]): void {
  try {
    window.localStorage.setItem(storageKey(), JSON.stringify({ books, entries }))
  } catch {
    /* quota */
  }
}
