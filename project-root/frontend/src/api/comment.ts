import { coerceSpringPage, type NormalizedPage } from './coercePage'
import { httpRequest } from './http'

export type CommentTargetType = 'destination' | 'food' | 'diary'

export type CommentVO = {
  id: number
  targetType: CommentTargetType
  targetId: number
  userId: number
  nickname?: string
  avatarUrl?: string | null
  contentText: string
  createdAt?: string
}

const LIST_PATH: Record<CommentTargetType, (id: number) => string> = {
  destination: (id) => `/api/v1/destinations/${id}/comments`,
  food: (id) => `/api/v1/foods/${id}/comments`,
  diary: (id) => `/api/v1/diaries/${id}/comments`,
}

const POST_PATH = LIST_PATH

export async function fetchComments(
  targetType: CommentTargetType,
  targetId: number,
  pageNum = 1,
  pageSize = 20,
): Promise<NormalizedPage<CommentVO>> {
  const query = new URLSearchParams({
    pageNum: String(pageNum),
    pageSize: String(pageSize),
  })
  const raw = await httpRequest<unknown>(`${LIST_PATH[targetType](targetId)}?${query}`, {
    method: 'GET',
  })
  return coerceSpringPage<CommentVO>(raw)
}

export async function postComment(
  targetType: CommentTargetType,
  targetId: number,
  contentText: string,
): Promise<CommentVO> {
  return httpRequest<CommentVO>(POST_PATH[targetType](targetId), {
    method: 'POST',
    body: JSON.stringify({ contentText: contentText.trim() }),
  })
}

export async function deleteComment(
  commentType: CommentTargetType,
  commentId: number,
): Promise<boolean> {
  return httpRequest<boolean>(`/api/v1/comments/${commentType}/${commentId}`, {
    method: 'DELETE',
  })
}
