const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { runInNewContext } = require("node:vm");

const script = readFileSync("src/main/resources/static/js/theme.js", "utf8");

function assertDestination(p, mode) {
  assert.equal(p.button.textContent, mode + " mode");
  assert.equal(p.attributes["aria-label"], "Switch to " + mode.toLowerCase() + " mode");
  assert.equal(p.attributes["aria-pressed"], undefined);
}

function page({ stored, dark = false, blocked = false } = {}) {
  const dataset = {};
  const attributes = {};
  const writes = [];
  const listeners = {};
  const button = {
    hidden: true,
    setAttribute: (key, value) => attributes[key] = value,
    addEventListener: (event, listener) => listeners[event] = listener,
  };
  const system = {
    matches: dark,
    addEventListener: (event, listener) => listeners.system = listener,
  };
  runInNewContext(script, {
    document: {
      documentElement: { dataset },
      addEventListener: (event, listener) => listeners.ready = listener,
      getElementById: () => button,
    },
    window: {
      matchMedia: () => system,
      localStorage: {
        getItem: () => {
          if (blocked) throw new Error("Storage blocked");
          return stored;
        },
        setItem: (key, value) => {
          if (blocked) throw new Error("Storage blocked");
          writes.push([key, value]);
        },
      },
    },
  });
  return {
    dataset, attributes, button, writes,
    ready: () => listeners.ready(),
    click: () => listeners.click(),
    systemChange: (dark) => {
      system.matches = dark;
      listeners.system();
    },
  };
}

test("system preference stays CSS-owned and tracks live changes", () => {
  const p = page({ dark: true });
  assert.equal(p.dataset.theme, undefined);
  assert.equal(p.button.hidden, true);
  p.ready();
  assert.equal(p.button.hidden, false);
  assertDestination(p, "Light");
  p.systemChange(false);
  assertDestination(p, "Dark");
  assert.equal(p.dataset.theme, undefined);
  assert.deepEqual(p.writes, []);
});

for (const theme of ["light", "dark"]) {
  test(`saved ${theme} is applied before DOM readiness and overrides the system`, () => {
    const p = page({ stored: theme, dark: theme === "light" });
    assert.equal(p.dataset.theme, theme);
    p.ready();
    assertDestination(p, theme === "dark" ? "Light" : "Dark");
    p.systemChange(true);
    assertDestination(p, theme === "dark" ? "Light" : "Dark");
  });
}

test("toggle persists both directions and restores the choice on navigation", () => {
  const p = page();
  p.ready();
  p.click();
  assert.equal(p.dataset.theme, "dark");
  assertDestination(p, "Light");
  assert.deepEqual(p.writes[0], ["jira-clone-theme", "dark"]);
  const next = page({ stored: p.writes[0][1] });
  assert.equal(next.dataset.theme, "dark");
  next.ready();
  assertDestination(next, "Light");
  p.systemChange(false);
  assertDestination(p, "Light");
  p.click();
  assert.equal(p.dataset.theme, "light");
  assertDestination(p, "Dark");
  assert.deepEqual(p.writes[1], ["jira-clone-theme", "light"]);
});

test("unrecognized storage values fall back to system preference", () => {
  const p = page({ stored: "invalid", dark: true });
  p.ready();
  assert.equal(p.dataset.theme, undefined);
  assertDestination(p, "Light");
});

test("blocked storage does not prevent switching on the current page", () => {
  const p = page({ blocked: true, dark: true });
  p.ready();
  p.click();
  assert.equal(p.dataset.theme, "light");
  assertDestination(p, "Dark");
  p.click();
  assert.equal(p.dataset.theme, "dark");
  assertDestination(p, "Light");
});
