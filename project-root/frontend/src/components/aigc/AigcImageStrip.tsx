import { useEffect, useRef } from 'react'
import type { AigcImageItem } from '../../lib/aigc/pipeline'

/** 单张卡片：图 168px + 操作栏 ~36px + 间距 12px ≈ 216px；列表区约 2 张可见 */
const PHOTO_SCROLL_HEIGHT_PX = 432

type AigcImageStripProps = {
  images: AigcImageItem[]
  activeId: string | null
  onSelect: (id: string) => void
  onRemove: (id: string) => void
  onMoveUp: (id: string) => void
  onMoveDown: (id: string) => void
  onUpload: (files: FileList | null) => void
  maxImages: number
  disabled?: boolean
}

export function AigcImageStrip({
  images,
  activeId,
  onSelect,
  onRemove,
  onMoveUp,
  onMoveDown,
  onUpload,
  maxImages,
  disabled,
}: AigcImageStripProps) {
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!activeId || !listRef.current) return
    const card = listRef.current.querySelector(`[data-aigc-id="${activeId}"]`)
    card?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [activeId, images.length])

  return (
    <div
      className={`flex flex-col rounded-[28px] border border-white/80 bg-white/65 p-4 shadow-[0_8px_32px_rgba(42,107,78,0.07)] backdrop-blur-xl ${disabled ? 'opacity-70' : ''}`}
    >
      <div className="mb-3 flex shrink-0 items-center justify-between gap-2">
        <p className="font-body text-[13px] font-bold uppercase tracking-[0.12em] text-[var(--ds-accent-foreground)]">
          旅行照片
        </p>
        <label className="cursor-pointer rounded-full bg-[var(--ds-primary)] px-3 py-1 text-[11px] font-semibold text-white">
          上传
          <input
            type="file"
            className="hidden"
            accept="image/*"
            multiple
            disabled={disabled || images.length >= maxImages}
            onChange={(e) => {
              onUpload(e.target.files)
              e.target.value = ''
            }}
          />
        </label>
      </div>

      <p className="mb-3 shrink-0 text-[11px] text-[var(--ds-muted-foreground)]">
        已选 {images.length}/{maxImages} · 约两张可见 · 区域内纵向滚动
      </p>

      <div
        ref={listRef}
        style={{ height: PHOTO_SCROLL_HEIGHT_PX }}
        className="shrink-0 space-y-3 overflow-y-auto pr-1 [scrollbar-width:thin] [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb]:bg-[var(--ds-primary)]/35"
      >
        {images.length === 0 ? (
          <div className="flex h-full flex-col items-center justify-center rounded-2xl border border-dashed border-[var(--ds-border)]/60 bg-white/50 text-center text-xs text-[var(--ds-muted-foreground)]">
            <p>上传景点或校园旅行照片</p>
            <p className="mt-1">AI 将为你生成旅行短片</p>
          </div>
        ) : (
          images.map((img, index) => {
            const active = img.id === activeId
            return (
              <div
                key={img.id}
                data-aigc-id={img.id}
                className={`relative shrink-0 overflow-hidden rounded-2xl border transition ${
                  active
                    ? 'border-[var(--ds-primary)] ring-2 ring-[var(--ds-primary)]/25'
                    : 'border-[var(--ds-border)]/50'
                }`}
              >
                <button type="button" className="block w-full text-left" onClick={() => onSelect(img.id)}>
                  <img
                    src={img.previewUrl}
                    alt={`旅行照片 ${index + 1}`}
                    className="h-[168px] w-full object-cover"
                  />
                </button>
                <div className="absolute left-2 top-2 rounded-full bg-black/55 px-2 py-0.5 text-[10px] font-bold text-white">
                  {index + 1}
                </div>
                <div className="flex gap-1 border-t border-[var(--ds-border)]/40 bg-white/90 p-1.5">
                  <button
                    type="button"
                    disabled={index === 0 || disabled}
                    onClick={() => onMoveUp(img.id)}
                    className="flex-1 rounded-lg border border-[var(--ds-border)]/50 py-0.5 text-[10px] disabled:opacity-40"
                  >
                    上移
                  </button>
                  <button
                    type="button"
                    disabled={index === images.length - 1 || disabled}
                    onClick={() => onMoveDown(img.id)}
                    className="flex-1 rounded-lg border border-[var(--ds-border)]/50 py-0.5 text-[10px] disabled:opacity-40"
                  >
                    下移
                  </button>
                  <button
                    type="button"
                    disabled={disabled}
                    onClick={() => onRemove(img.id)}
                    className="rounded-lg border border-red-200 px-2 py-0.5 text-[10px] text-red-600"
                  >
                    删
                  </button>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
