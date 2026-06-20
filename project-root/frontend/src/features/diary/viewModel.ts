import type { DiaryBook, DiaryEntry } from './types'

export type DiaryBookView = {
  id: string
  title: string
  date: string
  days: number
  coverImage: string
  day1title: string
  content: string
  diaryImgs: string[]
}

export function toDiaryBookView(book: DiaryBook, entries: DiaryEntry[]): DiaryBookView {
  const dayOne = [...entries]
    .sort((a, b) => a.dayIndex - b.dayIndex)
    .find((entry) => entry.dayIndex === 1) ?? entries[0]

  const textBlock = dayOne?.blocks.find((block) => block.type === 'text')
  const imageBlocks = dayOne?.blocks.filter((block) => block.type === 'image') ?? []

  return {
    id: book.id,
    title: book.title,
    date: book.startDate,
    days: book.days,
    coverImage: book.coverAssetUrl,
    day1title: dayOne?.title ?? '旅程记录',
    content: textBlock?.type === 'text' ? textBlock.text : '',
    diaryImgs: imageBlocks
      .map((block) => block.assetUrl)
      .filter((url): url is string => Boolean(url)),
  }
}
