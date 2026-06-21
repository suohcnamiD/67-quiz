import { AxiosError } from 'axios'

export interface ApiError {
  code: string
  details?: Record<string, unknown>
}

export interface Failure {
  status?: string
  errors?: ApiError[]
}

const FALLBACK = 'Something went wrong. Look into console for details.'

const COPY: Record<string, string> = {
  // Domain — quizzes
  QUIZ_NOT_FOUND: 'That quiz could not be found.',
  NO_ACCESS_TO_QUIZ: 'You don’t have access to this quiz.',
  BLANK_OPTION_TEXT: 'Option text cannot be blank.',
  // Domain — questions
  QUESTION_NOT_FOUND: 'That question could not be found.',
  // Domain — attempts
  ATTEMPT_NOT_FOUND: 'That attempt could not be found.',
  NO_ACCESS_TO_ATTEMPT: 'You don’t have access to this attempt.',
  ATTEMPT_ALREADY_FINISHED: 'This attempt is already finished.',
  OPTION_NOT_FOUND: 'That option could not be found.',
  // Auth
  USERNAME_ALREADY_TAKEN: 'That username is already taken.',
  INVALID_USERNAME: 'That username doesn’t meet the requirements.',
  INVALID_PASSWORD: 'That password doesn’t meet the requirements.',
  UNAUTHORIZED: 'Wrong username or password.',
  FORBIDDEN: 'You don’t have permission to do that.',
  // Profile
  USER_NOT_FOUND: 'That user could not be found.',
  INVALID_DISPLAY_NAME: 'Display name must be 1–32 characters.',
  INVALID_BIO: 'Bio must be at most 280 characters.',
  INVALID_IMAGE: 'That image couldn’t be read. Use a PNG, JPEG, or WebP.',
  AVATAR_TOO_LARGE: 'That image is over 2 MB. Pick a smaller one.',
  AVATAR_NOT_FOUND: 'No avatar has been set.',
  // Generic / framework
  VALIDATION_ERROR: 'Some fields are missing or invalid.',
  INVALID_FORMAT: 'A field has the wrong format.',
  BAD_REQUEST: 'The request was rejected.',
  NOT_FOUND: 'That resource could not be found.',
  METHOD_NOT_ALLOWED: 'That action isn’t supported here.',
  UNSUPPORTED_MEDIA_TYPE: 'Unsupported content type.',
  INTERNAL_SERVER_ERROR: 'The server hit an unexpected error.',
}

export function firstError(err: unknown): ApiError | undefined {
  if (err instanceof AxiosError) {
    const data = err.response?.data as Failure | undefined
    return data?.errors?.[0]
  }
  return undefined
}

export function firstErrorCode(err: unknown): string | undefined {
  return firstError(err)?.code
}

/**
 * Resolve any error into a user-facing message.
 * Falls back to a generic message and logs the original error
 * to the console so it can still be inspected.
 */
export function errorMessage(err: unknown): string {
  const code = firstErrorCode(err)
  if (code && COPY[code]) return COPY[code]
  console.error(err)
  return FALLBACK
}
