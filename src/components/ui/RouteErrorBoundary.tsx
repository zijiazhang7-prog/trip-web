import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Props = { children: ReactNode }
type State = { error: Error | null }

export class RouteErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[NavigatePage]', error, info.componentStack)
  }

  render() {
    if (this.state.error) {
      return (
        <div className="mx-auto max-w-lg px-6 py-16 text-center">
          <h2 className="font-display text-xl font-semibold text-[var(--ds-foreground)]">旅行导航页加载异常</h2>
          <p className="font-body mt-3 text-sm text-[var(--ds-destructive)]">{this.state.error.message}</p>
          <Link
            to="/route"
            className="font-body mt-6 inline-block rounded-full bg-[var(--ds-primary)] px-6 py-2.5 text-sm font-semibold text-white"
          >
            返回路径规划
          </Link>
        </div>
      )
    }
    return this.props.children
  }
}
