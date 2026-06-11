import { useCallback, useState } from 'react'
import { hasStoredToken } from '../../api/http'
import { useTripContext } from '../../context/tripContext'
import { publishHandAccountEntry } from './publish'
import type { DiaryBook, DiaryEntry } from './types'

export function useDiaryPublish() {
  const { destinationId } = useTripContext()
  const [publishing, setPublishing] = useState(false)
  const [publishMsg, setPublishMsg] = useState<string | null>(null)
  const [publishError, setPublishError] = useState<string | null>(null)

  const publish = useCallback(
    async (
      book: DiaryBook,
      entry: DiaryEntry,
      leftText: string,
      rightText: string,
      visibility: 'public' | 'private',
    ) => {
      if (!hasStoredToken()) {
        setPublishError('请先登录后再发布手账')
        return null
      }
      const destId = destinationId ?? 101
      setPublishing(true)
      setPublishError(null)
      setPublishMsg(null)
      try {
        const diaryId = await publishHandAccountEntry({
          book,
          entry,
          leftText,
          rightText,
          destinationId: destId,
          visibility,
        })
        setPublishMsg(
          visibility === 'public'
            ? `已发布到手账社群（#${diaryId}），可在社群页查看`
            : `已保存为私密手账（#${diaryId}）`,
        )
        return diaryId
      } catch (err) {
        setPublishError(err instanceof Error ? err.message : '发布失败')
        return null
      } finally {
        setPublishing(false)
      }
    },
    [destinationId],
  )

  return { publish, publishing, publishMsg, publishError, clearPublishFeedback: () => { setPublishMsg(null); setPublishError(null) } }
}
