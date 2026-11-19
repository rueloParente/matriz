# Theme implementation

1. Define shared light/dark colors and native control schemes (THM-1, THM-2).
2. Add an early preference script and accessible navbar toggle (THM-3–THM-5).
3. Check shared layout/asset integration and preference behavior; run the Maven suite. Exercise browser rendering if browser tooling is available.

Preference regression tests use Node's built-in test runner (no packages required):
`node --test src/test/js/theme.test.cjs`. The Java integration checks run through
the existing `./mvnw verify` command.
