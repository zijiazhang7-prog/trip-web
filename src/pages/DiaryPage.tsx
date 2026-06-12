import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAiApi } from '../api/ai'
import { getDiaryApi } from '../api/diary'
import {
  describeTravelImage,
  fileToDataUrl,
  generateDiaryDayLayout,
  hasDoubaoKey,
  hasGlmKey,
  polishDiaryText,
} from '../api/llm'
import { appendLayoutBlock, readLayoutFromBlocks } from '../features/diary/layoutPersist'
import { useDiaryPublish } from '../features/diary/useDiaryPublish'
import type { DiaryBook, DiaryContentBlock, DiaryEntry } from '../features/diary/types'
import { InlineNotice } from '../components/ui/InlineNotice'

const coverLoaders = import.meta.glob('/src/features/diary/components/cover/*.{png,jpg,jpeg,webp}', {
  query: '?url',
  import: 'default',
})
const paperLoaders = import.meta.glob('/src/features/diary/components/paper/*.{png,jpg,jpeg,webp}', {
  query: '?url',
  import: 'default',
})
const stickerLoaders = {
  ...import.meta.glob('/src/features/diary/components/sticker/*.{png,jpg,jpeg,webp}', {
    query: '?url',
    import: 'default',
  }),
  ...import.meta.glob('/src/features/diary/components/paster/*.{png,jpg,jpeg,webp}', {
    query: '?url',
    import: 'default',
  }),
}

type AssetLoaders = Record<string, () => Promise<string>>
type TextLayer = { id: string; value: string; top: number; left: number; scale: number; rotate: number; z: number }
type StickerLayer = {
  id: string
  url: string
  left: number
  top: number
  scale: number
  rotate: number
  z: number
  kind?: 'sticker' | 'image' | 'video'
  blockId?: string
}
type MaterialTab = 'cover' | 'paper' | 'sticker'
type SnapGuide = { x: number | null; y: number | null }
type StickerPointerEvent = React.PointerEvent<HTMLElement>

const PAGE_SPLIT_MARK = '\n\n---PAGE_BREAK---\n\n'
const panel = 'ds-glass-panel rounded-[24px]'

function uid(prefix: string): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
}

function splitPages(raw: string): { left: string; right: string } {
  const [left, right] = raw.split(PAGE_SPLIT_MARK)
  return { left: left ?? '', right: right ?? '' }
}

function mergePages(left: string, right: string): string {
  return `${left}${PAGE_SPLIT_MARK}${right}`
}

function autoResizeTextarea(el: HTMLTextAreaElement | null) {
  if (!el) return
  el.style.height = '0px'
  el.style.height = `${el.scrollHeight}px`
}

async function loadAssetsPage(
  loaders: AssetLoaders,
  pageSize: number,
  offset = 0,
): Promise<{ items: string[]; hasMore: boolean; failed: number }> {
  const keys = Object.keys(loaders).sort((a, b) => a.localeCompare(b, 'zh-CN'))
  const slice = keys.slice(offset, offset + pageSize)
  const settled = await Promise.allSettled(slice.map((key) => loaders[key]()))
  const items = settled
    .filter((item): item is PromiseFulfilledResult<string> => item.status === 'fulfilled')
    .map((item) => item.value)
  const failed = settled.length - items.length
  return { items, hasMore: offset + pageSize < keys.length, failed }
}

export function DiaryPage() {
  const navigate = useNavigate()
  const diaryApi = useMemo(() => getDiaryApi(), [])
  const aiApi = useMemo(() => getAiApi(), [])
  const { publish, publishing, publishMsg, publishError, clearPublishFeedback } = useDiaryPublish()
  const [saveToast, setSaveToast] = useState(false)
  const layoutBlockIdRef = useRef<string>(uid('blk_layout'))
  const [books, setBooks] = useState<DiaryBook[]>([])
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null)
  const [selectedBook, setSelectedBook] = useState<DiaryBook | null>(null)
  const [bookEntries, setBookEntries] = useState<DiaryEntry[]>([])
  const [selectedEntry, setSelectedEntry] = useState<DiaryEntry | null>(null)
  const [currentEntryIndex, setCurrentEntryIndex] = useState(0)
  const [entryTitle, setEntryTitle] = useState('新的旅程')
  const [leftPageText, setLeftPageText] = useState('')
  const [rightPageText, setRightPageText] = useState('')
  const [loadingBook, setLoadingBook] = useState(false)
  const [uploadedCount, setUploadedCount] = useState(0)
  const [aiBusy, setAiBusy] = useState(false)
  const [uploadBusy, setUploadBusy] = useState(false)
  const [aiError, setAiError] = useState<string | null>(null)
  const [isShelfCollapsed, setIsShelfCollapsed] = useState(false)
  const [isMaterialCollapsed, setIsMaterialCollapsed] = useState(false)
  const [bookOpened, setBookOpened] = useState(false)
  const [pageFlipDir, setPageFlipDir] = useState<'next' | 'prev' | null>(null)
  const [pageTurning, setPageTurning] = useState(false)
  const [soundEnabled, setSoundEnabled] = useState(true)
  const [textLayers, setTextLayers] = useState<TextLayer[]>([])
  const [activeTextLayerId, setActiveTextLayerId] = useState<string | null>(null)
  const [stickerLayers, setStickerLayers] = useState<StickerLayer[]>([])
  const [activeStickerId, setActiveStickerId] = useState<string | null>(null)
  const [snapGuide, setSnapGuide] = useState<SnapGuide>({ x: null, y: null })
  const [deleteTarget, setDeleteTarget] = useState<DiaryBook | null>(null)
  const [activeMaterialTab, setActiveMaterialTab] = useState<MaterialTab>('cover')
  const spreadRef = useRef<HTMLDivElement | null>(null)
  const rightPageRef = useRef<HTMLDivElement | null>(null)
  const leftPageTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const rightPageTextareaRef = useRef<HTMLTextAreaElement | null>(null)
  const audioContextRef = useRef<AudioContext | null>(null)
  const stickerDragRef = useRef<{ id: string; offsetX: number; offsetY: number } | null>(null)
  const stickerResizeRef = useRef<{ id: string; startScale: number; startDist: number; cx: number; cy: number } | null>(null)
  const stickerRotateRef = useRef<{ id: string; startRotate: number; startAngle: number; cx: number; cy: number } | null>(null)
  const textLayerDragRef = useRef<{ id: string; offsetX: number; offsetY: number } | null>(null)
  const textLayerResizeRef = useRef<{
    id: string
    startScale: number
    startDist: number
    cx: number
    cy: number
  } | null>(null)
  const textLayerRotateRef = useRef<{
    id: string
    startRotate: number
    startAngle: number
    cx: number
    cy: number
  } | null>(null)
  const pendingTextFocusRef = useRef<string | null>(null)
  const uploadFileCacheRef = useRef<Map<string, File>>(new Map())

  const [coverAssets, setCoverAssets] = useState<string[]>([])
  const [paperAssets, setPaperAssets] = useState<string[]>([])
  const [stickerAssets, setStickerAssets] = useState<string[]>([])
  const [coverOffset, setCoverOffset] = useState(0)
  const [paperOffset, setPaperOffset] = useState(0)
  const [stickerOffset, setStickerOffset] = useState(0)
  const [coverHasMore, setCoverHasMore] = useState(false)
  const [paperHasMore, setPaperHasMore] = useState(false)
  const [stickerHasMore, setStickerHasMore] = useState(false)
  const [loadingCoverAssets, setLoadingCoverAssets] = useState(false)
  const [loadingPaperAssets, setLoadingPaperAssets] = useState(false)
  const [loadingStickerAssets, setLoadingStickerAssets] = useState(false)
  const [coverAssetError, setCoverAssetError] = useState<string | null>(null)
  const [paperAssetError, setPaperAssetError] = useState<string | null>(null)
  const [stickerAssetError, setStickerAssetError] = useState<string | null>(null)
  const [selectedPaper, setSelectedPaper] = useState<string | null>(null)
  const paperAssetsRef = useRef<string[]>([])
  paperAssetsRef.current = paperAssets

  useLayoutEffect(() => {
    autoResizeTextarea(leftPageTextareaRef.current)
    autoResizeTextarea(rightPageTextareaRef.current)
  }, [leftPageText, rightPageText, bookOpened, currentEntryIndex])

  useLayoutEffect(() => {
    textLayers.forEach((layer) => {
      autoResizeTextarea(document.getElementById(`text-layer-input-${layer.id}`) as HTMLTextAreaElement | null)
    })
  }, [textLayers])

  const applyEntryToEditor = (entry: DiaryEntry | null) => {
    setSelectedEntry(entry)
    setEntryTitle(entry?.title ?? '新的旅程')
    const textBlock = entry?.blocks.find((block) => block.type === 'text')
    const pages = splitPages(textBlock?.type === 'text' ? textBlock.text : '')
    setLeftPageText(pages.left)
    setRightPageText(pages.right)
    const paperBlock = entry?.blocks.find((block): block is Extract<DiaryContentBlock, { type: 'paperStyle' }> => block.type === 'paperStyle')
    setSelectedPaper(paperBlock?.paperUrl ?? paperAssetsRef.current[0] ?? null)
    const layout = readLayoutFromBlocks(entry?.blocks ?? [])
    const layoutBlock = entry?.blocks.find((block): block is Extract<DiaryContentBlock, { type: 'layout' }> => block.type === 'layout')
    layoutBlockIdRef.current = layoutBlock?.id ?? uid('blk_layout')
    setTextLayers(layout.textLayers)
    setActiveTextLayerId(null)
    if (layout.stickerLayers.length > 0) {
      setStickerLayers(layout.stickerLayers)
    } else {
      const mediaLayers: StickerLayer[] = []
      let z = 1
      for (const block of entry?.blocks ?? []) {
        if (block.type !== 'image' && block.type !== 'video') continue
        const url = block.assetUrl
        if (!url) continue
        mediaLayers.push({
          id: uid('media_restore'),
          url,
          kind: block.type,
          blockId: block.id,
          left: 34 + (mediaLayers.length % 4) * 7,
          top: 42 + (mediaLayers.length % 3) * 5,
          scale: block.type === 'video' ? 1.15 : 1,
          rotate: 0,
          z: z++,
        })
      }
      setStickerLayers(mediaLayers)
    }
    setActiveStickerId(null)
  }

  useEffect(() => {
    let cancelled = false
    const boot = async () => {
      const result = await diaryApi.listBooks()
      if (cancelled) return
      if (result.items.length === 0) {
        const created = await diaryApi.createBook({
          title: '我的手账 1',
          startDate: new Date().toISOString().slice(0, 10),
          days: 1,
          templateId: 'minimal',
        })
        if (!cancelled) {
          setBooks([created])
          setSelectedBookId(created.id)
        }
        return
      }
      setBooks(result.items)
      setSelectedBookId(result.items[0]?.id ?? null)
    }
    void boot()
    return () => {
      cancelled = true
    }
  }, [diaryApi])

  useEffect(() => {
    let cancelled = false
    if (activeMaterialTab !== 'cover' || coverAssets.length > 0 || loadingCoverAssets) return
    const run = async () => {
      setLoadingCoverAssets(true)
      setCoverAssetError(null)
      try {
        const { items, hasMore, failed } = await loadAssetsPage(coverLoaders as AssetLoaders, 9, 0)
        if (!cancelled) {
          setCoverAssets(items)
          setCoverOffset(items.length)
          setCoverHasMore(hasMore)
          if (failed > 0) setCoverAssetError(`有 ${failed} 张封面加载失败`)
        }
      } catch {
        if (!cancelled) setCoverAssetError('封面素材加载失败')
      } finally {
        setLoadingCoverAssets(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [activeMaterialTab, coverAssets.length, loadingCoverAssets])

  useEffect(() => {
    let cancelled = false
    if (activeMaterialTab !== 'paper' || paperAssets.length > 0 || loadingPaperAssets) return
    const run = async () => {
      setLoadingPaperAssets(true)
      setPaperAssetError(null)
      try {
        const { items, hasMore, failed } = await loadAssetsPage(paperLoaders as AssetLoaders, 9, 0)
        if (!cancelled) {
          setPaperAssets(items)
          setPaperOffset(items.length)
          setPaperHasMore(hasMore)
          if (!selectedPaper) setSelectedPaper(items[0] ?? null)
          if (failed > 0) setPaperAssetError(`有 ${failed} 张内页加载失败`)
        }
      } catch {
        if (!cancelled) setPaperAssetError('内页素材加载失败')
      } finally {
        setLoadingPaperAssets(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [activeMaterialTab, loadingPaperAssets, paperAssets.length, selectedPaper])

  useEffect(() => {
    let cancelled = false
    if (activeMaterialTab !== 'sticker' || stickerAssets.length > 0 || loadingStickerAssets) return
    const run = async () => {
      setLoadingStickerAssets(true)
      setStickerAssetError(null)
      try {
        const { items, hasMore, failed } = await loadAssetsPage(stickerLoaders as AssetLoaders, 24, 0)
        if (!cancelled) {
          setStickerAssets(items)
          setStickerOffset(items.length)
          setStickerHasMore(hasMore)
          if (failed > 0) setStickerAssetError(`有 ${failed} 张贴纸加载失败`)
        }
      } catch {
        if (!cancelled) setStickerAssetError('贴纸素材加载失败')
      } finally {
        setLoadingStickerAssets(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [activeMaterialTab, loadingStickerAssets, stickerAssets.length])

  useEffect(() => {
    const id = pendingTextFocusRef.current
    if (!id) return
    pendingTextFocusRef.current = null
    requestAnimationFrame(() => {
      document.getElementById(`text-layer-input-${id}`)?.focus()
    })
  }, [textLayers])

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      if (!selectedBookId) {
        setSelectedBook(null)
        setBookEntries([])
        setSelectedEntry(null)
        setCurrentEntryIndex(0)
        setTextLayers([])
        setActiveTextLayerId(null)
        return
      }
      setLoadingBook(true)
      try {
        const detail = await diaryApi.getBookDetail(selectedBookId)
        if (cancelled) return
        setSelectedBook(detail.book)
        const orderedEntries = [...detail.entries].sort((a, b) => a.dayIndex - b.dayIndex)
        setBookEntries(orderedEntries)
        const first = orderedEntries[0] ?? null
        setCurrentEntryIndex(0)
        applyEntryToEditor(first)
        if (isShelfCollapsed && detail.book) setBookOpened(true)
      } finally {
        if (!cancelled) setLoadingBook(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [diaryApi, selectedBookId, isShelfCollapsed])

  const routeBlocks = useMemo(
    () => selectedEntry?.blocks.filter((block) => block.type === 'routeSketch') ?? [],
    [selectedEntry],
  )

  const buildEntryBlocksForSave = (): DiaryContentBlock[] => {
    if (!selectedEntry) return []
    const mergedText = mergePages(leftPageText, rightPageText)
    const textMergedBlocks = selectedEntry.blocks.some((block) => block.type === 'text')
      ? selectedEntry.blocks.map((block) => (block.type === 'text' ? { ...block, text: mergedText } : block))
      : [{ id: uid('blk_txt'), type: 'text', text: mergedText } as DiaryContentBlock, ...selectedEntry.blocks]
    const withPaper = selectedPaper
      ? textMergedBlocks.some((block) => block.type === 'paperStyle')
        ? textMergedBlocks.map((block) => (block.type === 'paperStyle' ? { ...block, paperUrl: selectedPaper } : block))
        : [{ id: uid('blk_paper'), type: 'paperStyle', paperUrl: selectedPaper } as DiaryContentBlock, ...textMergedBlocks]
      : textMergedBlocks
    return appendLayoutBlock(
      withPaper,
      layoutBlockIdRef.current,
      textLayers.map((layer) => ({ ...layer })),
      stickerLayers.map((layer) => ({ ...layer })),
    )
  }

  useEffect(() => {
    if (!saveToast) return undefined
    const timer = window.setTimeout(() => setSaveToast(false), 1800)
    return () => window.clearTimeout(timer)
  }, [saveToast])

  useEffect(() => {
    if (!publishMsg) return undefined
    const timer = window.setTimeout(() => clearPublishFeedback(), 2200)
    return () => window.clearTimeout(timer)
  }, [publishMsg, clearPublishFeedback])

  /** 保存当前手账本：当前页 + 图层 + 封面等，写入本地书架（按登录账号隔离） */
  const saveCurrentBook = async (opts?: { toast?: boolean }) => {
    if (!selectedBook || !selectedEntry) return
    const showToast = opts?.toast !== false
    try {
      const nextBlocks = buildEntryBlocksForSave()
      const saved = await diaryApi.upsertEntry(selectedBook.id, {
        dayIndex: selectedEntry.dayIndex,
        title: entryTitle,
        entryDate: selectedEntry.entryDate,
        blocks: nextBlocks,
      })
      setSelectedEntry(saved)
      setBookEntries((prev) => prev.map((entry) => (entry.id === saved.id ? saved : entry)))
      applyEntryToEditor(saved)
      const updatedBook = await diaryApi.updateBook(selectedBook.id, {
        title: selectedBook.title,
        coverAssetUrl: selectedBook.coverAssetUrl,
        days: selectedBook.days,
        startDate: selectedBook.startDate,
      })
      setSelectedBook(updatedBook)
      setBooks((prev) => prev.map((book) => (book.id === updatedBook.id ? updatedBook : book)))
      if (showToast) setSaveToast(true)
    } catch {
      /* 静默失败，避免误提示已保存 */
    }
  }

  const createBook = async () => {
    const created = await diaryApi.createBook({
      title: `我的手账 ${books.length + 1}`,
      startDate: new Date().toISOString().slice(0, 10),
      days: 1,
      templateId: 'minimal',
    })
    const coverFallback = coverAssets[0] ?? ''
    const withCover = coverFallback ? await diaryApi.updateBook(created.id, { coverAssetUrl: coverFallback }) : created
    setBooks((prev) => [withCover, ...prev])
    setSelectedBookId(withCover.id)
  }

  const deleteBook = async () => {
    if (!deleteTarget) return
    await diaryApi.deleteBook(deleteTarget.id)
    setBooks((prev) => prev.filter((book) => book.id !== deleteTarget.id))
    if (selectedBookId === deleteTarget.id) {
      const rest = books.filter((book) => book.id !== deleteTarget.id)
      setSelectedBookId(rest[0]?.id ?? null)
    }
    setDeleteTarget(null)
  }

  const onPickCover = async (url: string) => {
    if (!selectedBook) return
    try {
      const blob = await fetch(url).then((res) => res.blob())
      if (blob.size > 0) {
        const ext = blob.type.includes('png') ? 'png' : 'jpg'
        uploadFileCacheRef.current.set(
          `cover-${selectedBook.id}`,
          new File([blob], `cover-${selectedBook.id}.${ext}`, { type: blob.type || 'image/jpeg' }),
        )
      }
    } catch {
      /* 发布时再尝试上传 */
    }
    const updated = await diaryApi.updateBook(selectedBook.id, { coverAssetUrl: url })
    setSelectedBook(updated)
    setBooks((prev) => prev.map((book) => (book.id === updated.id ? updated : book)))
  }

  const movePageLayer = (kind: 'text' | 'sticker', id: string, direction: 'up' | 'down') => {
    const stack = [
      ...textLayers.map((t) => ({ kind: 'text' as const, id: t.id, z: t.z })),
      ...stickerLayers.map((s) => ({ kind: 'sticker' as const, id: s.id, z: s.z })),
    ].sort((a, b) => a.z - b.z)
    const idx = stack.findIndex((item) => item.kind === kind && item.id === id)
    const swapIdx = direction === 'up' ? idx + 1 : idx - 1
    if (idx < 0 || swapIdx < 0 || swapIdx >= stack.length) return
    const curZ = stack[idx].z
    const swapZ = stack[swapIdx].z
    setTextLayers((prev) =>
      prev.map((layer) => {
        if (layer.id === id && kind === 'text') return { ...layer, z: swapZ }
        if (stack[swapIdx].kind === 'text' && layer.id === stack[swapIdx].id) return { ...layer, z: curZ }
        return layer
      }),
    )
    setStickerLayers((prev) =>
      prev.map((layer) => {
        if (layer.id === id && kind === 'sticker') return { ...layer, z: swapZ }
        if (stack[swapIdx].kind === 'sticker' && layer.id === stack[swapIdx].id) return { ...layer, z: curZ }
        return layer
      }),
    )
  }

  const addMediaLayer = (url: string, kind: 'image' | 'video', blockId: string) => {
    const nextId = uid('media')
    setActiveStickerId(nextId)
    setActiveTextLayerId(null)
    setStickerLayers((prev) => {
      const topZ = prev.reduce((max, it) => Math.max(max, it.z), 0)
      return [
        ...prev,
        {
          id: nextId,
          url,
          kind,
          blockId,
          left: 34 + (prev.length % 4) * 7,
          top: 42 + (prev.length % 3) * 5,
          scale: kind === 'video' ? 1.15 : 1,
          rotate: 0,
          z: topZ + 1,
        },
      ]
    })
  }

  const onUploadFiles = async (files: FileList | null) => {
    if (!files || !selectedBook || !selectedEntry) return
    setAiError(null)
    setUploadBusy(true)
    const appended: DiaryContentBlock[] = []
    try {
      for (const file of Array.from(files)) {
        const localUrl = URL.createObjectURL(file)
        if (file.type.startsWith('image/')) {
          const blockId = uid('blk_img')
          uploadFileCacheRef.current.set(blockId, file)
          appended.push({
            id: blockId,
            type: 'image',
            assetId: uid('asset_local'),
            assetUrl: localUrl,
            caption: '',
          })
          addMediaLayer(localUrl, 'image', blockId)
        } else if (file.type.startsWith('video/')) {
          const blockId = uid('blk_vid')
          uploadFileCacheRef.current.set(blockId, file)
          appended.push({
            id: blockId,
            type: 'video',
            assetId: uid('asset_local'),
            assetUrl: localUrl,
            caption: '',
          })
          addMediaLayer(localUrl, 'video', blockId)
        }
      }
      if (appended.length === 0) return
      const next = await diaryApi.upsertEntry(selectedBook.id, {
        dayIndex: selectedEntry.dayIndex,
        title: entryTitle,
        entryDate: selectedEntry.entryDate,
        blocks: [...selectedEntry.blocks, ...appended],
      })
      setUploadedCount((n) => n + appended.length)
      setSelectedEntry(next)
      setBookEntries((prev) => prev.map((entry) => (entry.id === next.id ? next : entry)))
    } catch (err) {
      setAiError(err instanceof Error ? err.message : '上传失败')
    } finally {
      setUploadBusy(false)
    }
  }

  const updateTextLayer = (id: string, patch: Partial<TextLayer>) => {
    setTextLayers((prev) => prev.map((it) => (it.id === id ? { ...it, ...patch } : it)))
  }

  const addTransparentTextLayerAt = (clientX: number, clientY: number) => {
    const point = getSpreadPoint(clientX, clientY)
    const nextId = uid('layer')
    pendingTextFocusRef.current = nextId
    setActiveTextLayerId(nextId)
    setActiveStickerId(null)
    setTextLayers((prev) => {
      const topZ = prev.reduce((max, it) => Math.max(max, it.z), 0)
      return [
        ...prev,
        {
          id: nextId,
          value: '',
          left: point?.leftPct ?? 58,
          top: point?.topPct ?? 38,
          scale: 1,
          rotate: 0,
          z: topZ + 1,
        },
      ]
    })
  }

  const getLayerCenterPx = (left: number, top: number, rect: DOMRect) => ({
    cx: (left / 100) * rect.width,
    cy: (top / 100) * rect.height,
  })

  const onTextLayerDragPointerDown = (e: StickerPointerEvent, layerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const layer = textLayers.find((it) => it.id === layerId)
    if (!point || !layer) return
    const { cx, cy } = getLayerCenterPx(layer.left, layer.top, point.rect)
    textLayerDragRef.current = {
      id: layerId,
      offsetX: point.x - cx,
      offsetY: point.y - cy,
    }
    const topZ = textLayers.reduce((max, it) => Math.max(max, it.z), 0)
    updateTextLayer(layerId, { z: topZ + 1 })
    setActiveTextLayerId(layerId)
    setActiveStickerId(null)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onTextLayerResizePointerDown = (e: StickerPointerEvent, layerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const layer = textLayers.find((it) => it.id === layerId)
    if (!point || !layer) return
    const { cx, cy } = getLayerCenterPx(layer.left, layer.top, point.rect)
    const startDist = Math.max(24, Math.hypot(point.x - cx, point.y - cy))
    textLayerResizeRef.current = { id: layerId, startScale: layer.scale, startDist, cx, cy }
    setActiveTextLayerId(layerId)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onTextLayerRotatePointerDown = (e: StickerPointerEvent, layerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const layer = textLayers.find((it) => it.id === layerId)
    if (!point || !layer) return
    const { cx, cy } = getLayerCenterPx(layer.left, layer.top, point.rect)
    textLayerRotateRef.current = {
      id: layerId,
      startRotate: layer.rotate,
      startAngle: Math.atan2(point.y - cy, point.x - cx),
      cx,
      cy,
    }
    setActiveTextLayerId(layerId)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const runAiForMediaSticker = async (stickerId: string) => {
    const sticker = stickerLayers.find((item) => item.id === stickerId)
    if (!sticker?.blockId || sticker.kind !== 'image' || !selectedBook || !selectedEntry || aiBusy) return
    if (!hasDoubaoKey()) {
      setAiError('未配置豆包 API Key，请在 .env 设置 VITE_DOUBAO_API_KEY（与后端无关）')
      return
    }
    setAiBusy(true)
    setAiError(null)
    try {
      const cached = uploadFileCacheRef.current.get(sticker.blockId)
      const dataUrl = cached
        ? await fileToDataUrl(cached)
        : await fileToDataUrl(
            new File(
              [await (await fetch(sticker.url)).blob()],
              'upload.jpg',
              { type: 'image/jpeg' },
            ),
          )
      const caption = await describeTravelImage({
        imageDataUrl: dataUrl,
        context: `为旅行手账「${entryTitle}」写 2-3 句读图配文，温馨具体，适合放在图片旁边。`,
      })
      const textId = uid('layer')
      const topZ = textLayers.reduce((max, it) => Math.max(max, it.z), 0)
      setTextLayers((prev) => [
        ...prev,
        {
          id: textId,
          value: caption,
          left: Math.min(88, sticker.left + 10),
          top: sticker.top,
          scale: 0.95,
          rotate: sticker.rotate,
          z: topZ + 1,
        },
      ])
      setActiveTextLayerId(textId)
      setActiveStickerId(null)
      pendingTextFocusRef.current = textId

      const next = await diaryApi.upsertEntry(selectedBook.id, {
        dayIndex: selectedEntry.dayIndex,
        title: entryTitle,
        entryDate: selectedEntry.entryDate,
        blocks: selectedEntry.blocks.map((block) =>
          block.id === sticker.blockId && block.type === 'image' ? { ...block, caption } : block,
        ),
      })
      setSelectedEntry(next)
      setBookEntries((prev) => prev.map((entry) => (entry.id === next.id ? next : entry)))
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'AI 配文失败'
      setAiError(
        msg.includes('fetch') || msg.includes('Failed')
          ? '豆包识图 API 请求失败，请检查 VITE_DOUBAO_API_KEY 与网络（非后端接口问题）'
          : msg,
      )
    } finally {
      setAiBusy(false)
    }
  }

  const runAiForTextLayer = async (layerId: string) => {
    const layer = textLayers.find((item) => item.id === layerId)
    if (!layer || aiBusy) return
    if (!hasGlmKey()) {
      setAiError('未配置智谱 API Key，请在 .env 中设置 VITE_GLM_API_KEY')
      return
    }
    setAiBusy(true)
    setAiError(null)
    try {
      const text = await polishDiaryText({ title: entryTitle, draft: layer.value })
      setTextLayers((prev) => prev.map((item) => (item.id === layerId ? { ...item, value: text } : item)))
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'AI 续写失败')
    } finally {
      setAiBusy(false)
    }
  }

  const runGenerateDiaryDay = async () => {
    if (!selectedBook || !selectedEntry || aiBusy) return
    if (!hasGlmKey()) {
      setAiError('未配置智谱 API Key，请在 .env 中设置 VITE_GLM_API_KEY')
      return
    }
    setAiBusy(true)
    setAiError(null)
    try {
      const layout = await generateDiaryDayLayout({
        bookTitle: selectedBook.title,
        dayIndex: selectedEntry.dayIndex,
        entryDate: selectedEntry.entryDate,
        roughNotes: mergePages(leftPageText, rightPageText),
      })
      setEntryTitle(layout.title)
      const timelineText = layout.timeline
        .map((item) => `${item.time} ${item.place} — ${item.note}`)
        .join('\n')
      const checklistText = layout.checklist.map((item) => `□ ${item}`).join('\n')
      setLeftPageText(
        [layout.subtitle, layout.leftPageText, timelineText ? `\n行程\n${timelineText}` : '']
          .filter(Boolean)
          .join('\n\n'),
      )
      setRightPageText(
        [layout.rightPageText, checklistText ? `\n清单\n${checklistText}` : '', layout.moodTag ? `\n#${layout.moodTag}` : '']
          .filter(Boolean)
          .join('\n\n'),
      )
      await saveCurrentBook({ toast: false })
    } catch (err) {
      setAiError(err instanceof Error ? err.message : 'AI 排版失败')
    } finally {
      setAiBusy(false)
    }
  }

  const addStickerToPage = (url: string) => {
    const nextId = uid('sticker')
    setActiveStickerId(nextId)
    setStickerLayers((prev) => {
      const currentTop = prev.reduce((max, it) => Math.max(max, it.z), 0)
      const created = {
        id: nextId,
        url,
        left: 68 + (prev.length % 3) * 3,
        top: 38 + (prev.length % 4) * 3,
        scale: 1,
        rotate: 0,
        z: currentTop + 1,
      }
      return [...prev, created]
    })
  }

  const updateSticker = (id: string, patch: Partial<StickerLayer>) => {
    setStickerLayers((prev) => prev.map((it) => (it.id === id ? { ...it, ...patch } : it)))
  }

  const getSpreadPoint = (clientX: number, clientY: number) => {
    const host = spreadRef.current
    if (!host) return null
    const rect = host.getBoundingClientRect()
    return {
      rect,
      x: clientX - rect.left,
      y: clientY - rect.top,
      leftPct: ((clientX - rect.left) / rect.width) * 100,
      topPct: ((clientY - rect.top) / rect.height) * 100,
    }
  }

  const getStickerCenterPx = (sticker: StickerLayer, rect: DOMRect) => ({
    cx: (sticker.left / 100) * rect.width,
    cy: (sticker.top / 100) * rect.height,
  })

  const onStickerBodyPointerDown = (e: StickerPointerEvent, stickerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const sticker = stickerLayers.find((it) => it.id === stickerId)
    if (!point || !sticker) return
    const { cx, cy } = getStickerCenterPx(sticker, point.rect)
    stickerDragRef.current = {
      id: stickerId,
      offsetX: point.x - cx,
      offsetY: point.y - cy,
    }
    const topZ = stickerLayers.reduce((max, it) => Math.max(max, it.z), 0)
    updateSticker(stickerId, { z: topZ + 1 })
    setActiveStickerId(stickerId)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onStickerResizePointerDown = (e: StickerPointerEvent, stickerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const sticker = stickerLayers.find((it) => it.id === stickerId)
    if (!point || !sticker) return
    const { cx, cy } = getStickerCenterPx(sticker, point.rect)
    const startDist = Math.max(24, Math.hypot(point.x - cx, point.y - cy))
    stickerResizeRef.current = { id: stickerId, startScale: sticker.scale, startDist, cx, cy }
    setActiveStickerId(stickerId)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onStickerRotatePointerDown = (e: StickerPointerEvent, stickerId: string) => {
    e.stopPropagation()
    const point = getSpreadPoint(e.clientX, e.clientY)
    const sticker = stickerLayers.find((it) => it.id === stickerId)
    if (!point || !sticker) return
    const { cx, cy } = getStickerCenterPx(sticker, point.rect)
    stickerRotateRef.current = {
      id: stickerId,
      startRotate: sticker.rotate,
      startAngle: Math.atan2(point.y - cy, point.x - cx),
      cx,
      cy,
    }
    setActiveStickerId(stickerId)
    e.currentTarget.setPointerCapture(e.pointerId)
  }

  const onSpreadPointerMove = (e: StickerPointerEvent) => {
    const point = getSpreadPoint(e.clientX, e.clientY)
    if (!point) return

    const textResize = textLayerResizeRef.current
    if (textResize) {
      const dist = Math.max(20, Math.hypot(point.x - textResize.cx, point.y - textResize.cy))
      const ratio = dist / textResize.startDist
      updateTextLayer(textResize.id, {
        scale: Math.max(0.35, Math.min(3.5, Number((textResize.startScale * ratio).toFixed(3)))),
      })
      return
    }

    const textRotate = textLayerRotateRef.current
    if (textRotate) {
      const angle = Math.atan2(point.y - textRotate.cy, point.x - textRotate.cx)
      const deltaDeg = ((angle - textRotate.startAngle) * 180) / Math.PI
      updateTextLayer(textRotate.id, {
        rotate: Number((textRotate.startRotate + deltaDeg).toFixed(2)),
      })
      return
    }

    const textDrag = textLayerDragRef.current
    if (textDrag) {
      let left = ((point.x - textDrag.offsetX) / point.rect.width) * 100
      let top = ((point.y - textDrag.offsetY) / point.rect.height) * 100
      left = Math.max(4, Math.min(96, left))
      top = Math.max(8, Math.min(92, top))
      updateTextLayer(textDrag.id, { left, top })
      return
    }

    const resize = stickerResizeRef.current
    if (resize) {
      const dist = Math.max(20, Math.hypot(point.x - resize.cx, point.y - resize.cy))
      const ratio = dist / resize.startDist
      updateSticker(resize.id, {
        scale: Math.max(0.25, Math.min(3.5, Number((resize.startScale * ratio).toFixed(3)))),
      })
      return
    }

    const rotate = stickerRotateRef.current
    if (rotate) {
      const angle = Math.atan2(point.y - rotate.cy, point.x - rotate.cx)
      const deltaDeg = ((angle - rotate.startAngle) * 180) / Math.PI
      updateSticker(rotate.id, {
        rotate: Number((rotate.startRotate + deltaDeg).toFixed(2)),
      })
      return
    }

    const drag = stickerDragRef.current
    if (!drag) return
    let left = ((point.x - drag.offsetX) / point.rect.width) * 100
    let top = ((point.y - drag.offsetY) / point.rect.height) * 100
    left = Math.max(4, Math.min(96, left))
    top = Math.max(8, Math.min(92, top))

    const snapThreshold = 1.5
    let guideX: number | null = null
    let guideY: number | null = null
    for (const snapX of [25, 50, 75]) {
      if (Math.abs(left - snapX) <= snapThreshold) {
        left = snapX
        guideX = snapX
        break
      }
    }
    for (const snapY of [33, 50, 66]) {
      if (Math.abs(top - snapY) <= snapThreshold) {
        top = snapY
        guideY = snapY
        break
      }
    }
    setSnapGuide({ x: guideX, y: guideY })
    updateSticker(drag.id, { left, top })
  }

  const endStickerGesture = (e: StickerPointerEvent) => {
    if (e.currentTarget.hasPointerCapture(e.pointerId)) e.currentTarget.releasePointerCapture(e.pointerId)
    stickerDragRef.current = null
    stickerResizeRef.current = null
    stickerRotateRef.current = null
    textLayerDragRef.current = null
    textLayerResizeRef.current = null
    textLayerRotateRef.current = null
    setSnapGuide({ x: null, y: null })
  }

  const playFlipSound = () => {
    if (!soundEnabled) return
    const AudioCtx = window.AudioContext || (window as Window & typeof globalThis & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AudioCtx) return
    const ctx = audioContextRef.current ?? new AudioCtx()
    audioContextRef.current = ctx
    const now = ctx.currentTime

    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.type = 'triangle'
    osc.frequency.setValueAtTime(560, now)
    osc.frequency.exponentialRampToValueAtTime(180, now + 0.16)
    gain.gain.setValueAtTime(0.0001, now)
    gain.gain.exponentialRampToValueAtTime(0.06, now + 0.02)
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.2)
    osc.connect(gain).connect(ctx.destination)
    osc.start(now)
    osc.stop(now + 0.22)
  }

  const triggerPageFlip = async (dir: 'next' | 'prev') => {
    if (pageTurning) return
    if (!selectedBook) return
    playFlipSound()
    setPageFlipDir(dir)
    setPageTurning(false)
    window.requestAnimationFrame(() => setPageTurning(true))
    window.setTimeout(async () => {
      if (dir === 'next') {
        const nextIndex = currentEntryIndex + 1
        const nextExisting = bookEntries[nextIndex]
        if (nextExisting) {
          setCurrentEntryIndex(nextIndex)
          applyEntryToEditor(nextExisting)
        } else {
          const templatePaper = selectedPaper ?? paperAssets[0] ?? ''
          const created = await diaryApi.upsertEntry(selectedBook.id, {
            dayIndex: bookEntries.length + 1,
            title: `Day ${bookEntries.length + 1}`,
            entryDate: new Date().toISOString().slice(0, 10),
            blocks: [
              { id: uid('blk_txt'), type: 'text', text: mergePages('', '') },
              ...(templatePaper ? [{ id: uid('blk_paper'), type: 'paperStyle', paperUrl: templatePaper } as DiaryContentBlock] : []),
            ],
          })
          setBookEntries((prev) => {
            const next = [...prev, created].sort((a, b) => a.dayIndex - b.dayIndex)
            const idx = next.findIndex((entry) => entry.id === created.id)
            setCurrentEntryIndex(idx >= 0 ? idx : prev.length)
            return next
          })
          applyEntryToEditor(created)
        }
      } else {
        const prevIndex = Math.max(0, currentEntryIndex - 1)
        const prevEntry = bookEntries[prevIndex]
        if (prevEntry) {
          setCurrentEntryIndex(prevIndex)
          applyEntryToEditor(prevEntry)
        }
      }
      setPageFlipDir(null)
      setPageTurning(false)
    }, 760)
  }

  const loadMoreCovers = async () => {
    if (loadingCoverAssets || !coverHasMore) return
    setLoadingCoverAssets(true)
    try {
      const { items, hasMore, failed } = await loadAssetsPage(coverLoaders as AssetLoaders, 9, coverOffset)
      setCoverAssets((prev) => [...prev, ...items])
      setCoverOffset((n) => n + items.length)
      setCoverHasMore(hasMore)
      if (failed > 0) setCoverAssetError(`有 ${failed} 张封面加载失败`)
    } finally {
      setLoadingCoverAssets(false)
    }
  }

  const loadMorePapers = async () => {
    if (loadingPaperAssets || !paperHasMore) return
    setLoadingPaperAssets(true)
    try {
      const { items, hasMore, failed } = await loadAssetsPage(paperLoaders as AssetLoaders, 9, paperOffset)
      setPaperAssets((prev) => [...prev, ...items])
      setPaperOffset((n) => n + items.length)
      setPaperHasMore(hasMore)
      if (failed > 0) setPaperAssetError(`有 ${failed} 张内页加载失败`)
    } finally {
      setLoadingPaperAssets(false)
    }
  }

  const loadMoreStickers = async () => {
    if (loadingStickerAssets || !stickerHasMore) return
    setLoadingStickerAssets(true)
    try {
      const { items, hasMore, failed } = await loadAssetsPage(stickerLoaders as AssetLoaders, 24, stickerOffset)
      setStickerAssets((prev) => [...prev, ...items])
      setStickerOffset((n) => n + items.length)
      setStickerHasMore(hasMore)
      if (failed > 0) setStickerAssetError(`有 ${failed} 张贴纸加载失败`)
    } finally {
      setLoadingStickerAssets(false)
    }
  }

  return (
    <div className="relative h-[calc(100dvh+50dvh)] w-full animate-fade-rise overflow-y-auto overflow-x-hidden px-3 pb-4 pt-5 md:px-5">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: "linear-gradient(180deg, rgba(255,255,255,0.74), rgba(246,251,255,0.8)), url('/images/diary-bg-main.png')",
          backgroundBlendMode: 'normal, multiply',
          backgroundSize: 'cover, cover',
          backgroundPosition: 'center',
        }}
      />
      <div className="relative mb-2 flex items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-[clamp(1.6rem,2.3vw,2.2rem)] font-semibold tracking-wide text-[var(--ds-foreground)]">
            旅游日记 · 纸感手账制作台
          </h1>
          <p className="mt-1 text-sm text-[var(--ds-muted-foreground)]">左侧可收回，收回后自动展开书本；支持封面、内页、贴纸与本地上传。</p>
        </div>
        <button
          type="button"
          onClick={createBook}
          className="rounded-full bg-gradient-to-br from-[var(--ds-primary)] to-[var(--ds-forest)] px-5 py-2.5 text-sm font-semibold text-white shadow-[0_14px_28px_-18px_rgba(48,102,132,0.7)]"
        >
          新建手账本
        </button>
      </div>

      <div
        className="relative grid h-[calc(100dvh+50dvh-9rem)] gap-3 overflow-hidden"
        style={{
          gridTemplateColumns: isShelfCollapsed
            ? isMaterialCollapsed
              ? '88px minmax(0,1fr) 86px'
              : '88px minmax(0,2.2fr) minmax(0,1fr)'
            : isMaterialCollapsed
              ? '1.05fr minmax(0,1fr) 86px'
              : '1.05fr 2fr 1fr',
        }}
      >
        <aside className={`h-full overflow-hidden p-3 ${panel}`}>
          <div className="mb-2 flex items-center justify-between">
            {!isShelfCollapsed ? <h3 className="font-display text-lg font-semibold text-[var(--ds-foreground)]">手账本</h3> : null}
            <button
              type="button"
              className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_52%,transparent)] bg-white/70 px-2.5 py-1 text-xs text-[var(--ds-muted-foreground)]"
              onClick={() =>
                setIsShelfCollapsed((v) => {
                  const next = !v
                  if (next && selectedBook) setBookOpened(true)
                  if (!next) setBookOpened(false)
                  return next
                })
              }
            >
              {isShelfCollapsed ? '展开' : '收起'}
            </button>
          </div>
          <div className={`h-[calc(100%-2.2rem)] overflow-auto pr-1 ${isShelfCollapsed ? 'space-y-2' : 'grid grid-cols-2 gap-2.5'}`}>
            {books.map((book) => {
              const active = book.id === selectedBookId
              return (
                <button
                  key={book.id}
                  type="button"
                  onClick={() => setSelectedBookId(book.id)}
                  onContextMenu={(e) => {
                    e.preventDefault()
                    setDeleteTarget(book)
                  }}
                  className={`group overflow-hidden rounded-2xl border text-left transition ${active ? 'border-[var(--ds-primary)] shadow-[0_16px_26px_-18px_rgba(47,91,119,0.55)]' : 'border-white/60 hover:border-[var(--ds-sage)]'}`}
                  title="右键可删除手账"
                >
                  <div className="aspect-[3/4] w-full bg-[var(--ds-muted)]">
                    {book.coverAssetUrl ? (
                      <img src={book.coverAssetUrl} alt="" className="h-full w-full object-cover transition group-hover:scale-[1.03]" loading="lazy" decoding="async" />
                    ) : (
                      <div className="flex h-full items-center justify-center text-[11px] text-[var(--ds-muted-foreground)]">空白封面</div>
                    )}
                  </div>
                  {!isShelfCollapsed ? (
                    <div className="px-2.5 py-2">
                      <p className="truncate text-xs font-semibold text-[var(--ds-foreground)]">{book.title}</p>
                      <p className="mt-0.5 text-[10px] text-[var(--ds-muted-foreground)]">{book.startDate}</p>
                    </div>
                  ) : null}
                </button>
              )
            })}
            <button
              type="button"
              onClick={createBook}
              className="flex h-[6.6rem] flex-col items-center justify-center rounded-2xl border border-dashed border-[color-mix(in_srgb,var(--ds-border)_58%,transparent)] bg-white/55 text-[var(--ds-muted-foreground)] transition hover:bg-white/78"
            >
              <span className="text-3xl leading-none">＋</span>
              {!isShelfCollapsed ? <span className="mt-0.5 text-[11px] font-semibold">新建手账</span> : null}
            </button>
          </div>
        </aside>

        <section className={`h-full overflow-hidden p-2 ${panel}`}>
          {loadingBook ? (
            <div className="flex h-full items-center justify-center text-[var(--ds-muted-foreground)]">正在加载手账内容...</div>
          ) : selectedBook && selectedEntry ? (
            <div className="flex h-full flex-col gap-2">
              <div className="flex-1 overflow-auto">
                <div className="flex h-full items-center justify-center">
                  <div
                    className={`relative ${bookOpened ? 'aspect-[3/2]' : 'aspect-[3/4]'} h-full [perspective:2000px]`}
                    style={{
                      width: bookOpened
                        ? isShelfCollapsed
                          ? 'min(96%, 1240px)'
                          : 'min(95%, 1080px)'
                        : isShelfCollapsed
                          ? 'min(88%, 760px)'
                          : 'min(84%, 700px)',
                      maxHeight: 'min(98%, 1180px)',
                    }}
                  >
                    {!bookOpened ? (
                      <div className="absolute inset-0 rounded-[24px] border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/68 p-2 shadow-[0_20px_44px_-24px_rgba(37,76,99,0.52)] [transform-style:preserve-3d]">
                        <button
                          type="button"
                          className="group relative h-full w-full overflow-hidden rounded-[20px] border border-white/65 bg-[var(--ds-muted)] [transform-origin:left_center] transition-transform duration-700 hover:shadow-xl"
                          onClick={() => setBookOpened(true)}
                        >
                          {selectedBook.coverAssetUrl ? (
                            <img src={selectedBook.coverAssetUrl} alt="" className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.02]" loading="lazy" decoding="async" />
                          ) : (
                            <div className="flex h-full items-center justify-center text-sm text-[var(--ds-muted-foreground)]">请从右侧选择封面素材</div>
                          )}
                          <div className="pointer-events-none absolute inset-y-0 left-0 w-6 bg-gradient-to-r from-[color-mix(in_srgb,var(--ds-forest)_28%,transparent)] to-transparent" />
                        </button>
                      </div>
                    ) : (
                      <div
                        ref={spreadRef}
                        className="absolute inset-0 grid grid-cols-2 gap-2 [transform-style:preserve-3d]"
                        onPointerMove={onSpreadPointerMove}
                        onPointerUp={endStickerGesture}
                        onPointerCancel={endStickerGesture}
                        onDoubleClick={(e) => {
                          if ((e.target as HTMLElement).closest('[data-text-layer],[data-sticker],textarea,input,button,video')) return
                          e.stopPropagation()
                          addTransparentTextLayerAt(e.clientX, e.clientY)
                        }}
                        onClick={() => {
                          setActiveStickerId(null)
                          setActiveTextLayerId(null)
                        }}
                      >
                        <div className="pointer-events-none absolute left-1/2 top-0 z-20 h-full w-3 -translate-x-1/2 bg-gradient-to-r from-[color-mix(in_srgb,var(--ds-sage)_45%,transparent)] via-white/80 to-[color-mix(in_srgb,var(--ds-sage)_45%,transparent)] shadow-[0_0_14px_rgba(58,95,119,0.22)]" />
                        <div className="pointer-events-none absolute left-1/2 top-0 z-30 h-[14%] w-2 -translate-x-1/2 rounded-b bg-gradient-to-b from-[var(--ds-moss)] to-[var(--ds-forest)] shadow-[0_4px_8px_rgba(91,24,44,0.32)]" />
                        <div className="pointer-events-none absolute inset-y-1 left-[0.6rem] z-10 w-[10px] rounded-full bg-gradient-to-r from-[color-mix(in_srgb,var(--ds-sage)_32%,transparent)] via-white/65 to-transparent" />
                        <div className="pointer-events-none absolute inset-y-1 right-[0.6rem] z-10 w-[10px] rounded-full bg-gradient-to-l from-[color-mix(in_srgb,var(--ds-sage)_32%,transparent)] via-white/65 to-transparent" />
                        <div className="absolute right-2 top-2 z-30 flex gap-1.5">
                          <button type="button" className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/82 px-2 py-0.5 text-[11px] text-[var(--ds-foreground)]" onClick={() => triggerPageFlip('prev')}>上一页</button>
                          <button type="button" className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/82 px-2 py-0.5 text-[11px] text-[var(--ds-foreground)]" onClick={() => triggerPageFlip('next')}>下一页</button>
                          <button type="button" className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/82 px-2 py-0.5 text-[11px] text-[var(--ds-foreground)]" onClick={() => setBookOpened(false)}>合上</button>
                          <span className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_40%,transparent)] bg-white/76 px-2 py-0.5 text-[11px] text-[var(--ds-muted-foreground)]">
                            第 {Math.max(1, currentEntryIndex + 1)} / {Math.max(1, bookEntries.length)} 页
                          </span>
                        </div>
                        <div
                          className="relative rounded-[18px] border border-[color-mix(in_srgb,var(--ds-border)_38%,transparent)] bg-white/76 p-4 shadow-[inset_-8px_0_16px_rgba(141,177,198,0.18)]"
                          style={{
                            backgroundImage: selectedPaper ? `linear-gradient(rgba(255,255,255,0.2),rgba(255,255,255,0.24)), url('${selectedPaper}')` : undefined,
                            backgroundSize: selectedPaper ? 'cover, cover' : undefined,
                            backgroundPosition: 'center',
                          }}
                        >
                          <div className="pointer-events-none absolute right-0 top-0 h-10 w-10 bg-gradient-to-bl from-[#8db2c7]/22 to-transparent" />
                          <textarea
                            ref={leftPageTextareaRef}
                            value={leftPageText}
                            onChange={(e) => {
                              setLeftPageText(e.target.value)
                              autoResizeTextarea(e.target)
                            }}
                            onBlur={() => void saveCurrentBook({ toast: false })}
                            rows={4}
                            className="min-h-[6rem] w-full resize-none overflow-hidden rounded-xl border-0 bg-transparent p-2.5 text-[14px] leading-6 text-[var(--ds-foreground)] outline-none transition focus:border focus:border-white/55 focus:bg-white/20"
                            placeholder="左页：可书写内容..."
                          />
                          <div className="pointer-events-none absolute bottom-2 right-3 text-[10px] text-[var(--ds-muted-foreground)]/80">P.1</div>
                        </div>
                        <div
                          ref={rightPageRef}
                          className="relative rounded-[18px] border border-[color-mix(in_srgb,var(--ds-border)_42%,transparent)] bg-white/78 p-4 shadow-[inset_8px_0_16px_rgba(141,177,198,0.2)]"
                          title="双击页面任意位置可添加透明文字框"
                          style={{
                            backgroundImage: selectedPaper ? `linear-gradient(rgba(255,255,255,0.2),rgba(255,255,255,0.24)), url('${selectedPaper}')` : undefined,
                            backgroundSize: selectedPaper ? 'cover, cover' : undefined,
                            backgroundPosition: 'center',
                          }}
                        >
                          <div className="pointer-events-none absolute right-0 top-0 h-10 w-10 bg-gradient-to-bl from-[#8db2c7]/24 to-transparent" />
                          <input
                            value={entryTitle}
                            onChange={(e) => setEntryTitle(e.target.value)}
                            onBlur={() => void saveCurrentBook({ toast: false })}
                            className="w-full bg-transparent text-lg font-semibold text-[var(--ds-foreground)] outline-none"
                            placeholder="Day 标题"
                          />
                          <textarea
                            ref={rightPageTextareaRef}
                            value={rightPageText}
                            onChange={(e) => {
                              setRightPageText(e.target.value)
                              autoResizeTextarea(e.target)
                            }}
                            onBlur={() => void saveCurrentBook({ toast: false })}
                            rows={6}
                            className="mt-2 min-h-[8rem] w-full resize-none overflow-hidden rounded-xl border-0 bg-transparent p-2.5 text-[14px] leading-6 text-[var(--ds-foreground)] outline-none transition focus:border focus:border-white/55 focus:bg-white/20"
                            placeholder="在透明文本框中写下旅行故事..."
                          />
                          <div className="pointer-events-none absolute bottom-2 right-3 text-[10px] text-[var(--ds-muted-foreground)]/80">P.2</div>
                          {pageFlipDir ? (
                            <div
                              className="pointer-events-none absolute inset-0 z-40 origin-left rounded-[18px] border border-[#8fb4c8]/45 bg-[linear-gradient(90deg,rgba(247,251,255,0.98),rgba(232,242,249,0.96)_56%,rgba(205,224,236,0.92))] shadow-[-16px_0_30px_rgba(52,89,113,0.26)] [backface-visibility:hidden]"
                              style={{
                                transform:
                                  pageFlipDir === 'next'
                                    ? `perspective(1200px) rotateY(${pageTurning ? -178 : 0}deg)`
                                    : `perspective(1200px) rotateY(${pageTurning ? 178 : 0}deg)`,
                                transition: 'transform 760ms cubic-bezier(0.2,0.65,0.2,1)',
                              }}
                            >
                              <div className="absolute inset-y-0 left-0 w-4 bg-gradient-to-r from-[#8cb1c6]/35 via-white/80 to-transparent" />
                              <div className="absolute inset-0 opacity-[0.08] [background-image:radial-gradient(circle_at_1px_1px,#4c6f83_1px,transparent_0)] [background-size:6px_6px]" />
                              <div className="absolute bottom-2 right-3 text-[10px] text-[var(--ds-muted-foreground)]/90">{pageFlipDir === 'next' ? 'P.3' : 'P.1'}</div>
                            </div>
                          ) : null}
                        </div>
                        {snapGuide.x !== null ? (
                          <div className="pointer-events-none absolute bottom-0 top-0 z-[25] w-px bg-[#5b8da8]/55" style={{ left: `${snapGuide.x}%` }} />
                        ) : null}
                        {snapGuide.y !== null ? (
                          <div className="pointer-events-none absolute left-0 right-0 z-[25] h-px bg-[#5b8da8]/55" style={{ top: `${snapGuide.y}%` }} />
                        ) : null}
                        {textLayers.map((layer) => {
                          const active = activeTextLayerId === layer.id
                          const empty = !layer.value.trim()
                          if (!active && empty) return null
                          const boxW = 200 * layer.scale
                          return (
                            <div
                              key={layer.id}
                              data-text-layer="root"
                              className="absolute touch-none"
                              style={{
                                left: `${layer.left}%`,
                                top: `${layer.top}%`,
                                width: boxW,
                                transform: `translate(-50%, -50%) rotate(${layer.rotate}deg)`,
                                transformOrigin: 'center',
                                zIndex: 10 + layer.z,
                              }}
                              onClick={(e) => e.stopPropagation()}
                            >
                              <div className={`relative w-full ${active ? 'ring-1 ring-[color-mix(in_srgb,var(--ds-primary)_35%,transparent)] ring-offset-1' : ''}`}>
                                {active ? (
                                  <div
                                    role="presentation"
                                    onPointerDown={(e) => onTextLayerDragPointerDown(e, layer.id)}
                                    className="absolute -top-1 left-0 right-0 h-2 cursor-grab rounded-t active:cursor-grabbing"
                                  />
                                ) : null}
                                <textarea
                                  id={`text-layer-input-${layer.id}`}
                                  value={layer.value}
                                  onChange={(e) => {
                                    updateTextLayer(layer.id, { value: e.target.value })
                                    autoResizeTextarea(e.target)
                                  }}
                                  onFocus={() => {
                                    setActiveTextLayerId(layer.id)
                                    setActiveStickerId(null)
                                  }}
                                  onBlur={(e) => {
                                    const next = e.relatedTarget as Node | null
                                    if (next && e.currentTarget.closest('[data-text-layer]')?.contains(next)) return
                                    setActiveTextLayerId((cur) => (cur === layer.id ? null : cur))
                                  }}
                                  rows={2}
                                  className={`w-full resize-none overflow-hidden leading-relaxed text-[var(--ds-foreground)] outline-none ${
                                    active
                                      ? 'min-h-[3.5rem] rounded-md border border-white/45 bg-white/18 p-2 text-[13px]'
                                      : 'cursor-text border-0 bg-transparent p-0 text-[14px] shadow-none'
                                  }`}
                                  placeholder={active ? '写下这一刻…' : ''}
                                />
                                {active ? (
                                  <>
                                    <div className="pointer-events-none absolute left-1/2 top-0 h-5 w-px -translate-x-1/2 -translate-y-full bg-[#4c7d97]/70" />
                                    <div
                                      role="presentation"
                                      onPointerDown={(e) => onTextLayerRotatePointerDown(e, layer.id)}
                                      className="absolute left-1/2 top-0 h-3.5 w-3.5 -translate-x-1/2 -translate-y-[calc(100%+6px)] cursor-grab rounded-full border-2 border-[var(--ds-primary)] bg-white shadow-sm active:cursor-grabbing"
                                    />
                                    <div
                                      role="presentation"
                                      data-text-layer="resize"
                                      onPointerDown={(e) => onTextLayerResizePointerDown(e, layer.id)}
                                      className="absolute bottom-0 right-0 h-2.5 w-2.5 translate-x-1/2 translate-y-1/2 cursor-nwse-resize rounded-sm border border-[color-mix(in_srgb,var(--ds-primary)_55%,transparent)] bg-white/90"
                                    />
                                    <div className="absolute -bottom-7 left-0 flex flex-wrap items-center gap-1">
                                      <button
                                        type="button"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => movePageLayer('text', layer.id, 'down')}
                                        className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-1.5 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm"
                                      >
                                        下移
                                      </button>
                                      <button
                                        type="button"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => movePageLayer('text', layer.id, 'up')}
                                        className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-1.5 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm"
                                      >
                                        上移
                                      </button>
                                      <button
                                        type="button"
                                        disabled={aiBusy}
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => void runAiForTextLayer(layer.id)}
                                        className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-2 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm disabled:opacity-50"
                                      >
                                        AI 续写
                                      </button>
                                      <button
                                        type="button"
                                        aria-label="删除文字"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => {
                                          setTextLayers((prev) => prev.filter((it) => it.id !== layer.id))
                                          setActiveTextLayerId(null)
                                        }}
                                        className="flex h-5 w-5 items-center justify-center rounded-full bg-[var(--ds-destructive)]/92 text-[11px] leading-none text-white shadow-sm"
                                      >
                                        ×
                                      </button>
                                    </div>
                                  </>
                                ) : null}
                              </div>
                            </div>
                          )
                        })}
                        {stickerLayers.map((sticker) => {
                          const active = sticker.id === activeStickerId
                          const baseSize = sticker.kind === 'video' ? 120 : 80
                          const box = baseSize * sticker.scale
                          return (
                            <div
                              key={sticker.id}
                              data-sticker="root"
                              className="absolute touch-none"
                              style={{
                                left: `${sticker.left}%`,
                                top: `${sticker.top}%`,
                                width: box,
                                height: box,
                                transform: `translate(-50%, -50%) rotate(${sticker.rotate}deg)`,
                                transformOrigin: 'center',
                                zIndex: 10 + sticker.z,
                              }}
                              onClick={(e) => e.stopPropagation()}
                            >
                              <div
                                className={`relative h-full w-full ${active ? 'ring-2 ring-[var(--ds-primary)] ring-offset-1' : ''}`}
                              >
                                <div
                                  role="presentation"
                                  onPointerDown={(e) => onStickerBodyPointerDown(e, sticker.id)}
                                  className={`h-full w-full cursor-grab overflow-hidden rounded-md p-1 active:cursor-grabbing ${
                                    active
                                      ? 'border border-[var(--ds-primary)] bg-white/15'
                                      : 'border-0 bg-transparent'
                                  }`}
                                >
                                  {sticker.kind === 'video' ? (
                                    <video src={sticker.url} className="h-full w-full object-contain" controls draggable={false} />
                                  ) : (
                                    <img src={sticker.url} alt="" className="h-full w-full object-contain select-none" draggable={false} />
                                  )}
                                </div>
                                {active ? (
                                  <>
                                    <div className="pointer-events-none absolute left-1/2 top-0 h-5 w-px -translate-x-1/2 -translate-y-full bg-[#4c7d97]/70" />
                                    <div
                                      role="presentation"
                                      onPointerDown={(e) => onStickerRotatePointerDown(e, sticker.id)}
                                      className="absolute left-1/2 top-0 h-3.5 w-3.5 -translate-x-1/2 -translate-y-[calc(100%+6px)] cursor-grab rounded-full border-2 border-[var(--ds-primary)] bg-white shadow-sm active:cursor-grabbing"
                                    />
                                    <div
                                      role="presentation"
                                      onPointerDown={(e) => onStickerResizePointerDown(e, sticker.id)}
                                      className="absolute bottom-0 right-0 h-3 w-3 translate-x-1/2 translate-y-1/2 cursor-nwse-resize rounded-sm border-2 border-[var(--ds-primary)] bg-white shadow-sm"
                                    />
                                    <div className="absolute -bottom-7 left-0 flex flex-wrap items-center gap-1">
                                      <button
                                        type="button"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => movePageLayer('sticker', sticker.id, 'down')}
                                        className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-1.5 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm"
                                      >
                                        下移
                                      </button>
                                      <button
                                        type="button"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={() => movePageLayer('sticker', sticker.id, 'up')}
                                        className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-1.5 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm"
                                      >
                                        上移
                                      </button>
                                      {sticker.kind === 'image' && hasDoubaoKey() ? (
                                        <button
                                          type="button"
                                          disabled={aiBusy}
                                          onMouseDown={(e) => e.preventDefault()}
                                          onClick={() => void runAiForMediaSticker(sticker.id)}
                                          className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] bg-white/88 px-2 py-0.5 text-[10px] text-[var(--ds-muted-foreground)] shadow-sm disabled:opacity-50"
                                        >
                                          AI 配文
                                        </button>
                                      ) : null}
                                    </div>
                                    <button
                                      type="button"
                                      aria-label="删除"
                                      className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full bg-[var(--ds-destructive)] text-[11px] font-bold leading-none text-white shadow"
                                      onClick={() => {
                                        setStickerLayers((prev) => prev.filter((it) => it.id !== sticker.id))
                                        setActiveStickerId(null)
                                      }}
                                    >
                                      ×
                                    </button>
                                  </>
                                ) : null}
                              </div>
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {routeBlocks.length > 0 ? (
                <div className="grid h-[5.2rem] grid-cols-4 gap-2 overflow-auto rounded-xl border border-white/70 bg-white/50 p-2">
                  {routeBlocks.map((block) => <img key={block.id} src={block.imageUrl} alt="路线图" className="h-full w-full rounded-lg object-cover" />)}
                </div>
              ) : null}
            </div>
          ) : (
            <div className="flex h-full items-center justify-center rounded-2xl border border-dashed border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-white/45 text-[var(--ds-muted-foreground)]">
              在左侧新建手账本开始创作
            </div>
          )}
        </section>

        <aside className={`${isMaterialCollapsed ? `h-full overflow-hidden p-2 ${panel}` : 'grid h-full grid-rows-4 gap-3 overflow-hidden'}`}>
          {isMaterialCollapsed ? (
            <div className="flex h-full flex-col items-center justify-start gap-2 pt-1">
              <button
                type="button"
                className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_52%,transparent)] bg-white/78 px-2.5 py-1 text-xs text-[var(--ds-muted-foreground)]"
                onClick={() => setIsMaterialCollapsed(false)}
              >
                展开
              </button>
              {(['cover', 'paper', 'sticker'] as MaterialTab[]).map((tab) => (
                <button
                  key={tab}
                  type="button"
                  className={`w-full rounded-xl border px-1 py-3 text-xs ${activeMaterialTab === tab ? 'border-[#6ba0b9] bg-white text-[#2b5f77]' : 'border-white/70 bg-white/55 text-[#6a8799]'}`}
                  onClick={() => {
                    setActiveMaterialTab(tab)
                    setIsMaterialCollapsed(false)
                  }}
                >
                  {tab === 'cover' ? '封面' : tab === 'paper' ? '内页' : '贴纸'}
                </button>
              ))}
            </div>
          ) : (
            <>
              <div className={`row-span-3 overflow-hidden p-3 ${panel}`}>
                <div className="mb-2 flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-[var(--ds-foreground)]">素材区</h3>
                  <button
                    type="button"
                    className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_52%,transparent)] bg-white/78 px-2.5 py-1 text-xs text-[var(--ds-muted-foreground)]"
                    onClick={() => setIsMaterialCollapsed(true)}
                  >
                    收起
                  </button>
                </div>
                <div className="mb-2 flex items-center justify-between rounded-lg border border-white/70 bg-white/55 px-2.5 py-1.5">
                  <span className="text-[11px] text-[#557487]">翻页音效</span>
                  <button
                    type="button"
                    className={`rounded-full px-2.5 py-1 text-[10px] ${soundEnabled ? 'bg-[#2f6b89] text-white' : 'bg-white text-[#4f6f82]'}`}
                    onClick={() => setSoundEnabled((v) => !v)}
                  >
                    {soundEnabled ? '开启' : '关闭'}
                  </button>
                </div>
                <div className="flex h-[calc(100%-4.5rem)] overflow-hidden rounded-xl border border-white/70 bg-white/35">
                  <div className="flex w-[52px] flex-col border-r border-[#9fc0d2]/35 bg-white/58">
                    {(['cover', 'paper', 'sticker'] as MaterialTab[]).map((tab) => (
                      <button
                        key={tab}
                        type="button"
                        className={`h-[33.333%] text-xs font-semibold transition ${activeMaterialTab === tab ? 'bg-white text-[#2b5f77]' : 'text-[#6a8799] hover:bg-white/60'}`}
                        onClick={() => setActiveMaterialTab(tab)}
                      >
                        {tab === 'cover' ? '封面' : tab === 'paper' ? '内页' : '贴纸'}
                      </button>
                    ))}
                  </div>
                  <div className="min-w-0 flex-1 overflow-auto p-2.5">
                    {activeMaterialTab === 'cover' ? (
                      <div className="grid grid-cols-3 gap-2">
                        {loadingCoverAssets ? <p className="col-span-3 text-xs text-[var(--ds-muted-foreground)]">封面素材加载中...</p> : null}
                        {coverAssetError ? <p className="col-span-3 text-xs text-[#b06a6a]">{coverAssetError}</p> : null}
                        {coverAssets.map((url) => (
                          <button key={url} type="button" onClick={() => void onPickCover(url)} className="overflow-hidden rounded-xl border border-white/70">
                            <img src={url} alt="" className="aspect-[3/4] w-full object-cover" loading="lazy" decoding="async" />
                          </button>
                        ))}
                        {coverHasMore ? <button type="button" onClick={() => void loadMoreCovers()} className="col-span-3 rounded-lg border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/80 py-1.5 text-xs text-[#38647b]">{loadingCoverAssets ? '加载中...' : '加载更多封面'}</button> : null}
                        {!loadingCoverAssets && coverAssets.length === 0 ? <p className="col-span-3 text-xs text-[var(--ds-muted-foreground)]">未检测到封面素材，可继续使用空白封面。</p> : null}
                      </div>
                    ) : null}

                    {activeMaterialTab === 'paper' ? (
                      <div className="grid grid-cols-3 gap-2">
                        {loadingPaperAssets ? <p className="col-span-3 text-xs text-[var(--ds-muted-foreground)]">内页素材加载中...</p> : null}
                        {paperAssetError ? <p className="col-span-3 text-xs text-[#b06a6a]">{paperAssetError}</p> : null}
                        {paperAssets.map((url) => (
                          <button
                            key={url}
                            type="button"
                            onClick={async () => {
                              setSelectedPaper(url)
                              if (!selectedBook || !selectedEntry) return
                              const mergedText = mergePages(leftPageText, rightPageText)
                              const withText = selectedEntry.blocks.some((block) => block.type === 'text')
                                ? selectedEntry.blocks.map((block) => (block.type === 'text' ? { ...block, text: mergedText } : block))
                                : [{ id: uid('blk_txt'), type: 'text', text: mergedText } as DiaryContentBlock, ...selectedEntry.blocks]
                              const withPaper = withText.some((block) => block.type === 'paperStyle')
                                ? withText.map((block) => (block.type === 'paperStyle' ? { ...block, paperUrl: url } : block))
                                : [{ id: uid('blk_paper'), type: 'paperStyle', paperUrl: url } as DiaryContentBlock, ...withText]
                              const saved = await diaryApi.upsertEntry(selectedBook.id, {
                                dayIndex: selectedEntry.dayIndex,
                                title: entryTitle,
                                entryDate: selectedEntry.entryDate,
                                blocks: withPaper,
                              })
                              setSelectedEntry(saved)
                              setBookEntries((prev) => prev.map((entry) => (entry.id === saved.id ? saved : entry)))
                            }}
                            className={`overflow-hidden rounded-xl border ${selectedPaper === url ? 'border-[#6aa0bb]' : 'border-white/70'}`}
                          >
                            <img src={url} alt="" className="aspect-[3/4] w-full object-cover" loading="lazy" decoding="async" />
                          </button>
                        ))}
                        {paperHasMore ? <button type="button" onClick={() => void loadMorePapers()} className="col-span-3 rounded-lg border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/80 py-1.5 text-xs text-[#38647b]">{loadingPaperAssets ? '加载中...' : '加载更多内页'}</button> : null}
                        {!loadingPaperAssets && paperAssets.length === 0 ? <p className="col-span-3 text-xs text-[var(--ds-muted-foreground)]">未检测到纸张素材，可先直接书写。</p> : null}
                      </div>
                    ) : null}

                    {activeMaterialTab === 'sticker' ? (
                      <div className="grid grid-cols-4 gap-2">
                        {loadingStickerAssets ? <p className="col-span-4 text-xs text-[var(--ds-muted-foreground)]">贴纸素材加载中...</p> : null}
                        {stickerAssetError ? <p className="col-span-4 text-xs text-[#b06a6a]">{stickerAssetError}</p> : null}
                        {stickerAssets.map((url, idx) => (
                          <button
                            key={`${url}_${idx}`}
                            type="button"
                            className="overflow-hidden rounded-lg border border-white/70 bg-white/60 p-1 transition hover:scale-[1.03]"
                            onClick={() => addStickerToPage(url)}
                            title="点击贴纸可添加到右页并拖拽"
                          >
                            <img src={url} alt="" className="h-10 w-full object-contain" loading="lazy" decoding="async" />
                          </button>
                        ))}
                        {stickerHasMore ? <button type="button" onClick={() => void loadMoreStickers()} className="col-span-4 rounded-lg border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/80 py-1.5 text-xs text-[#38647b]">{loadingStickerAssets ? '加载中...' : '加载更多贴纸'}</button> : null}
                        {!loadingStickerAssets && stickerAssets.length === 0 ? <p className="col-span-4 text-xs text-[var(--ds-muted-foreground)]">未检测到贴纸素材（当前会读取 sticker 与 paster 目录）。</p> : null}
                      </div>
                    ) : null}
                  </div>
                </div>
              </div>

              <div className={`row-span-1 overflow-auto p-3 ${panel}`}>
                <h4 className="text-base font-semibold text-[var(--ds-foreground)]">内页操作</h4>
                <p className="mt-1 text-[11px] leading-relaxed text-[var(--ds-muted-foreground)]">
                  双击页面添加文字；可拖拽、缩放、旋转，左右页均可放置。导入图片/视频后可自由摆放，AI 配文需手动点击。
                </p>

                {hasGlmKey() ? (
                  <div className="mt-3">
                    <p className="mb-1.5 text-[10px] font-semibold tracking-wide text-[color-mix(in_srgb,var(--ds-muted-foreground)_75%,transparent)]">AI</p>
                    <button
                      type="button"
                      disabled={aiBusy}
                      className="w-full rounded-xl border border-[color-mix(in_srgb,var(--ds-primary)_38%,transparent)] bg-[color-mix(in_srgb,var(--ds-sage)_28%,var(--ds-surface))] px-3 py-2 text-xs font-semibold text-[var(--ds-foreground)] disabled:opacity-50"
                      onClick={() => void runGenerateDiaryDay()}
                    >
                      {aiBusy ? 'AI 生成中…' : 'AI 生成一日排版'}
                    </button>
                  </div>
                ) : null}

                <div className="mt-3">
                  <p className="mb-1.5 text-[10px] font-semibold tracking-wide text-[color-mix(in_srgb,var(--ds-muted-foreground)_75%,transparent)]">编辑</p>
                  <div className="flex flex-col gap-2">
                    <button
                      type="button"
                      className="w-full rounded-xl bg-gradient-to-br from-[var(--ds-primary)] to-[var(--ds-forest)] px-3 py-2 text-xs font-semibold text-white"
                      onClick={() => void saveCurrentBook({ toast: true })}
                    >
                      保存
                    </button>
                    <button
                      type="button"
                      className="w-full rounded-xl border border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-white/80 px-3 py-2 text-xs font-semibold text-[var(--ds-foreground)]"
                      onClick={async () => {
                        if (!selectedBook || !selectedEntry) return
                        const task = await aiApi.createRouteSketchTask({
                          bookId: selectedBook.id,
                          prompt: '手绘风旅行路线，清新温暖',
                          waypoints: ['酒店', '景点A', '景点B'],
                        })
                        if (!task.resultUrl) return
                        const next = await diaryApi.upsertEntry(selectedBook.id, {
                          dayIndex: selectedEntry.dayIndex,
                          title: entryTitle,
                          entryDate: selectedEntry.entryDate,
                          blocks: [...selectedEntry.blocks, { id: uid('blk_route'), type: 'routeSketch', imageUrl: task.resultUrl, prompt: '手绘风路线图' }],
                        })
                        setSelectedEntry(next)
                        setBookEntries((prev) => prev.map((entry) => (entry.id === next.id ? next : entry)))
                      }}
                    >
                      生成路线草图（演示）
                    </button>
                  </div>
                </div>

                <div className="mt-3">
                  <p className="mb-1.5 text-[10px] font-semibold tracking-wide text-[color-mix(in_srgb,var(--ds-muted-foreground)_75%,transparent)]">发布</p>
                  <div className="flex flex-col gap-2">
                    <button
                      type="button"
                      disabled={publishing}
                      className="w-full rounded-xl bg-gradient-to-br from-[var(--ds-primary)] to-[var(--ds-forest)] px-3 py-2 text-xs font-semibold text-white disabled:opacity-60"
                      onClick={async () => {
                        if (!selectedBook || !selectedEntry) return
                        await saveCurrentBook({ toast: false })
                        const id = await publish(
                          selectedBook,
                          selectedEntry,
                          leftPageText,
                          rightPageText,
                          'public',
                          uploadFileCacheRef.current,
                        )
                        if (id) navigate('/community')
                      }}
                    >
                      {publishing ? '发布中…' : '发布到手账社群'}
                    </button>
                    <button
                      type="button"
                      disabled={publishing}
                      className="w-full rounded-xl border border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-white/80 px-3 py-2 text-xs font-semibold text-[var(--ds-foreground)] disabled:opacity-60"
                      onClick={async () => {
                        if (!selectedBook || !selectedEntry) return
                        await saveCurrentBook({ toast: false })
                        await publish(
                          selectedBook,
                          selectedEntry,
                          leftPageText,
                          rightPageText,
                          'private',
                          uploadFileCacheRef.current,
                        )
                      }}
                    >
                      保存私密草稿
                    </button>
                  </div>
                </div>
                {aiError ? (
                  <div className="mt-2">
                    <InlineNotice variant="error">{aiError}</InlineNotice>
                  </div>
                ) : null}
                {publishError ? (
                  <div className="mt-2">
                    <InlineNotice variant="error">{publishError}</InlineNotice>
                  </div>
                ) : null}
                {saveToast ? (
                  <p className="mt-2 text-center font-body text-xs font-semibold text-[var(--ds-primary)]">已保存</p>
                ) : null}
                {publishMsg ? (
                  <div className="mt-2">
                    <InlineNotice variant="success">{publishMsg}</InlineNotice>
                  </div>
                ) : null}
                <div className="mt-3">
                  <p className="mb-1.5 text-[10px] font-semibold tracking-wide text-[color-mix(in_srgb,var(--ds-muted-foreground)_75%,transparent)]">上传</p>
                  <label className={`block rounded-xl border border-dashed border-[color-mix(in_srgb,var(--ds-border)_62%,transparent)] bg-white/60 px-3 py-2.5 text-center text-xs font-semibold text-[var(--ds-foreground)] transition ${uploadBusy ? 'cursor-wait opacity-60' : 'cursor-pointer hover:bg-white/80'}`}>
                    {uploadBusy ? '导入中…' : '选择图片 / 视频'}
                    <input type="file" className="hidden" multiple accept="image/*,video/*" disabled={uploadBusy} onChange={(e) => void onUploadFiles(e.target.files)} />
                  </label>
                  <p className="mt-1 text-[11px] text-[var(--ds-muted-foreground)]">本次已添加 {uploadedCount} 个内容块</p>
                </div>
              </div>
            </>
          )}
        </aside>
      </div>

      {deleteTarget ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#0f1f2a]/26 p-4">
          <div className="w-full max-w-sm rounded-2xl border border-white/80 bg-white p-4 shadow-2xl">
            <p className="text-sm font-semibold text-[#315f76]">确认删除该手账本吗？</p>
            <p className="mt-1 text-xs text-[#708d9f]">删除后无法恢复：{deleteTarget.title}</p>
            <div className="mt-4 flex justify-end gap-2">
              <button type="button" className="rounded-full border border-[#8fb7ca]/40 px-3 py-1.5 text-xs text-[#42667b]" onClick={() => setDeleteTarget(null)}>取消</button>
              <button type="button" className="rounded-full bg-[var(--ds-destructive)] px-3 py-1.5 text-xs font-semibold text-white" onClick={() => void deleteBook()}>确认删除</button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
