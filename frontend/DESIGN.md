---
name: Premium Competitive Quiz
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#e4beb9'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#ab8985'
  outline-variant: '#5b403d'
  surface-tint: '#ffb4ab'
  primary: '#ffb4ab'
  on-primary: '#690005'
  primary-container: '#b71c1c'
  on-primary-container: '#ffcac4'
  inverse-primary: '#b91d1d'
  secondary: '#88d982'
  on-secondary: '#003909'
  secondary-container: '#005b14'
  on-secondary-container: '#81d27c'
  tertiary: '#c8c6c6'
  on-tertiary: '#303030'
  tertiary-container: '#5d5d5d'
  on-tertiary-container: '#d8d6d6'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdad6'
  primary-fixed-dim: '#ffb4ab'
  on-primary-fixed: '#410002'
  on-primary-fixed-variant: '#93000b'
  secondary-fixed: '#a3f69c'
  secondary-fixed-dim: '#88d982'
  on-secondary-fixed: '#002204'
  on-secondary-fixed-variant: '#005312'
  tertiary-fixed: '#e4e2e1'
  tertiary-fixed-dim: '#c8c6c6'
  on-tertiary-fixed: '#1b1c1c'
  on-tertiary-fixed-variant: '#474747'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style
The design system is engineered for a high-stakes, competitive atmosphere that feels prestigious rather than playful. The brand personality is "Elite Performance"—targeting intellectually driven users who value precision and a focused environment. 

The aesthetic follows a **Premium Tech** movement: a hybrid of Minimalism and subtle Glassmorphism. It utilizes deep obsidian surfaces, razor-sharp typography, and surgical use of a signature deep red to signal importance and action. The emotional response should be one of intense focus, clarity, and sophistication. Every element is intentional, avoiding unnecessary decorative "noise" or aggressive gradients.

## Colors
The palette is rooted in a deep, dark environment to reduce eye strain during intense focus. All colour references resolve to tokens in the frontmatter; raw hex should never appear in component code.
- **Signature Red (`primary-container` #b71c1c):** Used exclusively for high-priority actions, critical progress states, and brand-defining moments. It is a grounded, authoritative red. Text on top uses `on-primary-container` (#ffcac4). The lighter `primary` (#ffb4ab) is reserved for accents on dark surfaces where the container tone would be too heavy.
- **Success (`secondary-container` #005b14 / `secondary` #88d982):** Reserved for correct answers, achievement unlocks, and positive balance updates. Use the container tone for fills and the brighter `secondary` for outlines, glows, and on-dark text.
- **Background Tiers:** The foundation is `surface` (#131313). Cards and elevated content sit on `surface-container` (#201f1f); floating overlays use `surface-container-high` (#2a2a2a).
- **Borders:** Thin strokes from `outline-variant` (#5b403d) for low-emphasis dividers and `outline` (#ab8985) for hover/active emphasis. They replace heavy shadows to define structure.

## Typography
The system uses a dual-font approach. **Plus Jakarta Sans** provides a modern, slightly technical edge for headlines, while **Inter** ensures maximum legibility for body text and data-heavy quiz content. 

Tight tracking (-1% to -2%) is applied to larger headlines to create a "compact" and professional look. Labels use uppercase styling with slight letter spacing to differentiate metadata from body content.

## Layout & Spacing
This design system utilizes a **Fixed Grid** model for desktop and a **Fluid Grid** for mobile. 
- **Desktop:** 12-column grid, max-width 1200px, 24px gutters.
- **Mobile:** 4-column grid, 16px gutters and margins.

The spacing rhythm is based on a 4px baseline, ensuring all components align to a mathematical grid. Large negative space is used between sections to maintain the "Premium" feel, preventing the UI from feeling cluttered during fast-paced gameplay.

## Elevation & Depth
Depth is communicated through **Tonal Layering** and **Subtle Translucency** rather than traditional shadows.
- **Level 0 (Base):** `surface` (#131313). Background for the entire application.
- **Level 1 (Surface):** `surface-container` (#201f1f). Used for primary cards and content areas.
- **Level 2 (Overlay):** `surface-container-high` (#2a2a2a). Used for modals or floating elements, often with a `backdrop-filter: blur(8px)` and a 1px `outline-variant` border.

Shadows, if used, are extremely subtle: `0px 4px 20px rgba(0, 0, 0, 0.5)`. This creates a sense of "objects resting on glass" rather than floating in space.

## Shapes
The shape language is precise and architectural. Base components use the default **4px** corner radius (`rounded.DEFAULT`) to maintain a professional, slightly aggressive edge.

- **Small elements (Inputs/Chips):** `rounded.DEFAULT` (4px).
- **Medium elements (Cards):** `rounded.lg` (8px).
- **Large containers:** `rounded.xl` (12px).

The `rounded.full` token exists only for technical edge cases (e.g. avatar masks); avoid pills and fully rounded shapes in product UI, as they detract from the technical, high-end aesthetic.

## Components
- **Buttons:** Primary buttons fill with `primary-container` (#b71c1c) and use `on-primary-container` text. Secondary buttons use a ghost style: 1px `outline-variant` border with no fill, lifting to `outline` on hover.
- **Quiz Cards:** `surface-container` background, 1px `outline-variant` border, `rounded.lg` (8px) corners. On hover, the border switches to `outline` for the same effect as raising opacity.
- **Input Fields:** `surface-container-lowest` (#0e0e0e), darker than the surrounding surface, with `rounded.DEFAULT` (4px) and a 1px `outline-variant` border. Focus state changes the border to `primary-container`.
- **Timer/Progress Bars:** Thin 4px tracks on `surface-container-low`. The progress fill is `primary-container`, with no rounded caps (flush edges).
- **Correct/Incorrect Feedback:** Correct answers get a subtle `secondary` outer glow (5px blur) on the choice card; incorrect answers use `primary-container` (which is also the `error-container` family in the tokens).
- **Score Chips:** Small, rectangular containers with uppercase `label-sm` text on `surface-container-high`, used for displaying difficulty or points.