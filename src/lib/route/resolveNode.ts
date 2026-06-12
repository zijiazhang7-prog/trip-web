import { fetchDestinationMapNodes } from '../../api/mapNode'

export async function resolveSourceNodeId(
  destinationId: number,
  waypointName: string,
): Promise<number | null> {
  const { nodes } = await fetchDestinationMapNodes(destinationId)
  if (!nodes.length) return null
  const hit =
    nodes.find((n) => n.nodeName === waypointName) ??
    nodes.find((n) => waypointName.includes(n.nodeName) || n.nodeName.includes(waypointName))
  return hit?.nodeId ?? nodes[0]?.nodeId ?? null
}
