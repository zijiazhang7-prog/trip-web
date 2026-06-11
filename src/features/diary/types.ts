export type DiaryTemplateId = 'minimal' | 'polaroid' | 'timeline'

export type DiaryBlockType = 'text' | 'image' | 'video' | 'routeSketch'

export type DiaryTextBlock = {
  id: string
  type: 'text'
  text: string
}

export type DiaryImageBlock = {
  id: string
  type: 'image'
  assetId: string
  assetUrl?: string
  caption?: string
}

export type DiaryRouteSketchBlock = {
  id: string
  type: 'routeSketch'
  imageUrl: string
  prompt: string
}

export type DiaryVideoBlock = {
  id: string
  type: 'video'
  assetId: string
  assetUrl?: string
  caption?: string
}

export type DiaryPaperStyleBlock = {
  id: string
  type: 'paperStyle'
  paperUrl: string
}

export type DiaryContentBlock =
  | DiaryTextBlock
  | DiaryImageBlock
  | DiaryVideoBlock
  | DiaryPaperStyleBlock
  | DiaryRouteSketchBlock

export type DiaryBook = {
  id: string
  title: string
  startDate: string
  days: number
  templateId: DiaryTemplateId
  coverAssetUrl: string
}

export type DiaryEntry = {
  id: string
  bookId: string
  dayIndex: number
  title: string
  entryDate: string
  blocks: DiaryContentBlock[]
}

export type DiaryAsset = {
  id: string
  url: string
  width?: number
  height?: number
  mimeType: string
  sizeBytes: number
}

export type RouteSketchTaskStatus = 'queued' | 'running' | 'succeeded' | 'failed'

export type RouteSketchTask = {
  taskId: string
  status: RouteSketchTaskStatus
  resultUrl?: string
  errorMessage?: string
}
