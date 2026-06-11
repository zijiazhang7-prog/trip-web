import { AnimatePresence, motion } from 'framer-motion'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  fetchMyPreferences,
  saveMyPreferences,
  tagsToThemeList,
  themeListToTags,
} from '../../api/preferences'
import { hasStoredToken } from '../../api/http'
import { InlineNotice } from '../ui/InlineNotice'
import { PrimaryButton } from '../ui/PrimaryButton'

const TRAVEL_PREFERENCE_TAGS = [
  '亲子',
  '古镇',
  '海滨',
  '美食',
  '文化',
  '冒险',
  '放松',
] as const

const TRAVELERS = [
  { id: 'me', label: '我' },
  { id: 'companion-a', label: '同伴 A' },
  { id: 'companion-b', label: '同伴 B' },
] as const

const MAX_TRAVELERS = 3

export type PreferenceSavedPayload = {
  themes: string[]
  customText: string
  tags: string[]
}

type TravelPreferencesProps = {
  className?: string
  onSaved?: (payload: PreferenceSavedPayload) => void
  /** 从首页 CTA 跳入时自动展开偏好面板 */
  openPanel?: boolean
}

function toggleInList(list: string[], tag: string): string[] {
  return list.includes(tag) ? list.filter((t) => t !== tag) : [...list, tag]
}

const panelEase = [0.33, 1, 0.68, 1] as const

const listVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.04, delayChildren: 0.06 },
  },
}

const chipVariants = {
  hidden: { opacity: 0, y: 8, scale: 0.96 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { type: 'spring' as const, stiffness: 420, damping: 28 },
  },
}

export function TravelPreferences({ className = '', onSaved, openPanel = false }: TravelPreferencesProps) {
  const [panelOpen, setPanelOpen] = useState(openPanel)
  const [nested, setNested] = useState<null | 'single' | 'multi'>(null)
  const [singleSelected, setSingleSelected] = useState<string[]>([])
  const [multiSelected, setMultiSelected] = useState<[string[], string[], string[]]>([[], [], []])
  const [saveMsg, setSaveMsg] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [loadingPrefs, setLoadingPrefs] = useState(false)
  const [customAiText, setCustomAiText] = useState('')

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      await Promise.resolve()
      if (cancelled || !hasStoredToken()) return
      setLoadingPrefs(true)
      setSaveError(null)
      try {
        const prefs = await fetchMyPreferences()
        if (cancelled) return
        const tags = themeListToTags(prefs.preferThemeList)
        if (tags.length) {
          setNested('single')
          setSingleSelected(tags)
        }
        if (prefs.customPreferenceText?.trim()) {
          const firstLine = prefs.customPreferenceText.split('\n')[0]?.trim() ?? ''
          setCustomAiText(firstLine)
        }
      } catch (err) {
        if (!cancelled) setSaveError(err instanceof Error ? err.message : '偏好加载失败')
      } finally {
        if (!cancelled) setLoadingPrefs(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [])

  const snapshotValid = useMemo(() => {
    if (nested === 'single') return singleSelected.length > 0
    if (nested === 'multi') return multiSelected.every((row) => row.length > 0)
    return false
  }, [nested, singleSelected, multiSelected])

  const savePreferences = useCallback(async () => {
    if (!hasStoredToken()) {
      setSaveError('请先登录后再保存偏好')
      return
    }
    if (!nested) {
      setSaveError('请选择单人或多人出行模式')
      return
    }
    if (!snapshotValid) {
      setSaveError('请至少为每位旅伴选择一项偏好')
      return
    }
    setSaving(true)
    setSaveError(null)
    setSaveMsg(null)
    try {
      const tags = nested === 'single' ? singleSelected : [...new Set(multiSelected.flat())]
      const themeList = tagsToThemeList(tags)
      const multiSummary =
        nested === 'multi'
          ? TRAVELERS.map((p, i) => `${p.label}: ${multiSelected[i].join('、')}`).join('；')
          : ''
      const customText = [customAiText.trim(), multiSummary].filter(Boolean).join('\n')
      await saveMyPreferences({
        preferThemeList: themeList,
        travelStyle: nested === 'multi' ? '多人' : '单人',
        customPreferenceText: customText || undefined,
        preferHotLevel: 3,
        preferCrowdLevel: 2,
      })
      setSaveMsg('偏好已同步到云端')
      onSaved?.({ themes: themeList, customText: customAiText.trim(), tags })
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }, [nested, singleSelected, multiSelected, snapshotValid, onSaved, customAiText])

  const aiPreferenceField = (
    <div className="mt-4 rounded-2xl border border-dashed border-[color-mix(in_srgb,var(--ds-primary)_18%,transparent)] bg-[color-mix(in_srgb,var(--ds-background)_75%,white)] p-4">
      <p className="mb-2 font-body text-xs font-medium text-[var(--ds-muted-foreground)]">
        用一句话描述你想怎么玩（DeepSeek 智能匹配推荐）
      </p>
      <textarea
        value={customAiText}
        onChange={(e) => setCustomAiText(e.target.value)}
        rows={3}
        placeholder="例如：想带孩子去有文化底蕴又不太累的地方，周末两天，喜欢美食和古镇…"
        className="w-full resize-none rounded-xl border border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-white/90 px-3 py-2.5 font-body text-sm leading-relaxed text-[var(--ds-foreground)] outline-none focus:border-[var(--ds-primary)] focus:ring-1 focus:ring-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)]"
      />
    </div>
  )

  const toggleSingleTag = (tag: string) => setSingleSelected((prev) => toggleInList(prev, tag))

  const toggleMultiTag = (personIndex: number, tag: string) => {
    setMultiSelected((prev) => {
      const next: [string[], string[], string[]] = [[...prev[0]], [...prev[1]], [...prev[2]]]
      next[personIndex] = toggleInList(next[personIndex], tag)
      return next
    })
  }

  const selectMode = (mode: 'single' | 'multi') => setNested((prev) => (prev === mode ? null : mode))

  return (
    <div className={`relative z-[2] flex flex-col ${className}`}>
      <motion.button
        type="button"
        layout
        onClick={() => setPanelOpen((o) => !o)}
        aria-expanded={panelOpen}
        aria-controls="travel-preferences-panel"
        className="cursor-target inline-flex h-12 shrink-0 items-center justify-center gap-2 rounded-full border border-[color-mix(in_srgb,var(--ds-primary)_20%,transparent)] bg-[color-mix(in_srgb,var(--ds-background)_92%,white)] px-5 text-sm font-semibold text-[var(--ds-primary)] shadow-[var(--ds-shadow-soft)] transition hover:border-[color-mix(in_srgb,var(--ds-primary)_35%,transparent)] hover:bg-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--ds-primary)]"
        whileTap={{ scale: 0.98 }}
      >
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)] text-xs" aria-hidden>
          ◎
        </span>
        行程偏好
        <motion.span
          animate={{ rotate: panelOpen ? 180 : 0 }}
          transition={{ duration: 0.28, ease: panelEase }}
          className="text-[10px] leading-none text-[var(--ds-muted-foreground)]"
          aria-hidden
        >
          ▼
        </motion.span>
      </motion.button>

      <AnimatePresence initial={false}>
        {panelOpen ? (
          <motion.div
            id="travel-preferences-panel"
            key="panel"
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.38, ease: panelEase }}
            className="overflow-hidden"
          >
            <div className="mt-3 rounded-2xl border border-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)] bg-[color-mix(in_srgb,white_85%,var(--ds-background))] p-3 shadow-[var(--ds-shadow-soft)] backdrop-blur-md sm:p-4">
              {loadingPrefs ? (
                <p className="mb-3 font-body text-xs text-[var(--ds-muted-foreground)]">正在加载已保存偏好…</p>
              ) : null}
              {saveError ? <InlineNotice variant="error">{saveError}</InlineNotice> : null}
              {saveMsg ? <InlineNotice variant="success">{saveMsg}</InlineNotice> : null}

              <motion.div layout className="grid gap-3 sm:grid-cols-2">
                <motion.button
                  layout
                  type="button"
                  onClick={() => selectMode('single')}
                  className={`cursor-target flex flex-col rounded-2xl border p-4 text-left transition ${
                    nested === 'single'
                      ? 'border-[color-mix(in_srgb,var(--ds-primary)_40%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_8%,var(--ds-background))] ring-2 ring-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)]'
                      : 'border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-[color-mix(in_srgb,var(--ds-background)_90%,white)] hover:border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)]'
                  }`}
                >
                  <span className="font-display text-lg font-semibold text-[var(--ds-primary)]">单人出行</span>
                  <span className="mt-1 font-body text-xs text-[var(--ds-muted-foreground)]">为自己勾选旅行气质</span>
                </motion.button>
                <motion.button
                  layout
                  type="button"
                  onClick={() => selectMode('multi')}
                  className={`cursor-target flex flex-col rounded-2xl border p-4 text-left transition ${
                    nested === 'multi'
                      ? 'border-[color-mix(in_srgb,var(--ds-primary)_40%,transparent)] bg-[color-mix(in_srgb,var(--ds-primary)_8%,var(--ds-background))] ring-2 ring-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)]'
                      : 'border-[color-mix(in_srgb,var(--ds-border)_55%,transparent)] bg-[color-mix(in_srgb,var(--ds-background)_90%,white)] hover:border-[color-mix(in_srgb,var(--ds-primary)_25%,transparent)]'
                  }`}
                >
                  <span className="font-display text-lg font-semibold text-[var(--ds-primary)]">多人出行</span>
                  <span className="mt-1 font-body text-xs text-[var(--ds-muted-foreground)]">
                    最多 {MAX_TRAVELERS} 人，分别记录偏好
                  </span>
                </motion.button>
              </motion.div>

              <AnimatePresence mode="sync">
                {nested === 'single' ? (
                  <motion.div
                    key="single"
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden"
                  >
                    <div className="mt-4 rounded-2xl border border-dashed border-[color-mix(in_srgb,var(--ds-primary)_15%,transparent)] bg-[color-mix(in_srgb,var(--ds-background)_70%,white)] p-4">
                      <p className="mb-3 font-body text-xs font-medium uppercase tracking-wide text-[var(--ds-muted-foreground)]">
                        旅游偏好（多选）
                      </p>
                      <motion.div className="flex flex-wrap gap-2" variants={listVariants} initial="hidden" animate="show">
                        {TRAVEL_PREFERENCE_TAGS.map((tag) => {
                          const on = singleSelected.includes(tag)
                          return (
                            <motion.button
                              key={tag}
                              type="button"
                              variants={chipVariants}
                              onClick={() => toggleSingleTag(tag)}
                              className={`cursor-target rounded-full border px-3.5 py-1.5 font-body text-sm font-semibold transition ${
                                on
                                  ? 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-[var(--ds-primary-foreground)]'
                                  : 'border-[color-mix(in_srgb,var(--ds-primary)_15%,transparent)] bg-white/90 text-[var(--ds-primary)]'
                              }`}
                            >
                              {tag}
                            </motion.button>
                          )
                        })}
                      </motion.div>
                      {aiPreferenceField}
                    </div>
                  </motion.div>
                ) : null}

                {nested === 'multi' ? (
                  <motion.div
                    key="multi"
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    className="overflow-hidden"
                  >
                    <div className="mt-4 grid gap-4 md:grid-cols-3">
                      {TRAVELERS.map((person, idx) => (
                        <div
                          key={person.id}
                          className="rounded-2xl border border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] bg-white/85 p-3 shadow-sm"
                        >
                          <div className="mb-2 font-display text-sm font-semibold text-[var(--ds-primary)]">
                            {person.label}
                          </div>
                          <div className="flex flex-wrap gap-1.5">
                            {TRAVEL_PREFERENCE_TAGS.map((tag) => {
                              const on = multiSelected[idx].includes(tag)
                              return (
                                <button
                                  key={`${person.id}-${tag}`}
                                  type="button"
                                  onClick={() => toggleMultiTag(idx, tag)}
                                  className={`cursor-target rounded-full border px-2.5 py-1 font-body text-xs font-semibold ${
                                    on
                                      ? 'border-[var(--ds-primary)] bg-[var(--ds-primary)] text-white'
                                      : 'border-[color-mix(in_srgb,var(--ds-primary)_12%,transparent)] text-[var(--ds-primary)]'
                                  }`}
                                >
                                  {tag}
                                </button>
                              )
                            })}
                          </div>
                        </div>
                      ))}
                    </div>
                    {aiPreferenceField}
                  </motion.div>
                ) : null}
              </AnimatePresence>

              <div className="mt-4 flex justify-end border-t border-[color-mix(in_srgb,var(--ds-border)_50%,transparent)] pt-3">
                <PrimaryButton onClick={() => void savePreferences()} disabled={saving}>
                  {saving ? '保存中…' : '保存偏好'}
                </PrimaryButton>
              </div>
            </div>
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  )
}
