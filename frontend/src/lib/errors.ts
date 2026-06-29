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

type Details = Record<string, unknown> | undefined
type Resolver = (details: Details) => string

function str(d: Details, key: string): string | undefined {
  const v = d?.[key]
  return typeof v === 'string' ? v : undefined
}

function num(d: Details, key: string): number | undefined {
  const v = d?.[key]
  return typeof v === 'number' ? v : undefined
}

const INVALID_USERNAME: Resolver = (d) => {
  const v = str(d, 'violation')
  const min = num(d, 'minimumLength')
  const max = num(d, 'maximumLength')
  if (v === 'TOO_SHORT' && min != null) return `Username must be at least ${min} characters.`
  if (v === 'TOO_LONG' && max != null) return `Username must be at most ${max} characters.`
  return 'That username doesn’t meet the requirements.'
}

const INVALID_PASSWORD: Resolver = (d) => {
  const v = str(d, 'violation')
  const min = num(d, 'minimumLength')
  if (v === 'TOO_SHORT' && min != null) return `Password must be at least ${min} characters.`
  return 'That password doesn’t meet the requirements.'
}

const BLANK_OPTION_TEXT: Resolver = (d) => {
  const i = num(d, 'index')
  if (i != null) return `Option ${i + 1} text cannot be blank.`
  return 'Option text cannot be blank.'
}

const INVALID_QUESTION_SHAPE: Resolver = (d) => {
  const reason = str(d, 'reason')
  if (reason) return `Invalid question: ${reason}`
  return 'Single-choice questions need exactly one correct option, and every question needs at least two options.'
}

const AVATAR_TOO_LARGE: Resolver = (d) => {
  const max = num(d, 'maxBytes')
  if (max != null) {
    const mb = max / (1024 * 1024)
    const pretty = Number.isInteger(mb) ? `${mb} MB` : `${mb.toFixed(1)} MB`
    return `That image is over ${pretty}. Pick a smaller one.`
  }
  return 'That image is too large. Pick a smaller one.'
}

const INVALID_DISPLAY_NAME: Resolver = (d) =>
  str(d, 'message') ?? 'Display name must be 1–32 characters.'

const INVALID_IMAGE: Resolver = (d) =>
  str(d, 'message') ?? 'That image couldn’t be read. Use a PNG, JPEG, or WebP.'

const INVALID_RATING: Resolver = (d) => {
  const min = num(d, 'minimum')
  const max = num(d, 'maximum')
  if (min != null && max != null) return `Rating must be between ${min} and ${max}.`
  return 'That rating is out of range.'
}

const RATING_NOT_ELIGIBLE: Resolver = () =>
  'Finish the quiz before rating it.'

const QUIZ_NOT_FOUND: Resolver = () => 'That quiz could not be found.'
const QUESTION_NOT_FOUND: Resolver = () => 'That question could not be found.'
const ATTEMPT_NOT_FOUND: Resolver = () => 'That attempt could not be found.'
const OPTION_NOT_FOUND: Resolver = () => 'That option could not be found.'
const USER_NOT_FOUND: Resolver = (d) => {
  const u = str(d, 'username')
  return u ? `User "${u}" could not be found.` : 'That user could not be found.'
}

const VALIDATION_ERROR: Resolver = (d) => {
  const field = str(d, 'field')
  if (field) return `“${field}” is missing or invalid.`
  return 'Some fields are missing or invalid.'
}

const INVALID_FORMAT: Resolver = (d) => {
  const field = str(d, 'field')
  if (field) return `“${field}” has the wrong format.`
  return 'A field has the wrong format.'
}

const STATIC: Record<string, string> = {
  NO_ACCESS_TO_QUIZ: 'You don’t have access to this quiz.',
  NO_ACCESS_TO_ATTEMPT: 'You don’t have access to this attempt.',
  ATTEMPT_ALREADY_FINISHED: 'This attempt is already finished.',
  USERNAME_ALREADY_TAKEN: 'That username is already taken.',
  UNAUTHORIZED: 'Wrong username or password.',
  FORBIDDEN: 'You don’t have permission to do that.',
  INVALID_BIO: 'Bio must be at most 280 characters.',
  AVATAR_NOT_FOUND: 'No avatar has been set.',
  BAD_REQUEST: 'The request was rejected.',
  NOT_FOUND: 'That resource could not be found.',
  METHOD_NOT_ALLOWED: 'That action isn’t supported here.',
  UNSUPPORTED_MEDIA_TYPE: 'Unsupported content type.',
  INTERNAL_SERVER_ERROR: 'The server hit an unexpected error.',
}

const RESOLVERS: Record<string, Resolver> = {
  INVALID_USERNAME,
  INVALID_PASSWORD,
  BLANK_OPTION_TEXT,
  INVALID_QUESTION_SHAPE,
  AVATAR_TOO_LARGE,
  INVALID_DISPLAY_NAME,
  INVALID_IMAGE,
  INVALID_RATING,
  RATING_NOT_ELIGIBLE,
  QUIZ_NOT_FOUND,
  QUESTION_NOT_FOUND,
  ATTEMPT_NOT_FOUND,
  OPTION_NOT_FOUND,
  USER_NOT_FOUND,
  VALIDATION_ERROR,
  INVALID_FORMAT,
}

const STATUS_FALLBACK: Record<number, string> = {
  401: 'Please sign in to continue.',
  403: 'You don’t have permission to do that.',
  404: 'That resource could not be found.',
  408: 'The request timed out. Try again.',
  413: 'The file is too large.',
  415: 'Unsupported content type.',
  429: 'You’re going too fast. Wait a moment and retry.',
  500: 'The server hit an unexpected error.',
  502: 'The server is unreachable. Try again shortly.',
  503: 'The service is unavailable. Try again shortly.',
  504: 'The server timed out. Try again shortly.',
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

export function allErrors(err: unknown): ApiError[] {
  if (err instanceof AxiosError) {
    const data = err.response?.data as Failure | undefined
    return data?.errors ?? []
  }
  return []
}

export function httpStatus(err: unknown): number | undefined {
  if (err instanceof AxiosError) return err.response?.status
  return undefined
}

function messageFor(code: string, details: Details): string {
  const resolver = RESOLVERS[code]
  if (resolver) return resolver(details)
  const staticMsg = STATIC[code]
  if (staticMsg) return staticMsg
  return FALLBACK
}

/**
 * Resolve any error into a user-facing message. Uses the first error's code
 * and details when available; otherwise falls back to a status-code message;
 * otherwise a generic fallback (with the original error logged for debugging).
 */
export function errorMessage(err: unknown): string {
  const first = firstError(err)
  if (first?.code) {
    const resolver = RESOLVERS[first.code]
    if (resolver) return resolver(first.details)
    const staticMsg = STATIC[first.code]
    if (staticMsg) return staticMsg
  }
  const status = httpStatus(err)
  if (status != null) {
    const statusMsg = STATUS_FALLBACK[status]
    if (statusMsg) return statusMsg
  }
  console.error(err)
  return FALLBACK
}

/**
 * For VALIDATION_ERROR responses, returns a map of field → message so forms
 * can highlight specific bad inputs. Each errors[] entry contributes one
 * field (the backend produces one entry per invalid field via
 * MethodArgumentNotValidException and HandlerMethodValidationException).
 *
 * Returns an empty object if the error is not a validation error or no field
 * details are present.
 */
export function validationFieldErrors(err: unknown): Record<string, string> {
  const out: Record<string, string> = {}
  for (const e of allErrors(err)) {
    if (e.code !== 'VALIDATION_ERROR' && e.code !== 'INVALID_FORMAT') continue
    const field = str(e.details, 'field')
    if (!field) continue
    const msg = str(e.details, 'message') ?? messageFor(e.code, e.details)
    if (!out[field]) out[field] = msg
  }
  return out
}
