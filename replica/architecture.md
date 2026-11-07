# Architecture: Lembar (Android)

From `replica/recon.md` and `replica/features.csv`. Single user, offline first, no backend in v1.

## Stack

| layer | choice | why |
| --- | --- | --- |
| app | Kotlin + Jetpack Compose (Material 3) | the native Android default; the user asked for native |
| storage | Room (SQLite) on the device | offline by design, transactions, no server to run or pay for |
| state | ViewModel + StateFlow | standard, survives rotation |
| navigation | navigation-compose | five routes, no need for more |
| DI | none (Application holds Repository) | one database, one repository |
| build | Gradle 8.11, AGP 8.7, Kotlin 2.0, KSP | current stable line |
| CI | GitHub Actions builds the debug APK + runs unit tests | the APK is downloadable from every run |
| later: sync | Supabase (Postgres + auth) or similar | only when accounts are added; out of v1 |

## Schema (Room / SQLite)

```sql
create table pages (
  id text primary key,            -- uuid
  parentId text,                  -- null = top level; app-enforced, no cycles (PageTree.subtreeIds)
  title text not null,
  icon text not null,             -- emoji or ''
  isFavourite integer not null,
  inTrash integer not null,
  trashedAt integer,
  createdAt integer not null, updatedAt integer not null, lastOpenedAt integer not null
);
create index on pages(parentId); create index on pages(inTrash); create index on pages(lastOpenedAt);

create table blocks (
  id text primary key,
  pageId text not null references pages(id) on delete cascade,
  position integer not null,
  type text not null,             -- TEXT H1 H2 H3 TODO BULLET NUMBERED QUOTE CALLOUT CODE DIVIDER PAGE
  text text not null,
  checked integer not null,
  childPageId text                -- PAGE blocks link to a nested page
);
create index on blocks(pageId); create index on blocks(childPageId);
```

Access rules: single local user; the database file is private to the app.

Invariants kept by `Repository`:
- a nested page has exactly one PAGE block in its parent while both are active
- trashing removes that link; restoring re-adds it, or moves the page to the top level if the parent is gone
- moving a page into its own subtree is refused
- deleting forever removes the subtree; blocks cascade

Deferred from recon: databases (table/board views), properties, filters. They need
`databases`, `properties`, `prop_values`, `views` tables; planned as v0.2.

## Routes

| route | screen | flows |
| --- | --- | --- |
| home | S02 home (+ S03 sidebar drawer) | F01, F03, F07 |
| page/{id} | S04 editor, S05 picker, S06 block actions, S07 page menu, S08 move, S15 icon | F01-F03, F06 |
| search | S09 | F04 |
| trash | S16 | F06 |
| settings | S17 | F07 |

## Build order (done for v0.1)

1. Room schema + repository, unit-tested pure logic (EditorOps, PageTree, MarkdownExport)
2. Vertical slice: home -> new page -> type blocks -> autosave -> reopen
3. Block types, picker, shortcuts, block actions, undo/redo
4. Sidebar tree, nested pages, move, trash/restore
5. Search, favourites, theme, share-in, Markdown export
