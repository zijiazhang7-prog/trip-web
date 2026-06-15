import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { RoutePlanProvider } from './context/RoutePlanProvider'
import { TripProvider } from './context/TripProvider'
import { HomePage } from './pages/HomePage'

const RoutePlanningPage = lazy(() =>
  import('./pages/RoutePlanningPage').then((m) => ({ default: m.RoutePlanningPage })),
)
const NavigatePage = lazy(() =>
  import('./pages/NavigatePage').then((m) => ({ default: m.NavigatePage })),
)
const FoodPage = lazy(() =>
  import('./pages/FoodPage').then((m) => ({ default: m.FoodPage })),
)
const CommunityPage = lazy(() =>
  import('./pages/CommunityPage').then((m) => ({ default: m.CommunityPage })),
)
const DiaryPage = lazy(() =>
  import('./pages/DiaryPage').then((m) => ({ default: m.DiaryPage })),
)
const AigcPage = lazy(() =>
  import('./pages/AigcPage').then((m) => ({ default: m.AigcPage })),
)

function PageFallback() {
  return (
    <div className="mx-auto max-w-[1400px] animate-pulse px-6 py-10">
      <div className="mb-6 h-8 w-48 rounded-lg bg-[var(--ds-muted)]" />
      <div className="grid gap-4 md:grid-cols-[260px_1fr]">
        <div className="h-64 rounded-2xl bg-[var(--ds-muted)]" />
        <div className="space-y-4">
          <div className="h-48 rounded-2xl bg-[var(--ds-muted)]" />
          <div className="h-48 rounded-2xl bg-[var(--ds-muted)]" />
        </div>
      </div>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <TripProvider>
        <RoutePlanProvider>
          <Routes>
            <Route element={<AppShell />}>
              <Route path="/" element={<HomePage />} />
              <Route
                path="/route"
                element={
                  <Suspense fallback={<PageFallback />}>
                    <RoutePlanningPage />
                  </Suspense>
                }
              />
              <Route
                path="/navigate"
                element={
                  <Suspense fallback={<PageFallback />}>
                    <NavigatePage />
                  </Suspense>
                }
              />
              <Route
                path="/food"
                element={
                  <Suspense fallback={<PageFallback />}>
                    <FoodPage />
                  </Suspense>
                }
              />
              <Route
                path="/community"
                element={
                  <Suspense fallback={<PageFallback />}>
                    <CommunityPage />
                  </Suspense>
                }
              />
              <Route
                path="/diary"
                element={
                  <Suspense
                    fallback={
                      <div className="mx-auto max-w-[1400px] animate-pulse px-6 py-10">
                        <div className="mb-6 h-8 w-48 rounded-lg bg-[var(--ds-muted)]" />
                        <div className="grid gap-4 md:grid-cols-[280px_1fr]">
                          <div className="h-64 rounded-2xl bg-[var(--ds-muted)]" />
                          <div className="h-96 rounded-2xl bg-[var(--ds-muted)]" />
                        </div>
                      </div>
                    }
                  >
                    <DiaryPage />
                  </Suspense>
                }
              />
              <Route
                path="/aigc"
                element={
                  <Suspense fallback={<PageFallback />}>
                    <AigcPage />
                  </Suspense>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </RoutePlanProvider>
      </TripProvider>
    </BrowserRouter>
  )
}

export default App
