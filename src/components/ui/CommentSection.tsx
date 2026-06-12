import { useCallback, useEffect, useState } from 'react'
import {
  deleteComment,
  fetchComments,
  postComment,
  type CommentTargetType,
  type CommentVO,
} from '../../api/comment'
import { hasStoredToken } from '../../api/http'
import { useAuthUi } from '../../context/authUi'

type CommentSectionProps = {
  targetType: CommentTargetType
  targetId: number
  title?: string
  className?: string
}

function formatTime(iso?: string): string {
  if (!iso) return ''
  try {
    const d = new Date(iso)
    return d.toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

export function CommentSection({
  targetType,
  targetId,
  title = '评论',
  className = '',
}: CommentSectionProps) {
  const { requestLogin } = useAuthUi()
  const [comments, setComments] = useState<CommentVO[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [posting, setPosting] = useState(false)

  const load = useCallback(async () => {
    if (!targetId) return
    setLoading(true)
    setError(null)
    try {
      const page = await fetchComments(targetType, targetId, 1, 30)
      setComments(page.list)
    } catch (err) {
      setError(err instanceof Error ? err.message : '评论加载失败')
      setComments([])
    } finally {
      setLoading(false)
    }
  }, [targetType, targetId])

  useEffect(() => {
    void load()
  }, [load])

  const handlePost = async () => {
    const text = draft.trim()
    if (!text) return
    if (!hasStoredToken()) {
      requestLogin()
      return
    }
    setPosting(true)
    setError(null)
    try {
      const created = await postComment(targetType, targetId, text)
      setComments((prev) => [created, ...prev])
      setDraft('')
    } catch (err) {
      setError(err instanceof Error ? err.message : '发表评论失败')
    } finally {
      setPosting(false)
    }
  }

  const handleDelete = async (comment: CommentVO) => {
    if (!hasStoredToken()) {
      requestLogin()
      return
    }
    try {
      await deleteComment(targetType, comment.id)
      setComments((prev) => prev.filter((c) => c.id !== comment.id))
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除失败')
    }
  }

  return (
    <div className={`rounded-xl border border-[var(--ds-border)]/50 bg-[var(--ds-muted)]/25 p-4 ${className}`}>
      <p className="mb-3 font-body text-sm font-semibold text-[var(--ds-foreground)]">
        {title} {comments.length > 0 ? `(${comments.length})` : ''}
      </p>

      <div className="mb-3 flex gap-2">
        <input
          type="text"
          value={draft}
          maxLength={500}
          placeholder={hasStoredToken() ? '写下你的看法…' : '登录后可评论'}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') void handlePost()
          }}
          className="min-w-0 flex-1 rounded-xl border border-[var(--ds-border)] px-3 py-2 font-body text-sm"
        />
        <button
          type="button"
          disabled={posting || !draft.trim()}
          onClick={() => void handlePost()}
          className="shrink-0 rounded-full bg-[var(--ds-primary)] px-4 py-2 font-body text-xs font-semibold text-white disabled:opacity-50"
        >
          {posting ? '发送中…' : '发送'}
        </button>
      </div>

      {error ? (
        <p className="mb-2 font-body text-xs text-red-700">{error}</p>
      ) : null}

      {loading ? (
        <p className="font-body text-xs text-[var(--ds-muted-foreground)]">加载评论…</p>
      ) : comments.length === 0 ? (
        <p className="font-body text-xs text-[var(--ds-muted-foreground)]">暂无评论，来抢沙发吧</p>
      ) : (
        <ul className="max-h-48 space-y-3 overflow-y-auto">
          {comments.map((c) => (
            <li key={c.id} className="rounded-lg bg-white/80 px-3 py-2">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <p className="font-body text-xs font-semibold text-[var(--ds-foreground)]">
                    {c.nickname || `用户 ${c.userId}`}
                    {c.createdAt ? (
                      <span className="ml-2 font-normal text-[var(--ds-muted-foreground)]">
                        {formatTime(c.createdAt)}
                      </span>
                    ) : null}
                  </p>
                  <p className="mt-1 font-body text-sm leading-relaxed text-[var(--ds-foreground)]">
                    {c.contentText}
                  </p>
                </div>
                {hasStoredToken() ? (
                  <button
                    type="button"
                    onClick={() => void handleDelete(c)}
                    className="shrink-0 font-body text-[10px] text-[var(--ds-muted-foreground)] hover:text-red-600"
                  >
                    删除
                  </button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
