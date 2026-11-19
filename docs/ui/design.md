# UI Design

This document describes the screens of the Jira clone and records the low-fidelity wireframe of its main screen, the **Kanban board**. Behaviour is owned by the specs in [`specs/`](../../specs); the web conventions are in [`architecture.md`](../architecture.md).

**Wireframe:** [`board-wireframe.html`](./board-wireframe.html). Open it in a browser; no server is needed. Cards can be dragged to try the interaction; nothing is saved.

**How it was made:** generated with an AI coding assistant (Claude, in OpenCode) from the specs, then checked against them (section 4).

## 1. Visual direction

| Aspect | Choice |
| --- | --- |
| Toolkit | App: plain CSS (`static/css/app.css`), no design system. Wireframe: Bootstrap 5 from CDN, default theme |
| Wireframe style | Greyscale, dashed boxes, handwritten-style font, yellow notes for behaviour |
| Layout | Global navbar, then a project header with project tabs, then the page content |
| Interaction | Links and forms work as plain HTML; htmx swaps fragments; SortableJS only on the board |
| Text | Plain text only; user content is always escaped |
| Theme | System-aware light/dark palettes using CSS `light-dark()` (modern browsers); shared navbar “Dark mode” toggle with a browser-local preference |

The theme script applies a saved choice before the stylesheet renders; without a
saved choice (or without JavaScript), CSS follows the system color scheme.
The toggle is hidden without JavaScript. This is browser presentation state,
not account or business state; HTMX fragments inherit the page palette.
See [theme requirements](../../specs/theme-spec.md).

The assignee picker reuses the shared select, label, and button styles in both
the issue page and HTMX fragments. Its add controls wrap on narrow screens.

### Board-first workspace

- The signed-in home is **Your boards**: project entry cards with three labeled,
  server-rendered status counts, a direct **Open board** link and a secondary
  **Issue list** link. New users get a create-project call to action.
- **Boards** in the global navigation returns to this overview. Existing project
  routes and redirects are unchanged.
- The board uses the available window width rather than the standard content
  container. A compact project heading and Board / List / Sprints tabs keep
  navigation above the work; New issue is shown only to owners.
- Equal-height lanes have status dots and counts, with visible empty states.
  Cards separate the issue key, linked title, type/priority badges and all assignee
  names. The full key/title region links to the existing issue detail screen.
- On narrow screens, the three lanes scroll horizontally in a labeled,
  keyboard-focusable region. They do not stack vertically. Colors use the shared
  light/dark tokens, and drag animation respects reduced motion.
- Owners see drag guidance; members see a read-only hint and no drag affordance.
  Sprint filtering remains a native GET form with an explicit Filter button.
  Status changes without dragging remain available on issue detail for owners.

## 2. Screens

| Screen | URL | Spec | MR |
| --- | --- | --- | --- |
| Login, register | `/login`, `/register` | AUTH | 1, — |
| Public landing page (guests): feature overview, static sample board, login and sign-up links | `/` | AUTH-22 | — |
| Your boards (accessible projects and status counts) | `/` | ACC | — |
| Projects list (search, paging) and create | `/projects`, `/projects/new` | PRJ, ACC | 2, — |
| Edit or delete project (owner only) | `/projects/{key}/edit`, `/projects/{key}/delete` | ACC | — |
| Issue list (filters, search, paging) | `/projects/{key}/issues` | ISS | 3–4 |
| Create issue | `/projects/{key}/issues/new` | ISS | 3 |
| Issue detail (inline edit, assignees, comments, history, sprint) | `/projects/{key}/issues/{n}` | ISS, ASG, CMT, HST, SPR | 3, 5, 7, 8, 10, — |
| **Kanban board** (wireframed) | `/projects/{key}/board` | BRD, SPR | 6, 10 |
| Sprints | `/projects/{key}/sprints` | SPR | 9 |

Guests see a responsive landing page at `/`, with a feature overview, a clearly labeled static sample board, and ordinary links to login and registration. Signed-in users see their board directory at the same URL. The shared navbar shows login/sign-up links for guests and board/project navigation, the user's name, and CSRF-protected logout for signed-in users. The landing page needs no custom JavaScript.

## 3. Navigation map

```mermaid
flowchart LR
    landing[Public landing page] --> login
    landing --> register[Register]
    register -->|account created| dashboard[Dashboard]
    login -->|no saved destination| dashboard
    login[Login] -->|valid login| projects[Projects list]
    projects -->|New project| newProject[Create project]
    newProject -->|created| issues
    projects -->|open project| issues[Issue list]
    issues <-->|project tabs| board[Kanban board]
    issues <-->|project tabs| sprints[Sprints]
    board <-->|project tabs| sprints
    issues -->|New issue| newIssue[Create issue]
    board -->|+ New issue| newIssue
    newIssue -->|created| detail[Issue detail]
    issues -->|click key| detail
    board -->|click card key| detail
    detail -->|project tabs| issues
    any([Any page]) -->|Projects| projects
    any -->|Log out| login
```

## 4. Board wireframe

### Layout

```text
+--------------------------------------------------------------------+
| [Jira Clone]  Projects                          Alice Smith [Log out]|  1 navbar
+--------------------------------------------------------------------+
| PROJ — Demo project   (Issues) [Board] (Sprints)      [+ New issue]|  2 project header
+--------------------------------------------------------------------+
| Sprint: [Sprint 3 (active) v]                                      |  3 sprint filter
+----------------------+----------------------+----------------------+
| TO DO (2)            | IN PROGRESS (1)      | DONE (0)             |  4 columns
| +------------------+ | +------------------+ |                      |
| | PROJ-3 Write ... | | | PROJ-5 Add ...   | |   (empty column      |
| | STORY LOW  Unass.| | | TASK MEDIUM Alice| |    still shown)      |
| +------------------+ | +------------------+ |                      |
| | PROJ-8 Login ... | |                      |                      |
| | BUG HIGH     Bob | |                      |                      |
| +------------------+ |                      |                      |
+----------------------+----------------------+----------------------+
```

### Validation against the MVP

Every element on the wireframe maps to a spec requirement, and every user-visible board requirement appears on the wireframe.

| # | Element | Behaviour | Requirement |
| --- | --- | --- | --- |
| 1 | Navbar: Projects link | Back to the projects the user can access | PRJ-2, ACC |
| 1 | Navbar: user name, Log out | Logout is a `POST` with CSRF; then the login page | AUTH-5, AUTH-6 |
| 2 | Project tabs Issues, Board, Sprints | Move between the project's screens | ISS-9, BRD-1, SPR-17 |
| 2 | + New issue | Opens the create form | ISS-1 |
| 3 | Sprint selector: All, a sprint, No sprint | Filters the board; invalid values return `422` | SPR-15, SPR-16 |
| 4 | Three fixed columns in order | To Do, In Progress, Done; counts per column | BRD-1 |
| 4 | Empty Done column | Empty columns are still shown | BRD-2 |
| 4 | Card order by number | PROJ-3 above PROJ-8 | BRD-3 |
| 4 | Card content | Key, title, type, priority, assignee or "Unassigned"; key links to detail | Board spec "Card content" note |
| 4 | Drag and drop | Changes status only; a failed move snaps back; the move is in history | BRD-4, BRD-6, BRD-9 |

### Not on the wireframe, on purpose

These are out of scope in the specs, so the wireframe does not show them: search or other filters on the board, swimlanes, WIP limits, manual ordering inside a column, avatars, notifications, live updates from other users, settings or admin menus.

### Findings from the review

| # | Finding | Decision |
| --- | --- | --- |
| F-1 | The card count per column header is not in the board spec | Kept as a presentation detail; no behaviour depends on it. Remove if unwanted |
| F-2 | The sprint selector exists only from MR 10; before that the board shows all issues | The board renders without the selector until then |
| F-3 | "+ New issue" on the board is not in the board spec; it links to the issues create page | Kept as a shortcut to an existing screen; no new behaviour |
| F-4 | Without JavaScript the board still renders and links work, but drag-and-drop does not | Accepted for the MVP; status can still be changed from the issue detail page |
