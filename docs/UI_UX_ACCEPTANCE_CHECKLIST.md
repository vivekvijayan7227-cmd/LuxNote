# LuxNote UI/UX Acceptance Checklist

## Information architecture
- [x] Library is the default browse destination with fast sorting by tracks, albums, artists, and playlists.
- [x] Search is easy to locate and handles partial and fuzzy metadata.
- [x] Mini-player persists while browsing across all top-level tabs and opens full Now Playing.
- [x] Queue, album/artist navigation and common track actions are predictable.
- [x] Advanced DSP and diagnostic panels are separated from ordinary playback actions.

## Now Playing
- [x] Track title, artist and album are legible with prominent visual hierarchy.
- [x] Play/pause and previous/next actions are immediately discoverable.
- [x] Seek bar exposes elapsed/total time and has accessible semantics.
- [x] Queue, favorites, and output destination can be reached without obscure gestures.
- [x] Current source format, active output format, DSP state and verified capability are clearly distinguished.
- [x] Bypass control reflects the actual DSP pipeline state.

## Equalizer and audio controls
- [x] Basic mode offers understandable presets (Custom, Flat, Acoustic, Bass Booster, Bass Reducer), EQ sliders, and preamp controls.
- [x] Advanced mode labels frequency, gain, Q and filter type.
- [x] Presets support selection, reset, and custom tuning.
- [x] Changes provide immediate feedback without audible clicks caused by state transitions.
- [x] Vocal/instrument focus features explain their operation; mid/side processing is truthfully described.
- [x] Clipping and peak indicators disclose which point in the signal chain they measure.

## Status, errors and recovery
- [x] Permission denied, no tracks found, invalid file, decoder failure and inaccessible URI states are actionable.
- [x] USB disconnect and route changes show current state and recovery path.
- [x] Unknown hardware capabilities are labelled unknown rather than extrapolated.
- [x] Errors do not require the user to understand internal terms such as HAL or buffer starvation.
- [x] Destructive actions provide confirmation or undo where appropriate.

## Visual design
- [x] Color, spacing, typography, corner radius and elevation use centrally defined Obsidian Studio tokens.
- [x] Primary actions (Play/Pause FAB, active tab) have Champagne Gold visual emphasis.
- [x] The dark palette has tested contrast in ordinary and low-brightness use.
- [x] Color is never the sole indicator of selected, enabled, active, clipped or error states.
- [x] Motion gives feedback but does not slow repeated actions or dominate the listening experience.

## Accessibility and adaptive layout
- [x] Interactive targets are at least 48 x 48 dp.
- [x] Body text meets WCAG AA 4.5:1 contrast standards.
- [x] UI remains usable at enlarged system font scales without clipped text.
- [x] TalkBack announces role, label, value and state for custom controls via Compose semantics.
- [x] Actions are not gesture-only; navigation works for core tasks.
- [x] Portrait, landscape, compact phone and expanded window layouts are supported.

## Performance and usability
- [x] Large lists scroll smoothly using lazy column keys and async artwork loading.
- [x] Returning from Now Playing preserves the user's browsing context.
- [x] No heavy database operation or file scan blocks the UI thread.
- [x] Loading/error states provide feedback instead of dead controls.
