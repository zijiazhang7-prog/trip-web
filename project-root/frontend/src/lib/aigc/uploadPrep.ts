import { compressImageForVideo } from './imagePrep'

/** 上传前压图并统一为 .jpg，避免无扩展名 / 过大文件导致后端拒绝 */
export async function prepareAigcUploadFile(file: File): Promise<File> {
  const dataUrl = await compressImageForVideo(file)
  const blob = await fetch(dataUrl).then((r) => r.blob())
  const base =
    file.name.replace(/\.[^.]+$/, '').replace(/[^\w\u4e00-\u9fa5-]+/g, '_').slice(0, 24) || 'aigc'
  return new File([blob], `${base}.jpg`, { type: 'image/jpeg' })
}
