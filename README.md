<div align="center">

# 🎵 OMP – Open Music Player

**A lightweight, Spotify-inspired desktop music player for your local music library — built with JavaFX.**

![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.4-blue?logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Windows-0078D6?logo=windows&logoColor=white)
[![Build Windows](https://github.com/zenoxart/MusicPlayer/actions/workflows/build-windows.yml/badge.svg)](https://github.com/zenoxart/MusicPlayer/actions/workflows/build-windows.yml)

### [⬇️ Download the latest Windows build](https://github.com/zenoxart/MusicPlayer/releases/download/latest-windows-build/OMP-Windows.zip)

*Automatically rebuilt from the latest `main` commit by [`build-windows.yml`](.github/workflows/build-windows.yml). Unzip and run `OMP.exe` — no Java install required.*

</div>

<p align="center">
  <img src="docs/screenshot.png" alt="OMP – Open Music Player screenshot" width="800">
</p>

> 📸 The screenshot above predates this round of changes (playlists, the carousel UI, theming, and
> language selection). It's a placeholder until it's recaptured from the current build — grab a fresh one
> from the running app (or send one over) and drop it in `docs/screenshot.png` to replace it.

## Overview

**OMP (Open Music Player)** is a small desktop music player that scans a folder tree for audio files (WAV
by default, MP3/M4A/AAC optional via Settings), reads their ID3 metadata (title, artist, cover art) and
browses them as a 3-card carousel (arrow buttons, mouse-swipe, or click a side card) instead of a plain
table. Organize tracks into playlists (right-click or drag & drop, freely reorderable, shuffle or in-order
playback), rate them with a 5-star system, filter and sort on multiple criteria at once, search live, skip
tracks by swiping the now-playing preview, and pick a light/dark theme with a custom accent color and an
English/German UI on first launch — or change any of it later in Settings.

No account, no cloud, no telemetry — just a folder full of music files and a UI that doesn't get in the way.

## ✨ Features

| | |
|---|---|
| 📂 **Recursive folder scan** | Pick any folder — all matching audio files in it and its subfolders are found automatically. Duplicate filenames are only listed once. |
| 🏷️ **ID3 metadata** | Title, artist, album and embedded cover art are read via jaudiotagger. Falls back to the filename and a music-note placeholder when tags/art are missing. |
| 🎠 **3-card carousel** | Browse one focused (bigger) card at a time with a smaller preview on each side — via the arrow buttons, a mouse-swipe on the row, or by clicking a side card. Card size scales with the window. |
| 📃 **Playlists** | Create/delete playlists from the sidebar (via the always-visible "⋮" menu or right-click). Add songs via right-click on a card **or** by dragging a card onto a playlist row in the sidebar. |
| 🔀 **Reorder, shuffle, play in order** | Drag song cards to reorder a playlist (while it's shown in its own order). Toggle shuffle in the player bar to randomize next/previous — toggle off to go back to the playlist's stored order. |
| 🛡️ **Confirm before deleting a playlist** | A dedicated dialog lists what's in the playlist and defaults to the safe choice — deleting is a deliberate secondary action, not the prominent button. |
| ⚙️ **File-format settings** | Choose which audio formats get *scanned* (WAV, MP3, M4A, AAC) from the Settings dialog — the current folder is rescanned immediately when you change it. |
| ⭐ **5-star ratings** | Click a star to rate a track (click the same star again to clear it). Ratings persist across restarts. |
| 🎛️ **Multi-criteria Filter & Sort** | Combine rating, artist and length sort (each independently ascending/descending) and filter which already-scanned formats are *shown*, from one dialog opened via the top-bar icon. |
| 🔍 **Live search** | Filter the grid instantly by title, artist or album as you type — works within the Library or the currently open playlist. |
| ▶️ **Playback controls** | Play/pause, previous/next, a seekable progress bar and a volume slider — both rendered as filled, Spotify-style bars. |
| 👉 **Swipe to skip** | Click-and-drag the now-playing preview left/right to jump to the next/previous track. |
| 🎨 **Light/dark theme + custom accent** | Pick light or dark mode and an accent color (defaults to Spotify green) on first launch, or anytime from Settings — applied live across the whole app, including every dialog and context menu. |
| 🌐 **English/German UI** | Choose the app language on first launch (defaults to English) or change it anytime in Settings; every label, menu, toast and dialog is translated. |
| 💾 **Persistent everything** | Ratings, playlists, format/theme/language settings are stored as JSON in `%APPDATA%\MusicPlayer\` — survive rescans and app restarts. |
| 🖼️ **Custom app icon** | A generated green eighth-note icon, used for the window/taskbar icon and the packaged `.exe`. |
| 📦 **Standalone `.exe`** | Packaged via `jpackage` into a native Windows app image — bundles its own Java runtime, no separate JRE/JDK install needed to run it. |

## 🖥️ Tech stack

- **Java 21** (developed & tested against JDK 26)
- **JavaFX 21.0.4** — `javafx-controls`, `javafx-media`
- **jaudiotagger 3.0.1** — ID3 tag & artwork extraction
- **[org.json](https://github.com/stleary/JSON-java) 20240303** — rating, playlist, settings, theme & language persistence
- **Java `ResourceBundle`** — English/German UI strings (`i18n/messages*.properties`)
- **Maven** — build, with `maven-shade-plugin` for a self-contained runnable jar
- **`jpackage`** (bundled with the JDK) — native Windows app image / `.exe`

## 📁 Project structure

```
music-player/
├── pom.xml
├── packaging/
│   ├── make-icon.ps1        # generates the app icon (PNG + multi-size .ico)
│   └── app-icon.ico
├── docs/
│   └── screenshot.png
└── src/main/
    ├── java/com/musicplayer/
    │   ├── Main.java                  # Application entry point (Scene/Stage setup)
    │   ├── Launcher.java              # Plain entry point, see note below
    │   ├── controller/
    │   │   ├── MainController.java    # builds the whole UI, wires up events
    │   │   ├── SettingsDialog.java    # file formats + "change theme"/"change language" entry points
    │   │   ├── FilterSortDialog.java  # modal for multi-criteria sort + displayed formats
    │   │   ├── ThemeDialog.java       # light/dark + accent-color picker (first launch & Settings)
    │   │   ├── ThemeSupport.java      # applies the current theme to a window/dialog/context menu
    │   │   ├── LanguageDialog.java    # English/German picker (first launch & Settings)
    │   │   └── Toast.java             # lightweight transient feedback pill
    │   ├── model/
    │   │   ├── Song.java              # song metadata + observable rating
    │   │   ├── Playlist.java          # playlist name + ordered song paths
    │   │   ├── SortKey.java           # RATING / ARTIST / LENGTH
    │   │   ├── SortDirection.java     # ASC / DESC
    │   │   └── Language.java          # ENGLISH / GERMAN
    │   └── service/
    │       ├── MusicScanner.java      # recursive audio-file discovery, de-duplication
    │       ├── MetadataService.java   # ID3 tags + cover art (jaudiotagger)
    │       ├── AudioPlayer.java       # MediaPlayer wrapper
    │       ├── RatingStore.java       # JSON rating persistence in %APPDATA%
    │       ├── PlaylistStore.java     # JSON playlist persistence in %APPDATA%
    │       ├── SettingsStore.java     # JSON format-settings persistence in %APPDATA%
    │       ├── ThemeStore.java        # JSON theme (mode + accent) persistence in %APPDATA%
    │       ├── LanguageStore.java     # JSON language persistence in %APPDATA%
    │       ├── Messages.java          # loads/switches the active ResourceBundle at runtime
    │       └── AppDataLocations.java  # shared %APPDATA%\MusicPlayer path resolution
    └── resources/
        ├── css/style.css              # Spotify-inspired theme, driven by CSS custom properties
        ├── i18n/
        │   ├── messages.properties    # English strings (default/fallback)
        │   └── messages_de.properties # German strings
        └── icon/app-icon.png          # window/taskbar icon
```

## 🚀 Getting started

### Prerequisites

- **JDK 21+** (JavaFX modules are pulled in automatically via Maven — no separate SDK download needed)
- **Maven** (or open the folder directly in IntelliJ IDEA, which bundles its own Maven)

### Clone & run

```bash
git clone <repository-url>
cd music-player
mvn javafx:run
```

### Running from an IDE

> **Important:** run the **`Launcher`** class, not `Main`.
>
> `Main` extends `javafx.application.Application`. Launching it directly via a plain classpath command
> (which is what IntelliJ's auto-generated run configuration does) triggers:
> `Error: JavaFX runtime components are missing, and are required to run this application`
> even though the JavaFX jars are present. `Launcher` is a tiny indirection (`public static void main` that
> just calls `Main.main(args)`) that sidesteps this well-known JavaFX/IDE quirk.

## 📦 Building a standalone `.exe`

The app is packaged as a native Windows app image using `jpackage`, so end users don't need Java installed.

**1. Build a self-contained runnable jar:**

```bash
mvn clean package
```

This produces `target/music-player-1.0-SNAPSHOT-app.jar` — a shaded jar containing the app, JavaFX
runtime classes and all dependencies.

**2. Package it with `jpackage`:**

```bash
mkdir -p packaging/app-input
cp target/music-player-1.0-SNAPSHOT-app.jar packaging/app-input/music-player-app.jar

jpackage \
  --type app-image \
  --input packaging/app-input \
  --main-jar music-player-app.jar \
  --main-class com.musicplayer.Launcher \
  --name "OMP" \
  --app-version 1.0.0 \
  --vendor "ZenoxArt" \
  --icon packaging/app-icon.ico \
  --dest packaging/dist
```

The result is `packaging/dist/OMP/OMP.exe` — a folder containing the executable and a
bundled Java runtime. Copy or zip the whole `OMP` folder to distribute it.

> **Note:** `--type app-image` produces a runnable folder + `.exe`, no installer. Building a full installer
> (`--type exe`, with Start Menu shortcut and uninstaller) additionally requires the
> [WiX Toolset v3](https://wixtoolset.org/) to be installed on the build machine.

## 🎨 First launch: language, theme & accent color

The first time OMP starts, two short dialogs ask you to pick:

1. **Language** — English (default) or German. Every label, button, tooltip, menu, toast and dialog in
   the app is translated (`i18n/messages.properties` for English, `i18n/messages_de.properties` for
   German), loaded via a `ResourceBundle` that's swapped and re-applied to the whole UI at runtime.
2. **Appearance** — light or dark mode, and an accent color (six presets plus a custom color picker;
   Spotify green is the default). The picker live-previews your choice on itself as you click around.

Both choices persist in `%APPDATA%\MusicPlayer\` and are reused on every future launch — the dialogs only
appear once. You're never stuck with your first choice: the Settings dialog (gear icon) has **"🎨 Change
Appearance"** and **"🌐 Change Language"** buttons that reopen the same pickers and apply your change live,
across the main window and every open dialog/context menu.

## 🎠 Browsing: the carousel

Instead of a scrolling grid, the library/playlist view shows three cards at a time — a bigger, focused
card in the middle and a smaller preview on each side. Move through the list by clicking the arrow
buttons, clicking a side card (which also plays it), or **mouse-swiping** the row itself (press, drag,
release — same gesture as the now-playing preview). Whatever's currently playing (from anywhere — the
carousel, the player bar's prev/next, or auto-advance) always re-centers the carousel on it.

## ⚙️ Settings — supported file formats

Click the gear icon in the top bar to choose which audio formats get **scanned**. The choices are
intentionally limited to formats JavaFX's `MediaPlayer` can actually play — **WAV**, **MP3** and
**M4A/AAC** — so nothing gets scanned that would silently fail to play. FLAC and OGG aren't offered
for the same reason: JavaFX has no built-in decoder for them. At least one format must stay checked.
Saving with a folder already loaded immediately rescans it. The same dialog is also where you change
theme and language later (see above).

## 📃 Playlists

Use the sidebar to switch between your full **Library** and any playlist — playlist songs load directly
from their saved paths, so opening one never requires picking its folder first. "+ Neue Playlist" creates
one. Add a song to a playlist either **right-click a card → "Add to Playlist"**, or by **dragging a card
straight onto a playlist row** in the sidebar. Hover a playlist row for its **"⋮" menu** (or right-click
it) to delete it — a confirmation dialog lists what's inside and asks you to explicitly click "Delete
Anyway" rather than making deletion the default action.

While a playlist is open and no sort is active, **drag song cards to reorder them** — the new order is
saved immediately. The player bar's shuffle button (🔀) randomizes next/previous playback; toggle it off
to go back to playing the playlist (or Library) in its stored/scanned order. Search still works within
either view.

## 🎛️ Filter & Sort

The top-bar filter icon opens a dialog with two grouped sections instead of a single dropdown: **sort by**
— rating, artist and length can each be toggled ascending/descending *independently*, and combine into a
compound sort (e.g. rating first, ties broken by length) — and **which already-scanned formats to
display**. At least one format must stay checked. The icon itself turns green whenever a non-default
filter or sort is applied, and its tooltip shows exactly what's active.

## ⭐ Data storage

Ratings, playlists, format/theme/language settings are each stored as their own JSON file under
`%APPDATA%\MusicPlayer\` (falls back to the user home directory if `%APPDATA%` isn't set):

| File | Contents |
|---|---|
| `ratings.json` | flat map of absolute file path → rating (`1`–`5`) |
| `playlists.json` | array of `{id, name, songs[]}`, `songs` holding absolute file paths in playlist order |
| `settings.json` | array of enabled scan extensions, e.g. `{"extensions": ["wav", "mp3"]}` |
| `theme.json` | `{darkMode, accentColor, configured}` |
| `language.json` | `{language, configured}` |

Clicking a star that already matches a track's current rating clears its entry from `ratings.json`.

## 🗺️ Known limitations / possible next steps

- No repeat-one/repeat-all mode yet; shuffle and drag-to-reorder are covered above.
- Drag-to-reorder only applies within a playlist that has no sort active — reorder while viewing a
  rating/artist/length-sorted list wouldn't map onto a meaningful stored order.
- Cover art is only shown if it's embedded in the file's ID3 tag; there's no external artwork lookup.
- Playlists reference songs by absolute path — an entry silently stops appearing (but isn't removed
  from `playlists.json`) if the file is deleted or its format gets unchecked in Settings.
- Accent-colored buttons use fixed dark text for contrast; a very dark custom accent color could read
  poorly (no automatic contrast calculation yet).
- Windows-only packaging instructions (the JavaFX/Maven parts are cross-platform, but `jpackage` output
  and paths like `%APPDATA%` are Windows-specific).

## 📄 License

This project is licensed under the [MIT License](LICENSE).
