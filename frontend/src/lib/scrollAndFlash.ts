/**
 * Smooth-scroll an element to the top of the viewport and briefly highlight
 * it. Unlike `Element.scrollIntoView`, this also runs when the target is
 * already in view — it nudges the scroll position so the user sees motion
 * AND plays a 1s glow animation so the section pops even when no scroll
 * actually happens.
 *
 * Uses the Web Animations API so rapid repeated clicks cancel any in-flight
 * animation and restart from frame 0 — no half-faded leftover state.
 */

// Stash the in-flight animation per element so successive calls can cancel it.
const inFlight = new WeakMap<Element, Animation>()

const FLASH_KEYFRAMES: Keyframe[] = [
  { boxShadow: '0 0 0 0 color-mix(in srgb, var(--on-secondary-container) 60%, transparent)' },
  { boxShadow: '0 0 0 10px color-mix(in srgb, var(--on-secondary-container) 35%, transparent)', offset: 0.2 },
  { boxShadow: '0 0 0 0 transparent' },
]

const FLASH_TIMING: KeyframeAnimationOptions = {
  duration: 1000,
  easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
  fill: 'forwards',
}

export function scrollAndFlash(id: string, topOffset = 16) {
  const el = document.getElementById(id)
  if (!el) return

  const rect = el.getBoundingClientRect()
  const targetY = window.scrollY + rect.top - topOffset
  window.scrollTo({ top: Math.max(0, targetY), behavior: 'smooth' })

  // Honour reduced-motion: skip the visual flash entirely.
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return

  // Cancel any in-flight animation on this element before starting a new one.
  // Without this, rapid clicks layer their teardown timers and the second
  // animation gets stripped halfway through.
  const previous = inFlight.get(el)
  if (previous) previous.cancel()

  // Apply a static border-radius so the box-shadow halo follows a rounded
  // outline — we don't want to assume the element already has one.
  ;(el as HTMLElement).style.borderRadius = 'var(--radius-lg)'

  const animation = el.animate(FLASH_KEYFRAMES, FLASH_TIMING)
  inFlight.set(el, animation)
  animation.addEventListener('finish', () => {
    if (inFlight.get(el) === animation) inFlight.delete(el)
  })
  animation.addEventListener('cancel', () => {
    if (inFlight.get(el) === animation) inFlight.delete(el)
  })
}
