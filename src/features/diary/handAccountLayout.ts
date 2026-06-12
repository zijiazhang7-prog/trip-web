import type {
  DiaryLayoutStickerLayer,
  DiaryLayoutTextLayer,
} from './types'

export const LAYOUT_PREFIX = '[[HANDACCOUNT_LAYOUT]]'

export type HandAccountLayoutPayload = {
  paperUrl?: string
  textLayers: DiaryLayoutTextLayer[]
  stickerLayers: DiaryLayoutStickerLayer[]
}

export function injectLayoutIntoContent(
  contentText: string,
  layout: HandAccountLayoutPayload,
): string {
  const layoutLine = `${LAYOUT_PREFIX}${JSON.stringify(layout)}`
  const firstNl = contentText.indexOf('\n')
  if (firstNl < 0) return `${contentText}\n${layoutLine}`
  return `${contentText.slice(0, firstNl + 1)}${layoutLine}\n${contentText.slice(firstNl + 1)}`
}

export function parseHandAccountLayout(contentText: string | undefined): HandAccountLayoutPayload | null {
  if (!contentText) return null
  const line = contentText.split('\n').find((l) => l.startsWith(LAYOUT_PREFIX))
  if (!line) return null
  try {
    const parsed = JSON.parse(line.slice(LAYOUT_PREFIX.length)) as HandAccountLayoutPayload
    if (!parsed || !Array.isArray(parsed.textLayers) || !Array.isArray(parsed.stickerLayers)) return null
    return parsed
  } catch {
    return null
  }
}
