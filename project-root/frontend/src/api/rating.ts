import { httpRequest } from './http'

export type DiaryRatingVO = {
  diaryId: number
  userScore: number | null
  ratingScore: number | null
  ratingCount: number
}

export async function submitDiaryRating(diaryId: number, score: number): Promise<boolean> {
  return httpRequest<boolean>(`/api/v1/diaries/${diaryId}/ratings`, {
    method: 'POST',
    body: JSON.stringify({ score }),
  })
}

export async function fetchMyDiaryRating(diaryId: number): Promise<DiaryRatingVO> {
  return httpRequest<DiaryRatingVO>(`/api/v1/diaries/${diaryId}/ratings/me`, { method: 'GET' })
}
