type StarRatingInputProps = {
  value: number | null
  average?: number | null
  count?: number
  disabled?: boolean
  onChange?: (score: number) => void
  className?: string
}

export function StarRatingInput({
  value,
  average,
  count,
  disabled,
  onChange,
  className = '',
}: StarRatingInputProps) {
  const display = value ?? Math.round(average ?? 0)

  return (
    <div className={`flex flex-wrap items-center gap-2 ${className}`}>
      <div className="flex items-center gap-0.5" role="group" aria-label="评分">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            disabled={disabled || !onChange}
            onClick={() => onChange?.(star)}
            className={`text-lg leading-none transition ${
              star <= display ? 'text-[#D4B896]' : 'text-[#d0ddd6]'
            } ${disabled || !onChange ? 'cursor-default' : 'cursor-pointer hover:scale-110'}`}
            aria-label={`${star} 星`}
          >
            ★
          </button>
        ))}
      </div>
      {average != null ? (
        <span className="font-body text-xs text-[var(--ds-muted-foreground)]">
          均分 {average.toFixed(1)}
          {typeof count === 'number' && count > 0 ? ` · ${count} 人评` : ''}
        </span>
      ) : null}
      {value != null && onChange ? (
        <span className="font-body text-xs text-[var(--ds-primary)]">你的评分 {value} 星</span>
      ) : null}
    </div>
  )
}
