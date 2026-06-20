/**
 * 将后端分页 JSON（Spring/MyBatis-Plus 等常见字段名差异）规范成统一结构，
 * 避免 list 实际在 records 等字段时前端读到 undefined → 空白列表。
 */
export type NormalizedPage<T> = {
  list: T[]
  pageNum: number
  pageSize: number
  total: number
  pages: number
}

function num(v: unknown, fallback: number): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function pickList(o: Record<string, unknown>): unknown[] | null {
  const direct =
    o.list ??
    o.records ??
    o.rows ??
    o.content ??
    o.items ??
    (Array.isArray(o.data) ? o.data : undefined)
  if (Array.isArray(direct)) return direct

  const data = o.data
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    const inner = data as Record<string, unknown>
    const nested =
      inner.list ?? inner.records ?? inner.rows ?? inner.content ?? inner.items
    if (Array.isArray(nested)) return nested
  }
  return null
}

export function coerceSpringPage<T>(raw: unknown): NormalizedPage<T> {
  if (Array.isArray(raw)) {
    const len = raw.length
    return { list: raw as T[], pageNum: 1, pageSize: len || 10, total: len, pages: 1 }
  }
  if (!raw || typeof raw !== 'object') {
    return { list: [], pageNum: 1, pageSize: 10, total: 0, pages: 1 }
  }
  const envelope = raw as Record<string, unknown>
  if (
    envelope.success === true &&
    envelope.data != null &&
    typeof envelope.data === 'object' &&
    !Array.isArray(envelope.data)
  ) {
    return coerceSpringPage<T>(envelope.data)
  }

  const o = raw as Record<string, unknown>
  const listRaw = pickList(o)
  const list = Array.isArray(listRaw) ? (listRaw as T[]) : []

  const total = num(o.total ?? o.totalElements ?? o.totalCount, 0)
  let pageSize = num(o.pageSize ?? o.size, 0)
  if (pageSize <= 0) pageSize = list.length > 0 ? list.length : 10

  let pageNum = num(o.pageNum ?? o.current ?? o.pageNo ?? o.page, 1)
  if (pageNum < 1) pageNum = 1

  let pages = num(o.pages ?? o.pagesTotal ?? o.pageTotal ?? o.pagesCount ?? 0, 0)
  if (pages <= 0 && total > 0 && pageSize > 0) pages = Math.max(1, Math.ceil(total / pageSize))
  if (pages <= 0) pages = 1

  if (total > 0 && pageSize > 0) {
    const minPagesByTotal = Math.ceil(total / pageSize)
    if (pages < minPagesByTotal) pages = minPagesByTotal
  }

  return { list, pageNum, pageSize, total, pages }
}
