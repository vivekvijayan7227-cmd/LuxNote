# Obsidian Studio — Android UI/UX Starter Pack

A Kotlin + Jetpack Compose design-system starter for a premium local music player inspired by the Obsidian Studio palette.

## Contents
- `docs/UI_UX_SPEC.md` — screen-by-screen design and interaction specification.
- `docs/DESIGN_TOKENS.md` — palette, spacing, typography, component sizing, accessibility.
- `app/src/main/java/com/audioplayer/hires/ui/theme/StudioColors.kt`
- `app/src/main/java/com/audioplayer/hires/ui/theme/StudioTypography.kt`
- `app/src/main/java/com/audioplayer/hires/ui/theme/StudioShapes.kt`
- `app/src/main/java/com/audioplayer/hires/ui/theme/StudioTheme.kt`
- `app/src/main/java/com/audioplayer/hires/ui/components/StudioButton.kt`
- `app/src/main/java/com/audioplayer/hires/ui/components/TrackRow.kt`
- `app/src/main/java/com/audioplayer/hires/ui/components/PlaybackSeekBar.kt`
- `app/src/main/java/com/audioplayer/hires/ui/components/MiniPlayer.kt`
- `app/src/main/java/com/audioplayer/hires/ui/components/EmptyState.kt`
- `app/src/main/java/com/audioplayer/hires/ui/screens/LibraryScreen.kt`
- `app/src/main/java/com/audioplayer/hires/ui/screens/NowPlayingScreen.kt`
- `preview/StudioPreview.kt` — optional Compose previews.
- `assets/palette.json` — machine-readable color tokens.
- `assets/icon-reference.png` — not included; use the generated icon from the conversation as your reference asset.

## Integration
1. Copy the `app/src/main/java/com/audioplayer/hires/ui/` directory into your app module, merging with existing packages as needed.
2. The package namespace follows the application ID `com.audioplayer.hires` remembered from the current project context. If your Kotlin namespace differs, update package declarations.
3. Ensure your module uses Jetpack Compose Material 3 and has Compose enabled.
4. This starter uses Material 3, Foundation, and UI APIs. Add the appropriate Compose dependencies through your existing version catalog/BOM; do not add a second conflicting Compose version.
5. Call `StudioTheme { ... }` at the app root, or inside your existing theme wrapper.
6. Wire `LibraryScreen` and `NowPlayingScreen` to your actual Media3 playback service, MediaStore repository, queue, and favorites state. The included screens are UI scaffolds with callbacks, not a replacement playback engine.

## Design intent
- Gold = primary action / current selection.
- Blue = secondary technical accent / audio visualization.
- Obsidian and navy = structure and surfaces.
- Text and state must remain readable without relying on color alone.

## Important
This is a starter UI system, not a complete buildable player project. Existing app-specific models and playback state are deliberately not fabricated. Some component APIs may need minor adjustments to match the Compose BOM and Material 3 version already used by your app.
