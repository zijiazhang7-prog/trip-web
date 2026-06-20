import type { PageResult } from './destination'

const MAX_PAGE_SIZE = 100

function clampPageSize(pageSize: number): number {
  if (!Number.isFinite(pageSize) || pageSize < 1) return 10
  return Math.min(Math.floor(pageSize), MAX_PAGE_SIZE)
}

/** 用后端 pages / total 推算总页数，供无限滚动判断下一页 */
export function inferTotalPages<T>(
  res: PageResult<T>,
  pageSize: number,
  fetchedPageNum: number,
): number {
  const size = clampPageSize(pageSize)
  const backendPages = Number(res.pages)
  if (Number.isFinite(backendPages) && backendPages > 0) {
    return Math.floor(backendPages)
  }

  const total = Number(res.total)
  if (Number.isFinite(total) && total > 0) {
    return Math.max(1, Math.ceil(total / size))
  }

  const batchLen = (res.list ?? []).length
  let pages = Math.max(1, fetchedPageNum)
  if (batchLen >= size) pages = Math.max(pages, fetchedPageNum + 1)
  return pages
}

/** 景点推荐：按 id 合并后无新增则停止（兼容旧后端重复页） */
export function shouldStopRecommendPagination(addedCount: number, batchLength: number): boolean {
  if (batchLength === 0) return true
  return addedCount === 0
}
