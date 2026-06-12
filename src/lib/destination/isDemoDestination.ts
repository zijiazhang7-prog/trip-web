/** 过滤联调/导入测试用的假目的地，避免出现在推荐瀑布流 */
export function isDemoDestination(name: string | undefined | null, description?: string | null): boolean {
  const hay = `${name ?? ''} ${description ?? ''}`
  if (!hay.trim()) return false
  return (
    /联调测试/i.test(hay) ||
    /P0联调/i.test(hay) ||
    /导入验证/i.test(hay) ||
    /ImportService/i.test(hay) ||
    /无\s*BOM/i.test(hay) ||
    /multipart\s*实库导入/i.test(hay)
  )
}
