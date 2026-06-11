import { AnimatePresence, motion } from 'framer-motion'
import { useState, type RefObject } from 'react'
import { login, register } from '../../api/auth'
import type { AuthMode } from './authArt'

const inputClass =
  'w-full rounded-2xl border border-[color-mix(in_srgb,var(--ds-border)_65%,transparent)] bg-[color-mix(in_srgb,white_58%,var(--ds-muted))] px-4 py-3 text-[var(--ds-foreground)] shadow-[inset_0_1px_2px_rgba(255,255,255,0.88)] outline-none ring-[var(--ds-primary)] transition placeholder:text-[color-mix(in_srgb,var(--ds-muted-foreground)_50%,transparent)] focus:border-[color-mix(in_srgb,var(--ds-primary)_40%,var(--ds-border))] focus:ring-2'

export type AuthFormProps = {
  mode: AuthMode
  onModeChange: (mode: AuthMode) => void
  titleId: string
  firstFieldRef: RefObject<HTMLInputElement | null>
  onAuthSuccess?: () => void
}

export function AuthForm({ mode, onModeChange, titleId, firstFieldRef, onAuthSuccess }: AuthFormProps) {
  const isLogin = mode === 'login'
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const onSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    if (!username.trim() || !password.trim()) {
      setError('请填写账号和密码')
      return
    }
    if (!isLogin) {
      if (!nickname.trim()) {
        setError('请填写昵称')
        return
      }
      if (password !== confirmPassword) {
        setError('两次密码不一致')
        return
      }
    }
    setLoading(true)
    try {
      if (isLogin) {
        await login({ username: username.trim(), password })
        setSuccess('登录成功')
        onAuthSuccess?.()
      } else {
        await register({ username: username.trim(), password, nickname: nickname.trim() })
        setSuccess('注册成功，请登录')
        setConfirmPassword('')
        setPassword('')
        onModeChange('login')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '请求失败，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative">
      <AnimatePresence mode="wait" initial={false}>
        <motion.div
          key={mode}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -6 }}
          transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
        >
          <p className="mb-0.5 text-xs font-semibold uppercase tracking-[0.2em] text-[color-mix(in_srgb,var(--ds-primary)_72%,var(--ds-muted-foreground))]">
            智游行
          </p>
          <h2
            id={titleId}
            className="font-display text-2xl font-semibold tracking-tight text-[var(--ds-foreground)] sm:text-[1.65rem]"
          >
            {isLogin ? '欢迎回来' : '开启新旅程'}
          </h2>
          <p className="mt-1.5 text-sm leading-relaxed text-[var(--ds-muted-foreground)]">
            {isLogin ? '登录后继续规划你的下一段旅程' : '用一分钟注册，收藏路线与旅友动态'}
          </p>

          <form
            className="mt-6 space-y-4"
            onSubmit={onSubmit}
          >
            {isLogin ? (
              <>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">邮箱或手机号</span>
                  <input
                    ref={firstFieldRef}
                    type="text"
                    autoComplete="username"
                    placeholder="hello@example.com"
                    className={inputClass}
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">密码</span>
                  <input
                    type="password"
                    autoComplete="current-password"
                    placeholder="••••••••"
                    className={inputClass}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </label>
                <div className="flex items-center justify-between gap-3 pt-0.5 text-xs">
                  <label className="flex cursor-pointer items-center gap-2 text-[var(--ds-muted-foreground)]">
                    <input
                      type="checkbox"
                      className="h-3.5 w-3.5 rounded border-[var(--ds-border)] text-[var(--ds-primary)]"
                      defaultChecked
                    />
                    记住我
                  </label>
                  <button
                    type="button"
                    className="cursor-target font-semibold text-[color-mix(in_srgb,#3d7d72_90%,var(--ds-primary))] transition hover:underline"
                  >
                    忘记密码？
                  </button>
                </div>
                <motion.button
                  type="submit"
                  disabled={loading}
                  className="cursor-target mt-2 w-full rounded-full py-3.5 text-sm font-bold text-white shadow-[0_10px_28px_-8px_rgba(61,125,114,0.5)] transition hover:brightness-[1.05] active:scale-[0.99]"
                  style={{
                    background:
                      'linear-gradient(135deg, color-mix(in srgb, #3d8b7e 92%, #5d7052) 0%, color-mix(in srgb, #5d7052 88%, #2d4a42) 100%)',
                  }}
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.99 }}
                >
                  {loading ? '登录中...' : '登录'}
                </motion.button>
              </>
            ) : (
              <>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">怎么称呼你</span>
                  <input
                    ref={firstFieldRef}
                    type="text"
                    autoComplete="nickname"
                    placeholder="例如：小游"
                    className={inputClass}
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">邮箱或手机号</span>
                  <input
                    type="text"
                    autoComplete="email"
                    placeholder="用于登录与找回"
                    className={inputClass}
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">密码</span>
                  <input
                    type="password"
                    autoComplete="new-password"
                    placeholder="至少 8 位"
                    className={inputClass}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="mb-1.5 block text-xs font-semibold text-[var(--ds-muted-foreground)]">确认密码</span>
                  <input
                    type="password"
                    autoComplete="new-password"
                    placeholder="再输入一次"
                    className={inputClass}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />
                </label>
                <label className="mt-1 flex cursor-pointer items-start gap-2.5 text-xs leading-snug text-[var(--ds-muted-foreground)]">
                  <input type="checkbox" required className="mt-0.5 h-3.5 w-3.5 shrink-0 rounded border-[var(--ds-border)] text-[var(--ds-primary)]" />
                  <span>
                    我已阅读并同意
                    <button type="button" className="cursor-target mx-0.5 font-semibold text-[var(--ds-primary)] underline-offset-2 hover:underline">
                      服务条款
                    </button>
                    与
                    <button type="button" className="cursor-target mx-0.5 font-semibold text-[var(--ds-primary)] underline-offset-2 hover:underline">
                      隐私说明
                    </button>
                  </span>
                </label>
                <motion.button
                  type="submit"
                  disabled={loading}
                  className="cursor-target mt-2 w-full rounded-full py-3.5 text-sm font-bold text-white shadow-[0_12px_32px_-8px_rgba(165,95,58,0.45)] transition hover:brightness-[1.04] active:scale-[0.99]"
                  style={{
                    background:
                      'linear-gradient(135deg, color-mix(in srgb, #c1784f 95%, #b85c38) 0%, color-mix(in srgb, #a85d48 88%, #7a4a38) 100%)',
                  }}
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.99 }}
                >
                  {loading ? '提交中...' : '注册并加入智游行'}
                </motion.button>
              </>
            )}
            {error ? <p className="text-xs font-medium text-[#a83d3d]">{error}</p> : null}
            {success ? <p className="text-xs font-medium text-[#2c7a5d]">{success}</p> : null}
          </form>

          <p className="mt-5 text-center text-xs text-[var(--ds-muted-foreground)]">
            {isLogin ? (
              <>
                还没有账号？
                <button
                  type="button"
                  onClick={() => onModeChange('register')}
                  className="cursor-target ml-1 font-bold text-[var(--ds-primary)] underline-offset-2 hover:underline"
                >
                  注册智游行
                </button>
              </>
            ) : (
              <>
                已有账号？
                <button
                  type="button"
                  onClick={() => onModeChange('login')}
                  className="cursor-target ml-1 font-bold text-[var(--ds-primary)] underline-offset-2 hover:underline"
                >
                  去登录
                </button>
              </>
            )}
          </p>
        </motion.div>
      </AnimatePresence>
    </div>
  )
}
