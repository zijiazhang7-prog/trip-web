import { DetailOverlay } from '../ui/DetailOverlay'
import { WaypointInternalNavPanel } from './WaypointInternalNavPanel'
import type { RouteWaypoint } from '../../types/macroRoute'

type InternalNavModalProps = {
  open: boolean
  waypoint: RouteWaypoint | null
  onClose: () => void
}

export function InternalNavModal({ open, waypoint, onClose }: InternalNavModalProps) {
  if (!waypoint) return null

  return (
    <DetailOverlay
      open={open}
      title={`${waypoint.name} · 内部导航`}
      onClose={onClose}
    >
      <p className="mb-4 font-body text-sm text-[var(--ds-muted-foreground)]">
        景区内部道路图与节点路线，与城市导航独立，仅在此窗口内预览。
      </p>
      <WaypointInternalNavPanel waypoint={waypoint} modalOpen={open} />
    </DetailOverlay>
  )
}
