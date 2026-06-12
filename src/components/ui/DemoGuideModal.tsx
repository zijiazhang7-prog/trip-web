type DemoGuideModalProps = {
  open: boolean
  onClose: () => void
}

const STEPS = [
  { title: '1. 登录', desc: '右上角登录后，可使用内部路线、评论和评分。' },
  { title: '2. 选偏好 + 看推荐', desc: '首页保存旅行偏好，推荐页切换景区/校园，点 Top10 或卡片。' },
  { title: '3. 规划路线', desc: '路径规划页勾选景点生成高德路线；点「景区内部路线」走后端道路图。' },
  { title: '4. 旅行中', desc: '导航页看转弯指引；设施默认「道路距离」，可切换高德周边。' },
  { title: '5. 美食 + 手账', desc: '美食页按当前景点筛选；日记页写手账发布到社群。' },
  { title: '推荐演示景区', desc: '奥林匹克森林公园（后端已验证设施可达）；北邮沙河校区（校园内部图）。' },
]

export function DemoGuideModal({ open, onClose }: DemoGuideModalProps) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-[320] flex items-center justify-center bg-black/40 p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div className="max-h-[88vh] w-full max-w-md overflow-y-auto rounded-[2rem] border border-white/80 bg-white p-6 shadow-2xl">
        <div className="mb-4 flex items-start justify-between gap-3">
          <div>
            <h2 className="font-display text-lg font-semibold text-[var(--ds-foreground)]">答辩演示指引</h2>
            <p className="mt-1 font-body text-xs text-[var(--ds-muted-foreground)]">约 5 分钟完整流程</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-[var(--ds-border)] px-3 py-1 text-xs"
          >
            关闭
          </button>
        </div>
        <ol className="space-y-3">
          {STEPS.map((s) => (
            <li
              key={s.title}
              className="rounded-xl border border-[var(--ds-primary)]/10 bg-[var(--ds-muted)]/30 px-4 py-3"
            >
              <p className="font-body text-sm font-semibold text-[var(--ds-foreground)]">{s.title}</p>
              <p className="mt-1 font-body text-xs leading-relaxed text-[var(--ds-muted-foreground)]">
                {s.desc}
              </p>
            </li>
          ))}
        </ol>
        <p className="mt-4 font-body text-[11px] leading-relaxed text-[var(--ds-muted-foreground)]">
          交通方式：城市导航用高德；景区内部四选项中，公共交通在后端暂按步行计算，答辩时请说明数据限制。
        </p>
      </div>
    </div>
  )
}
