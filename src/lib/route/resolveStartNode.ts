import type { MapNodeOption } from '../../api/mapNode'

const ENTRANCE_NAME_PATTERNS = [
  /园内入口/,
  /景区入口/,
  /主入口/,
  /正门/,
  /大门/,
  /园门/,
  /入口/,
  /游客中心/,
  /检票/,
  /售票/,
  /entrance/i,
]

export function isTechnicalNodeName(name: string | undefined | null): boolean {
  if (!name?.trim()) return true
  return /^OSM/i.test(name.trim())
}

export function scoreStartNodeCandidate(node: MapNodeOption): number {
  let score = 0
  const name = node.nodeName?.trim() ?? ''
  const type = (node.placeType ?? '').toLowerCase()

  if (type.includes('entrance') || type.includes('gate') || type === '出入口') score += 120
  for (const re of ENTRANCE_NAME_PATTERNS) {
    if (re.test(name)) score += 60
  }
  if (/园内/.test(name)) score += 40
  if (/环球|影城|度假区/.test(name) && /入口/.test(name)) score += 80
  return score
}

/** 从节点列表中推断真实起点（优先入口类节点，而非列表第一项） */
export function resolveDefaultStartNode(nodes: MapNodeOption[]): number | null {
  if (!nodes.length) return null
  const scenic = nodes.filter((n) => !isTechnicalNodeName(n.nodeName))
  const pool = scenic.length ? scenic : nodes

  let best: MapNodeOption | null = null
  let bestScore = -1
  for (const node of pool) {
    const score = scoreStartNodeCandidate(node)
    if (score > bestScore) {
      bestScore = score
      best = node
    }
  }
  return best?.nodeId ?? pool[0]?.nodeId ?? null
}

export function resolveDefaultTargetNode(
  nodes: MapNodeOption[],
  startNodeId: number | null,
): number | null {
  const scenic = nodes.filter((n) => n.nodeId !== startNodeId && !isTechnicalNodeName(n.nodeName))
  if (scenic.length === 0) {
    const fallback = nodes.find((n) => n.nodeId !== startNodeId)
    return fallback?.nodeId ?? startNodeId
  }
  const nonEntrance = scenic.filter((n) => scoreStartNodeCandidate(n) < 40)
  return (nonEntrance[0] ?? scenic[0])?.nodeId ?? null
}
