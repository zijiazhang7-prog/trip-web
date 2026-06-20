import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  deleteCommunityDiary,
  fetchCommunityFeed,
  fetchCommunityDiaryDetail,
  fetchDiariesByDestination,
  searchCommunityByKeyword,
  searchCommunityByTitle,
  type CommunityFeedItem,
} from '../api/community'
import { getCurrentUser } from '../api/auth'
import { getDiaryApi } from '../api/diary'
import { hasStoredToken } from '../api/http'
import { useTripContext } from '../context/tripContext'
import { communityPosts } from '../data/siteData'
import { entryPlainText, publishHandAccountWithCustomText } from '../features/diary/publish'
import type { DiaryBook, DiaryEntry } from '../features/diary/types'
import { CommentSection } from '../components/ui/CommentSection'
import { DetailOverlay } from '../components/ui/DetailOverlay'
import { InlineNotice } from '../components/ui/InlineNotice'
import { PageHeader } from '../components/ui/PageHeader'
import { HandAccountBookViewer } from '../components/diary/HandAccountBookViewer'
import { RatingPanel } from '../components/ui/RatingPanel'

const glass =
  'ds-card-lift ds-glass-panel mb-6 break-inside-avoid rounded-[32px] p-7 last:mb-0 hover:-translate-y-1'
const communitySideVisual = '/images/community-side-visual.png'

type SearchMode = 'fulltext' | 'title' | 'destination'

export function CommunityPage() {
  const { destinationId } = useTripContext()
  const [searchParams] = useSearchParams()
  const INITIAL_VISIBLE = 10
  const LOAD_STEP = 6
  const [posts, setPosts] = useState<CommunityFeedItem[]>([])
  const [usingCommunityFallback, setUsingCommunityFallback] = useState(false)
  const [sortBy, setSortBy] = useState<'latest' | 'heat' | 'interest'>('latest')
  const [searchText, setSearchText] = useState('')
  const [searchMode, setSearchMode] = useState<SearchMode>('fulltext')
  const diaryApi = useMemo(() => getDiaryApi(), [])
  const [publishTitle, setPublishTitle] = useState('')
  const [publishText, setPublishText] = useState('')
  const [diaryBooks, setDiaryBooks] = useState<DiaryBook[]>([])
  const [selectedBookId, setSelectedBookId] = useState<string | null>(null)
  const [selectedEntry, setSelectedEntry] = useState<DiaryEntry | null>(null)
  const [loadingBooks, setLoadingBooks] = useState(false)
  const [feedLoading, setFeedLoading] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [publishMsg, setPublishMsg] = useState<string | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [selectedPost, setSelectedPost] = useState<CommunityFeedItem | null>(null)
  const [highlightedPostId, setHighlightedPostId] = useState<number | null>(null)
  const [selectedPostId, setSelectedPostId] = useState<number | null>(null)
  const [visibleCount, setVisibleCount] = useState(INITIAL_VISIBLE)
  const [revealedIds, setRevealedIds] = useState<Set<number>>(new Set())
  const [currentUserId, setCurrentUserId] = useState<number | null>(null)
  const [deletingPostId, setDeletingPostId] = useState<number | null>(null)
  const loadMoreRef = useRef<HTMLDivElement | null>(null)

  const applyFeedPosts = (nextPosts: CommunityFeedItem[]) => {
    setPosts(nextPosts)
    setVisibleCount(INITIAL_VISIBLE)
    setRevealedIds(new Set())
  }

  const handleDeletePost = async (postId: number) => {
    if (!hasStoredToken()) {
      setError('请先登录后再删除')
      return
    }
    if (!window.confirm('确定删除这条手账？删除后不可恢复。')) return
    setDeletingPostId(postId)
    setError(null)
    try {
      await deleteCommunityDiary(postId)
      setPosts((prev) => prev.filter((p) => p.id !== postId))
      if (selectedPostId === postId) {
        setDetailOpen(false)
        setSelectedPost(null)
        setSelectedPostId(null)
      }
      setPublishMsg('已删除你的手账')
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除失败')
    } finally {
      setDeletingPostId(null)
    }
  }

  useEffect(() => {
    if (!hasStoredToken()) {
      setCurrentUserId(null)
      return
    }
    let cancelled = false
    void getCurrentUser()
      .then((user) => {
        if (!cancelled) setCurrentUserId(user.id)
      })
      .catch(() => {
        if (!cancelled) setCurrentUserId(null)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      await Promise.resolve()
      if (cancelled) return
      setLoadingBooks(true)
      try {
        const { items } = await diaryApi.listBooks()
        if (!cancelled) setDiaryBooks(items)
      } catch {
        if (!cancelled) setDiaryBooks([])
      } finally {
        if (!cancelled) setLoadingBooks(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [diaryApi])

  const selectHandAccount = async (bookId: string) => {
    setSelectedBookId(bookId)
    setPublishMsg(null)
    try {
      const detail = await diaryApi.getBookDetail(bookId)
      const entry = detail.entries[detail.entries.length - 1] ?? detail.entries[0] ?? null
      setSelectedEntry(entry)
      if (!publishTitle.trim()) setPublishTitle(detail.book.title)
      if (!publishText.trim() && entry) setPublishText(entryPlainText(entry))
    } catch (err) {
      setError(err instanceof Error ? err.message : '手账加载失败')
      setSelectedEntry(null)
    }
  }

  useEffect(() => {
    if (searchParams.get('destinationId')) return undefined
    let cancelled = false
    const run = async () => {
      setFeedLoading(true)
      setError(null)
      try {
        const feed = await fetchCommunityFeed(sortBy)
        if (!cancelled) {
          applyFeedPosts(feed)
          setUsingCommunityFallback(false)
        }
      } catch (err) {
        if (!cancelled) {
          const msg = err instanceof Error ? err.message : '社群数据加载失败'
          setError(msg)
          applyFeedPosts(communityPosts)
          setUsingCommunityFallback(true)
        }
      } finally {
        if (!cancelled) setFeedLoading(false)
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [sortBy, searchParams])

  useEffect(() => {
    const destParam = searchParams.get('destinationId')
    if (!destParam) return
    const id = Number(destParam)
    if (!Number.isFinite(id)) return
    let cancelled = false
    setFeedLoading(true)
    setSearchMode('destination')
    void fetchDiariesByDestination(id, sortBy === 'heat' ? 'heat' : 'latest')
      .then((feed) => {
        if (!cancelled) {
          applyFeedPosts(feed)
          setUsingCommunityFallback(false)
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : '目的地日记加载失败')
      })
      .finally(() => {
        if (!cancelled) setFeedLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [searchParams, sortBy])

  useEffect(() => {
    if (!detailOpen) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setDetailOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [detailOpen])

  useEffect(() => {
    if (!loadMoreRef.current) return
    const node = loadMoreRef.current
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          setVisibleCount((prev) => Math.min(prev + LOAD_STEP, posts.length))
        })
      },
      { rootMargin: '160px 0px 220px 0px' },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [posts.length])

  useEffect(() => {
    const cards = Array.from(document.querySelectorAll<HTMLElement>('[data-reveal-card]'))
    if (cards.length === 0) return
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          const id = Number((entry.target as HTMLElement).dataset.postId)
          if (!Number.isFinite(id)) return
          setRevealedIds((prev) => {
            if (prev.has(id)) return prev
            const next = new Set(prev)
            next.add(id)
            return next
          })
          observer.unobserve(entry.target)
        })
      },
      { threshold: 0.14, rootMargin: '0px 0px -8% 0px' },
    )
    cards.forEach((card) => observer.observe(card))
    return () => observer.disconnect()
  }, [visibleCount, posts])

  const runSearch = async () => {
    const keyword = searchText.trim()
    if (!keyword && searchMode !== 'destination') return
    setFeedLoading(true)
    setError(null)
    try {
      let result: CommunityFeedItem[]
      if (searchMode === 'title') {
        result = await searchCommunityByTitle(keyword)
      } else if (searchMode === 'destination') {
        const id = destinationId ?? Number(keyword)
        if (!Number.isFinite(id)) throw new Error('请输入有效的目的地编号，或从推荐页进入')
        result = await fetchDiariesByDestination(id, sortBy === 'heat' ? 'heat' : 'latest')
      } else {
        result = await searchCommunityByKeyword(keyword)
      }
      applyFeedPosts(result)
      setUsingCommunityFallback(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索失败')
      applyFeedPosts(communityPosts)
      setUsingCommunityFallback(true)
    } finally {
      setFeedLoading(false)
    }
  }

  const resetFeed = async () => {
    setSearchText('')
    setFeedLoading(true)
    setError(null)
    try {
      const feed = await fetchCommunityFeed(sortBy)
      applyFeedPosts(feed)
      setUsingCommunityFallback(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : '刷新失败')
      applyFeedPosts(communityPosts)
      setUsingCommunityFallback(true)
    } finally {
      setFeedLoading(false)
    }
  }

  const onPublish = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setPublishMsg(null)
    setError(null)
    if (!selectedBookId || !selectedEntry) {
      setPublishMsg('请先选择要分享的手账')
      return
    }
    if (!publishTitle.trim() || !publishText.trim()) {
      setPublishMsg('请先填写标题和正文')
      return
    }
    if (!hasStoredToken()) {
      setPublishMsg('请先登录后再发布')
      return
    }
    const book = diaryBooks.find((b) => b.id === selectedBookId)
    if (!book) {
      setPublishMsg('手账不存在，请重新选择')
      return
    }
    setPublishing(true)
    try {
      const diaryId = await publishHandAccountWithCustomText({
        book,
        entry: selectedEntry,
        title: publishTitle.trim(),
        bodyText: publishText.trim(),
        destinationId: destinationId ?? 101,
      })
      setPublishMsg(`发布成功，手账已上架（#${diaryId}）`)
      setPublishTitle('')
      setPublishText('')
      setSelectedBookId(null)
      setSelectedEntry(null)
      const { items } = await diaryApi.listBooks()
      setDiaryBooks(items)
      await resetFeed()
      setHighlightedPostId(diaryId)
      window.setTimeout(() => {
        const target = document.getElementById(`community-post-${diaryId}`)
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'center' })
        } else {
          setPublishMsg((prev) =>
            prev
              ? `${prev}。当前页未命中该卡片，可用标题检索查看`
              : '发布成功。当前页未命中该卡片，可用标题检索查看',
          )
        }
      }, 180)
      window.setTimeout(() => {
        setHighlightedPostId((prev) => (prev === diaryId ? null : prev))
      }, 2200)
    } catch (err) {
      setError(err instanceof Error ? err.message : '发布失败')
    } finally {
      setPublishing(false)
    }
  }

  const openDetail = (id: number) => {
    const cached = posts.find((p) => p.id === id) ?? null
    setDetailOpen(true)
    setSelectedPostId(id)
    setSelectedPost(cached)
    setDetailLoading(true)
    void fetchCommunityDiaryDetail(id)
      .then((detail) => {
        setSelectedPost(detail)
        setPosts((prev) =>
          prev.map((p) => (p.id === detail.id ? { ...p, likes: detail.likes, fullText: detail.fullText, videos: detail.videos, imgs: detail.imgs } : p)),
        )
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : '加载详情失败')
        if (!cached) setSelectedPost(null)
      })
      .finally(() => setDetailLoading(false))
  }

  const stepDetail = (dir: -1 | 1) => {
    if (posts.length === 0) return
    const currentId = selectedPostId ?? selectedPost?.id ?? posts[0].id
    const currentIndex = posts.findIndex((p) => p.id === currentId)
    const baseIndex = currentIndex >= 0 ? currentIndex : 0
    const nextIndex = Math.max(0, Math.min(posts.length - 1, baseIndex + dir))
    const next = posts[nextIndex]
    if (!next) return
    void openDetail(next.id)
  }

  const visiblePosts = posts.slice(0, visibleCount)

  return (
    <div className="mx-auto max-w-[1400px] animate-fade-rise px-4 py-8 md:px-8">
      <PageHeader
        eyebrow="Hand Account Gallery"
        title="手账社群"
        description="这里展示大家分享的旅游手账。在旅游日记中编辑并发布后，会以手账封面形式出现在此书架。"
      />
      <InlineNotice variant="info">
        在 <Link to="/diary" className="font-semibold underline">旅游日记</Link> 做好手账后，可在下方选择手账并填写标题、正文分享到社群。
      </InlineNotice>

      <div className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <aside className="space-y-4 lg:sticky lg:top-24 lg:h-[calc(100vh-7rem)] lg:overflow-y-auto pr-1">
          <div className="rounded-2xl border border-[var(--ds-primary)]/12 bg-white/75 p-4 shadow-sm backdrop-blur-md">
            <div>
              <p className="text-sm font-semibold text-[#2C3E36]">我的社群空间</p>
              <p className="text-xs text-[#6B8076]">记录旅行，分享灵感</p>
            </div>
            <div className="mt-3 overflow-hidden rounded-xl border border-[var(--ds-primary)]/10 bg-[var(--ds-muted)]">
              <img
                src={communitySideVisual}
                alt="社群视觉"
                className="h-28 w-full object-cover transition duration-500 hover:scale-[1.03]"
              />
            </div>
          </div>

          <form onSubmit={onPublish} className="space-y-3 rounded-2xl border border-[var(--ds-primary)]/12 bg-white/75 p-4 shadow-sm backdrop-blur-md">
            <p className="text-xs font-semibold uppercase tracking-wide text-[#6B8076]">快速分享我的手账</p>
            <input
              type="text"
              value={publishTitle}
              onChange={(e) => setPublishTitle(e.target.value)}
              placeholder="发布标题"
              className="w-full rounded-xl border border-[var(--ds-primary)]/20 bg-white px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary)]"
            />
            <textarea
              value={publishText}
              onChange={(e) => setPublishText(e.target.value)}
              placeholder="发布正文"
              rows={5}
              className="w-full rounded-xl border border-[var(--ds-primary)]/20 bg-white px-3 py-2 text-sm outline-none focus-visible:ring-2 focus-visible:ring-[var(--ds-primary)]"
            />
            <div className="rounded-xl border border-dashed border-[var(--ds-primary)]/25 bg-white/75 p-3">
              <p className="mb-2 text-xs font-semibold text-[var(--ds-primary)]">选择手账</p>
              {loadingBooks ? (
                <p className="text-xs text-[#6B8076]">加载我的手账…</p>
              ) : diaryBooks.length === 0 ? (
                <p className="text-xs text-[#6B8076]">
                  还没有手账，请先到{' '}
                  <Link to="/diary" className="font-semibold text-[var(--ds-primary)] underline">
                    旅游日记
                  </Link>{' '}
                  创建
                </p>
              ) : (
                <div className="grid max-h-44 grid-cols-2 gap-2 overflow-y-auto">
                  {diaryBooks.map((book) => {
                    const active = selectedBookId === book.id
                    return (
                      <button
                        key={book.id}
                        type="button"
                        onClick={() => void selectHandAccount(book.id)}
                        className={`overflow-hidden rounded-xl border text-left transition ${
                          active
                            ? 'border-[var(--ds-primary)] bg-[color-mix(in_srgb,var(--ds-primary)_8%,white)] shadow-sm'
                            : 'border-[var(--ds-primary)]/15 bg-white hover:border-[var(--ds-primary)]/35'
                        }`}
                      >
                        <div className="aspect-[4/3] bg-[var(--ds-muted)]">
                          {book.coverAssetUrl ? (
                            <img src={book.coverAssetUrl} alt="" className="h-full w-full object-cover" />
                          ) : (
                            <div className="flex h-full items-center justify-center text-[10px] text-[var(--ds-muted-foreground)]">
                              无封面
                            </div>
                          )}
                        </div>
                        <p className="truncate px-2 py-1.5 text-xs font-semibold text-[var(--ds-foreground)]">
                          {book.title}
                        </p>
                      </button>
                    )
                  })}
                </div>
              )}
              {selectedBookId ? (
                <p className="mt-2 text-[11px] text-[#6B8076]">已选手账，封面与内页素材将随发布一并带上</p>
              ) : null}
            </div>
            <button
              type="submit"
              disabled={publishing}
              className="w-full rounded-xl bg-[var(--ds-primary)] px-4 py-2 text-sm font-semibold text-[var(--ds-primary-foreground)] transition hover:brightness-110 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {publishing ? '发布中…' : '分享我的手账'}
            </button>
            {publishMsg ? <p className="text-sm text-[#2c7a5d]">{publishMsg}</p> : null}
          </form>
        </aside>

        <section className="min-w-0">
          <div className="mb-4 grid gap-3 rounded-2xl border border-[var(--ds-primary)]/12 bg-white/75 p-4 shadow-sm backdrop-blur-md md:grid-cols-[auto_1fr_auto_auto_auto]">
            <select
              value={searchMode}
              onChange={(e) => setSearchMode(e.target.value as SearchMode)}
              className="rounded-xl border border-[var(--ds-primary)]/20 bg-white px-3 py-2 text-sm outline-none"
            >
              <option value="fulltext">全文</option>
              <option value="title">标题</option>
              <option value="destination">目的地</option>
            </select>
            <input
              type="text"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              placeholder={
                searchMode === 'title'
                  ? '输入日记标题（精确检索）'
                  : searchMode === 'destination'
                    ? `目的地编号${destinationId ? `（当前 ${destinationId}）` : ''}`
                    : '输入关键词（正文全文检索）'
              }
              className="rounded-xl border border-[var(--ds-primary)]/20 bg-white px-3 py-2 text-sm outline-none"
            />
            <button
              type="button"
              onClick={() => setSortBy('latest')}
              className="rounded-xl border border-[var(--ds-primary)]/20 bg-white px-4 py-2 text-sm text-[var(--ds-primary)] transition hover:-translate-y-0.5"
            >
              最新发布
            </button>
            <button
              type="button"
              onClick={() => setSortBy('heat')}
              className="rounded-xl border border-[var(--ds-primary)]/20 bg-white px-4 py-2 text-sm text-[var(--ds-primary)] transition hover:-translate-y-0.5"
            >
              最受欢迎
            </button>
            <button
              type="button"
              onClick={() => setSortBy('interest')}
              className="rounded-xl border border-[var(--ds-primary)]/20 bg-white px-4 py-2 text-sm text-[var(--ds-primary)] transition hover:-translate-y-0.5"
            >
              兴趣推荐
            </button>
            <button
              type="button"
              onClick={searchText.trim() ? runSearch : resetFeed}
              className="rounded-xl bg-[var(--ds-primary)] px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
            >
              {searchText.trim() ? '执行检索' : '刷新列表'}
            </button>
          </div>

          {feedLoading ? <p className="mb-4 text-sm text-[#6B8076]">正在同步社群日记...</p> : null}
          {usingCommunityFallback ? (
            <p className="mb-4 rounded-xl border border-amber-200/90 bg-amber-50/95 px-4 py-3 text-sm text-amber-950">
              后端不可用或请求失败，当前展示本地示例数据。{error ? `详情：${error}` : ''}
            </p>
          ) : error ? (
            <p className="mb-4 text-sm text-[#a24a4a]">{error}</p>
          ) : null}
          {!feedLoading && !usingCommunityFallback && posts.length === 0 ? (
            <p className="mb-4 text-sm text-[#6B8076]">暂无社群日记，发布一条或稍后再试。</p>
          ) : null}

          <div className="columns-1 gap-5 md:columns-2 xl:columns-3">
            {visiblePosts.map((post) => (
              <article
                data-reveal-card
                data-post-id={post.id}
                id={`community-post-${post.id}`}
                key={post.id}
                className={`${glass} ${
                  revealedIds.has(post.id) ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
                } ${
                  highlightedPostId === post.id
                    ? 'ring-2 ring-[#3E8C70]/50 shadow-[0_28px_80px_rgba(42,107,78,0.22)]'
                    : ''
                } duration-500`}
                style={{ transitionProperty: 'opacity, transform, box-shadow' }}
              >
            {post.handAccount ? (
              <div className="relative mb-4 overflow-hidden rounded-2xl border border-[color-mix(in_srgb,var(--ds-border)_45%,transparent)] shadow-md">
                {post.coverUrl ? (
                  <img
                    src={post.coverUrl}
                    alt=""
                    className="img-warm aspect-[3/4] max-h-56 w-full object-cover"
                  />
                ) : (
                  <div className="flex aspect-[3/4] max-h-56 w-full flex-col justify-end bg-gradient-to-b from-[#f7f2ea] via-[#efe6d8] to-[#e5d9c8] p-4">
                    <p className="font-display text-lg font-bold text-[var(--ds-foreground)]">
                      {post.bookTitle || post.title}
                    </p>
                    {post.days ? (
                      <p className="mt-1 text-xs text-[var(--ds-muted-foreground)]">{post.days} 天旅程</p>
                    ) : null}
                  </div>
                )}
                {post.coverUrl ? (
                  <>
                    <div className="absolute inset-0 bg-gradient-to-t from-[color-mix(in_srgb,var(--ds-foreground)_55%,transparent)] via-transparent to-transparent" />
                    <div className="absolute bottom-0 left-0 right-0 p-4">
                      <p className="font-display text-lg font-bold text-white drop-shadow">{post.bookTitle || post.title}</p>
                      {post.days ? (
                        <span className="mt-1 inline-block rounded-full bg-white/20 px-2 py-0.5 text-xs text-white backdrop-blur">
                          {post.days} 天旅程
                        </span>
                      ) : null}
                    </div>
                  </>
                ) : null}
              </div>
            ) : null}
            <div className="mb-3 flex items-center gap-3">
              <div className="h-9 w-9 overflow-hidden rounded-full border border-white shadow-sm">
                <img src={post.avatar} alt="" className="h-full w-full object-cover" />
              </div>
              <div>
                <div className="font-body text-sm font-semibold text-[var(--ds-foreground)]">{post.name}</div>
                <div className="text-xs text-[var(--ds-muted-foreground)]">{post.location}</div>
              </div>
              {post.handAccount ? (
                <span className="ml-auto rounded-full bg-[color-mix(in_srgb,var(--ds-primary)_12%,white)] px-2 py-0.5 text-[10px] font-bold uppercase tracking-wide text-[var(--ds-primary)]">
                  手账
                </span>
              ) : null}
            </div>
            <p className="mb-3 font-body text-sm leading-relaxed text-[var(--ds-muted-foreground)] line-clamp-4">
              {post.excerpt}
            </p>
            <button
              type="button"
              className="mb-3 rounded-full border border-[color-mix(in_srgb,var(--ds-primary)_22%,transparent)] px-3 py-1 text-xs font-semibold text-[var(--ds-primary)]"
              onClick={() => openDetail(post.id)}
            >
              翻开手账
            </button>
            {!post.handAccount && post.imgs.length > 0 ? (
              <div className="mb-4 grid grid-cols-2 gap-2">
                {post.imgs.slice(0, 2).map((img, i) => (
                  <img key={i} src={img} alt="" className="h-24 w-full rounded-xl object-cover" />
                ))}
              </div>
            ) : null}
            <div className="flex items-center justify-between border-t border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] pt-3 text-xs text-[var(--ds-muted-foreground)]">
              <span>热度 {post.likes}</span>
              {currentUserId != null && post.userId === currentUserId ? (
                <button
                  type="button"
                  disabled={deletingPostId === post.id}
                  className="rounded-full border border-[color-mix(in_srgb,var(--ds-destructive)_35%,transparent)] px-2.5 py-0.5 text-[10px] font-semibold text-[var(--ds-destructive)] disabled:opacity-50"
                  onClick={() => void handleDeletePost(post.id)}
                >
                  {deletingPostId === post.id ? '删除中…' : '删除'}
                </button>
              ) : null}
            </div>
              </article>
            ))}
          </div>

          <div ref={loadMoreRef} className="mt-6 flex h-14 items-center justify-center">
            {visibleCount < posts.length ? (
              <span className="text-xs text-[#6B8076]">下滑加载更多内容...</span>
            ) : (
              <span className="text-xs text-[#8ca49a]">已显示全部内容</span>
            )}
          </div>
        </section>
      </div>

      {detailOpen ? (
        <DetailOverlay
          open={detailOpen}
          title="手账详情"
          onClose={() => setDetailOpen(false)}
          onPrev={() => stepDetail(-1)}
          onNext={() => stepDetail(1)}
          indexLabel={
            selectedPost
              ? `${Math.max(0, posts.findIndex((p) => p.id === selectedPost.id)) + 1} / ${posts.length}`
              : undefined
          }
        >
            {detailLoading ? (
              <p className="text-sm text-[#6B8076]">加载中...</p>
            ) : selectedPost ? (
              <div className="space-y-5">
                {selectedPost.handAccount ? (
                  <HandAccountBookViewer post={selectedPost} />
                ) : (
                  <div className="space-y-4">
                    <p className="text-sm text-[#6B8076]">作者：{selectedPost.name} · {selectedPost.location}</p>
                    <p className="text-sm leading-relaxed text-[#4f655c]">{selectedPost.fullText || selectedPost.excerpt}</p>
                    {selectedPost.imgs.length > 0 ? (
                      <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                        {selectedPost.imgs.map((img) => (
                          <img key={img} src={img} alt="" className="h-32 w-full rounded-lg object-cover" />
                        ))}
                      </div>
                    ) : null}
                  </div>
                )}
                <RatingPanel
                  targetType="diary"
                  targetId={selectedPost.id}
                  average={selectedPost.ratingScore}
                  count={selectedPost.ratingCount}
                />
                <CommentSection targetType="diary" targetId={selectedPost.id} title="手账评论" />
              </div>
            ) : (
              <p className="text-sm text-[#6B8076]">暂无详情</p>
            )}
        </DetailOverlay>
      ) : null}
    </div>
  )
}
