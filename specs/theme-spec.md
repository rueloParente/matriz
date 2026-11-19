# Theme

## Acceptance criteria

- **THM-1:** All shared application pages and HTMX fragments support light and dark palettes, including forms, tables, cards, alerts, badges, and board states.
- **THM-2:** Without an explicit preference, follow the browser's system color scheme, including changes while a page is open. This also works without JavaScript.
- **THM-3:** With JavaScript enabled, guests and authenticated users can switch themes using a keyboard-accessible button at the far right of the shared navbar, after the account controls. Its visible label names the destination theme (“Light mode” in dark mode, “Dark mode” in light mode), and its accessible label describes the switch action. Labels track saved preferences, clicks, and system theme changes.
- **THM-4:** Remember an explicit choice in this browser across navigation and reloads. Apply it before rendering page content. If storage is unavailable, switching still works on the current page.
- **THM-5:** Theme selection is presentation-only: no new routes, account fields, or changes to authorization, CSRF, navigation, or HTMX contracts.

## Out of scope

Account synchronization, custom palettes, theming documentation wireframes, and a separate settings page.
