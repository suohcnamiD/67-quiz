/**
 * Build endpoints for cover / question / option image fetches. The FE
 * appends ?v=... so a fresh upload bypasses the browser cache. Callers
 * pass any increasing version (timestamp, mutation counter, etc).
 */
export function coverUrl(quizId: string, version?: number | string): string {
  const v = version != null ? `?v=${version}` : ''
  return `/api/quiz/${quizId}/cover${v}`
}

export function questionImageUrl(questionId: string, version?: number | string): string {
  const v = version != null ? `?v=${version}` : ''
  return `/api/question/${questionId}/image${v}`
}

export function optionImageUrl(optionId: string, version?: number | string): string {
  const v = version != null ? `?v=${version}` : ''
  return `/api/option/${optionId}/image${v}`
}
