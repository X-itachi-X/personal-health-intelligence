# PHI Mobile Design System

Calm, clinical, trustworthy. Teal accent on neutral surfaces. Data-first layout for health information.

## Character

- Generous whitespace, restrained palette
- Color reserved for health status (green/amber/red) and primary actions (teal)
- No gradients, glow effects, or decorative emoji
- Tabular numbers for biomarker values

## Tokens (`lib/theme.ts`)

| Token | Value | Use |
|-------|-------|-----|
| `colors.bg` | `#f0f4f8` | Screen background |
| `colors.surface` | `#ffffff` | Cards, header |
| `colors.primary` | `#0d9488` | Primary buttons, active accents |
| `colors.primaryDark` | `#0f766e` | Secondary buttons, links |
| `colors.primaryLight` | `#ccfbf1` | Chips, icon backgrounds |
| `colors.text` | `#0f172a` | Headings, primary text |
| `colors.textMuted` | `#64748b` | Body, metadata |

## Components (`components/ui/`)

Build screens by composing shared primitives. New screens should not duplicate card/button styles inline.

```
Screen
 └── FadeInView (staggered)
      └── Card
           ├── Icon + section title
           ├── content
           └── Button / AnimatedPressable rows
```

## Icons

All icons via `components/ui/Icon.tsx` (Ionicons). Navigation icons defined in `lib/icons.ts`.

## Motion (`lib/motion.ts`)

- Entrance: fade + 16px slide-up, 200ms ease-out
- Press: scale to 0.97 via `AnimatedPressable`
- Family switch overlay: spring scale + pulsing dots
- Trend sparklines: staggered bar height spring

## Do

- Use `Screen` for every scrollable page
- Use `EmptyState` with one clear CTA when lists are empty
- Use `Skeleton` during data fetch
- Wrap all pressable text in `<Text>` or use `Button`

## Don't

- Hardcode colors outside `theme.ts`
- Use emoji as UI icons
- Use `ActivityIndicator` as full-page loader
- Animate width, height, or margin
