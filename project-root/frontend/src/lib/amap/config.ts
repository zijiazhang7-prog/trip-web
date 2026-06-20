/** Web 端 JS API Key（tripweb2 · 地图显示） */
export function getAmapJsKey(): string {
  return (import.meta.env.VITE_AMAP_JS_KEY ?? import.meta.env.VITE_AMAP_KEY)?.trim() ?? ''
}

/** Web 服务 Key（tripweb1 · 路径规划 / 地理编码 / 周边搜索） */
export function getAmapWebKey(): string {
  return (import.meta.env.VITE_AMAP_WEB_KEY ?? import.meta.env.VITE_AMAP_KEY)?.trim() ?? ''
}

export function getAmapSecurityCode(): string {
  return import.meta.env.VITE_AMAP_SECURITY_CODE?.trim() ?? ''
}

export function hasAmapJsKey(): boolean {
  return Boolean(getAmapJsKey())
}

export function hasAmapWebKey(): boolean {
  return Boolean(getAmapWebKey())
}

/** 地图 + 路径服务均已配置 */
export function hasFullAmapSetup(): boolean {
  return hasAmapJsKey() && hasAmapWebKey()
}

export function hasAmapSecurityCode(): boolean {
  return Boolean(getAmapSecurityCode())
}

/** @deprecated 使用 hasAmapJsKey */
export function hasAmapKey(): boolean {
  return hasAmapJsKey()
}
