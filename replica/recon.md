# Recon map: Notion (Android)

Scope: the core workspace loop. Pages made of blocks, pages nested inside pages,
a simple database (table + board views), search, favourites, trash, offline use.
Not in scope for v1: real-time multi-user collaboration, AI, public web publishing.
For: the user's own original Android app (own name, own brand), possibly sold later.
Date: 2026-11-07

> Method note: built from Notion's publicly documented feature set (help center,
> pricing, Play Store listing). Pages were not fetched live in this session, so
> rows marked `verify` should be checked against the source before the build.
> No account, no source code, no private APIs were used. Reference screenshots
> taken later go in `replica/screens/` and never ship.

## Sources

| # | source | URL | notes |
| --- | --- | --- | --- |
| 1 | help center | https://www.notion.com/help | block types, pages, databases, views, sharing, trash |
| 2 | pricing page | https://www.notion.com/pricing | what is gated: page history length, guests, file upload size, AI |
| 3 | changelog / releases | https://www.notion.com/releases | recent mobile changes (offline, home) - verify |
| 4 | Play Store listing | https://play.google.com/store/apps/details?id=notion.id | screenshots of key mobile screens, rating, review volume |
| 5 | public API docs | https://developers.notion.com/reference | data model for pages, blocks, databases, properties |
| 6 | marketing site | https://www.notion.com/product | positioning: "one workspace" for notes, docs, tasks |
| 7 | user's own account | - | optional; only if the user drives it, and terms check first |

Terms check: if the user tests with their own Notion account, Notion's terms
should be read for clauses on building a competing product. Default: public
sources only.

## Core loop

Open the app, open or create a page, write with blocks (text, headings, to-dos,
lists), nest pages, find it again later via search or sidebar.

## Screens

| ID | screen | route / how to reach | purpose | key components | states seen |
| --- | --- | --- | --- | --- | --- |
| S01 | Onboarding / sign-in | first launch | create local workspace (account optional later) | Button, TextField, logo | first run, error |
| S02 | Home | app launch | recent pages, favourites, quick create | TopBar, PageRow, FAB, Section header | empty, filled, loading |
| S03 | Sidebar / page tree | swipe or menu from S02 | browse nested pages | TreeRow (expand/collapse), Add button | empty, filled, deep nesting |
| S04 | Page editor | tap a page | read and edit a page's blocks | Title, Icon/Cover, Block list, Keyboard toolbar | empty page, filled, offline, read-only (trashed) |
| S05 | Block picker ("/" menu or + button) | "/" in S04 or toolbar + | insert a block type | BottomSheet, BlockTypeRow, filter input | full list, filtered, no match |
| S06 | Block actions sheet | long-press a block | delete, duplicate, turn into, move, colour | BottomSheet, ActionRow | default |
| S07 | Page menu | "..." on S04 | favourite, move, duplicate, export, delete, page font/width | BottomSheet, Toggle | default |
| S08 | Move-to picker | S06 or S07 "Move to" | pick a new parent page | Search field, TreeRow | filled, no results |
| S09 | Search | search icon on S02 | full-text search across pages | Search field, ResultRow, recent searches | empty, typing, results, no results |
| S10 | Database - table view | database page | rows with typed properties | Table grid, Column header, Cell editors | empty, filled, horizontal scroll |
| S11 | Database - board view | view switcher on S10 | rows grouped by select property | Columns, Card, drag | empty column, filled |
| S12 | Database row page | tap a row/card | properties + page body | Property list, Block list | empty, filled |
| S13 | Property editor | add/edit column on S10 | name, type, select options | Dialog, TypeRow, OptionChip | default, validation error |
| S14 | Filter / sort sheet | S10/S11 toolbar | filter and sort a view | BottomSheet, Dropdown, Chip | none set, active |
| S15 | Emoji / icon & cover picker | tap icon area on S04 | page icon and cover | Grid, Tabs, Upload | default |
| S16 | Trash | sidebar or settings | restore or delete permanently | ListRow, Restore, Delete | empty, filled |
| S17 | Settings | sidebar | theme (light/dark/system), export, backup, about | ListRow, Toggle | default |
| S18 | Templates gallery | "New page" options | start from a template | Card grid | default (own templates only) |

## Flows

```
F01 Capture a quick note
    S02 -> FAB -> S04 (type title, type body)
    happy path clicks: 1 tap + typing
    edge: app killed mid-typing (must autosave), offline, very long page

F02 Build a structured page with blocks
    S04 -> "/" -> S05 (pick heading / to-do / bullet / code / image) -> S04
    happy path clicks: 2 per block
    edge: "turn into" between types, markdown shortcuts ("# ", "- ", "[] "), undo

F03 Nest a page and find it via the tree
    S04 -> "/" -> S05 "Page" -> new child S04 -> back -> S03 expand parent
    happy path clicks: 4
    edge: 10+ levels deep, moving a parent moves its subtree, cycle prevention

F04 Find something later
    S02 -> S09 -> type -> tap result -> S04
    happy path clicks: 2 + typing
    edge: matches in body not title, trashed pages excluded, no results

F05 Track tasks in a database
    S04 -> "/" -> S05 "Database" -> S10 -> add rows/props (S13) -> switch to S11 -> drag card
    happy path clicks: ~8
    edge: delete a select option in use, empty group, 1000+ rows

F06 Reorganise and recover
    S04 -> long-press block -> S06 "Move to" -> S08 ; or S07 delete -> S16 restore
    happy path clicks: 3-4
    edge: restore when parent is also trashed, permanent delete of a subtree

F07 Favourite and theme
    S07 "Add to favourites" -> appears on S02 ; S17 dark mode
    happy path clicks: 2
```

## Components

| component | variants | states | used on |
| --- | --- | --- | --- |
| Button | primary, secondary, text, danger | default, pressed, disabled, loading | all |
| FAB | create page | default, pressed | S02 |
| TopBar | home, page, search | default, scrolled | all |
| TextField | single-line, search | empty, focused, error | S01, S08, S09, S13 |
| PageRow / TreeRow | with icon, with chevron, nested indent | default, pressed, expanded, collapsed | S02, S03, S08 |
| BlockView | text, h1-h3, to-do, bullet, numbered, toggle, quote, callout, divider, code, image, child page, database | display, editing, selected, dragging | S04, S12 |
| KeyboardToolbar | block insert, indent/outdent, undo/redo, done | default | S04 |
| BottomSheet | list, grid | open, dragging, closed | S05, S06, S07, S14 |
| TableGrid / Cell | text, number, select, multi-select, date, checkbox, url | display, editing, invalid | S10 |
| BoardColumn / Card | - | empty, filled, dragging | S11 |
| Chip | select option, filter | default, selected, coloured | S10, S11, S13, S14 |
| Dialog | confirm, form | open | S13, S16 |
| Snackbar | info, undo | shown | after delete, move |
| EmptyState | per screen | - | S02, S03, S09, S10, S16 |

## Inferred data model

```
Workspace  id, name, icon, created_at
           evidence: S01, sidebar header; confidence: high

Page       id, workspace_id, parent_id (page | database | null), title, icon,
           cover_url, is_favourite, in_trash, trashed_at, sort_order,
           created_at, updated_at
           evidence: S03 tree, S07 menu, S16 trash, API "page object"; confidence: high

Block      id, page_id, parent_block_id (nullable, for toggles / indent),
           type (text|h1|h2|h3|todo|bullet|numbered|toggle|quote|callout|
                 divider|code|image|child_page|child_database),
           content (json: rich text spans with bold/italic/code/link/colour),
           checked (todo), language (code), sort_order, created_at, updated_at
           evidence: S04, S05, API "block object"; confidence: high

Database   id, page_id (the page that holds it), title
           evidence: S10, API "database object"; confidence: high

Property   id, database_id, name, type (title|text|number|select|multi_select|
           date|checkbox|url), options (json for select), sort_order
           evidence: S13; confidence: high

Row        = a Page whose parent is a Database; property values stored as
PropValue  page_id, property_id, value (json)
           evidence: S12 "row is a page", API; confidence: high (storage shape: guess)

View       id, database_id, type (table|board), group_by_property_id,
           filters (json), sorts (json), sort_order
           evidence: S11, S14; confidence: medium

Asset      id, page_id, local_path, mime, size  (images, covers)
           evidence: S15, image block; confidence: medium
```

Relationships: Workspace 1-n Page; Page 1-n Page (children); Page 1-n Block;
Block 1-n Block (nested); Page 1-1 Database (when it holds one);
Database 1-n Property, 1-n View, 1-n Row(Page); Row 1-n PropValue.

## Feature matrix

See `features.csv`. Must: 20, should: 12, could: 8, skip: 6.

## Out of scope (cannot or should not be cloned)

- Notion's name, logo, icon set, illustrations, fonts, template gallery content and help text.
- Their user network, public template marketplace and integrations partners.
- Notion AI (it is their service; an AI feature in the clone would use its own provider - later, optional).
- Real-time multi-user editing in v1 (possible later with a CRDT + sync server; it is an L on its own).
- Importing directly from a Notion account (only via files the user exports, e.g. Markdown - could).

## Size

Screens 18, flows 7, entities 8.
Hard parts:
1. The block editor on Android: a list of editable blocks with focus moving between
   them, Enter/Backspace splitting and merging blocks, rich text spans, drag to reorder.
2. Database views: a horizontally scrolling typed table and a drag-and-drop board.
3. Data durability: autosave on every keystroke, offline-first storage, undo, and
   (later) sync and conflict handling.

Size: **M** for the single-user, offline-first v1 (a few weeks of focused build).
Adding accounts + cloud sync: +M. Real-time collaboration: L.
