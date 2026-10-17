# Obsidian Studio — Design Tokens

## Core palette

| Token | Hex | Use |
|---|---|---|
| Background | `#07090E` | App canvas |
| Surface | `#101521` | Navigation, cards, panels |
| Surface Elevated | `#182033` | Dialogs, sheets, raised panels |
| Surface Selected | `#202B40` | Selected list rows |
| Gold | `#F5C76A` | Primary action, active track |
| Gold Highlight | `#FFE5A3` | Rare highlight |
| Blue | `#67B7FF` | Technical accent, waveform |
| Text Primary | `#E8ECF4` | Main text |
| Text Secondary | `#A0A9BA` | Artist, metadata |
| Text Muted | `#737F93` | Nonessential metadata |
| Divider | `#293246` | Separators |
| Progress Track | `#343D50` | Inactive seek/progress |
| Success | `#66D6A3` | Completed/saved |
| Warning | `#F2B866` | Warnings |
| Error | `#FF777F` | Errors |

## Semantics
- Gold is for the primary action and current playback/selection state.
- Blue is for secondary technical details and audio visualizations.
- Do not use gold and blue indiscriminately on the same control.
- Use labels/icons as well as color to communicate state.
- Gradients and glows should be rare: app icon, selected hero artwork, or playback visualization only.

## Spacing
- XS: 4 dp
- SM: 8 dp
- MD: 12 dp
- LG: 16 dp
- XL: 24 dp
- XXL: 32 dp
- Screen horizontal padding: 20–24 dp
- Standard list row: 68–76 dp
- Compact row: 56–64 dp
- Touch target: minimum 48 × 48 dp

## Shapes
- Artwork thumbnail: 8–12 dp
- Standard card: 14–18 dp
- Dialog: 20–24 dp
- Pill controls: fully rounded

## Typography
Use Roboto for native Android feel or Inter for a more editorial look. Keep one family throughout.
- Hero title: 30–34 sp semibold
- Screen title: 26–28 sp semibold
- Section title: 18–20 sp semibold
- Track title: 15–16 sp medium
- Metadata: 13–14 sp regular
- Timestamps: 12 sp medium, tabular numerals

## Motion
- Button feedback: 100–160 ms
- Tab transition: 160–220 ms
- Bottom sheet: 220–300 ms
- Artwork crossfade: 180–280 ms
Respect reduced-motion settings and never delay playback actions for animation.

## Accessibility
- Target WCAG 2.2 AA contrast: 4.5:1 normal text, 3:1 large text and meaningful graphical controls.
- Validate each foreground/background pairing in the actual UI.
- Gold used for decorative lighting may not pass for small text on every surface.
- Icon-only controls require semantic labels.
- Support TalkBack, font scaling, and non-color cues for active states.
