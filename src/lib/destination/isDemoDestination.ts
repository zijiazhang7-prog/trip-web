/** 过滤联调/导入测试用的假目的地，避免出现在推荐瀑布流 */
export function isDemoDestination(name: string | undefined | null, description?: string | null): boolean {
  const n = (name ?? '').trim()
  if (/^P[01]\b/i.test(n) || /^P[01]/i.test(n)) return true
  const hay = `${name ?? ''} ${description ?? ''}`
  if (!hay.trim()) return false
  return (
    /联调测试/i.test(hay) ||
    /P0联调/i.test(hay) ||
    /P1\s*导入/i.test(hay) ||
    /导入验证/i.test(hay) ||
    /ImportService/i.test(hay) ||
    /无\s*BOM/i.test(hay) ||
    /multipart\s*实库导入/i.test(hay)
  )
}

/** 展示用：去掉名称前的 P0/P1 测试前缀 */
export function sanitizeDestinationDisplayName(name: string): string {
  return name.replace(/^P[01]\s*/i, '').trim() || name
}
