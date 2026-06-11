export {}

declare global {
  interface Window {
    AMap: {
      Map: new (
        container: string | HTMLElement,
        opts: Record<string, unknown>,
      ) => {
        destroy: () => void
        add: (overlays: unknown | unknown[]) => void
        remove: (overlays: unknown | unknown[]) => void
        setFitView: (overlays?: unknown[], immediately?: boolean, avoid?: number[]) => void
        setCenter: (center: [number, number]) => void
      }
      Marker: new (opts: Record<string, unknown>) => {
        on: (type: string, cb: () => void) => void
      }
      Polyline: new (opts: Record<string, unknown>) => unknown
      Icon: new (opts: Record<string, unknown>) => unknown
      Size: new (w: number, h: number) => unknown
      Pixel: new (x: number, y: number) => unknown
      LngLat: new (lng: number, lat: number) => unknown
      plugin: (names: string | string[], cb: () => void) => void
      event?: { addListener: (target: unknown, type: string, cb: () => void) => void }
      Driving: new (opts: Record<string, unknown>) => {
        search: (
          start: unknown,
          end: unknown,
          callback: (status: string, result?: unknown) => void,
        ) => void
      }
      Walking: new (opts: Record<string, unknown>) => {
        search: (
          start: unknown,
          end: unknown,
          callback: (status: string, result?: unknown) => void,
        ) => void
      }
      Riding: new (opts: Record<string, unknown>) => {
        search: (
          start: unknown,
          end: unknown,
          callback: (status: string, result?: unknown) => void,
        ) => void
      }
      Transfer: new (opts: Record<string, unknown>) => {
        search: (
          start: unknown,
          end: unknown,
          callback: (status: string, result?: unknown) => void,
        ) => void
      }
      TransferPolicy: { LEAST_TIME: number }
      Geolocation: new (opts: Record<string, unknown>) => {
        getCurrentPosition: (
          onSuccess: (pos: { position: { lng: number; lat: number } }) => void,
          onError: (err: { message: string }) => void,
        ) => void
      }
    }
    _AMapSecurityConfig?: { securityJsCode: string }
  }
}
