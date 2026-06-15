import type { ButtonHTMLAttributes, ReactNode } from 'react'

type PrimaryButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  children: ReactNode
  variant?: 'primary' | 'secondary' | 'ghost'
  fullWidth?: boolean
}

export function PrimaryButton({
  children,
  variant = 'primary',
  fullWidth = false,
  className = '',
  ...rest
}: PrimaryButtonProps) {
  const base =
    'cursor-target rounded-full font-body text-sm font-semibold transition duration-300 disabled:cursor-not-allowed disabled:opacity-50'
  const variants = {
    primary:
      'bg-[linear-gradient(135deg,var(--ds-primary)_0%,var(--ds-forest)_100%)] px-8 py-3.5 text-[var(--ds-primary-foreground)] shadow-[var(--ds-shadow-soft)] hover:scale-[1.02] hover:brightness-110 active:scale-[0.98]',
    secondary:
      'border border-[color-mix(in_srgb,var(--ds-primary)_28%,transparent)] bg-[color-mix(in_srgb,var(--ds-surface)_85%,var(--ds-cream))] px-8 py-3.5 text-[var(--ds-foreground)] hover:bg-[color-mix(in_srgb,var(--ds-sage)_35%,var(--ds-surface))]',
    ghost:
      'px-4 py-2 text-[var(--ds-muted-foreground)] hover:bg-[color-mix(in_srgb,var(--ds-primary)_6%,transparent)] hover:text-[var(--ds-primary)]',
  }
  return (
    <button
      type="button"
      className={`${base} ${variants[variant]} ${fullWidth ? 'w-full' : ''} ${className}`}
      {...rest}
    >
      {children}
    </button>
  )
}
