import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { getAmapSecurityCode } from './lib/amap/config'
import './index.css'
import App from './App.tsx'

const amapSec = getAmapSecurityCode()
if (amapSec && typeof window !== 'undefined') {
  window._AMapSecurityConfig = { securityJsCode: amapSec }
}

// #region agent log
fetch('http://127.0.0.1:7366/ingest/833cbd2c-bedc-4c5a-a10e-f459fd4b77d5', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-Debug-Session-Id': '50cf78' },
  body: JSON.stringify({
    sessionId: '50cf78',
    runId: 'pre-fix',
    hypothesisId: 'H4',
    location: 'main.tsx:before-render',
    message: 'main entry reached',
    data: {},
    timestamp: Date.now(),
  }),
}).catch(() => {})
// #endregion

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
