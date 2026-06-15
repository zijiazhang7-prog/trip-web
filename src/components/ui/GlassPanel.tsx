import type { ReactNode } from 'react'

type GlassPanelProps = {
  children: ReactNode
  className?: string
  as?: 'div' | 'section' | 'article' | 'aside'
}

export function GlassPanel({ children, className = '', as: Tag = 'div' }: GlassPanelProps) {
  return (
    <Tag
      className={`ds-glass-panel rounded-[28px] shadow-[var(--ds-shadow-soft)] ${className}`}
    >
      {children}
    </Tag>
  )
}
