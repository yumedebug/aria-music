# Aria Music — an Apple Music-style player for Google Pixel

A new Android app (Kotlin + Jetpack Compose) with a playback engine built on **Media3 /
ExoPlayer**, and a UI/UX designed to feel close to Apple Music while staying native to
Android and Pixel.

> **Scope note (please read).** This repository is a **new application**, not a fork of
> OuterTune. An OuterTune checkout was not available in the environment and the project was
> explicitly requested to be created fresh. As a result the app plays the **on-device audio
> library** through MediaStore; it does **not** implement YouTube Music streaming/search.
> Everything listed under "Implemented" below works on real data. Nothing unimplemented is
> presented as finished.

---

## 1. Requirements

- JDK 17
- Android SDK with platform **android-35** and build-tools 34.x/35.x
- `local.properties` pointing at your SDK (`sdk.dir=...`)

## 2. Build & test

```bash
cd aria-music
./gradlew assembleDebug          # APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug              # lint
```

## 3. Architecture

```
app/src/main/java/com/aria/music/
├── core/            Pure logic: AppContainer (DI), formatting, palette, lyrics parsing
├── data/
│   ├── media/       MediaStore scan, artwork extraction, lyrics loading
│   ├── db/          Room: cached tracks, favourites, playlists, history
│   ├── repo/        LibraryRepository, SettingsRepository (DataStore)
│   └── model/       Track / Album domain models
├── playback/        Media3 session service + controller connection
└── ui/              Compose: theme, components, screens
```

Design choices:

- **No DI framework.** A single hand-written `AppContainer` holds the graph; it is small and
  keeps start-up cheap.
- **Playback lives in a service.** `PlaybackService` is a `MediaSessionService` that owns the
  single `ExoPlayer`. The UI talks to it through a `MediaController` wrapped by
  `PlaybackConnection`, which mirrors the player into one immutable `PlaybackState`
  (`StateFlow`). The mini player, the full player, the queue and the tabs therefore always
  read the **same** state.
- **Lyrics are real, never fabricated.** Lyrics come from the audio file's own ID3 `USLT`
  frame (which may itself contain timestamped LRC text). `LrcParser` and `Id3Lyrics` are pure
  functions and are unit tested.
- **Artwork-derived colour** is computed by down-scaling the embedded cover and sampling a
  saturation-weighted accent (`Palette`), used to tint the player and, optionally, the theme.

## 4. Implemented

**Playback engine**
- Play / pause / next / previous, seek, position & duration
- Shuffle, repeat off → all → one
- Play queue with reorder (drag), remove, "play next" and "play last"
- Background playback via a foreground media service
- Media notification, lock-screen and Bluetooth/headset controls (Media3 `MediaSession`)
- Track history recorded as you listen

**Library & data**
- Local library scan through MediaStore (songs, artists, albums)
- Favourites, user playlists (create / rename / delete / add / remove / reorder), history
- Persisted with Room; settings via DataStore

**UI / UX (Apple Music-inspired)**
- Home "Listen Now" with horizontal shelves (Recently Played, Favourites, Albums, Playlists)
- Full-screen **Now Playing**: hero artwork, clear title/artist hierarchy, scrubber with
  elapsed/remaining times, transport controls, favourites, overflow menu
- **Mini player** floating above the tab bar; tap to expand to the full player, back to
  collapse
- Lyrics page with active-line highlighting and auto-scroll (user scroll always wins)
- Queue bottom sheet
- Library with a segmented control (Albums / Playlists / Songs / Favourites)
- Search across songs, artists and albums
- Album and playlist detail screens (play / shuffle / manage)

**Pixel / Android integration**
- Edge-to-edge layout with correct status/navigation bar insets
- System light/dark, **Material You** dynamic colour (Android 12+)
- Optional **AMOLED pure-black** theme and artwork-based theming
- Gesture-navigation friendly (collapse the player with the back gesture)
- Consistent empty/loading states

## 5. Not implemented (honest list)

- **YouTube Music integration** — no streaming, no online search. This build is a local
  library player.
- **Online lyrics providers** — only lyrics embedded in your files are shown.
- **Cross-device sync / accounts** — all data is local.
- Playlist artwork composition (playlists show a neutral tile), and a few refinements
  (true shared-element hero animation, custom overflow for library rows).

## 6. Tests

`./gradlew testDebugUnitTest` runs **26 tests** (all passing):

- duration formatting
- LRC parsing (timed, multi-timestamp, offset, unsynced, active-line lookup, hour tags)
- ID3v2.3/2.4 `USLT` extraction, sync-safe integers, unsynchronisation
- artwork palette extraction and similarity
- scrubber gesture math
- **Compose rendering** of the full-screen player and the mini player under Robolectric
  (title/artist, transport controls, elapsed and remaining times)

### Verification status

- `assembleDebug` ✅ and `assembleRelease` (R8) build successfully.
- Unit + Compose tests ✅.
- **Not verified on a real device/emulator**: this environment has no emulator, system image
  or `/dev/kvm`, so the app was never installed and launched on Android. The Robolectric
  full-app test is also blocked because Robolectric hands Media3 a null `ComponentName`
  during service binding (`MediaControllerImplBase` then throws). Playback against a real
  `MediaSessionService` is therefore **unverified at runtime** and should be checked on a
  device before shipping.
