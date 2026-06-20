import type {
  DiaryContentBlock,
  DiaryLayoutBlock,
  DiaryLayoutStickerLayer,
  DiaryLayoutTextLayer,
} from './types'

export function withoutLayoutBlocks(blocks: DiaryContentBlock[]): DiaryContentBlock[] {
  return blocks.filter((block) => block.type !== 'layout')
}

export function readLayoutFromBlocks(blocks: DiaryContentBlock[]): {
  textLayers: DiaryLayoutTextLayer[]
  stickerLayers: DiaryLayoutStickerLayer[]
} {
  const layout = blocks.find((block): block is DiaryLayoutBlock => block.type === 'layout')
  return {
    textLayers: layout?.textLayers ?? [],
    stickerLayers: layout?.stickerLayers ?? [],
  }
}

export function appendLayoutBlock(
  blocks: DiaryContentBlock[],
  layoutId: string,
  textLayers: DiaryLayoutTextLayer[],
  stickerLayers: DiaryLayoutStickerLayer[],
): DiaryContentBlock[] {
  const base = withoutLayoutBlocks(blocks)
  return [
    ...base,
    {
      id: layoutId,
      type: 'layout',
      textLayers,
      stickerLayers,
    },
  ]
}
