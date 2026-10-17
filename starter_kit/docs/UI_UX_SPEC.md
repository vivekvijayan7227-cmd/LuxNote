# Obsidian Studio — UI/UX Specification

## Product principles
1. Efficient local-library browsing before decoration.
2. Playback actions stay obvious and immediate.
3. Gold means primary action/current selection; blue means secondary technical emphasis.
4. Real album art supplies most of the screen's color.
5. Shared components behave consistently across library, queue, and search.
6. Keep playback state in the app's playback/domain layer, not in individual composables.

## Navigation
Primary destinations:
- Library
- Queues
- Favorites
- Settings

Keep a persistent mini-player above bottom navigation when a track is loaded. Library categories may be tabs/chips: Songs, Albums, Artists, Genres, Folders, Playlists.

For tablets/landscape, consider a navigation rail or side panel.

## Library
- Header with screen title, search, and overflow actions.
- Category tabs/chips.
- Optional Recently Added / Continue Listening section.
- Track rows: artwork, title, artist/album metadata, duration, overflow menu.
- Sorting/filtering and multi-select actions.
- Current track: subtle navy highlight plus gold icon/indicator.
- Empty state explains why no files are shown and provides permission/rescan action.

## Now Playing
- Large square artwork, title, artist, optional album.
- Favorite and track actions.
- Seek bar with elapsed/total time.
- Shuffle, previous, play/pause, next, repeat.
- Queue, lyrics, equalizer, sleep timer, output controls where supported.
- Gold main play/pause; secondary controls pearl/gray.
- Artwork-derived background tint must not reduce contrast.
- Use a compact layout on short screens; avoid putting primary controls too high.

## Queue
- Clearly distinguish current queue from saved playlists.
- Highlight currently playing track.
- Support reorder/remove and provide undo after removal.
- Make queue persistence and independent queues explicit if supported.
- Use visible drag handles and accessible alternatives to drag gestures.

## Search
- Search local metadata: title, artist, album, genre, and supported tags.
- Provide category filters, clear action, recent searches if useful.
- Design loading, no-results, permission, and error states.

## Album / artist / genre / folder
- Reuse a shared detail-page template.
- Header metadata and artwork first, then actions and track list.
- Folder view should represent the actual device hierarchy.
- Avoid inventing artist metadata not present in local files.

## Equalizer
- Show only controls supported by the device/audio effect stack.
- Presets, bands, gain values, reset, custom presets.
- Blue for spectrum visualization; gold for selected preset and active controls.
- Explain unavailable effects rather than presenting fake controls.

## Settings sections
- Playback: gapless, crossfade if supported, resume, audio focus.
- Audio: equalizer/effects/output.
- Library: scan, excluded folders, sorting, metadata.
- Queues: persistence, history, shuffle/repeat behavior.
- Appearance: theme, artwork, density.
- Storage: indexed files, cache, permissions.
- Notifications: media notification and lock screen.
- Accessibility: text scaling, motion, contrast.
- About: version, licenses, diagnostics, feedback.

## Component rules
- Screen padding 20–24 dp.
- Touch targets at least 48 dp.
- Artwork thumbnails 48–56 dp.
- Mini-player 68–84 dp excluding navigation.
- Main play/pause 64–76 dp.
- Cards 14–18 dp radius.
- Use subtle dividers, not bright borders on every card.
- Never use color alone to indicate current track, shuffle, repeat, or errors.

## State coverage checklist
Every screen should define:
- Loading
- Content
- Empty
- Permission denied / unavailable
- Recoverable error
- Disabled state where applicable
- Selected/active state
- Large-font layout
- TalkBack labels
- Reduced-motion behavior
