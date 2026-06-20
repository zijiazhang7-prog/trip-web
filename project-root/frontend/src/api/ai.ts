import type {
  CreateRouteSketchTaskRequest,
  GetRouteSketchTaskResponse,
} from '../features/diary/contracts'
import type { RouteSketchTask } from '../features/diary/types'
import { createRouteSketchTaskMock, getRouteSketchTaskMock } from '../features/diary/mock'

export type AiApi = {
  createRouteSketchTask: (input: CreateRouteSketchTaskRequest) => Promise<RouteSketchTask>
  getRouteSketchTask: (taskId: string) => Promise<GetRouteSketchTaskResponse>
}

/**
 * Phase 0: mock task pipeline.
 * Phase 1/2: replace with real async task endpoint.
 */
export function getAiApi(): AiApi {
  return {
    createRouteSketchTask: createRouteSketchTaskMock,
    getRouteSketchTask: getRouteSketchTaskMock,
  }
}
