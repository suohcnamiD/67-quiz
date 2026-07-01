/**
 * DHBW percent-to-note conversion.
 *
 * The DHBW "Wirtschaft" Punkte-/Noten-Tabellen from 2022-10-14 publishes six
 * point-scale tables (60, 90, 100, 120, 150, 180 max points) that all map to
 * the same relative percentage curve. We express the curve once, at the 100-
 * point resolution, and derive the note from a percentage input.
 *
 * Reference points from the 100-point column:
 *   100.0%   -> 1.0
 *    82.0%   -> 2.0
 *    65.5%   -> 3.0
 *    50.0%   -> 4.0
 *   <33.34%  -> 5.0 (fail cut-off is X <= 33)
 *
 * Between 1.0 and 4.0 the curve advances 0.1 per ~1.5 percentage points; below
 * 4.0 the fail zone (50% down to ~33%) is compressed. This table encodes the
 * upper bound (inclusive) of each note band on the 100-point scale, matching
 * the source PDF exactly.
 */

// Lower bound of the band on the 100-point scale (in points, so 100 = 100%).
// The band with note N covers [BAND_LOWER[i], BAND_LOWER[i-1]) points. Note 1.0
// occupies [98.5, 100]; note 5.0 is anything <= 33 (X <= 33).
const BANDS: { lower: number; note: number }[] = [
  { lower: 98.5, note: 1.0 },
  { lower: 97.0, note: 1.1 },
  { lower: 95.5, note: 1.2 },
  { lower: 93.5, note: 1.3 },
  { lower: 92.0, note: 1.4 },
  { lower: 90.5, note: 1.5 },
  { lower: 88.5, note: 1.6 },
  { lower: 87.0, note: 1.7 },
  { lower: 85.5, note: 1.8 },
  { lower: 83.5, note: 1.9 },
  { lower: 82.0, note: 2.0 },
  { lower: 80.5, note: 2.1 },
  { lower: 78.5, note: 2.2 },
  { lower: 77.0, note: 2.3 },
  { lower: 75.5, note: 2.4 },
  { lower: 73.5, note: 2.5 },
  { lower: 72.0, note: 2.6 },
  { lower: 70.5, note: 2.7 },
  { lower: 68.5, note: 2.8 },
  { lower: 67.0, note: 2.9 },
  { lower: 65.5, note: 3.0 },
  { lower: 63.5, note: 3.1 },
  { lower: 62.0, note: 3.2 },
  { lower: 60.5, note: 3.3 },
  { lower: 58.5, note: 3.4 },
  { lower: 57.0, note: 3.5 },
  { lower: 55.5, note: 3.6 },
  { lower: 53.5, note: 3.7 },
  { lower: 52.0, note: 3.8 },
  { lower: 50.5, note: 3.9 },
  { lower: 50.0, note: 4.0 },
  { lower: 47.0, note: 4.1 },
  { lower: 45.5, note: 4.2 },
  { lower: 43.5, note: 4.3 },
  { lower: 42.0, note: 4.4 },
  { lower: 40.5, note: 4.5 },
  { lower: 38.5, note: 4.6 },
  { lower: 37.0, note: 4.7 },
  { lower: 35.5, note: 4.8 },
  { lower: 33.5, note: 4.9 },
]

/**
 * Convert a score percentage in [0, 100] to the DHBW note. Values >= 98.5%
 * clamp to 1.0; values <= 33% return 5.0 (fail).
 */
export function dhbwGrade(percent: number): number {
  if (!Number.isFinite(percent) || percent < 0) return 5.0
  if (percent >= 98.5) return 1.0
  for (const band of BANDS) {
    if (percent >= band.lower) return band.note
  }
  return 5.0
}

/** Format the note in German convention: one decimal, comma separator. */
export function formatGrade(note: number): string {
  return note.toFixed(1).replace('.', ',')
}

/** Short verdict tone that pairs with the grade for UI colouring. */
export function gradeTone(note: number): 'great' | 'good' | 'tried' {
  if (note <= 2.0) return 'great'
  if (note <= 4.0) return 'good'
  return 'tried'
}
