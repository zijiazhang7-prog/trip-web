import { fetchRecommendedDestinationsPage } from './destination'
import { inferTotalPages } from './pagination'
import { isDemoDestination } from '../lib/destination/isDemoDestination'

/** 从推荐接口分页拉取真实目的地 id（排除联调测试数据） */
export async function fetchNonDemoDestinationIds(maxPages = 8, pageSize = 50): Promise<number[]> {
  const ids: number[] = []
  const seen = new Set<number>()
  let pageNum = 1
  let totalPages = 1

  while (pageNum <= totalPages && pageNum <= maxPages) {
    const res = await fetchRecommendedDestinationsPage({
      pageNum,
      pageSize,
      sortBy: 'recommend',
    })
    totalPages = inferTotalPages(res, pageSize, pageNum)

    for (const row of res.list) {
      if (row.id == null || seen.has(row.id)) continue
      if (isDemoDestination(row.name, row.description)) continue
      seen.add(row.id)
      ids.push(row.id)
    }
    pageNum += 1
  }

  return ids
}
