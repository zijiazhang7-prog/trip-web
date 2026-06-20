import { TOKEN_STORAGE_KEY } from './http'

export type RatingTargetType = 'destination' | 'food' | 'diary'

type StoredRatings = Record<string, number>

function storageKey(): string {
  try {
    const token = window.localStorage.getItem(TOKEN_STORAGE_KEY)
    return token ? `trip_user_ratings_v1_${token.slice(-24)}` : 'trip_user_ratings_v1_guest'
  } catch {
    return 'trip_user_ratings_v1_guest'
  }
}

function ratingKey(type: RatingTargetType, id: number): string {
  return `${type}:${id}`
}

function readAll(): StoredRatings {
  try {
    const raw = window.localStorage.getItem(storageKey())
    if (!raw) return {}
    return JSON.parse(raw) as StoredRatings
  } catch {
    return {}
  }
}

function writeAll(data: StoredRatings) {
  try {
    window.localStorage.setItem(storageKey(), JSON.stringify(data))
  } catch {
    /* quota */
  }
}

export function getLocalUserRating(type: RatingTargetType, id: number): number | null {
  const score = readAll()[ratingKey(type, id)]
  return typeof score === 'number' && score >= 1 && score <= 5 ? score : null
}

export function setLocalUserRating(type: RatingTargetType, id: number, score: number): void {
  const all = readAll()
  all[ratingKey(type, id)] = score
  writeAll(all)
}
