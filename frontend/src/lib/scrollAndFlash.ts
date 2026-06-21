/**
 * Smooth-scroll an element to the top of the viewport and briefly highlight
 * it. Unlike `Element.scrollIntoView`, this also runs when the target is
 * already in view — it nudges the scroll position so the user sees motion
 * AND adds a `.flash-target` class for ~1s so the section pops visually.
 */
export function scrollAndFlash(id: string, topOffset = 16) {
  const el = document.getElementById(id)
  if (!el) return
  const rect = el.getBoundingClientRect()
  const targetY = window.scrollY + rect.top - topOffset
  window.scrollTo({ top: Math.max(0, targetY), behavior: 'smooth' })
  el.classList.remove('flash-target')
  // Force a reflow so re-adding the class restarts the animation.
  void el.offsetWidth
  el.classList.add('flash-target')
  window.setTimeout(() => el.classList.remove('flash-target'), 1100)
}
