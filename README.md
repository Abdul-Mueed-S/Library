# Library (com.evu.library) — v6.5.3

A personal book-cataloguing Android app for tracking a book collection across one or more
physical locations ("libraries").

## Core features

- **Book catalogue** — add, edit, and delete books (title, author, ISBN) stored in a local
  Room (SQLite) database, with list/grid browsing and search.
- **Categories** — organize books into user-defined categories, with a dedicated
  category-detail view listing every book in a category.
- **Favourites** — flag books as favourites and browse them in a filtered/sortable list.
- **Drafts** — save incomplete/in-progress book entries separately before they're finalized.
- **Multiple libraries ("vaults")** — maintain more than one library/collection (e.g. Home,
  Office), each tracked as a separate vault.
- **Location tagging for libraries** — attach a GPS location (or a manually entered one) to
  each library/vault, and get a "Check Location" prompt that detects when you're near a
  saved library and suggests switching to it. Uses `ACCESS_FINE_LOCATION` /
  `ACCESS_COARSE_LOCATION`.
- **Import / Export**
  - Export the catalogue to CSV or JSON via the system file picker (Storage Access
    Framework `CreateDocument`/`OpenDocument`).
  - Import books from a file with a preview/edit screen before committing.
  - Full backup & restore (JSON), including books, categories, and vaults, with the option
    to restore each library's books separately.
- **Duplicate detection** — imported/added books can be flagged as possible duplicates;
  dedicated screens let you compare matches side-by-side and resolve or clear pending
  duplicate flags.
- **Automatic scheduled backups** — a background `WorkManager` job (`BackupWorker`) runs
  periodic backups (daily/weekly/monthly/custom interval) to a user-chosen folder,
  configurable from Settings.
- **Themes** — multiple built-in visual themes (Default, Light, Forest, Ocean, Sunset,
  Monochrome, True Dark, and light variants), selectable from a Themes screen.
- **Settings** — manage backup frequency/folder, run a manual backup, and other app
  preferences.

## Notable permissions

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Auto-detect/tag library locations |
| `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `WAKE_LOCK` | Scheduled background backups via WorkManager |
| `ACCESS_NETWORK_STATE` | Network-state-aware background work scheduling |

## Tech notes
- Min SDK 26, target SDK 34.
- Data layer: Room (`BookDao`, `CategoryDao`, `AppDatabase` with migrations).
- Background work: AndroidX WorkManager for periodic backups.
