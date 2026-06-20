import { useCallback, useEffect, useState } from 'react'
import { hasStoredToken } from '../../api/http'
import { fetchMyDiaryRating, submitDiaryRating } from '../../api/rating'
import { getLocalUserRating, setLocalUserRating, type RatingTargetType } from '../../api/userRating'
import { StarRatingInput } from './StarRatingInput'

type RatingPanelProps = {
  targetType: RatingTargetType
  targetId: number
  average?: number | null
  count?: number
  className?: string
}

export function RatingPanel({
  targetType,
  targetId,
  average,
  count,
  className = '',
}: RatingPanelProps) {
  const [userScore, setUserScore] = useState<number | null>(null)
  const [avg, setAvg] = useState<number | null>(average ?? null)
  const [ratingCount, setRatingCount] = useState(count ?? 0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setError(null)
    if (targetType === 'diary') {
      try {
        const r = await fetchMyDiaryRating(targetId)
        setUserScore(r.userScore)
        setAvg(r.ratingScore ?? average ?? null)
        setRatingCount(r.ratingCount ?? count ?? 0)
        return
      } catch {
        setUserScore(null)
      }
    }
    setUserScore(getLocalUserRating(targetType, targetId))
    setAvg(average ?? null)
    setRatingCount(count ?? 0)
  }, [targetType, targetId, average, count])

  useEffect(() => {
    void load()
  }, [load])

  const onRate = (score: number) => {
    if (!hasStoredToken()) {
      setError('请先登录后评分')
      return
    }
    setBusy(true)
    setError(null)
    if (targetType === 'diary') {
      void submitDiaryRating(targetId, score)
        .then(() => load())
        .catch((err) => setError(err instanceof Error ? err.message : '评分失败'))
        .finally(() => setBusy(false))
      return
    }
    setLocalUserRating(targetType, targetId, score)
    setUserScore(score)
    setBusy(false)
  }

  return (
    <div className={`space-y-1 ${className}`}>
      <StarRatingInput
        value={userScore}
        average={avg}
        count={ratingCount}
        disabled={busy || !hasStoredToken()}
        onChange={onRate}
      />
      {!userScore ? (
        <p className="font-body text-xs text-[var(--ds-muted-foreground)]">点击「我的评分」星星即可打分</p>
      ) : null}
      {error ? <p className="font-body text-xs text-red-600">{error}</p> : null}
    </div>
  )
}
