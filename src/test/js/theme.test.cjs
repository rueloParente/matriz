const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { runInNewContext } = require("node:vm");

const script = readFileSync("src/main/resources/static/js/theme.js", "utf8");

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
  assert.equal(p.attributes["aria-pressed"], "true");
  p.systemChange(false);
  assert.equal(p.attributes["aria-pressed"], "false");
  assert.equal(p.dataset.theme, undefined);
  assert.deepEqual(p.writes, []);
});

for (const theme of ["light", "dark"]) {
  test(`saved ${theme} is applied before DOM readiness and overrides the system`, () => {
    const p = page({ stored: theme, dark: theme === "light" });
    assert.equal(p.dataset.theme, theme);
    p.ready();
    assert.equal(p.attributes["aria-pressed"], String(theme === "dark"));
    p.systemChange(true);
    assert.equal(p.attributes["aria-pressed"], String(theme === "dark"));
  });
}

test("toggle persists both directions and restores the choice on navigation", () => {
  const p = page();
  p.ready();
  p.click();
  assert.equal(p.dataset.theme, "dark");
  assert.equal(p.attributes["aria-pressed"], "true");
  assert.deepEqual(p.writes[0], ["jira-clone-theme", "dark"]);
  const next = page({ stored: p.writes[0][1] });
  assert.equal(next.dataset.theme, "dark");
  p.systemChange(false);
  assert.equal(p.attributes["aria-pressed"], "true");
  p.click();
  assert.equal(p.dataset.theme, "light");
  assert.equal(p.attributes["aria-pressed"], "false");
  assert.deepEqual(p.writes[1], ["jira-clone-theme", "light"]);
});

test("unrecognized storage values fall back to system preference", () => {
  const p = page({ stored: "invalid", dark: true });
  p.ready();
  assert.equal(p.dataset.theme, undefined);
  assert.equal(p.attributes["aria-pressed"], "true");
});

test("blocked storage does not prevent switching on the current page", () => {
  const p = page({ blocked: true, dark: true });
  p.ready();
  p.click();
  assert.equal(p.dataset.theme, "light");
  assert.equal(p.attributes["aria-pressed"], "false");
  p.click();
  assert.equal(p.dataset.theme, "dark");
});
