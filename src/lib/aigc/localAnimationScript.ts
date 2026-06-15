import type { AnimationScript, DiaryAnimationResult } from './types'

const MOTIONS = ['zoom_in', 'pan_left', 'zoom_out', 'pan_right'] as const
const TRANSITIONS = ['fade', 'dissolve', 'slide'] as const
const SCENE_DURATION_MS = 4000

export function buildLocalAnimationScript(input: {
  items: Array<{ fileUrl: string; mediaId?: number }>
  destinationName?: string
  diaryTitle?: string
}): DiaryAnimationResult {
  const destinationName = input.destinationName?.trim() || '旅行目的地'
  const diaryTitle = input.diaryTitle?.trim() || '旅行回忆'
  const title = `${destinationName} · ${diaryTitle} 动画回顾`.slice(0, 150)
  const narration = `跟随照片回顾这次 ${destinationName} 之旅。`

  const scenes = input.items.map((item, index) => {
    const order = index + 1
    return {
      order,
      mediaId: item.mediaId ?? order,
      fileUrl: item.fileUrl,
      visualDescription: `第 ${order} 张旅行照片的画面内容。`,
      durationMs: SCENE_DURATION_MS,
      motion: MOTIONS[index % MOTIONS.length],
      transition: TRANSITIONS[index % TRANSITIONS.length],
      subtitle: `第 ${order} 幕 · ${destinationName}`,
      narration:
        order === 1
          ? `旅程从 ${destinationName} 的第一张照片开始。`
          : order === input.items.length
            ? '用最后一张照片结束这次旅行回顾。'
            : `继续浏览旅途中的第 ${order} 个画面。`,
    }
  })

  const script: AnimationScript = {
    schemaVersion: '1.0',
    aspectRatio: '16:9',
    totalDurationMs: scenes.length * SCENE_DURATION_MS,
    backgroundMusic: 'light-travel',
    scenes,
  }

  return {
    id: 0,
    diaryId: 0,
    provider: 'local-fallback',
    title,
    narration,
    status: 'ready',
    script,
  }
}
