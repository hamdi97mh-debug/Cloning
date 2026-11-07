# Lembar

An Android notes and workspace app: pages made of blocks, pages inside pages,
search, favourites and a trash. Everything is stored on the device; it works
fully offline.

Native Android: Kotlin, Jetpack Compose (Material 3), Room. Min Android 8.0 (API 26).
English and Indonesian UI.

## Get the APK

Every push that touches `lembar/` builds the app on GitHub Actions
(`.github/workflows/android.yml`). Open the run under the repo's **Actions** tab
and download the `lembar-debug-apk` artifact, unzip it, and install
`app-debug.apk` on the phone (allow "install unknown apps" for your browser or
file manager).

## Build locally

Needs JDK 17 and the Android SDK (Android Studio installs both).

```bash
cd lembar
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # editor, tree and export logic
```

Or open the `lembar/` folder in Android Studio and press Run.

## What it does (v0.1)

- Home: favourites, recently opened, top-level pages; + button for a new page
- Sidebar: page tree with expand/collapse and "add page inside"
- Editor blocks: text, heading 1-3, to-do, bulleted and numbered lists, quote,
  callout, code, divider, nested page
- Typing: Enter splits a block, Backspace at the start merges or un-styles it,
  `/` opens the block menu, Markdown shortcuts (`# `, `## `, `### `, `- `, `* `,
  `1. `, `[] `, `> `, ```` ``` ````, `---`)
- Block actions: turn into, move up/down, duplicate, delete; undo/redo
- Page: emoji icon, favourite, move to another page, export as Markdown (share),
  move to trash
- Trash: restore (with its sub-pages) or delete forever, empty trash
- Search across titles and page text
- Share text from any app into a new page
- Light, dark or system theme

## Code map

```
app/src/main/java/app/lembar/
  data/        Room entities and DAOs, Repository, pure logic (EditorOps, PageTree, MarkdownExport)
  ui/editor/   EditorViewModel, EditorScreen, BlockRow
  ui/home/     Home screen and sidebar tree
  ui/search, ui/trash, ui/settings
```

Pages are saved 0.5 s after the last edit, when leaving a page, and when the
app goes to the background.
