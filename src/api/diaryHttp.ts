import type { DiaryApi } from './diary'
import {
  createDiaryBookMock,
  deleteDiaryBookMock,
  getDiaryBookDetailMock,
  listDiaryBooksMock,
  updateDiaryBookMock,
  upsertDiaryEntryMock,
  uploadAssetMock,
} from '../features/diary/mock'

/**
 * 书架编辑仍用本地 Mock；发布/社群走 community diaries API（见 useDiaryPublish）。
 * HTTP 版预留接口，待后端提供 diaries/me 后切换。
 */
export function createDiaryHttpApi(): DiaryApi {
  return {
    listBooks: listDiaryBooksMock,
    createBook: createDiaryBookMock,
    deleteBook: deleteDiaryBookMock,
    getBookDetail: getDiaryBookDetailMock,
    updateBook: updateDiaryBookMock,
    upsertEntry: upsertDiaryEntryMock,
    uploadAsset: uploadAssetMock,
  }
}
