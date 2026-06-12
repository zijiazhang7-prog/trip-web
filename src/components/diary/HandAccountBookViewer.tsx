import { useMemo, useState } from 'react'
import type { CommunityFeedItem } from '../../api/community'
import { parseHandAccountLayout } from '../../features/diary/handAccountLayout'
import { parseHandAccountMeta, stripHandAccountMachineLines } from '../../features/diary/publish'
import type { DiaryLayoutStickerLayer, DiaryLayoutTextLayer } from '../../features/diary/types'

const PAGE_SPLIT_MARK = '\n\n---PAGE_BREAK---\n\n'

type SpreadPage = {
  left: string
  right: string
  title: string
}

function splitBodyPages(fullText: string, entryTitle: string): SpreadPage[] {
  const body = stripHandAccountMachineLines(fullText)
  if (!body) {
    return [{ left: '（空白页）', right: '', title: entryTitle }]
  }
  if (body.includes(PAGE_SPLIT_MARK)) {
    const [left, right] = body.split(PAGE_SPLIT_MARK)
    return [{ left: left?.trim() || '', right: right?.trim() || '', title: entryTitle }]
  }
  const chunks = body.split(/\n\n+/).filter(Boolean)
  if (chunks.length <= 2) {
    return [{ left: chunks[0] ?? '', right: chunks[1] ?? '', title: entryTitle }]
  }
  const spreads: SpreadPage[] = []
  for (let i = 0; i < chunks.length; i += 2) {
    spreads.push({
      left: chunks[i] ?? '',
      right: chunks[i + 1] ?? '',
      title: i === 0 ? entryTitle : `${entryTitle} · 续 ${Math.floor(i / 2) + 1}`,
    })
  }
  return spreads
}

function resolveMediaUrl(url: string, imgs: string[]): string {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/files/') || url.startsWith('data:')) return url
  const hit = imgs.find((img) => img.includes(url.split('/').pop() ?? '___'))
  return hit ?? url
}

function ReadonlyTextLayer({ layer }: { layer: DiaryLayoutTextLayer }) {
  const boxW = `${Math.max(28, Math.min(92, 52 * layer.scale))}%`
  return (
    <div
      className="pointer-events-none absolute"
      style={{
        left: `${layer.left}%`,
        top: `${layer.top}%`,
        width: boxW,
        transform: `translate(-50%, -50%) rotate(${layer.rotate}deg)`,
        transformOrigin: 'center',
        zIndex: 10 + layer.z,
      }}
    >
      <p className="whitespace-pre-wrap font-body text-[14px] leading-relaxed text-[var(--ds-foreground)]">
        {layer.value}
      </p>
    </div>
  )
}

function ReadonlyStickerLayer({
  sticker,
  imgs,
}: {
  sticker: DiaryLayoutStickerLayer
  imgs: string[]
}) {
  const baseSize = sticker.kind === 'video' ? 120 : 80
  const box = baseSize * sticker.scale
  const src = resolveMediaUrl(sticker.url, imgs)
  return (
    <div
      className="pointer-events-none absolute"
      style={{
        left: `${sticker.left}%`,
        top: `${sticker.top}%`,
        width: box,
        height: box,
        transform: `translate(-50%, -50%) rotate(${sticker.rotate}deg)`,
        transformOrigin: 'center',
        zIndex: 10 + sticker.z,
      }}
    >
      {sticker.kind === 'video' ? (
        <video src={src} className="h-full w-full object-contain" controls playsInline />
      ) : (
        <img src={src} alt="" className="h-full w-full object-contain" loading="lazy" />
      )}
    </div>
  )
}

type HandAccountBookViewerProps = {
  post: CommunityFeedItem
}

export function HandAccountBookViewer({ post }: HandAccountBookViewerProps) {
  const meta = parseHandAccountMeta(post.fullText)
  const layout = parseHandAccountLayout(post.fullText)
  const coverUrl = meta?.coverUrl || post.coverUrl || post.imgs[0] || ''
  const bookTitle = meta?.bookTitle || post.title || post.bookTitle || '手账'
  const entryTitle = meta?.entryTitle || post.location || '旅程'
  const paperUrl = layout?.paperUrl

  const spreads = useMemo(
    () => splitBodyPages(post.fullText || post.excerpt, entryTitle),
    [post.fullText, post.excerpt, entryTitle],
  )

  const [bookOpened, setBookOpened] = useState(false)
  const [pageIndex, setPageIndex] = useState(0)

  const current = spreads[pageIndex] ?? spreads[0]

  const flip = (dir: -1 | 1) => {
    setPageIndex((i) => Math.max(0, Math.min(spreads.length - 1, i + dir)))
  }

  const pageStyle = () =>
    ({
      backgroundImage: paperUrl
        ? `linear-gradient(rgba(255,255,255,0.2),rgba(255,255,255,0.24)), url('${paperUrl}')`
        : undefined,
      backgroundSize: paperUrl ? 'cover, cover' : undefined,
      backgroundPosition: 'center',
    }) as const

  if (!bookOpened) {
    return (
      <div className="flex min-h-[min(72vh,820px)] items-center justify-center py-4">
        <button
          type="button"
          className="group relative aspect-[3/4] w-full max-w-md overflow-hidden rounded-[24px] border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/68 p-2 shadow-[0_24px_60px_-28px_rgba(37,76,99,0.55)] transition hover:scale-[1.01]"
          onClick={() => setBookOpened(true)}
        >
          {coverUrl ? (
            <img src={coverUrl} alt="" className="h-full w-full rounded-[20px] object-cover" loading="lazy" />
          ) : (
            <div className="flex h-full items-center justify-center rounded-[20px] bg-[var(--ds-muted)] text-sm text-[var(--ds-muted-foreground)]">
              {bookTitle}
            </div>
          )}
          <div className="pointer-events-none absolute inset-x-0 bottom-0 rounded-b-[20px] bg-gradient-to-t from-black/55 to-transparent px-4 py-6 text-left">
            <p className="font-display text-xl font-semibold text-white">{bookTitle}</p>
            <p className="mt-1 text-sm text-white/85">点击翻开手账</p>
          </div>
          <div className="pointer-events-none absolute inset-y-0 left-0 w-6 rounded-l-[20px] bg-gradient-to-r from-[color-mix(in_srgb,var(--ds-forest)_28%,transparent)] to-transparent" />
        </button>
      </div>
    )
  }

  return (
    <div className="flex min-h-[min(72vh,820px)] flex-col items-center justify-center py-2">
      <div
        className="relative aspect-[3/2] w-full max-w-[min(96vw,1180px)] [perspective:2000px]"
        style={{ maxHeight: 'min(78vh, 760px)' }}
      >
        <div className="absolute right-0 top-0 z-30 flex flex-wrap items-center gap-1.5">
          <button
            type="button"
            className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/90 px-2.5 py-1 text-[11px]"
            onClick={() => flip(-1)}
            disabled={pageIndex <= 0}
          >
            上一页
          </button>
          <button
            type="button"
            className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/90 px-2.5 py-1 text-[11px]"
            onClick={() => flip(1)}
            disabled={pageIndex >= spreads.length - 1}
          >
            下一页
          </button>
          <button
            type="button"
            className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_48%,transparent)] bg-white/90 px-2.5 py-1 text-[11px]"
            onClick={() => {
              setBookOpened(false)
              setPageIndex(0)
            }}
          >
            合上
          </button>
          <span className="rounded-full border border-[color-mix(in_srgb,var(--ds-border)_40%,transparent)] bg-white/85 px-2.5 py-1 text-[11px] text-[var(--ds-muted-foreground)]">
            第 {pageIndex + 1} / {spreads.length} 页
          </span>
        </div>

        <div className="absolute inset-0 grid grid-cols-2 gap-2 pt-10">
          <div className="pointer-events-none absolute left-1/2 top-10 z-20 h-[calc(100%-2.5rem)] w-3 -translate-x-1/2 bg-gradient-to-r from-[color-mix(in_srgb,var(--ds-sage)_45%,transparent)] via-white/80 to-[color-mix(in_srgb,var(--ds-sage)_45%,transparent)] shadow-[0_0_14px_rgba(58,95,119,0.22)]" />
          <div className="pointer-events-none absolute left-1/2 top-10 z-30 h-[12%] w-2 -translate-x-1/2 rounded-b bg-gradient-to-b from-[var(--ds-moss)] to-[var(--ds-forest)]" />

          <div
            className="relative overflow-hidden rounded-[18px] border border-[color-mix(in_srgb,var(--ds-border)_38%,transparent)] bg-white/76 p-5 shadow-[inset_-8px_0_16px_rgba(141,177,198,0.18)]"
            style={pageStyle()}
          >
            {current?.left ? (
              <p className="whitespace-pre-wrap font-body text-sm leading-relaxed text-[var(--ds-foreground)]">
                {current.left}
              </p>
            ) : (
              <p className="text-sm text-[var(--ds-muted-foreground)]">—</p>
            )}
          </div>

          <div
            className="relative min-h-[280px] overflow-hidden rounded-[18px] border border-[color-mix(in_srgb,var(--ds-border)_42%,transparent)] bg-white/78 p-5 shadow-[inset_8px_0_16px_rgba(141,177,198,0.2)]"
            style={pageStyle()}
          >
            <p className="font-display text-lg font-semibold text-[var(--ds-foreground)]">{current?.title}</p>
            {current?.right ? (
              <p className="mt-3 whitespace-pre-wrap font-body text-sm leading-relaxed text-[var(--ds-muted-foreground)]">
                {current.right}
              </p>
            ) : null}

            {layout?.textLayers.map((layer) => (
              <ReadonlyTextLayer key={layer.id} layer={layer} />
            ))}
            {layout?.stickerLayers.map((sticker) => (
              <ReadonlyStickerLayer key={sticker.id} sticker={sticker} imgs={post.imgs} />
            ))}
          </div>
        </div>
      </div>
      <p className="mt-3 font-body text-xs text-[var(--ds-muted-foreground)]">
        作者：{post.name} · {post.location}
      </p>
    </div>
  )
}
