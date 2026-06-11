import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { RoutePlanProvider } from './context/RoutePlanProvider'
import { TripProvider } from './context/TripProvider'
import { CommunityPage } from './pages/CommunityPage'
import { FoodPage } from './pages/FoodPage'
import { HomePage } from './pages/HomePage'
import { NavigatePage } from './pages/NavigatePage'
import { RoutePlanningPage } from './pages/RoutePlanningPage'

const DiaryPage = lazy(async () => {
  const mod = await import('./pages/DiaryPage')
  return { default: mod.DiaryPage }
})

function App() {
  return (
    <BrowserRouter>
      <TripProvider>
      <RoutePlanProvider>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/route" element={<RoutePlanningPage />} />
          <Route path="/navigate" element={<NavigatePage />} />
          <Route path="/food" element={<FoodPage />} />
          <Route path="/community" element={<CommunityPage />} />
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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
      </RoutePlanProvider>
      </TripProvider>
    </BrowserRouter>
  )
}

export default App
