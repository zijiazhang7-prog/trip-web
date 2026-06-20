export type AuthMode = 'login' | 'register'

export type AuthArt = {
  src: string
  objectPosition: string
  /** Soft layered washes — keep pigment visible, aid legibility without looking “masked” */
  overlay: string
}

/**
 * 图一：园林水彩 → 登录
 * 图二：山水渔舟（偏竖幅）→ 注册
 * 渐变尽量轻、透气，偏米白雾面，避免压暗画意
 */
export function getAuthArt(mode: AuthMode): AuthArt {
  return mode === 'login'
    ? {
        src: '/images/auth-bg-login.png',
        objectPosition: 'center 42%',
        overlay:
          'linear-gradient(152deg, color-mix(in srgb, #fdfcf8 58%, transparent) 0%, color-mix(in srgb, #fdfcf8 12%, transparent) 40%, transparent 62%), linear-gradient(to top, color-mix(in srgb, #fdfcf8 82%, transparent) 0%, color-mix(in srgb, #faf8f3 18%, transparent) 48%, transparent 76%), radial-gradient(ellipse 100% 75% at 50% 108%, color-mix(in srgb, #5a6b52 14%, transparent) 0%, transparent 55%)',
      }
    : {
        src: '/images/auth-bg-register.png',
        objectPosition: 'center 28%',
        overlay:
          'linear-gradient(178deg, color-mix(in srgb, #fbfcfa 55%, transparent) 0%, color-mix(in srgb, #f6f7f4 8%, transparent) 36%, transparent 55%), linear-gradient(to top, color-mix(in srgb, #f7f6f2 86%, transparent) 0%, color-mix(in srgb, #eceee9 12%, transparent) 42%, transparent 74%), radial-gradient(ellipse 95% 72% at 50% 100%, color-mix(in srgb, #6a7a72 12%, transparent) 0%, transparent 52%)',
      }
}
