import type { PageResult } from './destination'

/** 用 total、后端 pages、以及「本页是否满页」推算总页数，减轻后端 pages 字段不准导致的过早截断 */
export function inferTotalPages<T>(
  res: PageResult<T>,
  pageSize: number,
  fetchedPageNum: number,
): number {
  const size = pageSize > 0 ? pageSize : 10
  const total = Number(res.total)
  const fromTotal =
    Number.isFinite(total) && total > 0 ? Math.max(1, Math.ceil(total / size)) : 0

  /**
   * 当 total>0 时，真实页数不可能超过 ceil(total/size)。
   * 旧逻辑把 fetchedPageNum、满页+(pageNum+1) 并入 max，会在后端反复返回满页时把 pages 推到数千，
   * nextPage 永远不大于 infer 出的 pages → 目的地推荐接口被打爆。
   */
  if (fromTotal > 0) {
    return Math.max(1, fromTotal)
  }

  const fromBackend = Number(res.pages)
  const backendPages =
    Number.isFinite(fromBackend) && fromBackend > 0 ? Math.floor(fromBackend) : 0
  const batchLen = (res.list ?? []).length

  let pages = Math.max(backendPages, fetchedPageNum)
  if (batchLen >= size) pages = Math.max(pages, fetchedPageNum + 1)
  return Math.max(1, pages)
}
