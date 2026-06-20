type StarRatingInputProps = {
  value: number | null
  average?: number | null
  count?: number
  disabled?: boolean
  onChange?: (score: number) => void
  className?: string
}

function Stars({
  score,
  interactive,
  onPick,
  disabled,
}: {
  score: number
  interactive?: boolean
  onPick?: (n: number) => void
  disabled?: boolean
}) {
  const display = Math.round(score)
  return (
    <div className="flex items-center gap-0.5">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          disabled={!interactive || disabled}
          onClick={() => onPick?.(star)}
          className={`text-lg leading-none transition ${
            star <= display ? 'text-[#D4B896]' : 'text-[#d0ddd6]'
          } ${interactive && !disabled ? 'cursor-pointer hover:scale-110' : 'cursor-default'}`}
          aria-label={`${star} 星`}
        >
          ★
        </button>
      ))}
    </div>
  )
}

export function StarRatingInput({
  value,
  average,
  count,
  disabled,
  onChange,
  className = '',
}: StarRatingInputProps) {
  return (
    <div className={`flex flex-col gap-2 ${className}`}>
      {average != null ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-body text-xs text-[var(--ds-muted-foreground)]">均分</span>
          <Stars score={average} />
          <span className="font-body text-xs text-[var(--ds-muted-foreground)]">
            {average.toFixed(1)}
            {typeof count === 'number' && count > 0 ? ` · ${count} 人评` : ''}
          </span>
        </div>
      ) : null}
      {onChange ? (
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-body text-xs text-[var(--ds-primary)]">我的评分</span>
          <Stars
            score={value ?? 0}
            interactive
            disabled={disabled}
            onPick={onChange}
          />
          {value != null ? (
            <span className="font-body text-xs text-[var(--ds-muted-foreground)]">{value} 星</span>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
