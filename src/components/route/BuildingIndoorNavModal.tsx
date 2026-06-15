import { DetailOverlay } from '../ui/DetailOverlay'
import { BuildingIndoorNavPanel } from './BuildingIndoorNavPanel'

type BuildingIndoorNavModalProps = {
  open: boolean
  onClose: () => void
}

export function BuildingIndoorNavModal({ open, onClose }: BuildingIndoorNavModalProps) {
  return (
    <DetailOverlay
      open={open}
      title="朝阳大悦城 · 高德室内地图导航"
      onClose={onClose}
    >
      <BuildingIndoorNavPanel />
    </DetailOverlay>
  )
}
