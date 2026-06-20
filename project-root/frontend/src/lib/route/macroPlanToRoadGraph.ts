import type { PathNodeResult } from '../../api/route'
import type { MapNodeOption } from '../../api/mapNode'
import type { MacroRoutePlan } from '../../types/macroRoute'

export function macroPlanToRoadGraph(plan: MacroRoutePlan): {
  nodeCatalog: MapNodeOption[]
  pathNodes: PathNodeResult[]
} {
  const nodeCatalog: MapNodeOption[] = plan.waypoints.map((wp, i) => {
    const nodeId = typeof wp.id === 'number' && wp.id > 0 ? wp.id : i + 1
    return {
      nodeId,
      nodeName: wp.name,
      lng: wp.lng,
      lat: wp.lat,
      placeType: 'waypoint',
    }
  })
  const pathNodes: PathNodeResult[] = nodeCatalog.map((n) => ({
    nodeId: n.nodeId,
    nodeName: n.nodeName,
  }))
  return { nodeCatalog, pathNodes }
}
